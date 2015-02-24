/*
 * This file is part of the LIRE project: http://www.semanticmetadata.net/lire
 * LIRE is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * LIRE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LIRE; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * We kindly ask you to refer the any or one of the following publications in
 * any publication mentioning or employing Lire:
 *
 * Lux Mathias, Savvas A. Chatzichristofis. Lire: Lucene Image Retrieval –
 * An Extensible Java CBIR Library. In proceedings of the 16th ACM International
 * Conference on Multimedia, pp. 1085-1088, Vancouver, Canada, 2008
 * URL: http://doi.acm.org/10.1145/1459359.1459577
 *
 * Lux Mathias. Content Based Image Retrieval with LIRE. In proceedings of the
 * 19th ACM International Conference on Multimedia, pp. 735-738, Scottsdale,
 * Arizona, USA, 2011
 * URL: http://dl.acm.org/citation.cfm?id=2072432
 *
 * Mathias Lux, Oge Marques. Visual Information Retrieval using Java and LIRE
 * Morgan & Claypool, 2013
 * URL: http://www.morganclaypool.com/doi/abs/10.2200/S00468ED1V01Y201301ICR025
 *
 * Copyright statement:
 * ====================
 * (c) 2002-2013 by Mathias Lux (mathias@juggle.at)
 *  http://www.semanticmetadata.net/lire, http://www.lire-project.net
 *
 * Updated: 11.07.13 11:21
 */
package net.semanticmetadata.lire.solr.utils;

import net.semanticmetadata.lire.clustering.Cluster;
import net.semanticmetadata.lire.clustering.KMeans;
import net.semanticmetadata.lire.clustering.ParallelKMeans;
import net.semanticmetadata.lire.imageanalysis.Histogram;
import net.semanticmetadata.lire.imageanalysis.LireFeature;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.util.Bits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * General class creating bag of visual words vocabularies parallel based on k-means. Works with SIFT, SURF and MSER.
 * Date: 24.09.2008
 * Time: 09:38:53
 * @author Erich Duda
 */
public class BOVWBuilder {

    public static Logger log = LoggerFactory.getLogger(BOVWBuilder.class);

    private Cluster[] clusters = null;
    private DecimalFormat df = (DecimalFormat) NumberFormat.getNumberInstance();

    protected LireFeature lireFeature;
    protected String localFeatureFieldName;
    protected String visualWordsFieldName;
    protected String clusterFile;
    protected boolean useParallelClustering = true;
    // number of documents used to build the vocabulary / clusters.
    protected int numDocsForVocabulary = 500;
    protected int numClusters = 512;
    protected IndexReader reader;


