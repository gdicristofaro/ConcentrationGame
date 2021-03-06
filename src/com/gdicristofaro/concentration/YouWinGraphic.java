package com.gdicristofaro.concentration;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Timer;
import java.util.TimerTask;

public class YouWinGraphic {
	private static final int SHOW_FRAMES = Concentration.FPS * 2;
	
	private final BufferedImage youWinImg;
	private final Runnable requestRepaint;
	
	private boolean visible = false;
	private int curFrame = 0;

	private Timer timer;
	private TimerTask task;
	
	public YouWinGraphic(BufferedImage youWinImg, Runnable requestRepaint, Timer timer) {
		this.youWinImg = youWinImg;
		this.requestRepaint = requestRepaint;
		this.timer = timer;
	}
	
	public void paint(Graphics2D g, int width, int startPos, int endPos, int centerX) {
		if (!visible)
			return;
		
		// bouncing function
		float showPos = ((float) curFrame) / SHOW_FRAMES;
		float heightFraction = (float) ( 1 - Math.abs(Math.pow((((Math.sqrt(0.2) + Math.sqrt(1.2)) * showPos) - Math.sqrt(1.2)), 2) - 0.2));
		
		int height = youWinImg.getHeight() * width / youWinImg.getWidth();
		int yPos = (int) (startPos * (1 - heightFraction) + heightFraction * (endPos - height / 2));

		int xStart = centerX - (width / 2);
		
		g.drawImage(youWinImg, xStart, yPos, xStart + width, yPos + height, 
			0, 0, youWinImg.getWidth(), youWinImg.getHeight(), null);;
	}

	public synchronized void show(final Runnable callback) {
		task = new TimerTask() {
			public void run() {
				if (curFrame > SHOW_FRAMES) {
					this.cancel();
					task = null;
					callback.run();
				}
					
				
				curFrame++;
				requestRepaint.run();
			}
		};
		visible = true;
		timer.schedule(task, 0, 1000 / Concentration.FPS);
	}
	
	public synchronized void hide() {
		if (task != null)
			task.cancel();
		
		visible = false;
		curFrame = 0;
	}
}
