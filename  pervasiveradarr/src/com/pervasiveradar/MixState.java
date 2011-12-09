
package com.pervasiveradar;

import com.pervasiveradar.render.Matrix;
import com.pervasiveradar.render.MixVector;

public class MixState {

	public static int NOT_STARTED = 0; 
	public static int PROCESSING = 1; 
	public static int READY = 2; 
	public static int DONE = 3; 

	int nextLStatus = MixState.NOT_STARTED;
	String downloadId;

	private float curBearing;
	private float curPitch;

	private boolean detailsView;

	public boolean handleEvent(MixContext ctx, String onPress) {
		if (onPress != null && onPress.startsWith("webpage")) {
			try {
				String webpage = MixUtils.parseAction(onPress);
				this.detailsView = true;
				ctx.loadMixViewWebPage(webpage);
			} catch (Exception ex) {
			}
		} 
		return true;
	}

	public float getCurBearing() {
		return curBearing;
	}

	public float getCurPitch() {
		return curPitch;
	}
	
	public boolean isDetailsView() {
		return detailsView;
	}
	
	public void setDetailsView(boolean detailsView) {
		this.detailsView = detailsView;
	}

	public void calcPitchBearing(Matrix rotationM) {
		MixVector looking = new MixVector();
		rotationM.transpose();
		looking.set(1, 0, 0);
		looking.prod(rotationM);
		this.curBearing = (int) (MixUtils.getAngle(0, 0, looking.x, looking.z)  + 360 ) % 360 ;

		rotationM.transpose();
		looking.set(0, 1, 0);
		looking.prod(rotationM);
		this.curPitch = -MixUtils.getAngle(0, 0, looking.y, looking.z);
	}
}
