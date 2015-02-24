/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.semanticmetadata.lire.solr.imagedescriptor;

import com.stromberglabs.jopensurf.SURFInterestPoint;
import com.stromberglabs.jopensurf.Surf;
import net.semanticmetadata.lire.clustering.Cluster;
import net.semanticmetadata.lire.imageanalysis.Histogram;
import net.semanticmetadata.lire.imageanalysis.SurfFeature;
import net.semanticmetadata.lire.impl.SimpleResult;
import net.semanticmetadata.lire.solr.SurfInterestPoint;
import net.semanticmetadata.lire.solr.utils.BOVWBuilder;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.search.SolrIndexSearcher;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.List;

/**
 * Implemenation of ImageDescriptor using the SURF descriptor.
 * @author Erich Duda
 */
public class SurfImageDescriptor implements ImageDescriptor, BoVWIndexer {

    public static org.slf4j.Logger log = LoggerFactory.getLogger(SurfImageDescriptor.class);

    private SurfFeature featureInstance = new SurfFeature();

    protected Cluster[] clusters = null;
    
    protected QueryParser qp;

    protected int imageShorterSide = 0;
    
    protected int imageLongerSide = 0;
    
    protected int numDocsForVocabulary;
    
    protected int numClusters;
    
    protected String clusterFile;

    public static final String HASH_FIELD_NAME = "su_ha";

    public static final String HISTOGRAM_FIELD_NAME = "su_hi";

    public static final String FEATURE_FIELD_NAME = "featureClass";

    public SurfImageDescriptor() {
        qp = new QueryParser(HASH_FIELD_NAME, new WhitespaceAnalyzer());
        BooleanQuery.setMaxClauseCount(10000);
    }
    
    public void setImageShorterSide(int length) {
        imageShorterSide = length;
    }
    
    public void setImageLongerSide(int length) {
        imageLongerSide = length;
    }

    public void setNumDocsForVocabulary(int numDocsForVocabulary) {
        this.numDocsForVocabulary = numDocsForVocabulary;
    }

    public void setNumClusters(int numClusters) {
        this.numClusters = numClusters;
    }

    public void setClusterFile(String clusterFile) {
        this.clusterFile = clusterFile;
    }

    @Override
    public Map<String, SolrInputField> indexImage(BufferedImage image, boolean createHash) {
        Map<String, SolrInputField> result = new HashMap<String, SolrInputField>();

        Document doc = createHistogram(image);
        IndexableField[] features = doc.getFields(HISTOGRAM_FIELD_NAME);
        SolrInputField field = new SolrInputField(HISTOGRAM_FIELD_NAME);
        for (IndexableField feature : features) {
            BytesRef featureBin = feature.binaryValue();
            field.addValue(ByteBuffer.wrap(featureBin.bytes, featureBin.offset, featureBin.length), 1.0F);
        }
        result.put(HISTOGRAM_FIELD_NAME, field);

        if (createHash) {
            if (hasClusters()) {
                String visualWords = createVisualWords(doc);
                SolrInputField hashField = new SolrInputField(HASH_FIELD_NAME);
                result.put(HASH_FIELD_NAME, hashField);
            } else {
                log.warn("Cluster file does not exist.");
            }
        }

        return result;
    }

    @Override
    public Map<String, SolrInputField> indexDocument(Document document) {
        String visualWords = createVisualWords(document);
        Map<String, SolrInputField> result = new HashMap<String, SolrInputField>();
        SolrInputField field = new SolrInputField(HASH_FIELD_NAME);
        field.setValue(visualWords, 1.0F);
        result.put(HASH_FIELD_NAME, field);
        return result;
    }

    @Override
    public SearchResult search(SolrIndexSearcher searcher, BufferedImage image, int count) throws IOException {
        try {
            searcher.setSimilarity(new DefaultSimilarity());
            Document doc = createHistogram(image);
            String visualWords = createVisualWords(doc);

            Query query = qp.parse(visualWords);
            TopDocs docs = searcher.search(query, count);

            SearchResult result = new SearchResult();
            result.setOrigin(image);
            result.setFeature(doc);
            result.setScoreDocs(docs.scoreDocs);
            return result;
        } catch (ParseException ex) {
            log.error(ex.getLocalizedMessage());
        }
        return null;
    }