    /**
     * Creates a new instance of the BOVWBuilder using the given reader. TODO: write
     *
     * @param reader               the index reader
     * @param lireFeature          lireFeature used
     */
    public BOVWBuilder(IndexReader reader, LireFeature lireFeature) {
        this.reader = reader;
        this.lireFeature = lireFeature;
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

    public String getLocalFeatureFieldName() {
        return localFeatureFieldName;
    }

    public void setLocalFeatureFieldName(String localFeatureFieldName) {
        this.localFeatureFieldName = localFeatureFieldName;
    }

    public String getVisualWordsFieldName() {
        return visualWordsFieldName;
    }

    public void setVisualWordsFieldName(String visualWordsFieldName) {
        this.visualWordsFieldName = visualWordsFieldName;
    }

    public String getClusterFile() {
        return clusterFile;
    }

    public void setClusterFile(String clusterFile) {
        this.clusterFile = clusterFile;
    }

    /**
     * @throws java.io.IOException
     */
    public void createClusterFile() throws IOException {
        df.setMaximumFractionDigits(3);
        // find the documents for building the vocabulary:
        HashSet<Integer> docIDs = selectVocabularyDocs();
        KMeans k;
        if (useParallelClustering) k = new ParallelKMeans(numClusters);
        else k = new KMeans(numClusters);
        // fill the KMeans object:
        LinkedList<double[]> features = new LinkedList<double[]>();
        // Needed for check whether the document is deleted.
        Bits liveDocs = MultiFields.getLiveDocs(reader);
        for (Iterator<Integer> iterator = docIDs.iterator(); iterator.hasNext(); ) {
            int nextDoc = iterator.next();
            if (reader.hasDeletions() && !liveDocs.get(nextDoc)) continue; // if it is deleted, just ignore it.
            Document d = reader.document(nextDoc);
            features.clear();
            IndexableField[] fields = d.getFields(localFeatureFieldName);
            String file = d.getValues("id")[0];
            for (int j = 0; j < fields.length; j++) {
                LireFeature f = getFeatureInstance();
                f.setByteArrayRepresentation(fields[j].binaryValue().bytes, fields[j].binaryValue().offset, fields[j].binaryValue().length);
                // copy the data over to new array ...
                double[] feat = new double[f.getDoubleHistogram().length];
                System.arraycopy(f.getDoubleHistogram(), 0, feat, 0, feat.length);
                features.add(f.getDoubleHistogram());
            }
            k.addImage(file, features);
        }
        if (k.getFeatureCount() < numClusters) {
            // this cannot work. You need more data points than clusters.
            throw new UnsupportedOperationException("Only " + features.size() + " features found to cluster in " + numClusters + ". Try to use less clusters or more images.");
        }
        // do the clustering:
        k.init();
        double laststress = k.clusteringStep();
        double newStress = k.clusteringStep();

        // critical part: Give the difference in between steps as a constraint for accuracy vs. runtime trade off.
        double threshold = Math.max(20d, (double) k.getFeatureCount() / 1000d);
        int cstep = 3;
        while (Math.abs(newStress - laststress) > threshold && cstep < 12) {
            laststress = newStress;
            newStress = k.clusteringStep();
            cstep++;
        }
        // Serializing clusters to a file on the disk ...
        clusters = k.getClusters();
        log.warn(clusterFile);
        Cluster.writeClusters(clusters, clusterFile);
    }

    /**
     * Find the appropriate cluster for a given featureClass.
     *
     * @param f
     * @param clusters
     * @return the index of the cluster.
     */
    public static int clusterForFeature(Histogram f, Cluster[] clusters) {
        double distance = clusters[0].getDistance(f);
        double tmp;
        int result = 0;
        for (int i = 1; i < clusters.length; i++) {
            tmp = clusters[i].getDistance(f);
            if (tmp < distance) {
                distance = tmp;
                result = i;
            }
        }
        return result;
    }

    public static String arrayToVisualWordString(double[] hist) {
        StringBuilder sb = new StringBuilder(1024);
        for (int i = 0; i < hist.length; i++) {
            int visualWordIndex = (int) hist[i];
            for (int j = 0; j < visualWordIndex; j++) {
                // sb.append('v');
                sb.append(Integer.toHexString(i));
                sb.append(' ');
            }
        }
        return sb.toString();
    }

    private HashSet<Integer> selectVocabularyDocs() throws IOException {
        // need to make sure that this is not running forever ...
        int loopCount = 0;
        float maxDocs = reader.maxDoc();
        int capacity = (int) Math.min(numDocsForVocabulary, maxDocs);
        if (capacity < 0) capacity = (int) (maxDocs / 2);
        HashSet<Integer> result = new HashSet<Integer>(capacity);
        int tmpDocNumber, tmpIndex;
        LinkedList<Integer> docCandidates = new LinkedList<Integer>();
        // three cases:
        //
        // either it's more or the same number as documents
        if (numDocsForVocabulary >= maxDocs) {
            for (int i = 0; i < maxDocs; i++) {
                result.add(i);
            }
            return result;
        } else if (numDocsForVocabulary >= maxDocs - 100) { // or it's slightly less:
            for (int i = 0; i < maxDocs; i++) {
                result.add(i);
            }
            while (result.size() > numDocsForVocabulary) {
                result.remove((int) Math.floor(Math.random() * result.size()));
            }
            return result;
        } else {
            for (int i = 0; i < maxDocs; i++) {
                docCandidates.add(i);
            }
            for (int r = 0; r < capacity; r++) {
                boolean worksFine = false;
                do {
                    tmpIndex = (int) Math.floor(Math.random() * (double) docCandidates.size());
                    tmpDocNumber = docCandidates.get(tmpIndex);
                    docCandidates.remove(tmpIndex);
                    // check if the selected doc number is valid: not null, not deleted and not already chosen.
                    worksFine = (reader.document(tmpDocNumber) != null) && !result.contains(tmpDocNumber);
                } while (!worksFine);
                result.add(tmpDocNumber);
                // need to make sure that this is not running forever ...
                if (loopCount++ > capacity * 100)
                    throw new UnsupportedOperationException("Could not get the documents, maybe there are not enough documents in the index?");
            }
            return result;
        }
    }

    protected LireFeature getFeatureInstance() {
        LireFeature result = null;
        try {
            result =  lireFeature.getClass().newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return result;
    }
}
