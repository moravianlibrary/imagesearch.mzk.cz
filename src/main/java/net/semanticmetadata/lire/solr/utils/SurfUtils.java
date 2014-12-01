package net.semanticmetadata.lire.solr.utils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.semanticmetadata.lire.DocumentBuilder;
import net.semanticmetadata.lire.imageanalysis.SurfFeature;
import net.semanticmetadata.lire.imageanalysis.bovw.SurfFeatureHistogramBuilder;
import net.semanticmetadata.lire.impl.SurfDocumentBuilder;
import net.semanticmetadata.lire.solr.SurfInterestPoint;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.solr.search.SolrIndexSearcher;

/**
 * Mathias Lux, mathias@juggle.at
 * Date: 29.09.2010
 * Time: 15:44:14
 */
public class SurfUtils {
    
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
    
    public static Document createSurfDocument(BufferedImage image) {
        SurfDocumentBuilder surfDocumentBuilder = new SurfDocumentBuilder();
        return surfDocumentBuilder.createDocument(image, "image");
    }
    
    public static ArrayList<SurfInterestPoint> extractFeature(Document surfDocument, String fieldName, boolean sort) {
        // load interest points from document
        // has to be loaded before getVisualWords (this method delete surf interest points)
        ArrayList<SurfInterestPoint> surfInterestPoints = new ArrayList<SurfInterestPoint>();
        IndexableField[] fields = surfDocument.getFields(fieldName);
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
    
    public static Document extractHash(SurfFeatureHistogramBuilder histogramBuilder, Document surfDocument) throws IOException {
        return histogramBuilder.getVisualWords(surfDocument);
    }
    
    public static TopDocs search(SolrIndexSearcher searcher, Document hash, int numSurfSimImages) throws ParseException, IOException {
        QueryParser qp = new QueryParser("su_ha", new WhitespaceAnalyzer());
        Query query = qp.parse(hash.getValues(DocumentBuilder.FIELD_NAME_SURF_VISUAL_WORDS)[0]);
        return searcher.search(query, numSurfSimImages);
    }
}
