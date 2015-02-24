package net.semanticmetadata.lire.solr.handler;

import net.semanticmetadata.lire.solr.imagedescriptor.ImageDescriptor;
import net.semanticmetadata.lire.solr.imagedescriptor.ImageDescriptorManager;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.params.UpdateParams;
import org.apache.solr.common.util.Base64;
import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.CommitUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.apache.solr.update.processor.UpdateRequestProcessorChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by dudae on 13.2.2015.
 */
public class ReIndexBoVWRequestHandler extends RequestHandlerBase {
    public static Logger log = LoggerFactory.getLogger(ReIndexBoVWRequestHandler.class);

    @Override
    public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp) throws Exception {
        String solrCore = req.getCore().getCoreDescriptor().getInstanceDir();
        SolrParams params = req.getParams();
        UpdateRequestProcessorChain processorChain =
                req.getCore().getUpdateProcessingChain(params.get(UpdateParams.UPDATE_CHAIN));
        UpdateRequestProcessor processor = processorChain.createProcessor(req, rsp);

        ImageDescriptorManager imageDescriptorManager = ImageDescriptorManager.getInstance(solrCore);

        IndexReader reader = req.getSearcher().getIndexReader();
        Bits liveDocs = MultiFields.getLiveDocs(reader);
        for (int i = 0; i < reader.maxDoc(); i++) {
            if (reader.hasDeletions() && !liveDocs.get(i)) continue; // if it is deleted, just ignore it.
            SolrInputDocument solrDocument = new SolrInputDocument();
            Document document = reader.document(i);
            for (IndexableField f : document) {
                String name = f.name();
                if ("_version_".equals(name)) {
                    continue;
                }
                String stringValue = f.stringValue();
                BytesRef binaryValue = f.binaryValue();
                if (stringValue != null) {
                    solrDocument.addField(name, stringValue);
                } else {
                    solrDocument.addField(name, Base64.byteArrayToBase64(binaryValue.bytes, binaryValue.offset, binaryValue.length));
                }
            }
            for (ImageDescriptor descriptor : imageDescriptorManager.getImageDescriptorList()) {
                Map<String, SolrInputField> fields = descriptor.indexDocument(document);
                solrDocument.putAll(fields);
            }
            AddUpdateCommand cmd = new AddUpdateCommand(req);
            cmd.solrDoc = solrDocument;
            processor.processAdd(cmd);
        }
        CommitUpdateCommand cmd = new CommitUpdateCommand(req, false);
        processor.processCommit(cmd);
    }

    @Override
    public String getDescription() {
        return "Re-index images using BoVW.";
    }

    @Override
    public String getSource() {
        return null;
    }
}
