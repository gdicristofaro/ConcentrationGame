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

		int maxWidth = CONTENTS_WIDTH + 1;
		int height = CONTENTS_HEIGHT + 1;
		
		// scale down font size until text fits
		while (maxWidth > CONTENTS_WIDTH || height > CONTENTS_HEIGHT) {
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
	
	private boolean isShowing = false;
	private boolean isVisible = true;
	
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
	
	
	// the front of the card is showing
	public boolean isShowing() {
		return isShowing;
	}

	// the card is still in play
	public boolean isVisible() {
		return isVisible;
	}
	
	public void onFlip(int millisDelay, final Runnable callback) {
		// don't flip if can't
		if (!isShowing)
			return;
		
		// for all frames, do the flip and repaint
		isShowing = false;
		flipPos = 0;
		timer.schedule(new TimerTask() {
			int framesRemaining = FLIP_FRAMES;
			
			@Override
			public void run() {
				if (framesRemaining < 0) {
					this.cancel();
					if (callback != null)
						callback.run();
				}
				
				framesRemaining--;
				flipPos = ((float) (FLIP_FRAMES - framesRemaining)) / FLIP_FRAMES;
				repaintRequest.run();
			}
			
		}, millisDelay, (long) (1000 / Concentration.FPS));
	}
	
	public void onFlipBack(int millisDelay, final Runnable callback) {
		// don't flip if can't
		if (isShowing)
			return;
		
		// for all frames, do the flip and repaint
		isShowing = true;
		flipPos = 1;
		timer.schedule(new TimerTask() {
			int framesRemaining = FLIP_FRAMES;
			
			@Override
			public void run() {
				if (framesRemaining < 0) {
					this.cancel();
					if (callback != null)
						callback.run();
				}
					
				
				framesRemaining--;
				flipPos = ((float) framesRemaining) / FLIP_FRAMES;
				repaintRequest.run();
			}
			
		}, millisDelay, (long) (1000 / Concentration.FPS));		
	}
	
	public void onDissolve(int millisDelay, final Runnable callback) {
		// don't dissolve if already done
		if (!isVisible)
			return;
		
		// for all frames, do the flip and repaint
		isVisible = false;
		dissolveAmt = 1;
		timer.schedule(new TimerTask() {
			int framesRemaining = DISSOLVE_FRAMES;
			
			@Override
			public void run() {
				if (framesRemaining < 0) {
					this.cancel();
					if (callback != null)
						callback.run();
				}
					
				
				framesRemaining--;
				dissolveAmt = ((float) framesRemaining) / FLIP_FRAMES;
				repaintRequest.run();
			}
			
		}, millisDelay, (long) (1000 / Concentration.FPS));	
	}
	

	/**
	 * draws the card at the specified location
	 * @param g				the graphics to use to draw with
	 * @param baseX			the base x position relative to the graphics
	 * @param baseY			the base y position
	 * @param scale			how much the image should be scaled
	 */
	public void draw(Graphics2D g, int baseX, int baseY, float scale) {
		if (dissolveAmt == 0)
			return;
		
		int cardYOffset = (int) (-flipPos * SHADOW_OFFSET * scale);

		// mid way through flipping, it will be 0
		int cardWidth = (int) ((Math.abs(flipPos - 0.5) + 0.5) * CARD_WIDTH * scale);
		
		// center the card width
		int shadowXOffset = (int) (cardWidth *.5 * scale);

		// offset for the shadow and lifting the card "up"
		int cardXOffset = (int) ((-flipPos * SHADOW_OFFSET + shadowXOffset) * scale); 

		// if flip amount is past halfway, switch to front
		BufferedImage toDraw = (flipPos > 0.5) ? this.cardFront : this.cardBack;
		
		int cardW = (int) (toDraw.getWidth() * scale);
		int cardH = (int) (toDraw.getHeight() * scale);
		
		// the location to draw each corner of the image
		int cardX1 = baseX + cardXOffset;
		int cardX2 = cardX1 + cardWidth;
		int cardY1 = baseY + cardYOffset;
		int cardY2 = cardY1 + cardH;
		
		// if there should be a shadow, draw appropriately
		if (flipPos > 0) {
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, SHADOW_ALPHA));
			g.fillRect(baseX + shadowXOffset, baseY, cardW, cardH);	
		}
		
		// draw card with dissolve amount
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) dissolveAmt));

		// draw the image
		g.drawImage(toDraw, cardX1, cardY1, cardX2, cardY2, 0, 0, cardW, cardH, null);
	}
	
	
	
	
	
	
/*	public void totalrowsandcolumns() { 
		SquareRoot = (int)Math.sqrt(totalcards);
		totalcolumns = SquareRoot;
		while (totalcards % totalcolumns != 0 ) {
			totalcolumns-=1;
		}
		totalrows = totalcards / totalcolumns;
		if ( ( totalrows / totalcolumns ) > 3) {
			totalcolumns = SquareRoot;      
			totalrows = ((totalcards / totalcolumns) + 1);
		}
	}

	public int getrow() { 
		row = cardnumber % totalrows;
		if (row == 0) {
			row += totalrows;
		}
		return row;
	}

	public int getcolumn() { 
		column = (((cardnumber - 1) / totalrows) + 1);    
		return column;
	}

	public static int lengthconstant() {
		int lengthconstant;
		if (((Concentration.mainwindow.getSize().height) / ((3.25 * Card.totalcolumns) + 1)) < ((Concentration.mainwindow.getSize().width) / ((2.25 * Card.totalrows) + 0.75))) {
			lengthconstant = (int)((Concentration.mainwindow.getSize().height) / ((3.25 * Card.totalcolumns) + 1));
		}
		else {
			lengthconstant = (int)((Concentration.mainwindow.getSize().width) / ((2.25 * Card.totalrows) + 0.75));
		}
		return lengthconstant;
	}

	public int getxpos() {
		lengthconstant = lengthconstant();
		int xpos = (int)(((Concentration.mainwindow.getSize().width - ((Card.lengthconstant() * totalrows * 2) + (Card.lengthconstant() * 0.25 * (totalrows - 1)))) / 2) + ((this.getrow() - 1) * 2.25 * Card.lengthconstant()));
		return xpos;
	}

	public int getxboundary() {
		lengthconstant = lengthconstant();
		int xpos = this.getxpos();
		int xboundary = xpos + (lengthconstant * 2);
		return xboundary;
	}

	public int getyboundary() {
		int lengthconstant = lengthconstant();
		int ypos = this.getypos();
		int yboundary = ypos + (lengthconstant * 3);
		return yboundary;
	}

	public int getypos() {
		lengthconstant = lengthconstant();
		int ypos = (int)(((Concentration.mainwindow.getSize().height - ((Card.lengthconstant() * totalcolumns * 3) + (Card.lengthconstant() * 0.25) + (Card.lengthconstant() * 0.25 * (totalcolumns - 1)))) / 2) + (Card.lengthconstant() * 0.25) + ((this.getcolumn() - 1) * 3.25 * Card.lengthconstant()));
		return ypos;
	}
*/
}