    @Override
    public RerankResult rerank(SolrIndexSearcher searcher, SearchResult searchResult, int count) throws IOException {
        Document queryDoc = null;
        if (searchResult.getFeature() instanceof Document) {
            Document doc = (Document) searchResult.getFeature();
            if (featureInstance.getFeatureName().equals(FEATURE_FIELD_NAME)) {
                queryDoc = doc;
            }
        }
        if (queryDoc == null) {
            queryDoc = createHistogram(searchResult.getOrigin());
        }
        
        ArrayList<SurfInterestPoint> queryFeature = extractFeature(queryDoc, true);
        RerankResult result = new RerankResult();
        IndexReader reader = searcher.getIndexReader();
        
        for (ScoreDoc scoreDoc: searchResult.getScoreDocs()) {
            Document doc = reader.document(scoreDoc.doc);
            ArrayList<SurfInterestPoint> docPoints = extractFeature(doc, false);
            float maxDistance = result.isEmpty()? -1 : result.last().getDistance();
            float distance = getDistance(docPoints, queryFeature);
            
            if (result.size() < count) {
                result.add(new SimpleResult(distance, doc, scoreDoc.doc));
            } else if (distance < maxDistance) {
                // if it is nearer to the sample than at least one of the current set:
                // remove the last one ...
                result.remove(result.last());
                // add the new one ...
                result.add(new SimpleResult(distance, doc, scoreDoc.doc));
            }
        }
        
        return result;
    }

    @Override
    public void createClusterFile(SolrIndexSearcher searcher) {
        BOVWBuilder builder = new BOVWBuilder(searcher.getIndexReader(), featureInstance);
        builder.setClusterFile(clusterFile);
        builder.setNumClusters(numClusters);
        builder.setNumDocsForVocabulary(numDocsForVocabulary);
        builder.setLocalFeatureFieldName(HISTOGRAM_FIELD_NAME);
        try {
            builder.createClusterFile();
        } catch (IOException e) {
            new SolrException(SolrException.ErrorCode.UNKNOWN, "IOException");
        }
    }

    private String createVisualWords(Document doc) {
        double[] tmpHist = new double[numClusters];
        Arrays.fill(tmpHist, 0d);
        IndexableField[] fields = doc.getFields(HISTOGRAM_FIELD_NAME);
        // find the appropriate cluster for each featureClass:
        for (int j = 0; j < fields.length; j++) {
            featureInstance.setByteArrayRepresentation(fields[j].binaryValue().bytes, fields[j].binaryValue().offset, fields[j].binaryValue().length);
            tmpHist[BOVWBuilder.clusterForFeature((Histogram) featureInstance, getClusters())]++;
        }
        return BOVWBuilder.arrayToVisualWordString(tmpHist);
    }

    private Cluster[] getClusters() {
        if (clusters == null) {
            try {
                clusters = Cluster.readClusters(clusterFile);
            } catch (IOException e) {
                new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Cluster file not found.");
            }
        }
        return  clusters;
    }

    private boolean hasClusters() {
        File f = new File(clusterFile);
        if (f.isFile()) {
            return true;
        } else {
            return false;
        }
    }

    private Document createHistogram(BufferedImage img) {
        Surf surf = new Surf(resizeQueryImage(img));
        List<SURFInterestPoint> interestPoints = surf.getFreeOrientedInterestPoints();
        Document result = new Document();
        result.add(new StoredField(FEATURE_FIELD_NAME, featureInstance.getFeatureName()));

        for (SURFInterestPoint sip : interestPoints) {
            SurfFeature feature = new SurfFeature(sip);
            result.add(new StoredField(HISTOGRAM_FIELD_NAME, feature.getByteArrayRepresentation()));
        }

        return result;
    }

