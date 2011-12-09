package com.pervasiveradar;

import com.pervasiveradar.data.DataSource.DATASOURCE;
import com.pervasiveradar.reality.PhysicalPlace;

import android.location.Location;


public class POIMarker extends Marker {
	
	public static final int MAX_OBJECTS=20;

	public POIMarker(String title, double latitude, double longitude,
			double altitude, String URL, DATASOURCE datasource) {
		super(title, latitude, longitude, altitude, URL, datasource);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void update(Location curGPSFix) {
		super.update(curGPSFix);
	}

	@Override
	public int getMaxObjects() {
		return MAX_OBJECTS;
	}

}
