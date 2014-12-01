package net.semanticmetadata.lire.solr.requesthandler;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import javax.imageio.ImageIO;
import net.semanticmetadata.lire.DocumentBuilder;
import net.semanticmetadata.lire.imageanalysis.ColorLayout;
import net.semanticmetadata.lire.impl.SimpleResult;
import net.semanticmetadata.lire.indexing.hashing.BitSampling;
import net.semanticmetadata.lire.solr.SurfInterestPoint;
import net.semanticmetadata.lire.solr.utils.ColorLayoutUtils;
import net.semanticmetadata.lire.solr.utils.PropertiesUtils;
import net.semanticmetadata.lire.solr.utils.QueryImageUtils;
import net.semanticmetadata.lire.solr.utils.SurfUtils;
import net.semanticmetadata.lire.utils.ImageUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiDocValues;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.search.SolrIndexSearcher;

public class IdentityRequestHandler extends RequestHandlerBase {
	
    @Override
    public String getDescription() {
            return "LIRE Request Handler determines if exists indentical image, which is equaled to the queried image.";
    }

    @Override
    public String getSource() {
            return "http://lire-project.net";
    }

    @Override
    public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse res)
                    throws Exception {
        String url = req.getParams().get("url");
        if (url == null || url.isEmpty()) {
                throw new IllegalArgumentException("You have to specify the url parameter.");
        }
        SolrIndexSearcher searcher = req.getSearcher();
        IndexReader reader = searcher.getIndexReader();
        // Init
        BitSampling.readHashFunctions();
        // Read properties
        Properties properties = PropertiesUtils.getProperties(searcher.getCore());
        int numColorLayoutImages = Integer.parseInt(properties.getProperty("numColorLayoutImages"));
        float threshold1 = Float.parseFloat(properties.getProperty("thresholdCLIdentity1"));
        float threshold2 = Float.parseFloat(properties.getProperty("thresholdCLIdentity2"));
        // Load image
        BufferedImage image = ImageIO.read(new URL(url).openStream());
        image = ImageUtils.trimWhiteSpace(image);
        // Use ColorLayout to extract feature from the image.
        ColorLayout clFeature = ColorLayoutUtils.extractFeature(image);
        // Create hashes
        int[] clHash = ColorLayoutUtils.extractHash(clFeature);
        
        // Searching
        // Taking the time of search for statistical purposes.
        long time = System.currentTimeMillis();
        
        TopDocs clSearchResult = ColorLayoutUtils.search(searcher, clHash, numColorLayoutImages);
        
        time = System.currentTimeMillis() - time;
        res.add("RawDocsCount", clSearchResult.scoreDocs.length + "");
        res.add("RawDocsSearchTime", time + "");
        
        // re-rank
        time = System.currentTimeMillis();
        ArrayList<SimpleResult> clReranked = new ArrayList<SimpleResult>();

        BinaryDocValues binaryValues = MultiDocValues.getBinaryValues(reader, "cl_hi"); // ***  #
        for (ScoreDoc scoreDoc : clSearchResult.scoreDocs) {
            // using DocValues to retrieve the field values ...
            BytesRef bytesRef = binaryValues.get(scoreDoc.doc);
            // Create feature
            ColorLayout feature = new ColorLayout();
            feature.setByteArrayRepresentation(bytesRef.bytes, bytesRef.offset, bytesRef.length);
            // compute a distance
            float distance = clFeature.getDistance(feature);
            // compare distance with the threshold
            if (distance < threshold1) {
                clReranked.add(new SimpleResult(distance, searcher.doc(scoreDoc.doc), scoreDoc.doc));
            }
        }
        time = System.currentTimeMillis() - time;
        res.add("ReRankSearchTime", time + "");
        // Surf re-rank
        time = System.currentTimeMillis();

        if (clReranked.size() == 1 && clReranked.get(0).getDistance() < threshold2) {
            res.add("identity", true);
            HashMap<String, String> map = new HashMap<String, String>(2);
            map.put("id", clReranked.get(0).getDocument().get("id"));
            map.put("d", "" + clReranked.get(0).getDistance());
            res.add("doc", map);
        } else if (clReranked.size() >= 1) {
            SimpleResult surfIdentityResult = surfIdentityCheck(image, clReranked, properties);
            if (surfIdentityResult != null) {
                res.add("identity", true);
                HashMap<String, String> map = new HashMap<String, String>(2);
                map.put("id", surfIdentityResult.getDocument().get("id"));
                map.put("d", "" + surfIdentityResult.getDistance());
                res.add("doc", map);
            } else {
                res.add("identity", false);
            }
        } else {
            res.add("identity", false);
        }
        time = System.currentTimeMillis() - time;
        res.add("ReRankSurfTime", time + "");
    }

    private SimpleResult surfIdentityCheck(BufferedImage queryImage, List<SimpleResult> candidates, Properties properties) {
        float threshold = Float.parseFloat(properties.getProperty("thresholdSUIdentity"));
        
        Document query = SurfUtils.createSurfDocument(QueryImageUtils.resizeQueryImage(queryImage, properties));
        ArrayList<SurfInterestPoint> queryPoints = SurfUtils.extractFeature(query, DocumentBuilder.FIELD_NAME_SURF, true);

        Document minDistanceDoc = null;
        int minDistanceDocIndexNumber = 0;
        float minDistance = Float.MAX_VALUE;

        for (SimpleResult candidate : candidates) {
            Document doc = candidate.getDocument();
            // extract interest points from document
            ArrayList<SurfInterestPoint> docPoints = SurfUtils.extractFeature(doc, "su_hi", false);
            
            float distance = SurfUtils.getDistance(docPoints, queryPoints);
            if (distance >= threshold) {
                continue;
            }
            distance *= candidate.getDistance();
            if (distance < minDistance) {
                minDistance = distance;
                minDistanceDoc = doc;
                minDistanceDocIndexNumber = candidate.getIndexNumber();
            }
        }

        if (minDistanceDoc != null) {
            return new SimpleResult(minDistance, minDistanceDoc, minDistanceDocIndexNumber);
        } else {
            return null;
        }
    }
}