    private static ArrayList<SurfInterestPoint> extractFeature(Document doc, boolean sort) {        
        ArrayList<SurfInterestPoint> surfInterestPoints = new ArrayList<SurfInterestPoint>();
        IndexableField[] fields = doc.getFields(HISTOGRAM_FIELD_NAME);
        for (IndexableField field : fields) {
            SurfFeature feature = new SurfFeature();
            feature.setByteArrayRepresentation(field.binaryValue().bytes, field.binaryValue().offset, field.binaryValue().length);
            SurfInterestPoint sip = new SurfInterestPoint(feature.getDoubleHistogram());
            surfInterestPoints.add(sip);
        }
        // sort for faster compare
        if (sort) {
            Collections.sort(surfInterestPoints);
        }
        return surfInterestPoints;
    }
    
    private BufferedImage resizeQueryImage(BufferedImage image) {
        int width;
        int height;
        double ratio;
        int imgWidth = image.getWidth();
        int imgHeight = image.getHeight();

        if (imageShorterSide > 0) {
            ratio = computeRatioShorterSide(image, imageShorterSide);
        } else if (imageLongerSide > 0) {
            ratio = computeRatioLongerSide(image, imageLongerSide);
        } else {
            return image;
        }
        
        width = (int) (imgWidth * ratio);
        height = (int) (imgHeight * ratio);
		
    	BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
    	Graphics2D g = resizedImage.createGraphics();
    	g.drawImage(image, 0, 0, width, height, null);
    	g.dispose();
    	g.setComposite(AlphaComposite.Src);
    	g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    	g.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
    	g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
    	return resizedImage;
    }
    
    private static double computeRatioShorterSide(BufferedImage image, int length) {
        int imgWidth = image.getWidth();
        int imgHeight = image.getHeight();

        return imgWidth < imgHeight? 1.0 * length / imgWidth : 1.0 * length / imgHeight;
        
    }
    
    private static double computeRatioLongerSide(BufferedImage image, int length) {
        int imgWidth = image.getWidth();
        int imgHeight = image.getHeight();

        return imgWidth > imgHeight? 1.0 * length / imgWidth : 1.0 * length / imgHeight;
    }
    
    public static float getDistance(List<SurfInterestPoint> points1, List<SurfInterestPoint> points2) {
    	int numberOfPoints = 0;
        
        for ( SurfInterestPoint a : points1 ){
            double smallestDistance = findSmallestDistance(a, points2);

            if ( smallestDistance < 0.15d ){
                ++numberOfPoints;
            }
        }
        if (numberOfPoints == 0) {
            return (float) 1.0;
        }
        return (float) (1.0/numberOfPoints);
    }
    
    private static double findSmallestDistance(SurfInterestPoint e, List<SurfInterestPoint> array) {
        double ed = e.getDistanceFromOne();
        // We find points with similiar distance from one by binary search.
        int start = 0;
        int end = array.size() - 1;

        while ((end - start) > 1) {
            int pivot = start + ((end - start) / 2);

            if (ed < array.get(pivot).getDistanceFromOne()) {
                    end = pivot - 1;
            } else if (ed == array.get(pivot).getDistanceFromOne()) {
                    return (double) 0;
            } else {
                    start = pivot + 1;
            }
        }

        int k = start;
        double smallestDistance = Double.MAX_VALUE;
        // We search neighborhood of the founded point.
        while (k >= 0 && Math.abs(e.getDistanceFromOne() - array.get(k).getDistanceFromOne()) < 0.05) {
            double distance = e.getDistance(array.get(k));
            if (distance < smallestDistance) {
                    smallestDistance = distance;
            }
            --k;
        }
        k = start;
        while (k < array.size() && Math.abs(e.getDistanceFromOne() - array.get(k).getDistanceFromOne()) < 0.05) {
            double distance = e.getDistance(array.get(k));
            if (distance < smallestDistance) {
                    smallestDistance = distance;
            }
            ++k;
        }
        return smallestDistance;
    }
}
