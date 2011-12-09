
package com.pervasiveradar;

import java.net.URLDecoder;
import java.text.DecimalFormat;

import com.pervasiveradar.data.DataSource;
import com.pervasiveradar.gui.PaintScreen;
import com.pervasiveradar.gui.ScreenLine;
import com.pervasiveradar.gui.ScreenObj;
import com.pervasiveradar.gui.TextObj;
import com.pervasiveradar.reality.PhysicalPlace;
import com.pervasiveradar.render.Camera;
import com.pervasiveradar.render.MixVector;

import android.location.Location;


abstract public class Marker implements Comparable<Marker> {

	private String ID;
	protected String title;
	private String URL;
	protected PhysicalPlace mGeoLoc;
	// distance from user to mGeoLoc in meters
	private double distance;
	// From which datasource does this marker originate
	protected DataSource.DATASOURCE datasource;
	private boolean active;

	// Draw properties
	protected boolean isVisible;
	public MixVector cMarker = new MixVector();
	protected MixVector signMarker = new MixVector();

	
	// Temp properties
	private MixVector tmpa = new MixVector();
	private MixVector tmpb = new MixVector();
	private MixVector tmpc = new MixVector();
	
	protected MixVector locationVector = new MixVector();
	private MixVector origin = new MixVector(0, 0, 0);
	private MixVector upV = new MixVector(0, 1, 0);
	private ScreenLine pPt = new ScreenLine();

	protected Label txtLab = new Label();
	protected TextObj textBlock;
	
	
	public Marker(String title, double latitude, double longitude, double altitude, String link, DataSource.DATASOURCE datasource) {
		super();

		this.active = false;
		this.title = title;
		this.mGeoLoc = new PhysicalPlace(latitude,longitude,altitude);
		if (link != null && link.length() > 0)
			URL = "webpage:" + URLDecoder.decode(link);
		this.datasource = datasource;
		
		this.ID=datasource+"##"+title; //mGeoLoc.toString();
	}
	
	public String getTitle(){
		return title;
	}

	public String getURL(){
		return URL;
	}

	public double getLatitude() {
		return mGeoLoc.getLatitude();
	}
	
	public double getLongitude() {
		return mGeoLoc.getLongitude();
	}
	
	public double getAltitude() {
		return mGeoLoc.getAltitude();
	}
	
	public MixVector getLocationVector() {
		return locationVector;
	}
	
	
	
	public DataSource.DATASOURCE getDatasource() {
		return datasource;
	}

	public void setDatasource(DataSource.DATASOURCE datasource) {
		this.datasource = datasource;
	}

	private void cCMarker(MixVector originalPoint, Camera viewCam, float addX, float addY) {
		tmpa.set(originalPoint); //1
		tmpc.set(upV); 
		tmpa.add(locationVector); //3 
		tmpc.add(locationVector); //3
		tmpa.sub(viewCam.lco); //4
		tmpc.sub(viewCam.lco); //4
		tmpa.prod(viewCam.transform); //5
		tmpc.prod(viewCam.transform); //5

		viewCam.projectPoint(tmpa, tmpb, addX, addY); //6
		cMarker.set(tmpb); //7
		viewCam.projectPoint(tmpc, tmpb, addX, addY); //6
		signMarker.set(tmpb); //7
	}

	private void calcV(Camera viewCam) {
		isVisible = false;


		if (cMarker.z < -1f) {
			isVisible = true;

			if (MixUtils.pointInside(cMarker.x, cMarker.y, 0, 0,
					viewCam.width, viewCam.height)) {


//				}
			}
		}
	}

	public void update(Location curGPSFix) {
		// An elevation of 0.0 probably means that the elevation of the
		// POI is not known and should be set to the users GPS height
		// Note: this could be improved with calls to 
		// http://www.geonames.org/export/web-services.html#astergdem 
		// to estimate the correct height with DEM models like SRTM, AGDEM or GTOPO30
		if(mGeoLoc.getAltitude()==0.0)
			mGeoLoc.setAltitude(curGPSFix.getAltitude());
		 
		// compute the relative position vector from user position to POI location
		PhysicalPlace.convLocToVec(curGPSFix, mGeoLoc, locationVector);
	}

