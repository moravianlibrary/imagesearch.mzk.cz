package net.semanticmetadata.lire.solr.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PropertiesUtils {

    public static Properties getProperties(String corePath) {
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream(corePath + "/conf/liresolr.properties"));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PropertiesUtils.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PropertiesUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return prop;
    }
}
