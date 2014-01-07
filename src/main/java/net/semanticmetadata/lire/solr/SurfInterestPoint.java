package net.semanticmetadata.lire.solr;

public class SurfInterestPoint implements Comparable<SurfInterestPoint> {

	private double[] descriptor;
	private double distanceFromOne;
	private boolean isSetDistanceFromOne = false;
	
	public SurfInterestPoint(double[] descriptor) {
		this.descriptor = descriptor;
	}
	
	public double getDistanceFromOne(){
		
		if (isSetDistanceFromOne) {
			return distanceFromOne;
		}
		
		double sum = 0;
		for ( int i = 0; i < descriptor.length; i++ ){
			double diff = descriptor[i] - 1;
			sum += diff*diff; 
		}
		distanceFromOne = (double)Math.sqrt(sum); 
		return distanceFromOne;
	}
	
	public double getDistance(SurfInterestPoint point){
		double sum = 0;
		if ( point.descriptor == null || descriptor == null ) return Float.MAX_VALUE;
		for ( int i = 0; i < descriptor.length; i++ ){
			double diff = descriptor[i] - point.descriptor[i];
			sum += diff*diff;
		}
		return (double)Math.sqrt(sum);
	}
	
	@Override
	public int compareTo(SurfInterestPoint o) {
		
		double distanceA = getDistanceFromOne();
		double distanceB = o.getDistanceFromOne();
		
		if (distanceA < distanceB) {
			return -1;
		} else if (distanceA == distanceB) {
			return 0;
		} else {
			return 1;
		}
	}
}
