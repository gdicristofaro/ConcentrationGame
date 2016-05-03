package com.gdicristofaro.concentration;

import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

// TODO fullscreen / window items
public class Concentration extends JFrame {
	// convert from one type to another
	@FunctionalInterface
	public static interface Function<I, O> {
		O convert(I input);
	}
	
	// used to read from file, string path, url
	@FunctionalInterface
	public static interface Reader<I, O> {
		O convert(I input) throws IOException;
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	// a record of an item that is clicked and what should be launched
	public static class ClickRecord {
		private Rectangle bounds;
		private Runnable action;
		
		public ClickRecord(Rectangle bounds, Runnable action) {
			this.bounds = bounds;
			this.action = action;
		}
		
		public Rectangle getBounds() {
			return bounds;
		}
		
		public void setBounds(Rectangle bounds) {
			this.bounds = bounds;
		}
		
		public Runnable getAction() {
			return action;
		}
		
		public void setAction(Runnable action) {
			this.action = action;
		}
	}
	
	
	public static Card getCard(String basePath, String line, 
			BufferedImage cardBack, Timer timer, Runnable repaintRequest) {
		
		// items in config starting with "file: " look for an image in directory with name
		String fileStart = "file: ";
		if (line.startsWith(fileStart)) {
			String fileName = line.substring(fileStart.length(), line.length());
			return new Card(new File(basePath + File.separator + fileName), cardBack, timer, repaintRequest);
		}
		else {
			return new Card(line, cardBack, timer, repaintRequest);
		}
	}
	
	public static ArrayList<Card> makecards(File configFile, 
			BufferedImage cardBack, Timer timer, Runnable repaintRequest) {
		
		ArrayList<Card> matches = new ArrayList<Card>();
		
		if (!configFile.exists())
			throw new IllegalStateException("Unable to open the config file found at " + configFile.getAbsolutePath());

		try {
			// read each pair of items in config
			BufferedReader in = new BufferedReader(new FileReader(configFile));
			
			String match1;
			while ((match1 = in.readLine()) != null) {
				String match2 = in.readLine();
				if (match2 == null)
					throw new IllegalStateException(
						"There are an odd number of items for matches in the config file at" + configFile.getAbsolutePath());
				
				Card card1 = getCard(configFile.getParent(), match1, cardBack, timer, repaintRequest);
				Card card2 = getCard(configFile.getParent(), match2, cardBack, timer, repaintRequest);
				
				matches.add(card1);
				matches.add(card2);
			}
			in.close();
		} 
		catch (IOException e) {
			throw new IllegalStateException("Error processing file: " + configFile.getAbsolutePath());
		}
		
		return matches;
	}
	
	// creates randomized array of ordered list
	public static Card[] randomizeCards(ArrayList<Card> ordered) {
		Card[] randomized = new Card[ordered.size()];
		

		ArrayList<Card> copy = new ArrayList<Card>();
		for (int i = 0; i < ordered.size(); i++)
			copy.add(ordered.get(i));

		Random randomGenerator = new Random();
		for (int newIndex = 0; newIndex < ordered.size(); newIndex++) {
			int index = randomGenerator.nextInt(copy.size());
			randomized[newIndex] = copy.get(index);
			copy.remove(index);
		}
		
		return randomized;
	}
	
	public static Clip loadClip(File path) throws IOException {
		try {
			Clip clip = AudioSystem.getClip();
	        AudioInputStream inputStream = AudioSystem.getAudioInputStream(path);
	        clip.open(inputStream);
	        return clip;
		}
		catch (LineUnavailableException | UnsupportedAudioFileException e) {
			throw new IllegalStateException("Problem loading clip " + path.getAbsolutePath() + " into memory");
		}
	}
	
	public static <T> T loadInternalResource(String file, Reader<URL, T> converter) throws IOException {
		return converter.convert(Concentration.class.getResource(file));
	}

	// loading an external resource that may or may not be present
	public static <T> T loadExternalResource(String basePath, String file, 
			Reader<File, T> converter, Function<Exception, T> onError) {
		try {
			File f = new File(basePath + File.separator + file);
			return converter.convert(f);
		}
		catch (Exception e) {
			return onError.convert(e);
		}
	}
	
