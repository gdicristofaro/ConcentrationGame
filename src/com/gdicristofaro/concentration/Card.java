package com.gdicristofaro.concentration;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;

public class Card {
	public static final int CARD_WIDTH = 400;
	public static final int CARD_HEIGHT = 600;
	public static final int CARD_SPACING = 100;
	private static final int CARD_MARGIN = 25;
	
	private static final int MAX_FONT_SIZE = 200;
	
	// the maximum width, height of any contents in card
	private static final int CONTENTS_WIDTH = CARD_WIDTH - (CARD_MARGIN * 2);
	private static final int CONTENTS_HEIGHT = CARD_HEIGHT - (CARD_MARGIN * 2);
	
	// how far the card comes "up"
	private static final int SHADOW_OFFSET = CARD_WIDTH / 4;
	
	private static final float SHADOW_ALPHA = .5f;
		
	private static final int FLIP_FRAMES = (int) (Concentration.FPS * .5);
	private static final int DISSOLVE_FRAMES = Concentration.FPS;
	
	
	// gets an image of the right dimensions for card
	private static BufferedImage getBaseCard() {
		BufferedImage cardImage = new BufferedImage(CARD_WIDTH, CARD_HEIGHT,  BufferedImage.TYPE_INT_RGB);
		Graphics2D gCard = cardImage.createGraphics();
		gCard.setColor(Color.white);
		gCard.fillRect (0, 0, CARD_WIDTH, CARD_HEIGHT);
		return cardImage;
	}
	
	// generates the back of the card based on the image
	public static BufferedImage cardFromImage(File file) {
		BufferedImage cardImage = getBaseCard();
		Graphics2D gCard = cardImage.createGraphics();
		
		try {
			BufferedImage imageEmbed = ImageIO.read(file);
			int imageHeight = imageEmbed.getHeight();
			int imageWidth = imageEmbed.getWidth();
			
			// if the image will fill more of the width of the card than the height
			if ((imageWidth/CONTENTS_WIDTH) > (imageHeight/CONTENTS_HEIGHT)) {
				// find proportional height
				imageHeight = ((CONTENTS_WIDTH * imageHeight) / imageWidth);
				imageWidth = CONTENTS_WIDTH;
			}
			else {
				// find proportional width
				imageWidth = ((CONTENTS_HEIGHT * imageWidth) / imageHeight);
				imageHeight = CONTENTS_HEIGHT;
			}
			
			// draw image centered in card
			gCard.drawImage(imageEmbed, (CARD_WIDTH - imageWidth) / 2, (CARD_HEIGHT - imageHeight) / 2, 
				imageWidth, imageHeight, null);
		} 
		catch (IOException e) {
			throw new IllegalStateException("Unable to load image at " + file.getAbsolutePath());
		}
		
		return cardImage;
	}

	public static BufferedImage cardFromText(String text) {
		BufferedImage cardImage = getBaseCard();
		Graphics2D gCard = cardImage.createGraphics();
		
		// draw text
		gCard.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		gCard.setColor(Color.black);
		
		// +5 because we decrement in loop
		int fontSize = MAX_FONT_SIZE + 5;
		String[] texts = text.split(" ");
		
		gCard.setFont(new Font("ComicSansMS", Font.BOLD, fontSize));
		FontMetrics fontMetrics = gCard.getFontMetrics();

		// +1 so greater than CONTENTS_WIDTH / HEIGHT for looping
		int maxWidth = 0;
		int height = CONTENTS_HEIGHT + 1;
		
		// scale down font size until text fits
		while (maxWidth > CONTENTS_WIDTH || height > CONTENTS_HEIGHT) {
			maxWidth = 0;
			fontSize -= 5;
			gCard.setFont(new Font("ComicSansMS", Font.BOLD, fontSize));
			fontMetrics = gCard.getFontMetrics();
			
			for (String s : texts) {
				int thisWidth = fontMetrics.stringWidth(s);
				if (thisWidth > maxWidth)
					maxWidth = thisWidth;
			}
			height = ((fontMetrics.getHeight()) * texts.length);
			
		}

		// draw strings to card
		int i = 0;
		for(String s : texts){
			gCard.drawString(s, 
				(CARD_WIDTH - fontMetrics.stringWidth(s)) / 2, 
				(int) (((CARD_HEIGHT - height) / 2) + ((i + 0.6) * fontMetrics.getHeight())));
			
			i++;	  
		}
		
		return cardImage;
	}

	
	// the image of the front of the card
	private final BufferedImage cardFront;
	private final BufferedImage cardBack;
	
	private final Timer timer;
	private final Runnable repaintRequest;
		
	// 0 is face down, 1 is face up
	private float flipPos = 0;
	// 0 is dissappeared, 1 is opaque
	private float dissolveAmt = 1;

	
	
	
	private Card(BufferedImage cardFront, BufferedImage cardBack, Timer timer, Runnable repaintRequest) {
		this.cardFront = cardFront;
		this.cardBack = cardBack;
		this.timer = timer;
		this.repaintRequest = repaintRequest;
	}
	
	public Card(File img, BufferedImage cardBack, Timer timer, Runnable repaintRequest) {
		this(cardFromImage(img), cardBack, timer, repaintRequest);
	}
	
