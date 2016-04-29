package com.gdicristofaro.concentration;

import java.applet.AudioClip;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFrame;
import java.util.Timer;

public class Concentration extends JFrame {
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
			return new Card(new File(basePath + fileName), cardBack, timer, repaintRequest);
		}
		else {
			return new Card(line, cardBack, timer, repaintRequest);
		}
	}
	
	public static ArrayList<Card> makecards(String basePath, String configLoc, 
			BufferedImage cardBack, Timer timer, Runnable repaintRequest) {
		
		ArrayList<Card> matches = new ArrayList<Card>();
		
		try {
			// read each pair of items in config
			BufferedReader in = new BufferedReader(new FileReader(configLoc));
			
			String match1;
			while ((match1 = in.readLine()) != null) {
				String match2 = in.readLine();
				if (match2 == null)
					throw new IllegalStateException("odd number of items for matches in config file");
				
				Card card1 = getCard(basePath, match1, cardBack, timer, repaintRequest);
				Card card2 = getCard(basePath, match2, cardBack, timer, repaintRequest);
				
				matches.add(card1);
				matches.add(card2);
			}
			
			in.close();
		} catch (IOException e) {
			throw new IllegalStateException("unable to open file found at " + configLoc);
		}
		
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
	
	public static final int FPS = 32;
	
	// locations of internal audio files
	private final static File soundtrackFile = new File("soundtrack.wav");
	private final static File flipSoundFile = new File("flip.wav");
	private final static File youWinSoundFile = new File("youwin.wav");
	private final static File rightChoiceSoundFile = new File("rightchoice.wav");
	private final static File wrongChoiceSoundFile = new File("wrongchoice.wav");
	

	
	// settings
	private boolean fullscreen = true;
	private boolean soundson = true;

	// all cards in order that they are rendered
	private ArrayList<Card> cards;
	//cards in match orrder (i.e. index 0 and 1 match)
	private ArrayList<Card> matches;
	
	// indexes of cards that should not be rendered
	private HashSet<Integer> found = new HashSet<Integer>();
	// indexes of visible cards
	private HashSet<Integer> showing = new HashSet<Integer>(); 
	
	// for animation and rendering
	private final Timer timer = new Timer();
	private final Runnable repaintRequest = new Runnable() {
		public void run() {
			Concentration.this.repaint();
		}
	};
	
	private BufferStrategy strategy;
	
	// image resources
	private BufferedImage background, youwin, cardBack;
	
	// icons
	private BufferedImage soundchoice, soundon, soundoff, windowchoice, windowsmall, windowlarge, close;
	
	// sound resources
	private Clip soundtrack, flipSound, youWinSound, rightChoiceSound, wrongChoiceSound;
	
	// items with listeners
	private Collection<ClickRecord> listeners = new ArrayList<ClickRecord>();
	
	private final MouseAdapter mouseAdapter = new MouseAdapter() {

		@Override
		public void mouseClicked(MouseEvent e) {
			int xClick = e.getX();
			int yClick = e.getY();

			// for this game, there is no overlapping items that can be clicked
			for (ClickRecord record : listeners)
				if (record.getBounds().contains(xClick, yClick))
					record.getAction().run();
		}
		
	};
	
	
	public void loadResources() {
		// TODO remove
		//System.out.println(Concentration.class.getProtectionDomain().getCodeSource().getLocation().getPath());

		try {
			background = ImageIO.read(new File("background.jpg"));
			cardBack = ImageIO.read(new File("cardback.jpg"));
			youwin = ImageIO.read(new File("youwin.gif"));
			close = ImageIO.read(Concentration.class.getResource("/close.jpg"));
			soundon = ImageIO.read(Concentration.class.getResource("/musicon.jpg"));
			soundoff = ImageIO.read(Concentration.class.getResource("/musicoff.jpg"));
			windowsmall = ImageIO.read(Concentration.class.getResource("/windowsmall.jpg"));
			windowlarge = ImageIO.read(Concentration.class.getResource("/windowlarge.jpg"));
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public void init() {
		loadInternalResources();
		File configFile = loadConfigFile();
		loadFileResources(configFile);
		this.matches = loadCards(configFile);
		this.cards = randomizeCards(this.matches);
		int rows = determineRows(this.cards.size());
		determinePlacement();
		startGame();
	}
	
	
	public static void main(String[] args) {    
		String theconfigfile = "config.txt";
		mycards = new ArrayList<Card> (makecards(theconfigfile));
		new PlayBackground().start();
		mainwindow = new Concentration();
	}



	public static void playgame() {
		if (CardsShowing.get(0).matchnumber == CardsShowing.get(1).matchnumber) {
			playAudio(rightchoice);
			CardsShowing.get(0).dissolve();
			CardsShowing.get(1).dissolve();
		}
		else {
			CardsShowing.get(0).putitback();
			CardsShowing.get(1).putitback();
			playAudio(wrongchoice);
		}
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



	public static void playAudio(File soundFile) {
		if (soundson) {
			try{
				System.out.println("playing audio...");
				audioInputStream = AudioSystem.getAudioInputStream(soundFile);
				audioFormat = audioInputStream.getFormat();
				DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
				sourceDataLine = (SourceDataLine)AudioSystem.getLine(dataLineInfo);
				soundsplaying++;
				new PlayThread(soundFile).start();
			}catch (Exception e) {
			}
		}
	}

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

class PlayBackground extends Thread{
	byte tempBuffer[] = new byte[10000];
	public void run(){
		try{
			while (!(Concentration.GameWon) && (Concentration.soundson)) {
				if (Concentration.soundson) {
					System.out.println("playing background");
					AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(Concentration.soundtrack);
					AudioFormat audioFormat = audioInputStream.getFormat();
					DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);

					SourceDataLine sourceDataLine = (SourceDataLine)AudioSystem.getLine(dataLineInfo);
					sourceDataLine.open(audioFormat);
					sourceDataLine.start();

					int count;
					while(((count = audioInputStream.read(tempBuffer,0,tempBuffer.length)) != -1) && (!(Concentration.GameWon)) && (Concentration.soundson)){
						if(count > 0){
							sourceDataLine.write(tempBuffer, 0, count);
						}
					}

					sourceDataLine.drain();
					sourceDataLine.close();
				}
			}

		}catch (Exception e) {
		}
	}
}


