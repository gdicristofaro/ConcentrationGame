package com.gdicristofaro.concentration;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
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


// TODO sound effect
// TODO you win

public class Concentration extends JFrame {
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
			return new Card(new File(basePath + File.pathSeparator + fileName), cardBack, timer, repaintRequest);
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
			File f = new File(basePath + File.pathSeparator + file);
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
	
	
	/**
	 * determines placement for cards and buttons
	 * 
	 * @param width		the screen width
	 * @param height	the screen height
	 * @param rows		how many rows of cards
	 * @param cards		all of the cards in order to be rendered
	 * @param buttons	all of the buttons in order to be rendered
	 * @return			a tuple of the scaling of cards along with the mapping of objects to their position
	 */
	public static Tuple<Double, Map<Component, Rectangle>> determinePlacement(
			int width, int height, int rows, Card[] cards, Button[] buttons) {
		
		Map<Component, Rectangle> positions = new HashMap<Component, Rectangle>();
		
		// place buttons
		for (int i = 0; i < buttons.length; i++) {
			int x = width - ((buttons.length - i) * (Button.BUTTON_SPACING + Button.BUTTON_WIDTH));
			positions.put(buttons[i], 
				new Rectangle(x, Button.BUTTON_SPACING, Button.BUTTON_WIDTH, Button.BUTTON_WIDTH));
		}
		
		// determine card placement
		int columns = (int) Math.ceil(((double) cards.length) / rows);
		
		double unscaledHeight = ((Card.CARD_HEIGHT + Card.CARD_SPACING) * rows) + Card.CARD_SPACING;
		double unscaledWidth = ((Card.CARD_WIDTH + Card.CARD_SPACING) * columns) + Card.CARD_SPACING;
		
		// determine how much the cards should be scaled to fit appropriately on the screen
		double scale = Math.min(((double) height) / unscaledHeight, ((double) width) / unscaledWidth);
			
		// determine upper left corner for where cards start
		int xStart = (int) (width - ((scale * unscaledWidth) / 2));	
		int yStart = (int) (height - ((scale * unscaledHeight) / 2));
		
		int cardWidth = (int) (Card.CARD_WIDTH * scale);
		int cardHeight = (int) (Card.CARD_HEIGHT * scale);
		
		// determine placement for cards 
		for (int i = 0; i < cards.length; i++) {
			int cardX = (int) (xStart + ((Card.CARD_SPACING + ((i % rows) * (Card.CARD_WIDTH + Card.CARD_SPACING))) * scale));
			int cardY = (int) (yStart + ((Card.CARD_SPACING + ((i / rows) * (Card.CARD_HEIGHT + Card.CARD_SPACING))) * scale));
			positions.put(cards[i], new Rectangle(cardX, cardY, cardWidth, cardHeight));
		}
		
		return Tuple.get(scale, positions);
	}
	
	
	public static final int FPS = 32;
	// wait 2000 millis before flipping cards back over
	private static final int CARD_SHOW_WAIT_TIME = 2000;
	
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
	
	private BufferStrategy strategy;

	
	// icons
	private BufferedImage soundOn, soundOff, windowSmall, windowLarge, close;
	
	// image resources
	private BufferedImage background, youWinImg, cardBack;
	
	// sound resources
	private Clip soundtrack, flipSound, youWinSound, rightChoiceSound, wrongChoiceSound;
	
	// drawable components
	private ArrayList<Tuple<Component, Rectangle>> components = new ArrayList<Tuple<Component, Rectangle>>();
	
	// items with listeners
	private ArrayList<ClickRecord> listeners = new ArrayList<ClickRecord>();
	
