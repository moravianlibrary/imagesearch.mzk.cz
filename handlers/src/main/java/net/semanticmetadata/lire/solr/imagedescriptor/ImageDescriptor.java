/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.semanticmetadata.lire.solr.imagedescriptor;

import org.apache.lucene.document.Document;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.search.SolrIndexSearcher;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;

/**
 * Interface for search engines.
 * @author Erich Duda
 */
public interface ImageDescriptor {

    Map<String, SolrInputField> indexImage(BufferedImage image, boolean createHash);

    Map<String, SolrInputField> indexDocument(Document document);

    SearchResult search(SolrIndexSearcher searcher, BufferedImage image, int count) throws IOException;
    
    RerankResult rerank(SolrIndexSearcher searcher, SearchResult searchResult, int count)  throws IOException;
    
}