	public Card(String imgText, BufferedImage cardBack, Timer timer, Runnable repaintRequest) {
		this(cardFromText(imgText), cardBack, timer, repaintRequest);
	}
	
	// the card is still in play
	public boolean isVisible() {
		return (dissolveAmt <= 0);
	}
	
	public void setVisible(boolean visible) {
		dissolveAmt = (visible) ? 1 : 0;
	}
	
	public void onFlip(int millisDelay, final Runnable callback) {
		// for all frames, do the flip and repaint
		flipPos = 0;
		timer.schedule(new TimerTask() {
			int framesRemaining = FLIP_FRAMES;
			
			@Override
			public void run() {
				if (framesRemaining <= 0) {
					this.cancel();
					if (callback != null)
						callback.run();
					
					return;
				}
				
				framesRemaining--;
				flipPos = ((float) (FLIP_FRAMES - framesRemaining)) / FLIP_FRAMES;
				repaintRequest.run();
			}
			
		}, millisDelay, (long) (1000 / Concentration.FPS));
	}
	
	public void onFlipBack(int millisDelay, final Runnable callback) {
		// for all frames, do the flip and repaint
		flipPos = 1;
		timer.schedule(new TimerTask() {
			int framesRemaining = FLIP_FRAMES;
			
			@Override
			public void run() {
				if (framesRemaining <= 0) {
					this.cancel();
					if (callback != null)
						callback.run();
					
					return;
				}
					
				
				framesRemaining--;
				flipPos = ((float) framesRemaining) / FLIP_FRAMES;
				repaintRequest.run();
			}
			
		}, millisDelay, (long) (1000 / Concentration.FPS));		
	}
	
	public void onDissolve(int millisDelay, final Runnable callback) {
		dissolveAmt = 1;
		timer.schedule(new TimerTask() {
			int framesRemaining = DISSOLVE_FRAMES;
			
			@Override
			public void run() {
				if (framesRemaining <= 0) {
					this.cancel();
					if (callback != null)
						callback.run();
					
					return;
				}
				
				framesRemaining--;
				// bound appropriately
				dissolveAmt = Math.min(1, Math.max(0, ((float) framesRemaining) / FLIP_FRAMES));
				repaintRequest.run();
			}
			
		}, millisDelay, (long) (1000 / Concentration.FPS));	
	}
	
	private static class PositionData {
		int cardWidth;
		int cardHeight;
		int cardX;
		int cardY;
		int shadowX;
		int shadowY;
	}
	
	private PositionData cache = new PositionData();
	
	// gets position data for the shadow and the card
	private synchronized PositionData getPosition(int baseX, int baseY, double scale) {
		double fullCardWidth = CARD_WIDTH * scale;
		
		// mid way through flipping, it will be 0
		cache.cardWidth = (int) (Math.abs(flipPos - 0.5) * 2 * fullCardWidth);
		
		cache.cardHeight = (int) (Card.CARD_HEIGHT * scale);
		
		// the shadow should be centered based on the width vs. the full width of the card
		cache.shadowX = (int) ((fullCardWidth - cache.cardWidth) * .5) + baseX;
				
		// the y for the shadow will be the same
		cache.shadowY = baseY;
		
		// the card will be offset to simulate being lifted up off the table
		double cardOffset = flipPos * SHADOW_OFFSET * scale;
		
		cache.cardX = (int) (cache.shadowX - cardOffset);
		cache.cardY = (int) (cache.shadowY - cardOffset);
		
		return cache;
	}
	

	/**
	 * draws the card at the specified location
	 * @param g				the graphics to use to draw with
	 * @param baseX			the base x position relative to the graphics
	 * @param baseY			the base y position
	 * @param scale			how much the image should be scaled
	 */
	public void paintCard(Graphics2D g, int baseX, int baseY, double scale) {
		if (dissolveAmt == 0)
			return;
		
		PositionData cache = getPosition(baseX, baseY, scale);
		
		// draw card with dissolve amount
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) dissolveAmt));

		// if flip amount is past halfway, switch to front
		BufferedImage toDraw = (flipPos > 0.5) ? this.cardFront : this.cardBack;
		
		// draw the image
		g.drawImage(toDraw, cache.cardX, cache.cardY, cache.cardX + cache.cardWidth, cache.cardY + cache.cardHeight, 
			0, 0, toDraw.getWidth(), toDraw.getHeight(), null);
		
		// reset alpha
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1));
	}
	
	/**
	 * draws the card at the specified location
	 * @param g				the graphics to use to draw with
	 * @param baseX			the base x position relative to the graphics
	 * @param baseY			the base y position
	 * @param scale			how much the image should be scaled
	 */
	public void paintShadow(Graphics2D g, int baseX, int baseY, double scale) {
		if (dissolveAmt == 0)
			return;
		
		PositionData cache = getPosition(baseX, baseY, scale);
		
		// if there should be a shadow, draw appropriately
		if (flipPos > 0) {
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, SHADOW_ALPHA * dissolveAmt));
			g.fillRect(cache.shadowX, cache.shadowY, cache.cardWidth, cache.cardHeight);
			
			// reset alpha
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1));
		}
	}
}