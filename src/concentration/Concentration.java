package concentration;


import java.util.ArrayList;
import java.util.Random;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.*; 

import java.awt.*;
import java.awt.image.*;

import javax.imageio.*;
import javax.sound.sampled.*;

import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import javax.swing.Timer;

public class Concentration extends JFrame {

	private static final long serialVersionUID = 1L;
	public static BufferStrategy strategy;

	public static boolean fullscreen = true;
	public static boolean soundson = true;

	public static boolean blackbackground = false;  
	public static boolean whitecard = false;  
	static int youwinInterval,youwinDuration, youwincounter;
	public static float heightfraction, fraction;
	public static Timer youwintimer;
	public static int howmanyshowing;
	public static ArrayList<Card> mycards = new ArrayList<Card>();
	public static ArrayList<Card> CardsShowing = new ArrayList<Card>();
	static Concentration mainwindow;
	public static BufferedImage background;
	public static BufferedImage youwin;
	public static BufferedImage img;
	public static BufferedImage soundchoice, soundon, soundoff, windowchoice, windowsmall, windowlarge, close;
	public static boolean GameWon = false;
	public static String userdir= (new File(Concentration.class.getProtectionDomain().getCodeSource().getLocation().getPath())).getAbsolutePath();

	//public static String userdir = (new File(".")).getAbsolutePath();

	public static AudioFormat audioFormat;
	public static AudioInputStream audioInputStream;
	public static SourceDataLine sourceDataLine;


	public static File soundtrack;
	public static File flip;
	public static File soundyouwin;
	public static File rightchoice;
	public static File wrongchoice;

	public static String OptionalError = "";
	public static String NecessaryError = "";
	public static int soundsplaying = 0;

	public static void main(String[] args) {  
		boolean goon = false;	
		while (!(goon)) {
			goon = NecessaryFileCheck();
		}	

		goon = false;
		while (!(goon)) {
			goon = OptionalFileCheck();
		}

		String theconfigfile = userdir + System.getProperty("file.separator") + "config.txt";
		mycards = new ArrayList<Card> (makecards(theconfigfile));
		ImageLoad();
		new PlayBackground().start();
		mainwindow = new Concentration();
	}


	public static boolean OptionalFileCheck() {
		OptionalError = "";
		soundtrack = new File(userdir + System.getProperty("file.separator") + "soundtrack.wav");
		if (!(soundtrack.exists())) {
			OptionalError = OptionalError + "soundtrack.wav\n";		  
		}
		flip = new File(userdir + System.getProperty("file.separator") + "flip.wav");
		if (!(flip.exists())) {
			OptionalError = OptionalError + "flip.wav\n";
		}
		soundyouwin = new File(userdir + System.getProperty("file.separator") + "youwin.wav");
		if (!(soundyouwin.exists())) {
			OptionalError = OptionalError + "youwin.wav\n";
		}
		rightchoice = new File(userdir + System.getProperty("file.separator") + "rightchoice.wav");
		if (!(rightchoice.exists())) {
			OptionalError = OptionalError + "rightchoice.wav\n";
		}
		wrongchoice = new File(userdir + System.getProperty("file.separator") + "wrongchoice.wav");
		if (!(wrongchoice.exists())) {
			OptionalError = OptionalError + "wrongchoice.wav\n";
		}
		File backgroundloc =  new File(userdir + System.getProperty("file.separator") + "background.jpg");
		if (!(backgroundloc.exists())) {
			blackbackground = true;
			OptionalError = OptionalError + "background.jpg\n";
		}
		else {
			blackbackground = false;
		}
		File imgloc =  new File(userdir + System.getProperty("file.separator") + "cardback.jpg");
		if (!(imgloc.exists())) {
			OptionalError = OptionalError + "cardback.jpg\n";
			whitecard = true;
		} 
		File youwinloc =  new File(userdir + System.getProperty("file.separator") + "youwin.gif");
		if (!(youwinloc.exists())) {
			OptionalError = OptionalError + "youwin.jpg\n";
		} 

		if (OptionalError != "" ) {

			Object[] options = {"Ignore", "Exit", "Locate Directory"};
			int choice = JOptionPane.showOptionDialog(null, "The program is having trouble locating some optional files:\n" + OptionalError, "Missing Optional Files", 
					JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
			switch (choice) {
			case 0: 
				return true;
			case 1:
				System.exit(0);
				break;
			case 2:
				JFileChooser fc = new JFileChooser();
				fc.setCurrentDirectory(new File(userdir));
				fc.setDialogTitle("Choose Directory");
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int returnVal = fc.showDialog(null, "Choose Directory");
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					userdir = fc.getSelectedFile().getAbsolutePath();
				}
				return false;
			default:
				return true;
			}
		}
		return true;
	}

