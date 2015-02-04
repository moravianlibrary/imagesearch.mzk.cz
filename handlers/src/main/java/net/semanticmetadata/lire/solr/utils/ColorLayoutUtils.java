/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.semanticmetadata.lire.solr.utils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import net.semanticmetadata.lire.imageanalysis.ColorLayout;
import net.semanticmetadata.lire.impl.SimpleResult;
import net.semanticmetadata.lire.indexing.hashing.BitSampling;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.MultiDocValues;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.search.SolrIndexSearcher;

/**
 *
 * @author dudae
 */
public class ColorLayoutUtils {
    
    public static ColorLayout extractFeature(BufferedImage image) {
        ColorLayout colorLayout = new ColorLayout();
        colorLayout.extract(image);
        return colorLayout;
    }
    
    public static int[] extractHash(ColorLayout feature) {
        return BitSampling.generateHashes(feature.getDoubleHistogram());
    }
    
    public static TopDocs search(SolrIndexSearcher searcher, int[] hash, int numColorLayoutImages) throws IOException {
        BooleanQuery clQuery = createQuery(hash, "cl_ha", 0.5d);
        return searcher.search(clQuery, numColorLayoutImages);
    }
    
    public static TreeSet<SimpleResult> rerank(SolrIndexSearcher searcher, TopDocs clSearchResult, ColorLayout clFeature, int numColorLayoutSimImages) throws IOException {
        TreeSet<SimpleResult> result = new TreeSet<SimpleResult>();
        float clTmpDistance;
        float maxClDistance = -1;
        
        BinaryDocValues binaryValues = MultiDocValues.getBinaryValues(searcher.getIndexReader(), "cl_hi"); // ***  #
        for (ScoreDoc scoreDoc : clSearchResult.scoreDocs) {
            // using DocValues to retrieve the field values ...
            BytesRef bytesRef = binaryValues.get(scoreDoc.doc);
            ColorLayout feature = new ColorLayout();
            feature.setByteArrayRepresentation(bytesRef.bytes, bytesRef.offset, bytesRef.length);
            // Getting the document from the index.
            // This is the slow step based on the field compression of stored fields.
            clTmpDistance = clFeature.getDistance(feature);
            if (result.size() < numColorLayoutSimImages) {
                result.add(new SimpleResult(clTmpDistance, searcher.doc(scoreDoc.doc), scoreDoc.doc));
                maxClDistance = result.last().getDistance();
            } else if (clTmpDistance < maxClDistance) {
                // if it is nearer to the sample than at least one of the current set:
                // remove the last one ...
                result.remove(result.last());
                // add the new one ...
                result.add(new SimpleResult(clTmpDistance, searcher.doc(scoreDoc.doc), scoreDoc.doc));
                // and set our new distance border ...
                maxClDistance = result.last().getDistance();
            }
        }
        return result;
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
