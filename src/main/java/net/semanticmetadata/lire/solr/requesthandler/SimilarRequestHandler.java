package net.semanticmetadata.lire.solr.requesthandler;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.TreeSet;

import javax.imageio.ImageIO;
import net.semanticmetadata.lire.DocumentBuilder;

import net.semanticmetadata.lire.imageanalysis.ColorLayout;
import net.semanticmetadata.lire.imageanalysis.SurfFeature;
import net.semanticmetadata.lire.impl.SimpleResult;
import net.semanticmetadata.lire.indexing.hashing.BitSampling;
import net.semanticmetadata.lire.solr.SolrSurfFeatureHistogramBuilder;
import net.semanticmetadata.lire.solr.SurfInterestPoint;
import net.semanticmetadata.lire.solr.utils.ColorLayoutUtils;
import net.semanticmetadata.lire.solr.utils.PropertiesUtils;
import net.semanticmetadata.lire.solr.utils.QueryImageUtils;
import net.semanticmetadata.lire.solr.utils.SurfUtils;
import net.semanticmetadata.lire.utils.ImageUtils;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.search.SolrIndexSearcher;

public class SimilarRequestHandler extends RequestHandlerBase {

    @Override
    public String getDescription() {
            return "LIRE Request Handler finds similar images.";
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
        searcher.setSimilarity(new BM25Similarity());
        IndexReader reader = searcher.getIndexReader();
        // Read properties
        Properties properties = PropertiesUtils.getProperties(searcher.getCore());
        int numColorLayoutImages = Integer.parseInt(properties.getProperty("numColorLayoutImages"));
        int numColorLayoutSimImages = Integer.parseInt(properties.getProperty("numColorLayoutSimImages"));
        int numSurfSimImages = Integer.parseInt(properties.getProperty("numSurfSimImages"));
        int numSimImages = Integer.parseInt(properties.getProperty("numSimilarImages"));
        int numDocsForVocabulary = Integer.parseInt(properties.getProperty("numDocsForVocabulary"));
        int numClusters = Integer.parseInt(properties.getProperty("numClusters"));
        // Init	
        BooleanQuery.setMaxClauseCount(10000);
        BitSampling.readHashFunctions();
        SolrSurfFeatureHistogramBuilder surfHistogramBuilder = new SolrSurfFeatureHistogramBuilder(null, numDocsForVocabulary, numClusters);
        surfHistogramBuilder.setClusterFile(req.getCore().getDataDir() + "/clusters-surf.dat");
        // Load image
        BufferedImage image = ImageIO.read(new URL(url).openStream());
        image = ImageUtils.trimWhiteSpace(image);
        // Extract image information
        // Color Layout
        ColorLayout clFeature = ColorLayoutUtils.extractFeature(image);
        int[] clHash = ColorLayoutUtils.extractHash(clFeature);
        // SURF
        Document surfDocument = SurfUtils.createSurfDocument(QueryImageUtils.resizeQueryImage(image, properties));
        ArrayList<SurfInterestPoint> suFeature = SurfUtils.extractFeature(surfDocument, DocumentBuilder.FIELD_NAME_SURF, true);
        Document suHash = SurfUtils.extractHash(surfHistogramBuilder, surfDocument);

        // Searching
        // Taking the time of search for statistical purposes.
        long time = System.currentTimeMillis();
        // Color Layout
        TopDocs clSearchResult = ColorLayoutUtils.search(searcher, clHash, numColorLayoutImages);
        // SURF
        TopDocs suSearchResult = SurfUtils.search(searcher, suHash, numSurfSimImages);

        time = System.currentTimeMillis() - time;
        res.add("RawDocsCount", clSearchResult.scoreDocs.length + suSearchResult.scoreDocs.length + "");
        res.add("RawDocsSearchTime", time + "");
        // re-rank
        time = System.currentTimeMillis();

        // Re-rank clSearchResult using ColorLayout feature
        TreeSet<SimpleResult> clReranked = ColorLayoutUtils.rerank(searcher, clSearchResult, clFeature, numColorLayoutSimImages);
        TreeSet<SimpleResult> suReranked = new TreeSet<SimpleResult>();
        // Re-rank clReranked using SURF feature and add the nearest images to suReranked
        for (SimpleResult r : clReranked) {
            rerank(suFeature, r.getDocument(), r.getIndexNumber(), suReranked, numSimImages);
        }
        // Re-rank suSearchResult using SURF feature and add the nearest images to suReranked
        for (ScoreDoc scoreDoc : suSearchResult.scoreDocs) {
            Document doc = reader.document(scoreDoc.doc);
            rerank(suFeature, doc, scoreDoc.doc, suReranked, numSimImages);
        }

        time = System.currentTimeMillis() - time;
        res.add("ReRankSearchTime", time + "");

        ArrayList<HashMap<String, String>> result = new ArrayList<HashMap<String, String>>();
        for (SimpleResult r : suReranked) {
            HashMap<String, String> map = new HashMap<String, String>(2);
            map.put("id", r.getDocument().get("id"));
            map.put("d", "" + r.getDistance());
            result.add(map);
        }
        res.add("docs", result);
    }

    private void rerank(ArrayList<SurfInterestPoint> query, Document doc, int indexNumber, TreeSet<SimpleResult> resultScoreDocs, int numSimImages) {
        // load interest points from document
        ArrayList<SurfInterestPoint> docPoints = new ArrayList<SurfInterestPoint>();
        IndexableField[] docFields = doc.getFields("su_hi");
        for (IndexableField docField : docFields) {
            SurfFeature feature = new SurfFeature();
            feature.setByteArrayRepresentation(docField.binaryValue().bytes, docField.binaryValue().offset, docField.binaryValue().length);
            SurfInterestPoint sip = new SurfInterestPoint(feature.getDoubleHistogram());
            docPoints.add(sip);
        }
        
        float maxDistance = resultScoreDocs.isEmpty()? -1 : resultScoreDocs.last().getDistance();
        float distance = SurfUtils.getDistance(docPoints, query);
        if (resultScoreDocs.size() < numSimImages) {
            resultScoreDocs.add(new SimpleResult(distance, doc, indexNumber));
        } else if (distance < maxDistance) {
            // if it is nearer to the sample than at least one of the current set:
            // remove the last one ...
            resultScoreDocs.remove(resultScoreDocs.last());
            // add the new one ...
            resultScoreDocs.add(new SimpleResult(distance, doc, indexNumber));
        }
    }
}
