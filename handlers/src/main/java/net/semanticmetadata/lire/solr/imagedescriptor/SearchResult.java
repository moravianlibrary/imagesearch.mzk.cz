/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.semanticmetadata.lire.solr.imagedescriptor;

import org.apache.lucene.search.ScoreDoc;

import java.awt.image.BufferedImage;

/**
 * Result returned by ImageDescriptor.search method
 * @author Erich Duda
 */
public class SearchResult {
    
    private BufferedImage origin;
    
    private Object feature;

    private String featureType;
    
    private ScoreDoc[] scoreDocs;

    public BufferedImage getOrigin() {
        return origin;
    }

    public void setOrigin(BufferedImage origin) {
        this.origin = origin;
    }

    public Object getFeature() {
        return feature;
    }

    public void setFeature(Object feature) {
        this.feature = feature;
    }

    public String getFeatureType() {
        return featureType;
    }

    public void setFeatureType(String featureType) {
        this.featureType = featureType;
    }

    public ScoreDoc[] getScoreDocs() {
        return scoreDocs;
    }

    public void setScoreDocs(ScoreDoc[] scoreDocs) {
        this.scoreDocs = scoreDocs;
    }
    
}
