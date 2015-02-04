package net.semanticmetadata.lire.solr;

import org.apache.lucene.index.IndexReader;

import net.semanticmetadata.lire.imageanalysis.bovw.SurfFeatureHistogramBuilder;

public class SolrSurfFeatureHistogramBuilder extends SurfFeatureHistogramBuilder {

	public SolrSurfFeatureHistogramBuilder(IndexReader reader) {
        super(reader);
    }

    public SolrSurfFeatureHistogramBuilder(IndexReader reader, int numDocsForVocabulary) {
        super(reader, numDocsForVocabulary);
    }

    public SolrSurfFeatureHistogramBuilder(IndexReader reader, int numDocsForVocabulary, int numClusters) {
        super(reader, numDocsForVocabulary, numClusters);
    }
    
    public void setClusterFile(String path) {
    	clusterFile = path;
    }

}
