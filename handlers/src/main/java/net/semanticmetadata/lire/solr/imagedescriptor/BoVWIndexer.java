package net.semanticmetadata.lire.solr.imagedescriptor;

import org.apache.solr.search.SolrIndexSearcher;

/**
 * Created by dudae on 13.2.2015.
 */
public interface BoVWIndexer {

    void createClusterFile(SolrIndexSearcher searcher);

}
