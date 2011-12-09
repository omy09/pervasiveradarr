
package com.pervasiveradar.render;

/**
 * The Camera class uses the Matrix and MixVector classes to store information
 * about camera properties like the view angle and calculates the coordinates of
 * the projected point
 * 
 */
public class Camera {

	public static final float DEFAULT_VIEW_ANGLE = (float) Math.toRadians(45);

	public int width, height;

	public Matrix transform = new Matrix();
	public MixVector lco = new MixVector();

	float viewAngle;
	float dist;

	public Camera(int width, int height) {
		this(width, height, true);
	}

	public Camera(int width, int height, boolean init) {
		this.width = width;
		this.height = height;

		transform.toIdentity();
		lco.set(0, 0, 0);
	}

	public void setViewAngle(float viewAngle) {
		this.viewAngle = viewAngle;
		this.dist = (this.width / 2) / (float) Math.tan(viewAngle / 2);
	}

	public void setViewAngle(int width, int height, float viewAngle) {
		this.viewAngle = viewAngle;
		this.dist = (width / 2) / (float) Math.tan(viewAngle / 2);
	}

	public void projectPoint(MixVector orgPoint, MixVector prjPoint,
			float addX, float addY) {
		prjPoint.x = dist * orgPoint.x / -orgPoint.z;
		prjPoint.y = dist * orgPoint.y / -orgPoint.z;
		prjPoint.z = orgPoint.z;
		prjPoint.x = prjPoint.x + addX + width / 2;
		prjPoint.y = -prjPoint.y + addY + height / 2;
	}

	@Override
	public String toString() {
		return "CAM(" + width + "," + height + ")";
	}
}
