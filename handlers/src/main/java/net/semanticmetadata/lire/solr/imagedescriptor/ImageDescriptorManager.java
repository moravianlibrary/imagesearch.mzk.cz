/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.semanticmetadata.lire.solr.imagedescriptor;

import net.semanticmetadata.lire.imageanalysis.*;
import net.semanticmetadata.lire.imageanalysis.joint.JointHistogram;
import net.semanticmetadata.lire.solr.utils.PropertiesUtils;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Erich Duda
 */
public final class ImageDescriptorManager {

    public static org.slf4j.Logger log = LoggerFactory.getLogger(ImageDescriptorManager.class);
    
    private static Map<String, ImageDescriptorManager> instances = new HashMap<String, ImageDescriptorManager>();
    
    private Map<String, ImageDescriptor> engines;
    
    private String corePath;

    private List<String> availImageDescriptors;

    private List<String> availBoVWIndexers;

    private ImageDescriptorManager() {
        engines = new HashMap<String, ImageDescriptor>();
        availImageDescriptors = new ArrayList<String>();
        availImageDescriptors.add("ColorLayout");
        availImageDescriptors.add("CEDD");
        availImageDescriptors.add("AutoColorCorrelogram");
        availImageDescriptors.add("BinaryPatternsPyramid");
        availImageDescriptors.add("SimpleColorHistogram");
        availImageDescriptors.add("EdgeHistogram");
        availImageDescriptors.add("FCTH");
        availImageDescriptors.add("Gabor");
        availImageDescriptors.add("JCD");
        availImageDescriptors.add("JointHistogram");
        availImageDescriptors.add("JpegCoefficientHistogram");
        availImageDescriptors.add("LocalBinaryPatterns");
        availImageDescriptors.add("LuminanceLayout");
        availImageDescriptors.add("OpponentHistogram");
        availImageDescriptors.add("PHOG");
        availImageDescriptors.add("RotationInvariantLocalBinaryPatterns");
        availImageDescriptors.add("ScalableColor");
        availImageDescriptors.add("Tamura");
        availImageDescriptors.add("LuminanceLayout");
        availImageDescriptors.add("Surf");
//        availImageDescriptors.add("Sift");
        availBoVWIndexers = new ArrayList<String>();
        availBoVWIndexers.add("Surf");
//        availBoVWIndexers.add("Sift");
    }


    public static synchronized ImageDescriptorManager getInstance(String corePath) {
        if (!instances.containsKey(corePath)) {
            ImageDescriptorManager instance = new ImageDescriptorManager();
            instance.corePath = corePath;
            instances.put(corePath, instance);
        }
        return instances.get(corePath);
    }
    
