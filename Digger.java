/*
 * CS341 - Direct Manipulation 
 * Starting code / Example code 
 * This code can be not used, if preferred, its purpose is to give some hints.
 * It needs to be made more modular/object oriented to handle 
 *    - composition of different shapes and 
 *    - possibly more complex recording of all types of events.
 * Version 1: Elodie Fourquet, July 2005
 * Some pieces are extracted from:
 * 1. Java Swing Tutorial http://java.sun.com/docs/books/tutorial/uiswing/
 * 2. Java 2D Graphics Tutorial http://java.sun.com/docs/books/tutorial/2d/
 */ 

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/*
 * Digger application class that creates the main frame
 */
public class Digger extends JFrame {
   
   public static final int FRAME_WIDTH = 500;
   public static final int FRAME_HEIGHT = 500;


   private DrawingPanel panel;

   /* 
    * Constructor that creates
    *    - a frame with title, 
    *    - a menubar and 
    *    - a DrawingPanel (JComponent) in the center: it contains the digger 
    *      geometry and affordances
    */
   public Digger(String title) {

      setTitle(title);
      JMenuBar menuBar = createMenuBar(); 
      setJMenuBar(menuBar);//with file n etc drop down options

      panel = new DrawingPanel();
      getContentPane().add(panel, BorderLayout.CENTER);
      panel.requestFocus();	
   }

   /*
    * Create the menubar that
    * - controls record and play of events from user interaction on JPanel, 
    * - includes a quit command with shortcut  
    */
   private JMenuBar createMenuBar() {
     
      JMenuBar menuBar = new JMenuBar();
      JMenu menu = new JMenu("File");; 
      menuBar.add(menu); 
    
      JRadioButtonMenuItem record = new JRadioButtonMenuItem("Record");
      menu.add(record);
      record.addActionListener(new RecordListener(record));
      record.setSelected(false);
    
      JMenuItem item = new JMenuItem("Play");
      menu.add(item);
      item.addActionListener(new PlayListener());

      JMenuItem item2 = new JMenuItem("Quit");
      // This accelerator does not handle the upper-case version: 'Q' 	
      // Alternate implementation: addKeyListener with new KeyAdapter
      item2.setAccelerator(KeyStroke.getKeyStroke('q'));
      menu.add(item2);
      item2.addActionListener(new QuitListener());

      JMenu menu2 = new JMenu("Options");
      menuBar.add(menu2); 

      JRadioButtonMenuItem debug = new JRadioButtonMenuItem("Debug");
      menu2.add(debug);
      debug.setAccelerator(KeyStroke.getKeyStroke('d'));
      debug.addActionListener(new DebugListener(debug));
      debug.setSelected(true);

      return menuBar;

   }

   private static void createAndShowGUI() {

     // Create and set up the window.
     Digger frame = new Digger("My Digger");
     frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
     
     // Set size and position of frame and display it
     frame.setSize(FRAME_WIDTH, FRAME_HEIGHT);
     Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
     frame.setLocation(screenSize.width/2 - FRAME_WIDTH/2,
                      screenSize.height/2 - FRAME_HEIGHT/2); //so you set the frame for GUI on  diff part of the screen
     frame.setVisible(true);

   }
  
   public static void main(String[] args) {
      
      // Schedule a job for the event-dispatching thread:
      // creating and showing this application's GUI.
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
         public void run() {
            createAndShowGUI();
        }
      });
   }

   
  /*
   * Four inner classes that implement ActionListeners for the menus 
   * of the frame
   */ 
   class RecordListener implements ActionListener { 
      JRadioButtonMenuItem control;

      public RecordListener(JRadioButtonMenuItem c) {
         control = c;
      } 

      public void actionPerformed(ActionEvent e) {
        
         if (control.isSelected()) { 
            panel.record();
            System.out.println("record started");
         } else {
            System.out.println("record stoped"); 
         }
      }
   }

   class PlayListener implements ActionListener { 
    
      public void actionPerformed(ActionEvent e) {
         panel.play();
      }
   }

   class QuitListener implements ActionListener { 
    
      public void actionPerformed(ActionEvent e) {
         panel.stop();
         System.exit(0); 
      }
   }


   class DebugListener implements ActionListener { 
      JRadioButtonMenuItem control;

      public DebugListener(JRadioButtonMenuItem c) {
         control = c;
      }
    
      public void actionPerformed(ActionEvent e) {
        
         if (control.isSelected()) { 
            MyRectangle.setDebug(true);
         } else
            MyRectangle.setDebug(false);
         
	 repaint();
       }

   }

}
