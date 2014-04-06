import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import javax.swing.*;
import java.io.*;

/*
 * Class extending a JComponent 
 * - to draw the hierarchical geometry of a digger and 
 * - to respond to user mouse events
 * 
 * As a single model is used a display list is unnecessary.
 * (If we were to manipulate many diggers a display list would be appropriate.)
 * 
 * However this single model, a digger, is complex: it is
 * a hierarchical model, made of multiple shapes, where each shape
 * has connection(s)/relation(s) with the definition of another shape. 
 * For example, the bucket should not detach of the extended arm: a
 * pivot on the extended arm defines its location and the range of
 * permissible rotations.
 * 
 * In general a hierarchical model uses a DAG (Direct Acyclic Graph)
 * In our case the model is simple enough that each shape has a 
 * single child at the next level of the hierarchical structure.
 * Thus keeping a reference to the root shape, displayRoot, is sufficient, since
 * each shape maintains the reference to its single child. 
 * Starting from a reference to the root it is possible 
 *   - to draw the model (rendering) and
 *   - to determine a selected part (picking)
 *
 * In this provided code the shapes constituting the hierarchical model are 
 * rectangles, but object oriented techniques can be used to provide the same 
 * capability for a model composed of various shapes
 
 */

 class DrawingPanel extends JComponent implements MouseListener, MouseMotionListener {
    
   private static final String RECORD_FILE = "events";

   private final static Color BG = Color.WHITE;
   private final static Color BLUE = Color.BLUE;
   private final static Color RED = Color.RED;
   
   // The root of the model (the only shape in the hierarchy without parent)
   // MyRectangle constructor sets the parent of the shape (null for root)
   // MyRectangle addChild() method is used to connect a parent to its children 
   // (one in our case but could be extended to many)
   private MyRectangle displayRoot = null;
   private MyRectangle base = null;
   private MyRectangle scalarArm = null;

   // Handle transformation on the entire object, i.e. the digger
   private AffineTransform objectTransform = new AffineTransform();

   // Needed to reposition before replay
   // (Not possible across program execution as not stored in text file:
   // do your recording from the initial digger location.)
   private AffineTransform oldObjectTransform;
   
   // Used for simple selection
   // Click with button1 anywhere on JComponent selects the entire object (simple minded)
   // Provided code does not include checking if mouse click is inside a shape
   private static final int NONE = -2;
   private static final int ALL_OBJECT = -1;
   private static final int BASE = ALL_OBJECT;
   private static final int ROOT = ALL_OBJECT;
   private static final int SCALE_ARM = 1;
   private static final int BENT_ARM = 2;
   private static final int BUCKET = 3;
   private int selected = NONE;
   private int lastX, lastY;

   // Used for recording and replay
   private ObjectOutputStream out;
   private boolean isRecording = false;
   // Record some mouse events, may want to also record more according to
   // user interaction choices/definitions
   private ArrayList<MouseEvent> events;

   private static RenderingHints rh = new RenderingHints(
		RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

   public DrawingPanel() {
      super();
      setBackground(BG);
      addMouseMotionListener(this);
      addMouseListener(this);
      init();
      objectTransform.translate(Digger.FRAME_WIDTH/3, 2*Digger.FRAME_HEIGHT/3);//100.0, 100.0);
   }

   /*
    * Create a hierarchical object.
    * Provided code uses 2 simple rectangles, which are connected at
    * an anchor point (75, 25) on the parent rectangle, i.e. displayRoot.
    * The axis of the local coordinate system of the child rectangle, 
    * i.e. rect2, are rotated by -45 degrees
    * Try the Option menu, Debug On to see how the local coordinates systems
    * of the 2 rectangles relate
    */ 
   public void init() {
      AffineTransform trans = new AffineTransform();

      displayRoot =  new MyRectangle(trans, 200, 50, BLUE, null, 4);

     // AffineTransform trans2 = AffineTransform.getTranslateInstance(75.0, 25.0); //ANCHOR POINT
      AffineTransform trans2 = AffineTransform.getTranslateInstance(75.0, 25.0); //ANCHOR POINT
      // From API AffineTransform public void rotate method: 
      // "Rotating with a positive angle theta rotates points on the positive x axis 
      // (x+) toward the positive y axis (y+)". 
      // Since y+ is pointing down on the JComponent, a negative rotation has the 
      // effect of rotating upward
      // (reverse of trigonometry circle you may be used to since y+ is pointing down on screen) 
      trans2.rotate(-Math.PI/2.0);
      base = new MyRectangle(trans2, 100, 60, RED, displayRoot, 4);
      displayRoot.addChild(base);   
      AffineTransform trans3 = AffineTransform.getTranslateInstance(95.0, 0);
      trans3.rotate(Math.PI/8.0);
      MyRectangle scalarArm = new MyRectangle(trans3, 150, 50, BLUE, base, 4);
      base.addChild(scalarArm); 
      AffineTransform trans4 = AffineTransform.getTranslateInstance(95.0, 0);
      //trans4.rotate(Math.PI/8.0);
      //THE width and  height is currently preset so it doesn't matter what you pass it
      MyRectangle bentArm = new MyRectangle(trans4, 0, 0, BLUE, scalarArm, 6); 
      scalarArm.addChild(bentArm); 
      AffineTransform trans5 = AffineTransform.getTranslateInstance(100.0, 20.0);
      MyRectangle bucket = new MyRectangle(trans5, 50, 50, BLUE, bentArm, 5);
      bentArm.addChild(bucket);
   }

   public void paintComponent(Graphics g) {
      super.paintComponent(g);
      // From API The Graphics object should have the same state when 
      // you're finished painting as it had when you started. Therefore
      // either restore state (translate(x,y) and end with translate(-x,-y)
      //     or make a copy (easier)
      // Example of copying the Graphics object
      Graphics2D g2 = (Graphics2D)g.create(); //copy g

      g2.setRenderingHints(rh);

      g2.clearRect(0, 0, getWidth(), getHeight());
      
      g2.transform(objectTransform);
     

      // Start painting with displayRoot, which inside its paint
      // method paints its own children (single child in our case)
      if (displayRoot != null) {
         displayRoot.paint(g2);
         
      }
      g2.draw(new Rectangle2D.Double(71, 35, 50, 20));
      g2.dispose(); //release the copy's resources
   }

   /*
    * Create new list of events when recording is started
    */
   public void record() { 
      try {
         oldObjectTransform = new AffineTransform(objectTransform); 
         out = new ObjectOutputStream(new FileOutputStream(RECORD_FILE)); 
         isRecording = true;
         events = new ArrayList<MouseEvent>();
      } catch (IOException e) {
         System.out.println("Unable to open output stream for " + RECORD_FILE); 
      }  
   }
   
   /*
    * Before to stop recording write arraylist of recorded events in file 
    */
   public void stop() {
      if (isRecording) {
         try {  
            out.writeObject(events);
            objectTransform = new AffineTransform(oldObjectTransform); 
            isRecording = false; 
            out.close();
         } catch (IOException e) {
            System.out.println("Unable to write record file "); 
         }  
      }
   }

   /*
    * Stop recording before playing back, read in and process events by
    * calling back appropriate methods
    */ 
   public void play() { 
      stop();

      // Starting a new Thread so replay can be interrupted
      // However the replay is not done on the EDT (see below println)
      // (Enclosing replay() call in another Thread remove the interruption capabilities)
      new Thread( new Runnable() { 
	public void run() {

          try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(RECORD_FILE)); 
            ArrayList<MouseEvent> ev = (ArrayList<MouseEvent>) in.readObject();
            in.close(); 
            System.out.println("Is EDT? " + SwingUtilities.isEventDispatchThread());
   	    replay(ev);

          } catch (IOException exp) {
            System.out.println("Unable to open file: " + RECORD_FILE); 
          } catch (ClassNotFoundException exp2) {
            System.out.println(exp2); 
          }

	}
      }).start(); 

   }

   
    private void replay(ArrayList<MouseEvent> events) {

       for (MouseEvent e: events) {
          
          switch(e.getID()) {
             case MouseEvent.MOUSE_PRESSED:
                  mousePressed(e);
                  break;
             case MouseEvent.MOUSE_DRAGGED:
                  mouseDragged(e);
                  break;
            }

            // While a call to repaint() exists in mouseDragged(e), it is
            // essential to force a synchronous paint by calling paintImmediately
            // (with repaint() only paint requests are coalesced: teleportation effect)
             
            paintImmediately(this.getBounds());

            // To slow down by a constant time the re-execution of events.
            // Would be better to use event timestamps to sleep appropriately but not required. 
            try { 
	      Thread.sleep(40);  
	    } catch(InterruptedException ex) {
	      Thread.currentThread().interrupt();
	    }  
      } 
   }
   

   /*
    * When mouse is pressed with button1 the main object (the displayList one)
    * is selected
    */
   public void mousePressed(MouseEvent e) {
      
      if (isRecording)
         events.add(e);

      if (e.getButton() == MouseEvent.BUTTON1) {
         lastX = e.getX();
         lastY = e.getY();
         /*if(displayRoot.getChild(lastX,lastY) !=null){
        	 selected = -3;
         }
         else{*/
         selected = ALL_OBJECT;
      }
   }
   //public void findChild(int x, int y){}
   /*
    * When the main object is selected (see above) dragging the mouse 
    * drags the main object by updating the transformation applied to 
    * it (objectTransform)
    */
   
   public void mouseDragged(MouseEvent e){
      if (isRecording)
         events.add(e);
     
      	if (selected == BASE || selected == ROOT) {
      	      //  AffineTransform trans = AffineTransform.getTranslateInstance(e.getX() - lastX,e.getY() - lastY);
      	         /*if(lastY - e.getY() > 0){
      	            trans.translate(lastX,lastY);
      	            objectTransform.concatenate(trans);
      	         }
      	         if(lastY - e.getY() < 0){*/
      	        //	trans.translate(e.getX(),e.getY());
      	        //    objectTransform.concatenate(trans);
      	         //}
           
         }
         
         if (selected == BASE) {
    		
    		AffineTransform transTemp = base.getChild(0).getTrans();
    		if(lastY - e.getY() > 0){
    			transTemp.rotate(-Math.PI/90.0);
    		}
    		if(lastY - e.getY() < 0){
         
          transTemp.rotate(Math.PI/90.0);
          repaint();
    		}
    	}
    	
        if (selected == SCALE_ARM) {
    		//make the scaling keyboard controlled
    		AffineTransform transTemp = base.getChild().getTrans();
    		if(lastY - e.getY() > 0){
    			transTemp.scale(1.05,1);
    		}
    		if(lastY - e.getY() < 0){
    			//AffineTransform trans = AffineTransform.getTranslateInstance(e.getX() - lastX,e.getY() - lastY);
    			transTemp.scale(.95,1);
            }
        }
 
        repaint();
        lastX = e.getX();
        lastY = e.getY();
      
   }

   /*
    * When mouse is released (any button, so also for button1), nothing
    * becomes selected
    */
   public void mouseReleased(MouseEvent e) {
      selected = NONE;
   }

   public void mouseMoved(MouseEvent e){}
   public void mouseClicked(MouseEvent e){}
   public void mouseExited(MouseEvent e){}
   public void mouseEntered(MouseEvent e){}
   
}


