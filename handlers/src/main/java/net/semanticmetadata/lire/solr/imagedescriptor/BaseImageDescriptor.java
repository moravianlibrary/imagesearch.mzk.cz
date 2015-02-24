/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.semanticmetadata.lire.solr.imagedescriptor;

import net.semanticmetadata.lire.imageanalysis.LireFeature;
import net.semanticmetadata.lire.impl.GenericDocumentBuilder;
import net.semanticmetadata.lire.impl.SimpleResult;
import net.semanticmetadata.lire.indexing.hashing.BitSampling;
import net.semanticmetadata.lire.utils.SerializationUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.search.SolrIndexSearcher;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Implemenation of ImageDescriptor using the Color layout descriptor.
 * @author Erich Duda
 */
public class BaseImageDescriptor implements ImageDescriptor {

    public static org.slf4j.Logger log = LoggerFactory.getLogger(BaseImageDescriptor.class);

    protected GenericDocumentBuilder documentBuilder;

    protected String histogramFieldName;

    protected String hashFieldName;

    protected Class<? extends LireFeature> featureClass;

    public BaseImageDescriptor(Class<? extends LireFeature> feature) throws IOException {
        this.featureClass = feature;
        BitSampling.readHashFunctions();
        documentBuilder = new GenericDocumentBuilder(feature, true);
    }

    public String getHistogramFieldName() {
        return histogramFieldName;
    }

    public void setHistogramFieldName(String histogramFieldName) {
        this.histogramFieldName = histogramFieldName;
    }

    public String getHashFieldName() {
        return hashFieldName;
    }

    public void setHashFieldName(String hashFieldName) {
        this.hashFieldName = hashFieldName;
    }

    @Override
    public Map<String, SolrInputField> indexImage(BufferedImage image, boolean createHash) {
        Field[] docFields = documentBuilder.createDescriptorFields(image);
        Map<String, SolrInputField> result = new HashMap<String, SolrInputField>();

        if (docFields.length != 2) {
            throw new SolrException(SolrException.ErrorCode.UNKNOWN, "ColorLayout document builder does not return two fields.");
        }

        BytesRef histogram = docFields[0].binaryValue();
        String hash = docFields[1].stringValue();

        SolrInputField histogramField = new SolrInputField(histogramFieldName);
        histogramField.addValue(ByteBuffer.wrap(histogram.bytes, histogram.offset, histogram.length), 1.0F);
        result.put(histogramFieldName, histogramField);

        if (createHash) {
            SolrInputField hashField = new SolrInputField(hashFieldName);
            hashField.addValue(hash, 1.0F);
            result.put(hashFieldName, hashField);
        }

        return  result;
    }

    @Override
    public Map<String, SolrInputField> indexDocument(Document document) {
        BytesRef histogram = document.getBinaryValue(histogramFieldName);
        LireFeature feature = null;
        try {
            feature = featureClass.newInstance();
        } catch (InstantiationException e) {
            log.error(e.getLocalizedMessage());
        } catch (IllegalAccessException e) {
            log.error(e.getLocalizedMessage());
        }
        feature.setByteArrayRepresentation(histogram.bytes, histogram.offset, histogram.length);
        int[] hash = BitSampling.generateHashes(feature.getDoubleHistogram());
        String hashString = SerializationUtils.arrayToString(hash);

        SolrInputField field = new SolrInputField(hashFieldName);
        field.addValue(hashString, 1.0F);

        Map<String, SolrInputField> result = new HashMap<String, SolrInputField>();
        result.put(hashFieldName, field);
        return result;
    }

    public SearchResult search(SolrIndexSearcher searcher, BufferedImage image, int count) throws IOException {
        // Extract featureClass from image.
        LireFeature feature = extractFeature(image);
        // Generate hash
        int[] hash = BitSampling.generateHashes(feature.getDoubleHistogram());
        // Search
        BooleanQuery clQuery = createQuery(hash, hashFieldName, 0.5d);
        TopDocs topDocs = searcher.search(clQuery, count);
        // Return result
        SearchResult result = new SearchResult();
        result.setOrigin(image);
        result.setFeature(feature);
        result.setScoreDocs(topDocs.scoreDocs);
        return result;
    }

    public RerankResult rerank(SolrIndexSearcher searcher, SearchResult searchResult, int count) throws IOException {
        RerankResult result = new RerankResult();
        float distance;
        float maxDistance = -1;
        
        LireFeature reqFeature;

        if (featureClass.isInstance(searchResult.getFeature())) {
            reqFeature = (LireFeature) searchResult.getFeature();
        } else {
            reqFeature = extractFeature(searchResult.getOrigin());
        }

        IndexReader reader = searcher.getIndexReader();
        for (ScoreDoc scoreDoc : searchResult.getScoreDocs()) {
            // using DocValues to retrieve the field values ...
            Document doc = reader.document(scoreDoc.doc);
            BytesRef bytesRef = doc.getBinaryValue(histogramFieldName);
            LireFeature feature = null;
            try {
                feature = featureClass.newInstance();
            } catch (InstantiationException e) {
                log.error(e.getLocalizedMessage());
            } catch (IllegalAccessException e) {
                log.error(e.getLocalizedMessage());
            }
            feature.setByteArrayRepresentation(bytesRef.bytes, bytesRef.offset, bytesRef.length);
            // Getting the document from the index.
            // This is the slow step based on the field compression of stored fields.
            distance = reqFeature.getDistance(feature);
            if (result.size() < count) {
                result.add(new SimpleResult(distance, searcher.doc(scoreDoc.doc), scoreDoc.doc));
                maxDistance = result.last().getDistance();
            } else if (distance < maxDistance) {
                // if it is nearer to the sample than at least one of the current set:
                // remove the last one ...
                result.remove(result.last());
                // add the new one ...
                result.add(new SimpleResult(distance, searcher.doc(scoreDoc.doc), scoreDoc.doc));
                // and set our new distance border ...
                maxDistance = result.last().getDistance();
            }
        }
        return result;
    }
    
    private LireFeature extractFeature(BufferedImage image) {
        LireFeature feature = null;
        try {
            feature = featureClass.newInstance();
        } catch (InstantiationException e) {
            log.error(e.getLocalizedMessage());
        } catch (IllegalAccessException e) {
            log.error(e.getLocalizedMessage());
        }
        feature.extract(image);
        return feature;
    }
    
    private static BooleanQuery createQuery(int[] hashes, String paramField, double size) {
        List<Integer> hList = new ArrayList<Integer>(hashes.length);
        for (int i = 0; i < hashes.length; i++) {
            hList.add(hashes[i]);
        }
        Collections.shuffle(hList);
        BooleanQuery query = new BooleanQuery();
        int numHashes = (int) Math.min(hashes.length, Math.floor(hashes.length * size));
        if (numHashes < 5) numHashes = hashes.length;
        for (int i = 0; i < numHashes; i++) {
            // be aware that the hashFunctionsFileName of the field must match the one you put the hashes in before.
            query.add(new BooleanClause(new TermQuery(new Term(paramField, Integer.toHexString(hashes[i]))), BooleanClause.Occur.SHOULD));
        }
        return query;
    }
}
