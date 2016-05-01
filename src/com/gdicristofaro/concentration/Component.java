package com.gdicristofaro.concentration;

import java.awt.Graphics2D;

public interface Component {
	void paint(Graphics2D g, int x, int y, double scale);
}
