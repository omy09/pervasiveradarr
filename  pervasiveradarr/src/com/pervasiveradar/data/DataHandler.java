package com.pervasiveradar.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import com.pervasiveradar.Marker;
import com.pervasiveradar.MixContext;
import com.pervasiveradar.MixView;
import com.pervasiveradar.NavigationMarker;
import com.pervasiveradar.POIMarker;
import com.pervasiveradar.SocialMarker;

import android.location.Location;
import android.util.Log;

//This class is taking care about markers
public class DataHandler {
	
	// complete marker list
	private List<Marker> markerList = new ArrayList<Marker>();
	
	public void addMarkers(List<Marker> markers) {

		Log.v(MixView.TAG, "Marker before: "+markerList.size());
		for(Marker ma:markers) {
			if(!markerList.contains(ma))
				markerList.add(ma);
		}
		
		Log.d(MixView.TAG, "Marker count: "+markerList.size());
	}
	
	public void sortMarkerList() {
		Collections.sort(markerList); 
	}
	
	public void updateDistances(Location location) {
		for(Marker ma: markerList) {
			float[] dist=new float[3];
			Location.distanceBetween(ma.getLatitude(), ma.getLongitude(), location.getLatitude(), location.getLongitude(), dist);
			ma.setDistance(dist[0]);
		}
	}
	
	public void updateActivationStatus(MixContext mixContext) {
		
		Hashtable<Class, Integer> map = new Hashtable<Class, Integer>();
				
		for(Marker ma: markerList) {

			Class mClass=ma.getClass();
			map.put(mClass, (map.get(mClass)!=null)?map.get(mClass)+1:1);
			
			boolean belowMax = (map.get(mClass) <= ma.getMaxObjects());
			boolean dataSourceSelected = mixContext.isDataSourceSelected(ma.getDatasource());
			
			ma.setActive((belowMax && dataSourceSelected));
		}
	}
		
	public void onLocationChanged(Location location) {
		for(Marker ma: markerList) {
			ma.update(location);
		}
		updateDistances(location);
		sortMarkerList();
	}
	

	public List getMarkerList() {
		return markerList;
	}
	

	public void setMarkerList(List markerList) {
		this.markerList = markerList;
	}

	public int getMarkerCount() {
		return markerList.size();
	}
	
	public Marker getMarker(int index) {
		return markerList.get(index);
	}
}
