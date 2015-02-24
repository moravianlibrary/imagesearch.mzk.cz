package net.semanticmetadata.lire.solr.handler;

import net.semanticmetadata.lire.impl.SimpleResult;
import net.semanticmetadata.lire.solr.imagedescriptor.ImageDescriptor;
import net.semanticmetadata.lire.solr.imagedescriptor.ImageDescriptorManager;
import net.semanticmetadata.lire.solr.imagedescriptor.RerankResult;
import net.semanticmetadata.lire.solr.imagedescriptor.SearchResult;
import net.semanticmetadata.lire.utils.ImageUtils;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.search.SolrIndexSearcher;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class SimilarRequestHandler extends RequestHandlerBase {

    public static org.slf4j.Logger log = LoggerFactory.getLogger(SimilarRequestHandler.class);

    @Override
    public String getDescription() {
            return "LIRE Request Handler finds similar images.";
    }

    @Override
    public String getSource() {
            return "http://lire-project.net";
    }

    @Override
    public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse res)
                    throws Exception {
        String url = req.getParams().get("url");
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("You have to specify the url parameter.");
        }
        String searchEngineName = req.getParams().get("searchDescriptor");
        int searchCount = req.getParams().getInt("searchCount", 30);
        String rerankEngineName = req.getParams().get("rerankDescriptor");
        int rerankCount = req.getParams().getInt("rerankCount", 30);
        if (searchEngineName == null || searchEngineName.isEmpty()) {
            throw new IllegalArgumentException("You have to specify the imageDescriptor parameter.");
        }
        if (rerankEngineName == null || rerankEngineName.isEmpty()) {
            throw new IllegalArgumentException("You have to specify the rerankEngineName parameter.");
        }
        String corePath = req.getCore().getCoreDescriptor().getInstanceDir();
        SolrIndexSearcher searcher = req.getSearcher();
        searcher.setSimilarity(new BM25Similarity());
        
        ImageDescriptor searchEngine = ImageDescriptorManager.getInstance(corePath).getImageDescriptor(searchEngineName);
        ImageDescriptor rerankEngine = ImageDescriptorManager.getInstance(corePath).getImageDescriptor(rerankEngineName);

        log.warn("SearchCount: " + searchCount);
        log.warn("RerankCount: " + rerankCount);

        // Load image
        BufferedImage image = ImageIO.read(new URL(url).openStream());
        image = ImageUtils.trimWhiteSpace(image);

        // Searching
        // Taking the time of search for statistical purposes.
        long time = System.currentTimeMillis();
        
        SearchResult searchResult = searchEngine.search(searcher, image, searchCount);
        
        time = System.currentTimeMillis() - time;
        res.add("RawDocsCount", searchResult.getScoreDocs().length + "");
        res.add("RawDocsSearchTime", time + "");
        // re-rank
        time = System.currentTimeMillis();

        RerankResult rerankResult = rerankEngine.rerank(searcher, searchResult, rerankCount);

        time = System.currentTimeMillis() - time;
        res.add("ReRankSearchTime", time + "");

        ArrayList<HashMap<String, String>> result = new ArrayList<HashMap<String, String>>();
        for (SimpleResult r : rerankResult) {
            HashMap<String, String> map = new HashMap<String, String>(2);
            map.put("id", r.getDocument().get("id"));
            map.put("d", "" + r.getDistance());
            result.add(map);
        }
        res.add("docs", result);
    }
}