	public static int determineRows(int cardsNum) { 
		int sqRt = (int)Math.sqrt(cardsNum);
		int rows = sqRt;
		while (cardsNum % rows != 0) {
			rows--;
		}
		
		int columns = cardsNum / rows;
		
		if ((columns / rows) > 3)
			rows = sqRt;
		
		return rows;
	}
	
	
	public static final int FPS = 32;
	// wait 2000 millis before flipping cards back over
	private static final int CARD_SHOW_WAIT_TIME = 1000;
	
	// locations of external audio files
	private static final String SOUND_TRACK_LOC = "soundtrack.wav";
	private static final String SOUND_FLIP_LOC = "flip.wav";
	private static final String SOUND_WIN_LOC = "youwin.wav";
	private static final String SOUND_RIGHTCHOICE_LOC = "rightchoice.wav";
	private static final String SOUND_WRONGCHOICE_LOC = "wrongchoice.wav";
	
	// location of external graphics files
	private static final String IMG_BACKGROUND_LOC = "background.png";
	private static final String IMG_YOUWIN_LOC = "youwin.png";
	private static final String IMG_CARDBACK_LOC = "cardback.png";

	// location of internal graphics files
	private static final String IMG_CLOSE_LOC = "close.png";
	private static final String IMG_MUSICOFF_LOC = "musicoff.png";
	private static final String IMG_MUSICON_LOC = "musicon.png";
	private static final String IMG_WINDOWLARGE_LOC = "windowlarge.png";
	private static final String IMG_WINDOWSMALL_LOC = "windowsmall.png";
	
	// default config file name
	private static final String CONFIG_FILE_NAME = "config.txt";
	
	
	
	// settings
	private boolean fullscreen = true;
	private boolean soundson = true;
	private int rows;
	
	// scaling for cards
	private double scale;

	
	// whether or not the game is accepting user actions for cards
	private boolean acceptingAction = true;
	
	private final Runnable returnControl = () -> acceptingAction = true;

	// all cards in order that they are rendered
	private Card[] cards;
	//cards in match orrder (i.e. index 0 and 1 match)
	private ArrayList<Card> matches;
	
	private YouWinGraphic youWin;
	
	// number of found cards
	private int found = 0;
	
	// indexes of visible card (when second card is clicked, it will be handled immediately)
	private Card showing = null; 
	
	// for animation and rendering
	private final Timer timer = new Timer();
	private final Runnable repaintRequest = () -> Concentration.this.repaint();
	private BufferStrategy buffStrategy;
	
	//private BufferStrategy strategy;

	
	// icons
	private BufferedImage soundOn, soundOff, windowSmall, windowLarge, close;
	
	// image resources
	private BufferedImage background, youWinImg, cardBack;
	
	// sound resources
	private byte[] flipSound, youWinSound, rightChoiceSound, wrongChoiceSound;
	private Clip soundtrack;
	
	// drawable components
	private ArrayList<Tuple<Button, Point>> buttonLoc = new ArrayList<Tuple<Button, Point>>();
	private ArrayList<Tuple<Card, Point>> cardLoc = new ArrayList<Tuple<Card, Point>>();
	
	
	// items with listeners
	private ArrayList<ClickRecord> listeners = new ArrayList<ClickRecord>();
	
	private final MouseAdapter mouseAdapter = new MouseAdapter() {
		public void mouseClicked(MouseEvent e) {
			int xClick = e.getX();
			int yClick = e.getY();

			for (ClickRecord record : listeners)
				if (record.getBounds() == null || record.getBounds().contains(xClick, yClick))
					record.getAction().run();
		}
	};
	
	private final Button buttonClose = 
			new Button(() -> System.exit(0)) {
		public BufferedImage getIcon() {
			return Concentration.this.close;
		}		
	};
	
	private final Button buttonToggleSize = 
			new Button(() -> Concentration.this.toggleFullScreen()) {
		public BufferedImage getIcon() {
			if (Concentration.this.fullscreen)
				return Concentration.this.windowSmall;
			else
				return Concentration.this.windowLarge;
		}
	};
	
	private final Button buttonToggleSound = 
			new Button(() -> Concentration.this.toggleSound()) {
		public BufferedImage getIcon() {
			if (Concentration.this.soundson)
				return Concentration.this.soundOn;
			else
				return Concentration.this.soundOff;
		}
	};
	
