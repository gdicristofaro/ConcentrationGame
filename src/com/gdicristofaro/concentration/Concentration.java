package com.gdicristofaro.concentration;

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
		
		return matches;
	}
	
	// creates randomized array of ordered list
	public static ArrayList<Card> randomizeCards(ArrayList<Card> ordered) {
		ArrayList<Card> randomized = new ArrayList<Card>();
		

		ArrayList<Card> copy = new ArrayList<Card>();
		for (int i = 0; i < ordered.size(); i++)
			copy.add(ordered.get(i));

		Random randomGenerator = new Random();
		while (copy.size() > 0) {
			int index = randomGenerator.nextInt(copy.size());
			randomized.add(copy.get(index));
			copy.remove(index);
		}
		
		return randomized;
	}
	
	public static Clip loadClip(File path) throws LineUnavailableException, UnsupportedAudioFileException, IOException {
		Clip clip = AudioSystem.getClip();
        AudioInputStream inputStream = AudioSystem.getAudioInputStream(path);
        clip.open(inputStream);
        return clip;
	}
	
	public static <T> T loadInternalResource(String file, Function<URL, T> converter) {
		return converter.convert(Concentration.class.getResource(file));
	}

	// loading an external resource that may or may not be present
	public static <T> T loadExternalResource(String basePath, String file, 
			Function<File, T> converter, Function<Exception, T> onError) {
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
	
	// whether or not the game is accepting user actions for cards
	private boolean acceptingAction = true;
	
	private final Runnable returnControl = () -> acceptingAction = true;

	// all cards in order that they are rendered
	private ArrayList<Card> cards;
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
	private BufferedImage soundChoice, windowChoice;
	private BufferedImage soundOn, soundOff, windowSmall, windowLarge, close;
	
	// image resources
	private BufferedImage background, youWinImg, cardBack;
	
	// sound resources
	private Clip soundtrack, flipSound, youWinSound, rightChoiceSound, wrongChoiceSound;
	
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
	
	
	public void init() {
		loadInternalResources();
		
		String configDir = 
			new File(Concentration.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParent();
		File configFile = new File(configDir + File.pathSeparator + CONFIG_FILE_NAME);
		
		Tuple<File, ArrayList<Card>> results = loadCards(configFile);
		configFile = results.getFirst();
		this.matches = results.getSecond();
		
		loadExternalResources(configFile.getParent());
		
		this.cards = randomizeCards(this.matches);
		int rows = determineRows(this.cards.size());
		
		setupListeners();
		determinePlacement();
	}
	
	
	public static void main(String[] args) {    
		String theconfigfile = "config.txt";
		mycards = new ArrayList<Card> (makecards(theconfigfile));
		new PlayBackground().start();
		mainwindow = new Concentration();
	}



	public Concentration () {
		loadResources();
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle("Concentration");
		
		if (fullscreen) {
			setUndecorated(true);
			setResizable(false);
			GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(this);
		}
		else {
			setSize(700,500);
			setLocationRelativeTo(null);
		}
		
		addMouseListener(new HandleMouse());
		setVisible(true);
		createBufferStrategy(2);
		strategy = getBufferStrategy();
	}
	
	

	public void loadInternalResources() {
		Function<URL, BufferedImage> c = (URL url) -> ImageIO.read(url);
		Function<String, BufferedImage> load = (String fileName) -> loadInternalResource(fileName, c);
		this.soundOn = load.convert(IMG_MUSICON_LOC);
		this.soundOff = load.convert(IMG_MUSICOFF_LOC);
		this.windowSmall = load.convert(IMG_WINDOWSMALL_LOC);
		this.windowLarge = load.convert(IMG_WINDOWLARGE_LOC);
		this.close = load.convert(IMG_CLOSE_LOC);
	}
	
	public void loadExternalResources(String dir) throws IllegalArgumentException {
		Function<File, BufferedImage> loadImg = (File f) -> ImageIO.read(f);

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

		Function<File, Clip> loadSnd = (File f) -> loadClip(f);
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
	
	
	
	
	// TODO might be a cleaner way of doing this...
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
									listeners.remove(clickRecord);
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
	
	




	/*
	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D) strategy.getDrawGraphics();

		((Graphics2D) g2).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.drawImage(background, 0, 0, mainwindow.getSize().width, mainwindow.getSize().height, null);
		if (GameWon) { 

			if ((youwin.getWidth()/mainwindow.getSize().width) >= (youwin.getHeight()/mainwindow.getSize().height)) {
				int imageheight = (int)(((0.75 * mainwindow.getSize().width) * youwin.getHeight()) / youwin.getWidth());
				int imagewidth = (int)(0.75 * mainwindow.getSize().width ); 
				g2.drawImage(youwin, (int)(0.125 * mainwindow.getSize().width ), (int)((heightfraction * (((mainwindow.getSize().height - imageheight) / 2) + imageheight)) - imageheight), imagewidth, imageheight, null);
			}
			else {
				int imageheight = (int)(0.75 * mainwindow.getSize().height );
				int imagewidth = (int)(((0.75 * mainwindow.getSize().height) * youwin.getWidth()) / youwin.getHeight());
				g2.drawImage(youwin, ((mainwindow.getSize().width - imagewidth) / 2), (int)(((heightfraction * 0.125 * mainwindow.getSize().height) + imageheight ) - imageheight), imagewidth, imageheight, null);
			}
		}
		for(Card s : mycards) {
			if (s.stillremaining && (!(CardsShowing.contains(s)))) { 
				g2.drawImage(img, s.getxpos(), s.getypos(), (2 * Card.lengthconstant()), (3 * Card.lengthconstant()), null);
			}
			if (CardsShowing.contains(s)) { 
				float alpha = (float) (.3 * s.dissolvefraction);
				((Graphics2D) g2).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
				g2.fillRect(s.getshadowx(), s.getypos(), s.getnewcardw(), (3 * Card.lengthconstant()));
				alpha = (float) (s.dissolvefraction);;
				((Graphics2D) g2).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
				g2.drawImage(s.ToShow, s.getnewcardx(), s.getnewcardy(), s.getnewcardw(), (3 * Card.lengthconstant()), null);
				((Graphics2D) g2).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1));
			}
		}
		g2.setColor(Color.white);

		if (soundson) {
			soundchoice = soundon;
		}
		else {
			soundchoice = soundoff;
		}

		if (fullscreen) {
			g2.drawImage(close, (int)(mainwindow.getSize().width - (2 + (0.25 * Card.lengthconstant()))), 5, (int)(0.25 * Card.lengthconstant()), (int)(0.25 * Card.lengthconstant()), null);
			g2.drawImage(windowsmall, (int)(mainwindow.getSize().width - (4 + (0.5 * Card.lengthconstant()))), 5, (int)(0.25 * Card.lengthconstant()), (int)(0.25 * Card.lengthconstant()), null);
			g2.drawImage(soundchoice, (int)(mainwindow.getSize().width - (6 + (0.75 * Card.lengthconstant()))), 5, (int)(0.25 * Card.lengthconstant()), (int)(0.25 * Card.lengthconstant()), null);
		}
		else {
			g2.drawImage(windowlarge, (int)(mainwindow.getSize().width - (2 + (0.25 * Card.lengthconstant()))), 25, (int)(0.25 * Card.lengthconstant()), (int)(0.25 * Card.lengthconstant()), null);
			g2.drawImage(soundchoice, (int)(mainwindow.getSize().width - (4 + (0.5 * Card.lengthconstant()))), 25, (int)(0.25 * Card.lengthconstant()), (int)(0.25 * Card.lengthconstant()), null);    		
		}
		g2.dispose();
		strategy.show();
	}

	public static void youwin () {

		youwinInterval = 5;
		youwintimer = new Timer(youwinInterval, youwingraphic);
		youwinDuration = 530;
		youwintimer.start();
	}

	
	static ActionListener youwingraphic = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			long currentTime = youwinInterval * youwincounter;
			fraction = (float)currentTime / youwinDuration;

			if ((youwincounter * youwinInterval) >= youwinDuration) {
				youwincounter = 0;
				mainwindow.repaint();
				youwintimer.stop();
			}

			heightfraction = (float) ( 1 - Math.abs(
					Math.pow((((Math.sqrt(0.2) + Math.sqrt(1.2)) * fraction) - Math.sqrt(1.2)), 2) - 0.2
					)); 		    	
			youwincounter++;			        
			mainwindow.repaint();
		}
	};



	private static class CardSelector implements Runnable {

		@Override
		public void run() {
			
				if((CardsShowing.size() < 2) && (s.stillremaining) && (!(CardsShowing.contains(s))) && (s.getxpos() <= xclick) && ( xclick <= s.getxboundary()) && (s.getypos() <= yclick) && ( yclick <= s.getyboundary())) {
					s.xposition = s.getxpos();
					s.yposition = s.getypos();
					Concentration.CardsShowing.add(s);
					playAudio(flip);
					s.flipcardover();
					if (Concentration.CardsShowing.size() == 2) {
						new WaitTime();
					}
				}
			
			// TODO Auto-generated method stub
			
		}
		
	}
	*/

	public static class HandleMouse extends MouseAdapter { 

		public void mouseReleased(MouseEvent e) {
			int xclick = e.getX();
			int yclick = e.getY();

			if (fullscreen) {
				
				Runnable closeWindow = new Runnable() {
					public void run() {
						System.out.println("close window");
						System.exit(0);
					}
				};
				
				Runnable fullScreenToggle = new Runnable() {
					public void run() {
						if (fullScreen) {
							// TODO
						}
						else {
							// TODO
						}
					}
				};
				
				Runnable muteToggle = new Runnable() {
					public void run() {
						if (soundsOn) {
							soundsOn = false;
							mainwindow.repaint();
						}
						else {
							soundsOn = true;
							new PlayBackground().start();
							mainwindow.repaint();
						}
					}
				};
				
				
				
				if ((xclick >= (int)(mainwindow.getSize().width - (2 + (0.25 * Card.lengthconstant())))) && (yclick >= 5) && (xclick <= (int)(mainwindow.getSize().width - 2)) && (yclick <= (int)(5 + (0.25 * Card.lengthconstant())))) {
					System.out.println("close window");
					System.exit(0);
				}
				if ((xclick >= (int)(mainwindow.getSize().width - (4 + (0.5 * Card.lengthconstant())))) && (yclick >= 5) && (xclick <= (int)(mainwindow.getSize().width - (2 + (0.25 * Card.lengthconstant()))) && (yclick <= (int)(5 + (0.25 * Card.lengthconstant()))))) {
					mainwindow.dispose();
					fullscreen = false;
					mainwindow = new Concentration();
					System.out.println("fullscreen change");
					return;
				}
				if ((xclick >= (int)(mainwindow.getSize().width - (6 + (0.75 * Card.lengthconstant())))) && (yclick >= 5) && (xclick <= (int)(mainwindow.getSize().width - (2 + (0.5 * Card.lengthconstant()))) && (yclick <= (int)(5 + (0.25 * Card.lengthconstant()))))) {
					System.out.println("sound change");
					if (soundson) {
						soundson = false;
						mainwindow.repaint();
					}
					else {
						soundson = true;
						new PlayBackground().start();
						mainwindow.repaint();
					}
					return;
				}    		
			}
			else {
				if ((xclick >= (int)(mainwindow.getSize().width - (2 + (0.25 * Card.lengthconstant())))) && (yclick >= 25) && (xclick <= (int)(mainwindow.getSize().width - 2)) && (yclick <= (int)(25 + (0.25 * Card.lengthconstant())))) {
					mainwindow.dispose();
					fullscreen = true;
					mainwindow = new Concentration();
					System.out.println("fullscreen change");
					return;
				}
				if ((xclick >= (int)(mainwindow.getSize().width - (4 + (0.5 * Card.lengthconstant())))) && (yclick >= 25) && (xclick <= (int)(mainwindow.getSize().width - (2 + (0.25 * Card.lengthconstant()))) && (yclick <= (int)(25 + (0.25 * Card.lengthconstant()))))) {
					System.out.println("sound change");
					if (soundson) {
						soundson = false;
						mainwindow.repaint();
					}
					else {
						soundson = true;
						new PlayBackground().start();
						mainwindow.repaint();
					}
					return;
				}
			}

			if (GameWon) {
				mycards.clear();
				Card.matchedcards = 0;
				Card.totalcards = 0;
				TempReadIn.cardnumber = 0;
				String theconfigfile = "config.txt";
				mycards = (makecards(theconfigfile));
				GameWon = false;
				ImageLoad();
				new PlayBackground().start();
				mainwindow.repaint();
			}
			else {

				for(Card s : mycards) {
					if((CardsShowing.size() < 2) && (s.stillremaining) && (!(CardsShowing.contains(s))) && (s.getxpos() <= xclick) && ( xclick <= s.getxboundary()) && (s.getypos() <= yclick) && ( yclick <= s.getyboundary())) {
						s.xposition = s.getxpos();
						s.yposition = s.getypos();
						Concentration.CardsShowing.add(s);
						playAudio(flip);
						s.flipcardover();
						if (Concentration.CardsShowing.size() == 2) {
							new WaitTime();
						}
					}
				}
			}
		}
	}
}