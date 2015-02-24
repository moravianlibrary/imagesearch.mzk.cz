package net.semanticmetadata.lire.solr.handler.params;

import org.apache.solr.common.SolrException;
import org.json.simple.JSONObject;

/**
 * Created by dudae on 11.2.2015.
 */
public class AddParam {

    private String id;

    private String url;

    private String rights;

    private String provider;

    private String providerLink;

    private boolean index;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getRights() {
        return rights;
    }

    public void setRights(String rights) {
        this.rights = rights;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderLink() {
        return providerLink;
    }

    public void setProviderLink(String providerLink) {
        this.providerLink = providerLink;
    }

    public boolean isIndex() {
        return index;
    }

    public void setIndex(boolean index) {
        this.index = index;
    }

    public void loadFromJSON(JSONObject json) {
        // ID
        Object id = json.get("id");
        if (id == null || !(id instanceof String)) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "id param must be presented in String representation");
        }
        this.id = (String) id;
        // URL
        Object url = json.get("url");
        if (url == null || !(url instanceof String)) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "url param must be presented in String representation");
        }
        this.url = (String) url;
        // RIGHTS
        Object rights = json.get("rights");
        if (rights == null || !(rights instanceof String)) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "rights param must be presented in String representation");
        }
        this.rights = (String) rights;
        // PROVIDER
        Object provider = json.get("provider");
        if (provider == null || !(provider instanceof String)) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "provider param must be presented in String representation");
        }
        this.provider = (String) provider;
        // PROVIDER LINK
        Object providerLink = json.get("provider_link");
        if (providerLink == null || !(providerLink instanceof String)) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "provider_link param must be presented in String representation");
        }
        this.providerLink = (String) providerLink;
        // INDEX
        Object index = json.get("index");
        if (index == null || !(index instanceof Boolean)) {
            this.index = true;
        } else {
            this.index = (Boolean) index;
        }
    }
}