	public static boolean NecessaryFileCheck() {
		NecessaryError = "";
		File configfileloc = new File(userdir + System.getProperty("file.separator") + "config.txt");
		if (!(configfileloc.exists())) {
			NecessaryError = NecessaryError + "config.txt missing\n";
		}
		else {
			try {
				String str;
				int count = 0; 
				BufferedReader in = new BufferedReader(new FileReader(configfileloc));
				while ((str = in.readLine()) != null) {
					count++;
				}
				in.close();
				if (count % 2 == 1){
					NecessaryError = NecessaryError + "config.txt has an uneven number of lines\n";    	  
				}
			} catch (IOException e) {}
		}

		if (NecessaryError != "" ) {
			Object[] options = {"Change Directory", "Exit"};
			int choice = JOptionPane.showOptionDialog(null, "The program is having problems:\n" + NecessaryError, "Error",
					JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,
					null, options, options[0]);

			if (choice == 0)
			{	  
				JFileChooser fc = new JFileChooser();
				fc.setCurrentDirectory(new File(userdir));
				fc.setDialogTitle("Choose Directory");
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int returnVal = fc.showDialog(null, "Choose Directory");
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					userdir = fc.getSelectedFile().getAbsolutePath();
				}
			}
			if (choice == 1)
			{
				System.exit(0);
			}  
			return false;
		}
		return true;
	}


	public static ArrayList<Card> makecards (String fileloc){
		String str;
		ArrayList<tempreadin> mytempcards = new ArrayList<tempreadin>();
		int k;
		int i;
		boolean imageorelsetext;
		String cardcontents;


		try {
			BufferedReader in = new BufferedReader(new FileReader(fileloc));
			while ((str = in.readLine()) != null) {
				mytempcards.add(new tempreadin(str));
			}
			in.close();
		} catch (IOException e) {
		}

		Random randomGenerator = new Random();

		while ((mytempcards.size()) > 0) {
			i = randomGenerator.nextInt(mytempcards.size());
			str = mytempcards.get(i).string;


			if (str.startsWith("file: ")) {

				imageorelsetext = true;
				cardcontents = str.replaceAll("^file: ", "");
			}
			else {
				imageorelsetext = false;
				cardcontents = str;
			}

			k = mytempcards.get(i).matchnumber;
			mycards.add(new Card(imageorelsetext, cardcontents, k));
			mytempcards.remove(i);
		}
		return mycards;
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
		if (fullscreen) {
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			setTitle("Concentration");
			setUndecorated(true);
			setResizable(false);
			GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(this);
			addMouseListener(new HandleMouse());
			setVisible(true);
			createBufferStrategy(2);
			strategy = getBufferStrategy();
			repaint();
		}
		else {
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			setTitle("Concentration");
			setSize(700,500);
			setLocationRelativeTo(null);
			addMouseListener(new HandleMouse());
			setVisible(true);
			createBufferStrategy(2);
			strategy = getBufferStrategy();
			repaint();
		}
	}

	public static void ImageLoad () {
		if (blackbackground) {
			background = new BufferedImage(1, 1,  BufferedImage.TYPE_INT_RGB);
			Graphics gback = background.createGraphics();
			gback.setColor(new Color(0, 128, 0));
			gback.fillRect (0, 0, 1, 1);
		}
		else {
			try {
				background = ImageIO.read(new File(userdir + System.getProperty("file.separator") + "background.jpg"));
			} catch (IOException e) {}
		}
		if (whitecard) {
			img = new BufferedImage(1, 1,  BufferedImage.TYPE_INT_RGB);
			Graphics gback = img.createGraphics();
			gback.setColor(Color.DARK_GRAY);
			gback.fillRect (0, 0, 1, 1);
		}
		else {
			try {
				img = ImageIO.read(new File(userdir + System.getProperty("file.separator") + "cardback.jpg"));
			} catch (IOException e) {}
		}
		try {
			youwin = ImageIO.read(new File(userdir + System.getProperty("file.separator") + "youwin.gif"));
		} catch (IOException e) {
		}


		try {
			close = ImageIO.read(Concentration.class.getResource("close.jpg"));
			soundon = ImageIO.read(Concentration.class.getResource("musicon.jpg"));
			soundoff = ImageIO.read(Concentration.class.getResource("musicoff.jpg"));
			windowsmall = ImageIO.read(Concentration.class.getResource("windowsmall.jpg"));
			windowlarge = ImageIO.read(Concentration.class.getResource("windowlarge.jpg"));
		} catch (IOException e) {
		}
		for(Card s : mycards) {
			s.setimage();
		}
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
		try {
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
		catch (Exception e) {}

	}

	public static void youwin () {

		youwinInterval = 10;
		youwintimer = new Timer(youwinInterval, youwingraphic);
		youwinDuration = 600;
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



	public static class HandleMouse extends MouseAdapter { 

		public void mouseReleased(MouseEvent e) {
			int xclick = e.getX();
			int yclick = e.getY();

			if (fullscreen) {
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
				tempreadin.cardnumber = 0;
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

class PlayThread extends Thread{

	private File playFile;
	public PlayThread(File s){
		playFile = s;
	}

	byte tempBuffer[] = new byte[10000];

	public void run(){
		try{
			System.out.println("playing sound");
			AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(playFile);
			AudioFormat audioFormat = audioInputStream.getFormat();
			DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);

			SourceDataLine sourceDataLine = (SourceDataLine)AudioSystem.getLine(dataLineInfo);
			sourceDataLine.open(audioFormat);
			sourceDataLine.start();

			int count;
			while((count = audioInputStream.read(tempBuffer,0,tempBuffer.length)) != -1){
				if(count > 0){
					sourceDataLine.write(tempBuffer, 0, count);
				}
			}

			sourceDataLine.drain();
			sourceDataLine.close();

		}catch (Exception e) {
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


class WaitTime implements Runnable {
	static boolean GoOn = true; 
	Thread t;
	WaitTime() {
		t = new Thread(this);
		t.start();
	}
	public void run() {
		try {
			Thread.sleep(2000);
			Concentration.playgame();
			Concentration.mainwindow.repaint();
		} catch (InterruptedException e) {}
	}
}

class tempreadin {
	public static int cardnumber = 0;
	public String string;
	public int matchnumber; 
	public tempreadin(String str) {
		string = str;
		matchnumber = (int)(cardnumber / 2);
		cardnumber++;
	}
}


class Card {
	public Graphics gCard, gToShow;
	public BufferedImage CardImage, ToShow;

	public boolean imageorelsetext;
	public String cardcontents;
	public int matchnumber;
	public int cardnumber;


	public boolean stillremaining;

	public static int showcard1;
	public static int showcard2;
	public static int matchedcards = 0;
	public static int totalcards = 0;
	public static int totalrows;
	public static int totalcolumns;
	public static int SquareRoot;
	public static int lengthconstant;
	public float dissolvefraction = 1;
	public float fraction;



	public int row;
	public int column;
	public int xposition, yposition;

	public Timer flipcard, putitback, dissolve;
	int flipcounter, putbackcounter, dissolvecounter;
	int PutItBackInterval, PutItBackDuration;
	int flipInterval, flipDuration;
	int dissolveInterval, dissolveDuration;
	int shadowx, newcardx, newcardy, newcardw;



	public Card(boolean a, String b, int c) {
		imageorelsetext = a;
		cardcontents = b;
		matchnumber = c;
		stillremaining = true;
		totalcards++;
		cardnumber = totalcards;
		totalrowsandcolumns();
		ToShow = Concentration.img; 
	}

	public void totalrowsandcolumns() { 
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

	public void matched() { 
		stillremaining = false;
		matchedcards++;
	}

	public void setimage () {

		CardImage = new BufferedImage(400, 600,  BufferedImage.TYPE_INT_RGB);
		gCard = CardImage.createGraphics();
		gCard.setColor(Color.white);
		gCard.fillRect (0, 0, 400, 600);

		int maxwidth = 350;
		int maxheight = 550;
		FontMetrics fm = gCard.getFontMetrics();

		if (imageorelsetext) {  
			try {
				BufferedImage ImageEmbed = ImageIO.read(new File(Concentration.userdir + System.getProperty("file.separator") + cardcontents));
				int imageheight = ImageEmbed.getHeight();
				int imagewidth = ImageEmbed.getWidth();
				if ((imagewidth/350) > (imageheight/550)) {
					imageheight = ((350 * imageheight) / imagewidth);
					imagewidth = 350;    	
					gCard.drawImage(ImageEmbed, 25, ((600 - imageheight) / 2), 350, imageheight, null);

				}
				else {
					imagewidth = ((550 * imagewidth) / imageheight);
					imageheight = 550;
					gCard.drawImage(ImageEmbed, ((400 - imagewidth) / 2), 25, imagewidth, 550, null);

				}
			} catch (IOException e) { e.printStackTrace(); }		
		}
		else {
			int height = 600;
			int width = 400;
			int fontsize = 200;
			String[] text = cardcontents.split(" ");
			((Graphics2D) gCard).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			gCard.setColor(Color.black);
			gCard.setFont(new Font("ComicSansMS",Font.BOLD,fontsize));

			while ((height >= maxheight) || (width >= maxwidth)) {
				width = 0;
				fontsize -= 5;
				gCard.setFont(new Font("ComicSansMS",Font.BOLD,fontsize));
				fm = gCard.getFontMetrics();
				for (String s : text) {
					if (fm.stringWidth(s) > width) {
						width = fm.stringWidth(s);
					}
				}
				height = ((fm.getHeight()) * text.length);
			}

			int i = 0;
			for(String s : text){
				gCard.drawString(s, ((400 - fm.stringWidth(s)) / 2), (int)(((600 - height) / 2) + ((i + 0.6) * fm.getHeight())) );
				i++;	  }  
		}
	}

	public void flipcardover () {
		flipInterval = 30;
		flipcard = new Timer(flipInterval, flipping);
		flipDuration = 350;
		ToShow = Concentration.img;
		flipcard.start();           
	}

	ActionListener flipping = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			long currentflipTime = flipInterval * flipcounter;

			fraction = (float)currentflipTime / flipDuration;
			fraction = Math.min(fraction, 1.0f);
			if ((flipcounter * flipInterval) >= flipDuration) {
				flipcounter = 0;
				Concentration.mainwindow.repaint();
				Card.this.flipcard.stop();
			}

			//newcardx = (int)(getxpos() + (((2 * lengthconstant()) - Math.abs((2 * lengthconstant()) - (fraction * (4 * lengthconstant())))) / 2) - (fraction * (0.5 * lengthconstant())));
			//newcardy = (int)(getypos() - (fraction * (0.5 * lengthconstant())));
			//shadowx = (int)(getxpos() + (((2 * lengthconstant()) - Math.abs((2 * lengthconstant()) - (fraction * (4 * lengthconstant())))) / 2));	        		        
			//newcardw = (int)(Math.abs((2 * lengthconstant()) - (fraction * (4 * lengthconstant()))));

			if (fraction >= 0.5) {
				ToShow = CardImage;
			}	
			else {
				ToShow = Concentration.img;
			}
			Concentration.mainwindow.repaint();
			flipcounter++;
		}
	};

	public int getnewcardx () {
		newcardx = (int)(getxpos() + (((2 * lengthconstant()) - Math.abs((2 * lengthconstant()) - (fraction * (4 * lengthconstant())))) / 2) - (fraction * (0.5 * lengthconstant())));
		return newcardx;
	}
	public int getnewcardy () {
		newcardy = (int)(getypos() - (fraction * (0.5 * lengthconstant())));
		return newcardy;
	}
	public int getshadowx () {
		shadowx = (int)(getxpos() + (((2 * lengthconstant()) - Math.abs((2 * lengthconstant()) - (fraction * (4 * lengthconstant())))) / 2));	        
		return shadowx;
	}
	public int getnewcardw () {
		newcardw = (int)(Math.abs((2 * lengthconstant()) - (fraction * (4 * lengthconstant()))));
		return newcardw;
	}


	public void putitback () {

		PutItBackInterval = 30;
		putitback = new Timer(PutItBackInterval, putback);
		PutItBackDuration = 350;
		putitback.start();

	}

	ActionListener putback = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			long currentputbackTime = PutItBackInterval * putbackcounter;

			fraction = (float)currentputbackTime / PutItBackDuration;
			fraction = 1 -fraction;
			fraction = Math.max(fraction, 0f);

			if ((putbackcounter * PutItBackInterval) >= PutItBackDuration) {
				putbackcounter = 0;
				Concentration.CardsShowing.remove(Card.this);
				Concentration.mainwindow.repaint();
				Card.this.putitback.stop();
			}

			//newcardx = (int)(getxpos() + (((2 * lengthconstant()) - Math.abs((2 * lengthconstant()) - (fraction * (4 * lengthconstant())))) / 2) - (fraction * (0.5 * lengthconstant())));
			//newcardy = (int)(getypos() - (fraction * (0.5 * lengthconstant())));
			//shadowx = (int)(getxpos() + (((2 * lengthconstant()) - Math.abs((2 * lengthconstant()) - (fraction * (4 * lengthconstant())))) / 2));	        
			//newcardw = (int)(Math.abs((2 * lengthconstant()) - (fraction * (4 * lengthconstant()))));

			if (fraction >= 0.5) {
				ToShow = CardImage;
			}	
			else {
				ToShow = Concentration.img;
			}
			Concentration.mainwindow.repaint();
			putbackcounter++;
		}
	};

	public void dissolve () {

		dissolveInterval = 30;
		dissolve = new Timer(dissolveInterval, dissolveit);
		dissolveDuration = 500;
		dissolve.start();
	}

	ActionListener dissolveit = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			long currentdissolveTime = dissolveInterval * dissolvecounter;
			float fraction = (float)currentdissolveTime / dissolveDuration;

			if ((dissolvecounter * dissolveInterval) >= dissolveDuration) {
				dissolvecounter = 0;
				//what needs to go here???
				Card.this.matched();
				Concentration.CardsShowing.remove(Card.this);
				Concentration.mainwindow.repaint();
				if (matchedcards == totalcards) {
					Concentration.GameWon = true;
					Concentration.youwin();
					Concentration.playAudio(Concentration.soundyouwin);
					System.out.println("You Won!");
				}
				Card.this.dissolve.stop();
			}

			dissolvefraction = 1 - fraction;

			dissolvecounter++;			        
			Concentration.mainwindow.repaint();
		}
	};
}