	public void calcPaint(Camera viewCam, float addX, float addY) {
		cCMarker(origin, viewCam, addX, addY);
		calcV(viewCam);
	}

//	private void calcPaint(Camera viewCam) {
//		cCMarker(origin, viewCam, 0, 0);
//	}

	private boolean isClickValid(float x, float y) {
		float currentAngle = MixUtils.getAngle(cMarker.x, cMarker.y,
				signMarker.x, signMarker.y);

		pPt.x = x - signMarker.x;
		pPt.y = y - signMarker.y;
		pPt.rotate(Math.toRadians(-(currentAngle + 90)));
		pPt.x += txtLab.getX();
		pPt.y += txtLab.getY();

		float objX = txtLab.getX() - txtLab.getWidth() / 2;
		float objY = txtLab.getY() - txtLab.getHeight() / 2;
		float objW = txtLab.getWidth();
		float objH = txtLab.getHeight();

		if (pPt.x > objX && pPt.x < objX + objW && pPt.y > objY
				&& pPt.y < objY + objH) {
			return true;
		} else {
			return false;
		}
	}
	
	public void draw(PaintScreen dw) {
		drawCircle(dw);
		drawTextBlock(dw);
	}

	public void drawCircle(PaintScreen dw) {

		if (isVisible) {
			float maxHeight = Math.round(dw.getHeight() / 10f) + 1;
			dw.setStrokeWidth(maxHeight / 10f);
			dw.setFill(false);
			dw.setColor(DataSource.getColor(datasource));
			
			//draw circle with radius depending on distance
			//0.44 is approx. vertical fov in radians 
			double angle = 2.0*Math.atan2(20,distance);
			double radius = angle/0.44 * maxHeight;
			dw.paintCircle(cMarker.x, cMarker.y, (float)radius);
		}
	}
	
	public void drawTextBlock(PaintScreen dw) {
		//TODO: grandezza cerchi e trasparenza
		float maxHeight = Math.round(dw.getHeight() / 10f) + 1;

		//TODO: change textblock only when distance changes
		String textStr="";

		double d = distance;
		DecimalFormat df = new DecimalFormat("@#");
		if(d<1000.0) {
			textStr = title + " ("+ df.format(d) + "m)";			
		}
		else {
			d=d/1000.0;
			textStr = title + " (" + df.format(d) + "km)";
		}
		
		textBlock = new TextObj(textStr, Math.round(maxHeight / 2f) + 1,
				250, dw);

		if (isVisible) {
			
			dw.setColor(DataSource.getColor(datasource));

			float currentAngle = MixUtils.getAngle(cMarker.x, cMarker.y, signMarker.x, signMarker.y);

			txtLab.prepare(textBlock);

			dw.setStrokeWidth(1f);
			dw.setFill(true);
			dw.paintObj(txtLab, signMarker.x - txtLab.getWidth()
					/ 2, signMarker.y + maxHeight, currentAngle + 90, 1);
		}

	}

	public boolean fClick(float x, float y, MixContext ctx, MixState state) {
		boolean evtHandled = false;

		if (isClickValid(x, y)) {
			evtHandled = state.handleEvent(ctx, URL);
		}
		return evtHandled;
	}

	public double getDistance() {
		return distance;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}
	

	public String getID() {
		return ID;
	}

	public void setID(String iD) {
		ID = iD;
	}

	@Override
	public int compareTo(Marker another) {

		Marker leftPm = this;
		Marker rightPm = another;

		return Double.compare(leftPm.getDistance(), rightPm.getDistance());

	}

	@Override
	public boolean equals (Object marker) {
		return this.ID.equals(((Marker) marker).getID());
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	abstract public int getMaxObjects();
 
}


class Label implements ScreenObj {
	private float x, y;
	private float width, height;
	private ScreenObj obj;

	public void prepare(ScreenObj drawObj) {
		obj = drawObj;
		float w = obj.getWidth();
		float h = obj.getHeight();

		x = w / 2;
		y = 0;

		width = w * 2;
		height = h * 2;
	}

	public void paint(PaintScreen dw) {
		dw.paintObj(obj, x, y, 0, 1);
	}
	
	public float getX() {
		return x;
	}
	
	public float getY() {
		return y;
	}

	public float getWidth() {
		return width;
	}

	public float getHeight() {
		return height;
	}
}