	private final Button[] buttons = new Button[]{buttonToggleSound, buttonToggleSize, buttonClose};
	
	
	public static void main(String[] args) {    
		new Concentration();
	}



	public Concentration() {

		setSize(700,500);
		setLocationRelativeTo(null);		
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle("Concentration");
		setUndecorated(true);
		
		//setExtendedState(JFrame.MAXIMIZED_BOTH);
		
		// TODO go back and fix this
		/*
		setUndecorated(true);
		setResizable(false);
		GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(this);
		 */
		loadInternalResources();
		
		String configDir = 
		//	new File(Concentration.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParent();
		//TODO fix this
		new File(Concentration.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile().getParent();
		File startConfig = new File(configDir + File.separator + CONFIG_FILE_NAME);
		
		loadConfigFileResources(startConfig);
			
		this.cards = randomizeCards(this.matches);
		this.rows = determineRows(this.cards.length);
		
		setPlacementListeners();
		addMouseListener(mouseAdapter);
		
		setVisible(true);
		createBufferStrategy(2);
		this.buffStrategy = getBufferStrategy();
		repaint();
		
		if (this.soundtrack != null)
			this.soundtrack.loop(Clip.LOOP_CONTINUOUSLY);
	}
		
	// plays audio data from byte array
	public void playSound(byte[] audioData) {
		if (audioData != null && this.soundson) {
			try {
				Clip clip = AudioSystem.getClip();
		        clip.open(AudioSystem.getAudioInputStream(new ByteArrayInputStream(audioData)));
		        clip.start();
			}
			catch (LineUnavailableException | UnsupportedAudioFileException | IOException e) {
				throw new IllegalStateException("Problem playing byte array");
			}
		}
	}
	
	
	// toggles sound on or off
	public void toggleSound() {
		if (this.soundson) {
			this.soundson = false;
			if (this.soundtrack != null)
				this.soundtrack.stop();
		}
		else {
			this.soundson = true;
			if (this.soundtrack != null)
				this.soundtrack.loop(Clip.LOOP_CONTINUOUSLY);
		}
		
		repaint();
	}
	
	public void toggleFullScreen() {
		if (this.fullscreen) {
			this.fullscreen = false;
			setResizable(false);
			GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(this);
		}
		else {
			this.fullscreen = true;
			setSize(700,500);
			setResizable(true);
			setLocationRelativeTo(null);
		}
		
		setPlacementListeners();
		repaint();
	}
	
	public void setPlacementListeners() {
		// place this.buttons
		listeners.clear();
		buttonLoc.clear();
		for (int i = 0; i < this.buttons.length; i++) {
			int x = this.getWidth() - ((this.buttons.length - i) * (Button.BUTTON_SPACING + Button.BUTTON_WIDTH));
			buttonLoc.add(Tuple.get(this.buttons[i], new Point(x, Button.BUTTON_SPACING)));
			listeners.add(
				new ClickRecord(
					new Rectangle(x, Button.BUTTON_SPACING, Button.BUTTON_WIDTH, Button.BUTTON_WIDTH), 
					this.buttons[i].getAction()));
		}
		
		// determine card placement
		int columns = (int) Math.ceil(((double) this.cards.length) / this.rows);
		
		double unscaledHeight = ((Card.CARD_HEIGHT + Card.CARD_SPACING) * this.rows) + Card.CARD_SPACING;
		
		double unscaledWidth = ((Card.CARD_WIDTH + Card.CARD_SPACING) * columns) + Card.CARD_SPACING;
		
		// make room for buttons before cards
		double upperMargin = Button.BUTTON_WIDTH + Button.BUTTON_SPACING;
		double adjustedHeight = this.getHeight() - upperMargin;
		
		// determine how much the cards should be scaled to fit appropriately on the screen
		this.scale = Math.min(adjustedHeight / unscaledHeight, 
			((double) this.getWidth()) / unscaledWidth);
			
		double cardWidth = Card.CARD_WIDTH * scale;
		double cardHeight = Card.CARD_HEIGHT * scale;
		double cardSpacing = Card.CARD_SPACING * scale;
		
		// determine upper left corner for where cards start
		int xStart = (int) (((this.getWidth() - scale * unscaledWidth) / 2) + cardSpacing);	
		int yStart = (int) ((((adjustedHeight - scale * unscaledHeight) / 2) + cardSpacing) + upperMargin);
				
		// determine placement for cards 
		cardLoc.clear();
		for (int i = 0; i < this.cards.length; i++) {
			int cardX = (int) (xStart + ((i % columns) * (cardSpacing + cardWidth)));
			int cardY = (int) (yStart + ((i / columns) * (cardSpacing + cardHeight)));
			final Card thisCard = this.cards[i];
			cardLoc.add(Tuple.get(thisCard, new Point(cardX, cardY)));
			listeners.add(new ClickRecord(new Rectangle((int) cardX, (int) cardY, (int) cardWidth, (int) cardHeight),
				() -> onCardClick(thisCard)));
		}
	}
	

	public void loadInternalResources() {
		try {
			Reader<URL, BufferedImage> c = (URL url) -> ImageIO.read(url);
			Reader<String, BufferedImage> load = (String fileName) -> loadInternalResource(fileName, c);
			this.soundOn = load.convert(IMG_MUSICON_LOC);
			this.soundOff = load.convert(IMG_MUSICOFF_LOC);
			this.windowSmall = load.convert(IMG_WINDOWSMALL_LOC);
			this.windowLarge = load.convert(IMG_WINDOWLARGE_LOC);
			this.close = load.convert(IMG_CLOSE_LOC);			
		}
		catch (IOException e) {
			throw new IllegalStateException("Could not find local file: " + e.getMessage());
		}
	}
	
	public void loadExternalResources(String dir) throws IllegalArgumentException {
		Reader<File, BufferedImage> loadImg = (File f) -> ImageIO.read(f);

		this.background = loadExternalResource(dir, IMG_BACKGROUND_LOC, loadImg,
			(Exception e) -> {
				BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
				img.setRGB(0, 0, 0xFF000000);		// set to black background
				return img;
			});

		this.cardBack = loadExternalResource(dir, IMG_CARDBACK_LOC, loadImg,
			(Exception e) -> {
				BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
				img.setRGB(0, 0, 0xFFFFFFFF);		// set to white background
				return img;
			});

		this.youWinImg = loadExternalResource(dir, IMG_YOUWIN_LOC, loadImg,
			(Exception e) -> {
				BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
				img.setRGB(0, 0, 0x0);		// set to transparent background
				return img;
			});
		
		this.youWin = new YouWinGraphic(this.youWinImg, this.repaintRequest, this.timer);

		Reader<File, Clip> loadSnd = (File f) -> loadClip(f);
		Function<Exception, Clip> onSndErr = (Exception e) -> null;
		
		Reader<File, byte[]> loadBytes = (File f) -> Files.readAllBytes(f.toPath());
		Function<Exception, byte[]> onByteErr = (Exception e) -> null;
		
		this.soundtrack = loadExternalResource(dir, SOUND_TRACK_LOC, loadSnd, onSndErr);
		this.flipSound = loadExternalResource(dir, SOUND_FLIP_LOC, loadBytes, onByteErr);
		this.youWinSound = loadExternalResource(dir, SOUND_WIN_LOC, loadBytes, onByteErr);
		this.rightChoiceSound = loadExternalResource(dir, SOUND_RIGHTCHOICE_LOC, loadBytes, onByteErr);
		this.wrongChoiceSound = loadExternalResource(dir, SOUND_WRONGCHOICE_LOC, loadBytes, onByteErr);
	}
	
	public void loadConfigFileResources(File configFile) {
		// loop while we have errors loading the config file
		try {
			loadExternalResources(configFile.getParent());
			this.matches = makecards(configFile, this.cardBack, this.timer, this.repaintRequest);
		}
		catch (Exception e) {
			// show a file chooser to pick another file or exit
			Object[] options = {"Locate Config File", "Exit"};
			int choice = JOptionPane.showOptionDialog(null, 
				"The program is having problems:\n" + e.getMessage(), "Error",
				JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,
				null, options, options[0]);

			if (choice == 0) {	  
				try {
					EventQueue.invokeAndWait(() -> {
						JFileChooser fc = new JFileChooser();
						fc.setDialogTitle("Choose Config File");
						fc.setCurrentDirectory(configFile.getParentFile());
						fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
						int returnVal = fc.showDialog(null, "Choose Config File");
						if (returnVal == JFileChooser.APPROVE_OPTION)
							loadConfigFileResources(fc.getSelectedFile());
					});
				} catch (InvocationTargetException | InterruptedException e1) {
					throw new IllegalStateException("unable to open file chooser menu");
				} 
			}
			else if (choice == 1) {
				System.exit(0);
			}
			else {
				throw new IllegalArgumentException("choice was " + choice);
			}
		}
	}
	
	public void resetGame() {
		this.cards = randomizeCards(this.matches);
		
		setPlacementListeners();
		found = 0;
		acceptingAction = true;
		
		// reset cards so they are visible
		for (Card c : this.cards)
			c.reset();
		
		repaint();
	}
	
	
	public void onCardClick(final Card c) {
		// don't allow if not accepting action or isn't visible
		if (!acceptingAction || !c.isVisible())
		//if (!c.isVisible()) // this speeds up gameplay by removing the user lockout when cards are flipping
			return;
		
		// after the card is flipped over, this is the callback to run
		Runnable callback;
		
		// there are no cards showing yet
		if (showing == null) {
			showing = c;
			callback = returnControl;
		}
		// there is a card showing
		else {
			Card lastShowing = showing;
			showing = null;
			
			// if showing cards are next to each other in matches, they match and can be dissolved
			if (matches.indexOf(lastShowing) / 2 == matches.indexOf(c) / 2) {
				// increment found items
				found += 2;

				callback = () -> {
					// when cards are dissolved
					Runnable onFinish = () -> {
						// if we have found all items
						if (found == matches.size()) {
							// show you win item
							final ClickRecord clickRecord = new ClickRecord(
								null, 
								() -> {
									// remove this listener from the listeners
									listeners.remove(this);
									youWin.hide();
									resetGame();
								});
							
							youWin.show(() -> listeners.add(clickRecord));
							playSound(this.youWinSound);
						}
						else {
							// otherwise, just return control after dissolve
							returnControl.run();
						}
					};

					// dissolve both cards since they match
					c.onDissolve(CARD_SHOW_WAIT_TIME, null);
					lastShowing.onDissolve(CARD_SHOW_WAIT_TIME, onFinish);
					playSound(this.rightChoiceSound);
				};
			}
			// if cards don't match, put them back
			else {
				callback = () -> {
					c.onFlipBack(CARD_SHOW_WAIT_TIME, null);
					lastShowing.onFlipBack(CARD_SHOW_WAIT_TIME, returnControl);
					playSound(this.wrongChoiceSound);
				};
			}
		}
		
		// run the card flip
		acceptingAction = false;
		c.onFlip(0, callback);
		playSound(this.flipSound);
	}
	
	

	public void paint(Graphics gfx) {
		// if frame is drawn before buffer strategy established
		Graphics2D g;
		try {
			g = (Graphics2D) this.buffStrategy.getDrawGraphics();	
		} catch (NullPointerException e) {
			return;
		}
		
		// draw background
		g.drawImage(this.background, 0, 0, this.getWidth(), this.getHeight(),
			0, 0, this.background.getWidth(), this.background.getHeight(), null);
		
		// draw shadows of cards
		for (Tuple<Card, Point> e : this.cardLoc)
			e.getFirst().paintShadow(g, (int) e.getSecond().getX(), (int) e.getSecond().getY(), scale);
			
		// draw the cards
		for (Tuple<Card, Point> e : this.cardLoc)
			e.getFirst().paintCard(g, (int) e.getSecond().getX(), (int) e.getSecond().getY(), scale);
		
		// draw you win graphic
		youWin.paint(g, (int) (getWidth() * .9), 0, getHeight() / 2, getWidth() / 2);
		
		// draw the buttons
		for (Tuple<Button, Point> e : this.buttonLoc)
			e.getFirst().paint(g, (int) e.getSecond().getX(), (int) e.getSecond().getY());

		g.dispose();
		this.buffStrategy.show();
	}
}