    public synchronized ImageDescriptor getImageDescriptor(String name) {
        if (engines.containsKey(name)) {
            ImageDescriptor engine = engines.get(name);
            return engine;
        } else {
            if ("ColorLayout".equals(name)) {
                ImageDescriptor engine = getColorLayoutEngine();
                engines.put(name, engine);
                return engine;
            } else if ("CEDD".equals(name)) {
                ImageDescriptor engine = getCEDDEngine();
                engines.put(name, engine);
                return engine;
            } else if ("AutoColorCorrelogram".equals(name)) {
                ImageDescriptor engine = getAutoColorCorrelogramEngine();
                engines.put(name, engine);
                return engine;
            } else if ("BinaryPatternsPyramid".equals(name)) {
                ImageDescriptor engine = getBinaryPatternsPyramidEngine();
                engines.put(name, engine);
                return engine;
            } else if ("SimpleColorHistogram".equals(name)) {
                ImageDescriptor engine = getSimpleColorHistogramEngine();
                engines.put(name, engine);
                return engine;
            } else if ("EdgeHistogram".equals(name)) {
                ImageDescriptor engine = getEdgeHistogramEngine();
                engines.put(name, engine);
                return engine;
            } else if ("FCTH".equals(name)) {
                ImageDescriptor engine = getFCTHEngine();
                engines.put(name, engine);
                return engine;
            } else if ("Gabor".equals(name)) {
                ImageDescriptor engine = getGaborEngine();
                engines.put(name, engine);
                return engine;
            } else if ("JCD".equals(name)) {
                ImageDescriptor engine = getJCDEngine();
                engines.put(name, engine);
                return engine;
            } else if ("JointHistogram".equals(name)) {
                ImageDescriptor engine = getJointHistogramEngine();
                engines.put(name, engine);
                return engine;
            } else if ("JpegCoefficientHistogram".equals(name)) {
                ImageDescriptor engine = getJpegCoefficientHistogramEngine();
                engines.put(name, engine);
                return engine;
            } else if ("LocalBinaryPatterns".equals(name)) {
                ImageDescriptor engine = getLocalBinaryPatternsEngine();
                engines.put(name, engine);
                return engine;
            } else if ("LuminanceLayout".equals(name)) {
                ImageDescriptor engine = getLuminanceLayoutEngine();
                engines.put(name, engine);
                return engine;
            } else if ("OpponentHistogram".equals(name)) {
                ImageDescriptor engine = getOpponentHistogramEngine();
                engines.put(name, engine);
                return engine;
            } else if ("PHOG".equals(name)) {
                ImageDescriptor engine = getPHOGEngine();
                engines.put(name, engine);
                return engine;
            } else if ("RotationInvariantLocalBinaryPatterns".equals(name)) {
                ImageDescriptor engine = getRotationInvariantLocalBinaryPatternsEngine();
                engines.put(name, engine);
                return engine;
            } else if ("ScalableColor".equals(name)) {
                ImageDescriptor engine = getScalableColorEngine();
                engines.put(name, engine);
                return engine;
            } else if ("Tamura".equals(name)) {
                ImageDescriptor engine = getTamuraEngine();
                engines.put(name, engine);
                return engine;
            } else if ("LuminanceLayout".equals(name)) {
                ImageDescriptor engine = getLuminanceLayoutEngine();
                engines.put(name, engine);
                return engine;
            } else if ("Surf".equals(name)) {
                ImageDescriptor engine = getSurfEngine();
                engines.put(name, engine);
                return engine;
            } else if ("Sift".equals(name)) {
                ImageDescriptor engine = getSiftEngine();
                engines.put(name, engine);
                return engine;
            } else {
                return null;
            }
        }
    }

    public List<ImageDescriptor> getImageDescriptorList() {
        List<ImageDescriptor> result = new ArrayList<ImageDescriptor>();
        for (String name : availImageDescriptors) {
            result.add(getImageDescriptor(name));
        }
        return result;
    }

