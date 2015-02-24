package net.semanticmetadata.lire.solr.handler;

import net.semanticmetadata.lire.solr.imagedescriptor.BoVWIndexer;
import net.semanticmetadata.lire.solr.imagedescriptor.ImageDescriptorManager;
import org.apache.lucene.index.IndexReader;
import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.search.SolrIndexSearcher;

/**
 * Created by dudae on 13.2.2015.
 */
public class CreateClustersHandler extends RequestHandlerBase {
    @Override
    public void handleRequestBody(SolrQueryRequest solrQueryRequest, SolrQueryResponse solrQueryResponse) throws Exception {
        IndexReader ir = solrQueryRequest.getSearcher().getIndexReader();
        String corePath = solrQueryRequest.getCore().getCoreDescriptor().getInstanceDir();
        SolrIndexSearcher searcher = solrQueryRequest.getSearcher();

        for (BoVWIndexer indexer : ImageDescriptorManager.getInstance(corePath).getBoVWIndexerList()) {
            indexer.createClusterFile(searcher);
        }
    }

    @Override
    public String getDescription() {
        return "Creates cluster files.";
    }

    @Override
    public String getSource() {
        return null;
    }
}
