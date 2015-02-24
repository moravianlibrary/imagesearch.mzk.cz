package net.semanticmetadata.lire.solr.utils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Properties;

public class QueryImageUtils {

    public static BufferedImage resizeQueryImage(BufferedImage image, Properties properties) {
        int width;
        int height;
        double ratio;
        int lengthShorter = Integer.parseInt(properties.getProperty("resizeQueryImageShorterSide"));
        int lengthLonger = Integer.parseInt(properties.getProperty("resizeQueryImageLongerSide"));
        int imgWidth = image.getWidth();
        int imgHeight = image.getHeight();

        if (lengthShorter > 0) {
            ratio = computeRatioShorterSide(image, lengthShorter);
        } else if (lengthLonger > 0) {
            ratio = computeRatioLongerSide(image, lengthLonger);
        } else {
            return image;
        }
        
        width = (int) (imgWidth * ratio);
        height = (int) (imgHeight * ratio);
		
    	BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
    	Graphics2D g = resizedImage.createGraphics();
    	g.drawImage(image, 0, 0, width, height, null);
    	g.dispose();
    	g.setComposite(AlphaComposite.Src);
    	g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    	g.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
    	g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
    	return resizedImage;
    }
    
    private static double computeRatioShorterSide(BufferedImage image, int length) {
        int imgWidth = image.getWidth();
        int imgHeight = image.getHeight();

        return imgWidth < imgHeight? 1.0 * length / imgWidth : 1.0 * length / imgHeight;
        
    }
    
    private static double computeRatioLongerSide(BufferedImage image, int length) {
        int imgWidth = image.getWidth();
        int imgHeight = image.getHeight();

        return imgWidth > imgHeight? 1.0 * length / imgWidth : 1.0 * length / imgHeight;
    }
	
}
