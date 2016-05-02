package com.gdicristofaro.concentration;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public abstract class Button {
	public static final int BUTTON_WIDTH = 20;
	public static final int BUTTON_SPACING = 5;
	
	private final Runnable action;
	
	public Button(Runnable action) {
		this.action = action;
	}
	
	public abstract BufferedImage getIcon();
	
	public Runnable getAction() {
		return action;
	}

	public void paint(Graphics2D g, int x, int y) {
		// ignore card scale and use button width
		g.drawImage(getIcon(), x, y, (int) (x + BUTTON_WIDTH), (int) (y + BUTTON_WIDTH), 
			0, 0, getIcon().getWidth(), getIcon().getHeight(), null);		
	}
}
