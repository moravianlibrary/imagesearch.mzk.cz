package net.semanticmetadata.lire.solr.utils;

import java.util.Collections;
import java.util.List;

import com.stromberglabs.jopensurf.SURFInterestPoint;

import net.semanticmedatada.lire.solr.lsh.LSHHashTable;
import net.semanticmedatada.lire.solr.lsh.SurfInterestPoint;

/**
 * Mathias Lux, mathias@juggle.at
 * Date: 29.09.2010
 * Time: 15:44:14
 */
public class SurfUtils {
    
    public static float getDistance(List<SurfInterestPoint> points, LSHHashTable hash) {
    	int numberOfPoints = 0;
    	
    	for (SurfInterestPoint p : points) {
    		double smallestDistance = findSmallestDistance(p, hash);
    		if ( smallestDistance < 0.15d ) {
                ++numberOfPoints;
    		}
    	}
    	
    	if (numberOfPoints == 0) {
        	return (float) 1.0;
        }
        return (float) (1.0/numberOfPoints);
    }
    
    private static double findSmallestDistance(SurfInterestPoint p, LSHHashTable hash) {
    	List<SurfInterestPoint> foundPoints = hash.get(p);
    	
    	double smallestDistance = Double.MAX_VALUE;
    	
		for (SurfInterestPoint fp : foundPoints) {
			double distance = p.getDistance(fp);
			if (distance < smallestDistance) {
				smallestDistance = distance;
			}
		}
		
		return smallestDistance;
    }
}