    public synchronized BoVWIndexer getBoVWIndexer(String name) {
        Object indexer = getImageDescriptor(name);
        if (indexer instanceof BoVWIndexer) {
            return (BoVWIndexer) indexer;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public List<BoVWIndexer> getBoVWIndexerList() {
        List<BoVWIndexer> result = new ArrayList<BoVWIndexer>();
        for (String name : availBoVWIndexers) {
            result.add(getBoVWIndexer(name));
        }
        return result;
    }
    
    private ImageDescriptor getColorLayoutEngine() {
        try {
            BaseImageDescriptor engine = new BaseImageDescriptor(ColorLayout.class);
            engine.setHashFieldName("cl_ha");
            engine.setHistogramFieldName("cl_hi");
            return engine;
        } catch (IOException ex) {
            log.error(ex.getLocalizedMessage());
        }
        return null;
    }

    private ImageDescriptor getCEDDEngine() {
        try {
            BaseImageDescriptor engine = new BaseImageDescriptor(CEDD.class);
            engine.setHashFieldName("cedd_ha");
            engine.setHistogramFieldName("cedd_hi");
            return engine;
        } catch (IOException ex) {
            log.error(ex.getLocalizedMessage());
        }
        return null;
    }

    private ImageDescriptor getAutoColorCorrelogramEngine() {
        try {
            BaseImageDescriptor engine = new BaseImageDescriptor(AutoColorCorrelogram.class);
            engine.setHashFieldName("acc_ha");
            engine.setHistogramFieldName("acc_hi");
            return engine;
        } catch (IOException ex) {
            log.error(ex.getLocalizedMessage());
        }
        return null;
    }

    private ImageDescriptor getBinaryPatternsPyramidEngine() {
        try {
            BaseImageDescriptor engine = new BaseImageDescriptor(BinaryPatternsPyramid.class);
            engine.setHashFieldName("bpp_ha");
            engine.setHistogramFieldName("bpp_hi");
            return engine;
        } catch (IOException ex) {
            log.error(ex.getLocalizedMessage());
        }
        return null;
    }

    private ImageDescriptor getSimpleColorHistogramEngine() {
        try {
            BaseImageDescriptor engine = new BaseImageDescriptor(SimpleColorHistogram.class);
            engine.setHashFieldName("sch_ha");
            engine.setHistogramFieldName("sch_hi");
            return engine;
        } catch (IOException ex) {
            log.error(ex.getLocalizedMessage());
        }
        return null;
    }

    private ImageDescriptor getEdgeHistogramEngine() {
        try {
            BaseImageDescriptor engine = new BaseImageDescriptor(EdgeHistogram.class);
            engine.setHashFieldName("eh_ha");
            engine.setHistogramFieldName("eh_hi");
            return engine;
        } catch (IOException ex) {
            log.error(ex.getLocalizedMessage());
        }
        return null;
    }

    private ImageDescriptor getFCTHEngine() {
        try {
            BaseImageDescriptor engine = new BaseImageDescriptor(FCTH.class);
            engine.setHashFieldName("fcth_ha");
            engine.setHistogramFieldName("fcth_hi");
            return engine;
        } catch (IOException ex) {
            log.error(ex.getLocalizedMessage());
        }
        return null;
    }

    private ImageDescriptor getGaborEngine() {
        try {
            BaseImageDescriptor engine = new BaseImageDescriptor(Gabor.class);
            engine.setHashFieldName("gabor_ha");
            engine.setHistogramFieldName("gabor_hi");
            return engine;
        } catch (IOException ex) {
            log.error(ex.getLocalizedMessage());
        }
        return null;
    }

    private ImageDescriptor getJCDEngine() {
        try {
            BaseImageDescriptor engine = new BaseImageDescriptor(JCD.class);
            engine.setHashFieldName("jcd_ha");
            engine.setHistogramFieldName("jcd_hi");
            return engine;
        } catch (IOException ex) {
            log.error(ex.getLocalizedMessage());
        }
        return null;
    }

    private ImageDescriptor getJointHistogramEngine() {
        try {
            BaseImageDescriptor engine = new BaseImageDescriptor(JointHistogram.class);
            engine.setHashFieldName("jh_ha");
            engine.setHistogramFieldName("jh_hi");
            return engine;
        } catch (IOException ex) {
            log.error(ex.getLocalizedMessage());
        }
        return null;
    }

    private ImageDescriptor getJpegCoefficientHistogramEngine() {
        try {
            BaseImageDescriptor engine = new BaseImageDescriptor(JpegCoefficientHistogram.class);
            engine.setHashFieldName("jch_ha");
            engine.setHistogramFieldName("jch_hi");
            return engine;
        } catch (IOException ex) {
            log.error(ex.getLocalizedMessage());
        }
        return null;
    }

    private ImageDescriptor getLocalBinaryPatternsEngine() {
        try {
            BaseImageDescriptor engine = new BaseImageDescriptor(LocalBinaryPatterns.class);
            engine.setHashFieldName("lbp_ha");
            engine.setHistogramFieldName("lbp_hi");
            return engine;
        } catch (IOException ex) {
            log.error(ex.getLocalizedMessage());
        }
        return null;
    }

    private ImageDescriptor getOpponentHistogramEngine() {
        try {
            BaseImageDescriptor engine = new BaseImageDescriptor(OpponentHistogram.class);
            engine.setHashFieldName("oh_ha");
            engine.setHistogramFieldName("oh_hi");
            return engine;
        } catch (IOException ex) {
            log.error(ex.getLocalizedMessage());
        }
        return null;
    }

    private ImageDescriptor getPHOGEngine() {
        try {
            BaseImageDescriptor engine = new BaseImageDescriptor(PHOG.class);
            engine.setHashFieldName("phog_ha");
            engine.setHistogramFieldName("phog_hi");
            return engine;
        } catch (IOException ex) {
            log.error(ex.getLocalizedMessage());
        }
        return null;
    }

    private ImageDescriptor getRotationInvariantLocalBinaryPatternsEngine() {
        try {
            BaseImageDescriptor engine = new BaseImageDescriptor(RotationInvariantLocalBinaryPatterns.class);
            engine.setHashFieldName("rilb_ha");
            engine.setHistogramFieldName("rilb_hi");
            return engine;
        } catch (IOException ex) {
            log.error(ex.getLocalizedMessage());
        }
        return null;
    }

    private ImageDescriptor getScalableColorEngine() {
        try {
            BaseImageDescriptor engine = new BaseImageDescriptor(ScalableColor.class);
            engine.setHashFieldName("sc_ha");
            engine.setHistogramFieldName("sc_hi");
            return engine;
        } catch (IOException ex) {
            log.error(ex.getLocalizedMessage());
        }
        return null;
    }

    private ImageDescriptor getTamuraEngine() {
        try {
            BaseImageDescriptor engine = new BaseImageDescriptor(Tamura.class);
            engine.setHashFieldName("tamura_ha");
            engine.setHistogramFieldName("tamura_hi");
            return engine;
        } catch (IOException ex) {
            log.error(ex.getLocalizedMessage());
        }
        return null;
    }

    private ImageDescriptor getLuminanceLayoutEngine() {
        try {
            BaseImageDescriptor engine = new BaseImageDescriptor(LuminanceLayout.class);
            engine.setHashFieldName("ll_ha");
            engine.setHistogramFieldName("ll_hi");
            return engine;
        } catch (IOException ex) {
            log.error(ex.getLocalizedMessage());
        }
        return null;
    }
    
    private ImageDescriptor getSurfEngine() {
        Properties props = PropertiesUtils.getProperties(corePath);
        SurfImageDescriptor engine = new SurfImageDescriptor();
        engine.setImageLongerSide(Integer.parseInt(props.getProperty("imageLongerSide", "0")));
        engine.setImageShorterSide(Integer.parseInt(props.getProperty("imageShorterSide", "0")));
        engine.setNumClusters(Integer.parseInt(props.getProperty("surfNumClusters", "1000")));
        engine.setNumDocsForVocabulary(Integer.parseInt(props.getProperty("surfNumDocsForVocabulary", "800")));
        engine.setClusterFile(corePath + "/" + props.getProperty("surfClustersFilePath"));
        return engine;
    }

    private ImageDescriptor getSiftEngine() {
        Properties props = PropertiesUtils.getProperties(corePath);
        SiftImageDescriptor engine = new SiftImageDescriptor();
        engine.setImageLongerSide(Integer.parseInt(props.getProperty("imageLongerSide", "0")));
        engine.setImageShorterSide(Integer.parseInt(props.getProperty("imageShorterSide", "0")));
        engine.setNumClusters(Integer.parseInt(props.getProperty("siftNumClusters", "1000")));
        engine.setNumDocsForVocabulary(Integer.parseInt(props.getProperty("siftNumDocsForVocabulary", "800")));
        engine.setClusterFile(corePath + "/" + props.getProperty("siftClustersFilePath"));
        return engine;
    }
    
}
