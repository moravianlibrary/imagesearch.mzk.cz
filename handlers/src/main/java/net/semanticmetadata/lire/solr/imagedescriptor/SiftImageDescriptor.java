package net.semanticmetadata.lire.solr.imagedescriptor;

import net.semanticmetadata.lire.clustering.Cluster;
import net.semanticmetadata.lire.imageanalysis.Histogram;
import net.semanticmetadata.lire.imageanalysis.LireFeature;
import net.semanticmetadata.lire.imageanalysis.sift.Extractor;
import net.semanticmetadata.lire.imageanalysis.sift.Feature;
import net.semanticmetadata.lire.impl.SimpleResult;
import net.semanticmetadata.lire.solr.utils.BOVWBuilder;
import net.semanticmetadata.lire.utils.MetricsUtils;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
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
import java.util.*;
import java.util.List;

/**
 * Created by dudae on 24.2.2015.
 */
public class SiftImageDescriptor implements ImageDescriptor, BoVWIndexer {

    public static org.slf4j.Logger log = LoggerFactory.getLogger(SiftImageDescriptor.class);

    public static final String HASH_FIELD_NAME = "si_ha";

    public static final String HISTOGRAM_FIELD_NAME = "si_hi";

    public static final String FEATURE_TYPE = "siftFeature";

    protected Feature featureInstance = new Feature();

    protected Cluster[] clusters = null;

    protected QueryParser qp;

    protected int imageShorterSide = 0;

    protected int imageLongerSide = 0;

    protected int numDocsForVocabulary;

    protected int numClusters;

    protected String clusterFile;

    protected Extractor extractor = new Extractor();

    public SiftImageDescriptor() {
        qp = new QueryParser(HASH_FIELD_NAME, new WhitespaceAnalyzer());
        BooleanQuery.setMaxClauseCount(10000);
    }

    public int getImageShorterSide() {
        return imageShorterSide;
    }

    public void setImageShorterSide(int imageShorterSide) {
        this.imageShorterSide = imageShorterSide;
    }

    public int getImageLongerSide() {
        return imageLongerSide;
    }

    public void setImageLongerSide(int imageLongerSide) {
        this.imageLongerSide = imageLongerSide;
    }

    public int getNumDocsForVocabulary() {
        return numDocsForVocabulary;
    }

    public void setNumDocsForVocabulary(int numDocsForVocabulary) {
        this.numDocsForVocabulary = numDocsForVocabulary;
    }

    public int getNumClusters() {
        return numClusters;
    }

    public void setNumClusters(int numClusters) {
        this.numClusters = numClusters;
    }

    public String getClusterFile() {
        return clusterFile;
    }

    public void setClusterFile(String clusterFile) {
        this.clusterFile = clusterFile;
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

    @Override
    public Map<String, SolrInputField> indexImage(BufferedImage image, boolean createHash) {
        try {
            Map<String, SolrInputField> result = new HashMap<String, SolrInputField>();
            List<Feature> features = extractor.computeSiftFeatures(resizeQueryImage(image));
            SolrInputField field = new SolrInputField(HISTOGRAM_FIELD_NAME);
            for (Feature feature : features) {
                field.addValue(feature.getByteArrayRepresentation(), 1.0F);
            }
            result.put(HISTOGRAM_FIELD_NAME, field);
            if (createHash) {
                if (hasClusters()) {
                    String visualWords = createVisualWords(features);
                    SolrInputField hashField = new SolrInputField(HASH_FIELD_NAME);
                    hashField.setValue(visualWords, 1.0F);
                    result.put(HASH_FIELD_NAME, hashField);
                } else {
                    log.warn("Cluster file does not exist.");
                }
            }

            return result;
        } catch (IOException e) {
            log.error(e.getLocalizedMessage());
        }
        return null;
    }

    @Override
    public Map<String, SolrInputField> indexDocument(Document document) {
        IndexableField[] fields = document.getFields(HISTOGRAM_FIELD_NAME);
        List<Feature> features = new ArrayList<Feature>();
        for (IndexableField field : fields) {
            BytesRef binaryValue = field.binaryValue();
            Feature feature = new Feature();
            feature.setByteArrayRepresentation(binaryValue.bytes, binaryValue.offset, binaryValue.length);
            features.add(feature);
        }
        String visualWords = createVisualWords(features);
        Map<String, SolrInputField> result = new HashMap<String, SolrInputField>();
        SolrInputField field = new SolrInputField(HASH_FIELD_NAME);
        field.setValue(visualWords, 1.0F);
        result.put(HASH_FIELD_NAME, field);
        return result;
    }

    @Override
    public SearchResult search(SolrIndexSearcher searcher, BufferedImage image, int count) throws IOException {
        try {
            BufferedImage resizedImage = resizeQueryImage(image);
            searcher.setSimilarity(new DefaultSimilarity());
            List<Feature> features = extractor.computeSiftFeatures(resizedImage);
            String visualWords = createVisualWords(features);
            log.warn("Visual words: " + visualWords);

            Query query = qp.parse(visualWords);
            TopDocs docs = searcher.search(query, count);

            SearchResult result = new SearchResult();
            result.setOrigin(resizedImage);
            result.setFeature(features);
            result.setFeatureType(FEATURE_TYPE);
            result.setScoreDocs(docs.scoreDocs);
            return result;
        } catch (ParseException ex) {
            log.error(ex.getLocalizedMessage());
        }
        return null;
    }

    @Override
    public RerankResult rerank(SolrIndexSearcher searcher, SearchResult searchResult, int count) throws IOException {
        List<Feature> queryFeatures = null;
        if (FEATURE_TYPE.equals(searchResult.getFeatureType())) {
            queryFeatures = (List<Feature>) searchResult.getFeature();
        }
        if (queryFeatures == null) {
            queryFeatures = extractor.computeSiftFeatures(searchResult.getOrigin());
        }

        RerankResult result = new RerankResult();
        IndexReader reader = searcher.getIndexReader();

        for (ScoreDoc scoreDoc: searchResult.getScoreDocs()) {
            Document doc = reader.document(scoreDoc.doc);
            List<Feature> features = extractFeatures(doc);
            float maxDistance = result.isEmpty()? -1 : result.last().getDistance();
            float distance = getDistance(queryFeatures, features);

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

    private String createVisualWords(List<Feature> features) {
        double[] tmpHist = new double[numClusters];
        Arrays.fill(tmpHist, 0d);
        // find the appropriate cluster for each featureClass:
        for (LireFeature feature : features) {
            tmpHist[BOVWBuilder.clusterForFeature((Histogram) feature, getClusters())]++;
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

    private static List<Feature> extractFeatures(Document doc) {
        List<Feature> result = new ArrayList<Feature>();
        IndexableField[] fields = doc.getFields(HISTOGRAM_FIELD_NAME);
        for (IndexableField field : fields) {
            BytesRef binaryValue = field.binaryValue();
            Feature feature = new Feature();
            feature.setByteArrayRepresentation(binaryValue.bytes, binaryValue.offset, binaryValue.length);
            result.add(feature);
        }
        return  result;
    }

    private static float getDistance(List<Feature> query, List<Feature> image) {
        int match = 0;
        for (int i = 0; i < 10; i++) {
            Feature queryFeat = query.get(i);
            for (int j = 0; j < 20; j++) {
                Feature imageFeat = image.get(j);
                if (MetricsUtils.distL2(queryFeat.location, imageFeat.location) < 10) {
                    match++;
                    break;
                }
            }
        }
        return 1.0F / match;
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

        log.warn("width: " + width);
        log.warn("height: " + height);

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
}