	private final MouseAdapter mouseAdapter = new MouseAdapter() {
		public void mouseClicked(MouseEvent e) {
			int xClick = e.getX();
			int yClick = e.getY();

			// for this game, take the first item
			for (ClickRecord record : listeners) {
				if (record.getBounds().contains(xClick, yClick)) {
					record.getAction().run();
					return;
				}
			}
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



	public Concentration () {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle("Concentration");
		
		setUndecorated(true);
		setResizable(false);
		GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(this);

		loadInternalResources();
		
		String configDir = 
			new File(Concentration.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParent();
		File configFile = new File(configDir + File.pathSeparator + CONFIG_FILE_NAME);
		
		Tuple<File, ArrayList<Card>> results = loadCards(configFile);
		configFile = results.getFirst();
		this.matches = results.getSecond();
		
		loadExternalResources(configFile.getParent());
		
		this.cards = randomizeCards(this.matches);
		this.rows = determineRows(this.cards.length);
		
		setPlacementListeners();
		addMouseListener(mouseAdapter);
		
		setVisible(true);
		createBufferStrategy(2);
		strategy = getBufferStrategy();
	}
	
	
	// toggles sound on or off
	public void toggleSound() {
		if (this.soundson) {
			this.soundson = false;
			this.soundtrack.stop();
		}
		else {
			this.soundson = true;
			this.soundtrack.start();
		}
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
	}
	
	public void setPlacementListeners() {
		Tuple<Double, Map<Component, Rectangle>> result = 
			determinePlacement(this.getWidth(), this.getHeight(), this.rows, this.cards, this.buttons);
		
		this.scale = result.getFirst();
		
		listeners.clear();
		components.clear();
		for (Entry<Component, Rectangle> e : result.getSecond().entrySet()) {
			if (e.getKey() instanceof Button)
				listeners.add(new ClickRecord(e.getValue(), ((Button) e.getKey()).getAction()));
			else if (e.getKey() instanceof Card)
				listeners.add(new ClickRecord(e.getValue(), () -> onCardClick((Card) e.getKey())));
			
			components.add(Tuple.get(e.getKey(), e.getValue()));
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
		
		this.soundtrack = loadExternalResource(dir, SOUND_TRACK_LOC, loadSnd, onSndErr);
		this.flipSound = loadExternalResource(dir, SOUND_FLIP_LOC, loadSnd, onSndErr);
		this.youWinSound = loadExternalResource(dir, SOUND_WIN_LOC, loadSnd, onSndErr);
		this.rightChoiceSound = loadExternalResource(dir, SOUND_RIGHTCHOICE_LOC, loadSnd, onSndErr);
		this.wrongChoiceSound = loadExternalResource(dir, SOUND_WRONGCHOICE_LOC, loadSnd, onSndErr);
	}
	
	public Tuple<File, ArrayList<Card>> loadCards(File configFile) {
		// loop while we have errors loading the config file
		while(true) {
			try {
				return Tuple.get(configFile, makecards(configFile, this.cardBack, this.timer, this.repaintRequest));
			}
			catch (Exception e) {
				// show a file chooser to pick another file or exit
				Object[] options = {"Locate Config File", "Exit"};
				int choice = JOptionPane.showOptionDialog(null, 
					"The program is having problems:\n" + e.getMessage(), "Error",
					JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,
					null, options, options[0]);

				if (choice == 0) {	  
					JFileChooser fc = new JFileChooser();
					fc.setCurrentDirectory(configFile.getParentFile());
					fc.setDialogTitle("Choose Config File");
					fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
					int returnVal = fc.showDialog(null, "Choose Config File");
					if (returnVal == JFileChooser.APPROVE_OPTION)
						configFile = fc.getSelectedFile();
				}
				else if (choice == 1) {
					System.exit(0);
				}
			}
		}
	}
	
	public void resetGame() {
		// TODO
	}
	
	
	public void onCardClick(final Card c) {
		// don't allow if not accepting action
		if (!acceptingAction)
			return;
		
		Runnable callback;
		
		// there are no cards showing yet
		if (showing == null) {
			showing = c;
			callback = returnControl;
		}
		// there is a card showing, so either
		else {
			// if showing cards are next to each other in matches, they match and can be dissolved
			if (matches.indexOf(showing) / 2 == matches.indexOf(c) / 2) {
				callback = () -> {
					// when cards are dissolved
					Runnable onFinish = () -> {
						// if we have found all items
						if (found == matches.size()) {
							final ClickRecord clickRecord = new ClickRecord(
								new Rectangle(0,0,
									Concentration.this.getWidth(), Concentration.this.getHeight()), 
								() -> {
									resetGame();
									// remove this listener from the listeners
									listeners.remove(this);
								});
							
							youWin.show(() -> listeners.add(0, clickRecord));	
						}
						else {
							// otherwise, just return control
							returnControl.run();
						}
					};

					found += 2;
					showing = null;
					c.onDissolve(CARD_SHOW_WAIT_TIME, null);
					showing.onDissolve(CARD_SHOW_WAIT_TIME, onFinish);
				};
			}
			// otherwise, put them back
			else {
				callback = () -> {
					showing = null;
					c.onFlipBack(CARD_SHOW_WAIT_TIME, null);
					showing.onFlipBack(CARD_SHOW_WAIT_TIME, returnControl);
				};
			}
		}
		
		// run the card flip
		c.onFlip(0, callback);
	}
	
	

	public void paint(Graphics gfx) {
		Graphics2D g = (Graphics2D) strategy.getDrawGraphics();
		
		// draw background
		g.drawImage(this.background, 0, 0, this.getWidth(), this.getHeight(), null);
		
		// draw all items
		for (Tuple<Component, Rectangle> entry : this.components)
			entry.getFirst().paint(g, (int) entry.getSecond().getX(), (int) entry.getSecond().getY(), this.scale);
	}
}