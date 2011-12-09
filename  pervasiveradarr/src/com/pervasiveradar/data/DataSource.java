

package com.pervasiveradar.data;

import com.pervasiveradar.MixListView;
import com.pervasiveradar.R;


import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;


public class DataSource {
	
	// Datasource and dataformat are not the same. datasource is where the data comes from
	// and dataformat is how the data is formatted. 
	
	public enum DATASOURCE { WIKIPEDIA, BUZZ, TWITTER, OSM, OWNURL};
	public enum DATAFORMAT { WIKIPEDIA, BUZZ, TWITTER, OSM, MIXARE};	

	/** default URL */
	private static final String WIKI_BASE_URL = "http://ws.geonames.org/findNearbyWikipediaJSON";
	private static final String TWITTER_BASE_URL = "http://search.twitter.com/search.json";
	private static final String BUZZ_BASE_URL = "https://www.googleapis.com/buzz/v1/activities/search?alt=json&max-results=20|1|0|true";
	private static final String OSM_BASE_URL =	"http://open.mapquestapi.com/xapi/api/0.6/node[railway=station]";

public static Bitmap twitterIcon;
	public static Bitmap buzzIcon;
	
	public DataSource() {
		
	}
	
	public static void createIcons(Resources res) {
		twitterIcon=BitmapFactory.decodeResource(res, R.drawable.twitter);
		buzzIcon=BitmapFactory.decodeResource(res, R.drawable.buzz);
	}
	
	public static Bitmap getBitmap(DATASOURCE ds) {
		Bitmap bitmap=null;
		switch (ds) {
			case TWITTER: bitmap=twitterIcon; break;
			case BUZZ: bitmap=buzzIcon; break;
		}
		return bitmap;
	}
	
	public static DATAFORMAT dataFormatFromDataSource(DATASOURCE ds) {
		DATAFORMAT ret;
		switch (ds) {
			case WIKIPEDIA: ret=DATAFORMAT.WIKIPEDIA; break;
			case BUZZ: ret=DATAFORMAT.BUZZ; break;
			case TWITTER: ret=DATAFORMAT.TWITTER; break;
			case OSM: ret=DATAFORMAT.OSM; break;
			case OWNURL: ret=DATAFORMAT.MIXARE; break;
			default: ret=DATAFORMAT.MIXARE; break;
		}
		return ret;
	}
	
	public static String createRequestURL(DATASOURCE source, double lat, double lon, double alt, float radius,String locale) {
		String ret="";
		switch(source) {
		
			case WIKIPEDIA: 
				ret= WIKI_BASE_URL + 
				"?lat=" + lat +
				"&lng=" + lon + 
				"&radius="+ radius +
				"&maxRows=50" +
				"&lang=" + locale; 
			break;
			
			case BUZZ: 
				ret= BUZZ_BASE_URL + 
				"&lat=" + lat+
				"&lon=" + lon + 
				"&radius="+ radius*1000;
			break;
			
			case TWITTER: 
				ret = TWITTER_BASE_URL +
				"?geocode=" + lat + "%2C" + lon + "%2C" + 
				Math.max(radius, 1.0) + "km" ;				
			break;
				
			case OSM: 
				ret = OSM_BASE_URL + XMLHandler.getOSMBoundingBox(lat, lon, radius);
			break;
			
			case OWNURL:
				ret = MixListView.customizedURL +  
				"?latitude=" + Double.toString(lat) + 
				"&longitude=" + Double.toString(lon) + 
				"&altitude=" + Double.toString(alt) +
				"&radius=" + Double.toString(radius);
			break;
			
		}
		return ret;
	}
	
	public static int getColor(DATASOURCE datasource) {
		int ret;
		switch(datasource) {
			case BUZZ:		ret=Color.rgb(4, 228, 20); break;
			case TWITTER:	ret=Color.rgb(50, 204, 255); break;
			case OSM:		ret=Color.rgb(255, 168, 0); break;
			case WIKIPEDIA:	ret=Color.RED; break;
			default:		ret=Color.WHITE; break;
		}
		return ret;
	}

}
