package com.gdicristofaro.concentration;

import java.awt.image.BufferedImage;

public class Button {
	private final BufferedImage icon;
	private final Runnable action;
	
	public Button(BufferedImage icon, Runnable action) {
		this.icon = icon;
		this.action = action;
	}
	
	public BufferedImage getIcon() {
		return icon;
	}
	
	public Runnable getAction() {
		return action;
	}
}
