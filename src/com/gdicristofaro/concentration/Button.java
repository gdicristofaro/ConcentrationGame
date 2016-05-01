package com.gdicristofaro.concentration;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public abstract class Button implements Component {
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

	@Override
	public void paint(Graphics2D g, int x, int y, double scale) {
		g.drawImage(getIcon(), x, y, (int) (x + scale * getIcon().getWidth()), (int) (y + scale * getIcon().getHeight()), 
			0, 0, getIcon().getWidth(), getIcon().getHeight(), null);		
	}
}
