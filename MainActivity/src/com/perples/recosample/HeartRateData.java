package com.perples.recosample;

public class HeartRateData {

	private static int hr;
	private static double lat;
	private static double lon;
	
	public static int getter(){
		return hr;
	}
	
	public void setter(int heartrate){
		HeartRateData.hr = heartrate;
	}
	
	/////////////////////////////////////////////////////////////////
	public static double getlat(){
		return lat;
	}
	
	public void setlat (Double lat){
		HeartRateData.lat = lat;
	}
	
	public static double getlon(){
		return lon;
	}
	
	public void setlon (Double lon){
		HeartRateData.lon = lon;
	}
	
}
