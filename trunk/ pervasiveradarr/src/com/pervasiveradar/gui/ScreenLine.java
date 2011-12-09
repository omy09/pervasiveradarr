
package com.pervasiveradar.gui;

public class ScreenLine {
	public float x, y;

	public ScreenLine() {
		set(0, 0);
	}

	public ScreenLine(float x, float y) {
		set(x, y);
	}

	public void set(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public void rotate(double t) {
		float xp = (float) Math.cos(t) * x - (float) Math.sin(t) * y;
		float yp = (float) Math.sin(t) * x + (float) Math.cos(t) * y;

		x = xp;
		y = yp;
	}

	public void add(float x, float y) {
		this.x += x;
		this.y += y;
	}
}
