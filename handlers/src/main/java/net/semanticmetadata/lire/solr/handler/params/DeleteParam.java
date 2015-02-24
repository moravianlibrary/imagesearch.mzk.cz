package net.semanticmetadata.lire.solr.handler.params;

import org.apache.solr.common.SolrException;
import org.json.simple.JSONObject;

/**
 * Created by dudae on 11.2.2015.
 */
public class DeleteParam {

    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void loadFromJSON(JSONObject json) {
        Object id = json.get("id");
        if (id == null || !(id instanceof String)) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "id param must be presented in String representation");
        }
        this.id = (String) id;
    }
}
