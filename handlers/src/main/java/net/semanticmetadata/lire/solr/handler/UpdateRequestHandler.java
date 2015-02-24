package net.semanticmetadata.lire.solr.handler;

import net.semanticmetadata.lire.solr.handler.params.AddParam;
import net.semanticmetadata.lire.solr.handler.params.DeleteParam;
import net.semanticmetadata.lire.solr.imagedescriptor.ImageDescriptor;
import net.semanticmetadata.lire.solr.imagedescriptor.ImageDescriptorManager;
import net.semanticmetadata.lire.utils.ImageUtils;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.handler.ContentStreamHandlerBase;
import org.apache.solr.handler.loader.ContentStreamLoader;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.CommitUpdateCommand;
import org.apache.solr.update.DeleteUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;

/**
 * Created by dudae on 10.2.2015.
 */
public class UpdateRequestHandler extends ContentStreamHandlerBase {

    public static Logger log = LoggerFactory.getLogger(UpdateRequestHandler.class);

    ContentStreamLoader instance = new ContentStreamLoader() {
        @Override
        public void load(SolrQueryRequest solrQueryRequest,
                         SolrQueryResponse solrQueryResponse,
                         ContentStream contentStream,
                         UpdateRequestProcessor updateRequestProcessor) throws Exception {
            Reader reader = contentStream.getReader();

            Object jsonArray = JSONValue.parse(reader);
            if (!(jsonArray instanceof JSONArray)) {
                throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Input JSON is not Array.");
            }
            JSONArray reqArray = (JSONArray) jsonArray;
            for (Object jsonItem : reqArray) {
                if (!(jsonItem instanceof JSONObject)) {
                    throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Item is not JSONObject instance");
                }
                JSONObject item = (JSONObject) jsonItem;
                Object status = item.get("status");
                if (status == null) {
                    AddParam addParam = new AddParam();
                    addParam.loadFromJSON(item);
                    handleAdd(addParam, solrQueryRequest, updateRequestProcessor);
                } else if ("deleted".equals(status)) {
                    DeleteParam deleteParam = new DeleteParam();
                    deleteParam.loadFromJSON(item);
                    handleDelete(deleteParam, solrQueryRequest, updateRequestProcessor);
                } else {
                    throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "if status param is presented, its value must be \"deleted\"");
                }
            }
            CommitUpdateCommand cmd = new CommitUpdateCommand(solrQueryRequest, false);
            cmd.waitSearcher = true;
            updateRequestProcessor.processCommit(cmd);
        }

        private void handleAdd(AddParam addParam, SolrQueryRequest req, UpdateRequestProcessor processor) throws IOException {
            String corePath = req.getCore().getCoreDescriptor().getInstanceDir();
            SolrInputDocument inputDocument = new SolrInputDocument();
            BufferedImage image = ImageIO.read(new URL((String) addParam.getUrl()).openStream());
            ImageIO.write(image, "png", new File(corePath + "images", addParam.getId()));
            image = ImageUtils.trimWhiteSpace(image);

            ImageDescriptorManager manager = ImageDescriptorManager.getInstance(corePath);
            for (ImageDescriptor descriptor : manager.getImageDescriptorList()) {
                inputDocument.putAll(descriptor.indexImage(image, addParam.isIndex()));
            }
            inputDocument.addField("id", addParam.getId());
            inputDocument.addField("url", addParam.getUrl());
            inputDocument.addField("rights", addParam.getRights());
            inputDocument.addField("provider", addParam.getProvider());
            inputDocument.addField("provider_link", addParam.getProviderLink());

            AddUpdateCommand cmd = new AddUpdateCommand(req);
            cmd.solrDoc = inputDocument;
            processor.processAdd(cmd);
        }

        private void handleDelete(DeleteParam deleteParam, SolrQueryRequest req, UpdateRequestProcessor processor) {
            String corePath = req.getCore().getCoreDescriptor().getInstanceDir();
            DeleteUpdateCommand cmd = new DeleteUpdateCommand(req);
            cmd.setId(deleteParam.getId());
            try {
                processor.processDelete(cmd);
                Files.deleteIfExists(FileSystems.getDefault().getPath(corePath, "images", deleteParam.getId()));
            } catch (IOException e) {
                throw new SolrException(SolrException.ErrorCode.UNKNOWN, "Error at deleting document: " + cmd.getId());
            }
        }
    };

    @Override
    protected ContentStreamLoader newLoader(SolrQueryRequest solrQueryRequest, UpdateRequestProcessor updateRequestProcessor) {
        return instance;
    }

    @Override
    public String getDescription() {
        return "Index image described by JSON.";
    }

    @Override
    public String getSource() {
        return null;
    }
}
