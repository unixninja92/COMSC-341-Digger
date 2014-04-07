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

 class DrawingPanel extends JComponent implements MouseListener, MouseMotionListener, KeyListener {
    
   private static final String RECORD_FILE = "events";

   private final static Color BG = Color.WHITE;
   private final static Color BLUE = Color.BLUE;
  private final static Color YELLOW = Color.yellow;
   
   // The root of the model (the only shape in the hierarchy without parent)
   // MyRectangle constructor sets the parent of the shape (null for root)
   // MyRectangle addChild() method is used to connect a parent to its children 
   // (one in our case but could be extended to many)
   private MyRectangle displayRoot = null;
   private MyRectangle base = null;
   private MyRectangle scalarArm = null;
   private MyRectangle bentArm = null;
   private MyRectangle bucket = null;

   private Polygon bottle;
   private MyRectangle selectedRect = null;

   // Handle transformation on the entire object, i.e. the digger
   private AffineTransform objectTransform = new AffineTransform();

   // Needed to reposition before replay
   // (Not possible across program execution as not stored in text file:
   // do your recording from the initial digger location.)
   private AffineTransform oldObjectTransform;

   private boolean rotated;
   
   private int frameNumber; // For animation, increases by 1 in each frame.
   
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

  private static final int WHEEL=70;

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
      addKeyListener(this);
      requestFocus();
      setFocusable(true);
      init();
      objectTransform.translate(Digger.FRAME_WIDTH/3, 2*Digger.FRAME_HEIGHT/3);//100.0, 100.0);
      rotated = false;

      
     /*  new Timer(60,new ActionListener() {
          public void actionPerformed(ActionEvent evt) {
             frameNumber++;
             repaint();
          }
       }).start();*/
       addMouseListener(this);
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

      displayRoot =  new MyRectangle(trans, 200, 100, YELLOW, null, 5, ROOT);
      

     // AffineTransform trans2 = AffineTransform.getTranslateInstance(75.0, 25.0); //ANCHOR POINT
      AffineTransform trans2 = AffineTransform.getTranslateInstance(75.0, 25.0); //ANCHOR POINT
      // From API AffineTransform public void rotate method: 
      // "Rotating with a positive angle theta rotates points on the positive x axis 
      // (x+) toward the positive y axis (y+)". 
      // Since y+ is pointing down on the JComponent, a negative rotation has the 
      // effect of rotating upward
      // (reverse of trigonometry circle you may be used to since y+ is pointing down on screen) 
      trans2.rotate(-Math.PI/2.0);
      base = new MyRectangle(trans2, 100, 40, YELLOW, displayRoot, 4, BASE);
      displayRoot.addChild(base);   
      AffineTransform trans3 = AffineTransform.getTranslateInstance(95.0, 0);
      trans3.rotate(Math.PI/8.0);
      scalarArm = new MyRectangle(trans3, 150, 40, YELLOW, base, 4, SCALE_ARM);
      base.addChild(scalarArm); 
      AffineTransform trans4 = AffineTransform.getTranslateInstance(135.0, 0);
      //trans4.rotate(Math.PI/8.0);
      //THE width and  height is currently preset so it doesn't matter wat you pass it
      bentArm = new MyRectangle(trans4, 0, 0, YELLOW, scalarArm, 6, BENT_ARM); 
      scalarArm.addChild(bentArm); 
      AffineTransform trans5 = AffineTransform.getTranslateInstance(40.0, 150.0);
     // trans5.rotate(-Math.PI);
      bucket = new MyRectangle(trans5, 50, 50, Color.LIGHT_GRAY, bentArm, 0, BUCKET);
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
      
      g2.clearRect(0, 0, getWidth(), getHeight());
      g2.setColor( new Color(200,200,255) );
      g2.fillRect(0,0,900,600);  // fills the sky, behind the dunes
      
      g2.setColor( new Color(251,193,115) );
      g2.fillRect(0,100,900,800);  // the sand
   
      g2.fill(new Arc2D.Double(100, 70, 80, 100, 0, 180, Arc2D.PIE));//sand dunes  
      g2.fill(new Arc2D.Double(700, 70, 120, 100, 0, 180, Arc2D.PIE));
      g2.fill(new Arc2D.Double(220, 70, 80, 100, 0, 180, Arc2D.PIE)); 
      g2.fill(new Arc2D.Double(400, 80, 100, 150, 0, 180, Arc2D.PIE));

      
       AffineTransform t = AffineTransform.getTranslateInstance(0, 100);
	  g2.transform(t);
	  drawBottle(g2);
	  
	 try {
			g2.transform(t.createInverse());
		} catch (NoninvertibleTransformException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
     
      
      g2.transform(objectTransform);
      drawWindow(g2);
     
      // Start painting with displayRoot, which inside its paint
      // method paints its own children (single child in our case)
      if (displayRoot != null) {
         displayRoot.paint(g2); 
      }
       g2.draw(new Rectangle2D.Double(71, 35, 50, 20));
      drawWheel(g2);
      
       AffineTransform current = g2.getTransform();
     try {
		AffineTransform inv = current.createInverse();
		g2.transform(inv);
	} catch (NoninvertibleTransformException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	drawClouds(g2);
      
      g2.dispose(); //release the copy's resources
   }

   private void drawWindow(Graphics2D g2){
	  Polygon poly = new Polygon();
	  
	  poly.addPoint(20,-WHEEL*2);
	  poly.addPoint(0,40);
	  poly.addPoint(180,40);
	  poly.addPoint(80,-WHEEL*2);
	
	  g2.setColor(Color.LIGHT_GRAY);
	  
	  g2.fill(poly);
	  
	  g2.setColor(Color.DARK_GRAY);
	  g2.setStroke( new BasicStroke(3) );
	  g2.draw(poly);
	  
	  g2.setStroke( new BasicStroke(10) );
	  g2.setColor(YELLOW);
	  g2.drawLine(20,-WHEEL*2,80,-WHEEL*2);
	  
   }
   
	 private void drawWheel(Graphics2D g2) {

		 g2.setColor(Color.BLACK);
	      g2.fill( new Ellipse2D.Double(15,WHEEL-25,80,80) );
	      g2.fill( new Ellipse2D.Double(155,WHEEL,60,60) );

	      g2.setColor(Color.ORANGE);
	      g2.fill( new Ellipse2D.Double(25,WHEEL-15,60,60) );
	      g2.fill( new Ellipse2D.Double(165,WHEEL+10,40,40) );

	   }
	   
	   private void drawBottle(Graphics2D g2){
		 bottle = new Polygon();

		  bottle.addPoint(400,80);
		  bottle.addPoint(400,90);
		  bottle.addPoint(390,100);
		  bottle.addPoint(390,110);
		 
		  bottle.addPoint(415,110);
		  bottle.addPoint(415,100);
		  bottle.addPoint(405,90);
		  bottle.addPoint(405,80);
		 
		 g2.setColor(Color.white);
		 g2.fill(bottle);
	 }
	 
	  public void drawClouds(Graphics2D g2){
	   g2.setColor(Color.WHITE);
	   
	   g2.fill( new Ellipse2D.Double(500,10,10,10) );//stationary clouds
	   g2.fill( new Ellipse2D.Double(505,5,15,15) );
	   g2.fill( new Ellipse2D.Double(515,10,10,10) );
	   
	   g2.fill( new Ellipse2D.Double(200,20,15,15) );//stationary clouds
	   g2.fill( new Ellipse2D.Double(210,10,25,25) );
	   g2.fill( new Ellipse2D.Double(230,20,15,15) );
	   
	   
	   g2.translate(-50+30*(frameNumber % 300) / 10, 0);//animation for moving clouds
	   g2.fill( new Ellipse2D.Double(5,10,10,10) );
	   g2.fill( new Ellipse2D.Double(10,5,15,15) );
	   g2.fill( new Ellipse2D.Double(20,10,10,10) );
	   
	   g2.fill( new Ellipse2D.Double(100,20,10,10) );
	   g2.fill( new Ellipse2D.Double(105,15,15,15) );
	   g2.fill( new Ellipse2D.Double(115,20,10,10) );
	   
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
      Point2D pInv;
      if (isRecording)
         events.add(e);

        // System.out.println("x:" +e.getX()+" y:"+e.getY());
      if (e.getButton() == MouseEvent.BUTTON1) {
        
        try {
          pInv = objectTransform.inverseTransform(new Point2D.Double(e.getX(), e.getY()), null);
        } catch (NoninvertibleTransformException er) {
          er.printStackTrace();
          return;
        }

        selectedRect = displayRoot.selectedShape(pInv);
        if(selectedRect != null){
          selectedRect.selected=true;
          selected = selectedRect.objectType;
          System.out.println("selected: "+selected);
        }
        else{
          selected = NONE;
        }
        lastX = e.getX();
        lastY = e.getY();
         // if(displayRoot.getChild(lastX,lastY) !=null){
        	//  selected = -3;
         // }
         // else{
         // selected = ALL_OBJECT;
      }
   }
   
   
   public void mouseDragged(MouseEvent e){
      if (isRecording)
         events.add(e);
      
      Point2D pInv;

        try {
          pInv = objectTransform.inverseTransform(new Point2D.Double(bottle.getBounds2D().getCenterX(),bottle.getBounds2D().getCenterX()), null);
        } catch (NoninvertibleTransformException er) {
          er.printStackTrace();
          return;
        }

      if (selected == BASE || selected == ROOT) {
         AffineTransform trans = AffineTransform.getTranslateInstance(e.getX() - lastX,e.getY() - lastY);
         objectTransform.concatenate(trans);
      }
      else if (selected == BENT_ARM) {
    		AffineTransform transTemp = bentArm.getTrans();//testing the rotation
    		if(lastY - e.getY() > 0){
    			transTemp.rotate(-Math.PI/90.0);
    		}
    		if(lastY - e.getY() < 0){
    			transTemp.rotate(Math.PI/90.0);
    		}
    	}
  	
      else if (selected == SCALE_ARM) {
  		//scalnig keyboard controlled
    		AffineTransform transTemp = base.getChild().getTrans();
    		if(lastY - e.getY() > 0){
    			transTemp.rotate(-Math.PI/90.0);
    		}
    		if(lastY - e.getY() < 0){
    			transTemp.rotate(Math.PI/90.0);
    		}
      }
  	
      else if (selected == BUCKET) {
  		//make the scaling keyboard controlled
    		AffineTransform transTemp = bucket.getTrans();
    		if(lastY - e.getY() > 0){
    			transTemp.rotate(-Math.PI/90.0);
    			//I believe for this code to work the bottle bounds must be pushed through the inverse trans
    			if(bucket.getBody().contains(pInv)){
    	    		   //bottle.reset();
    	    		   System.out.println("You grabbed bottle!");
    	    	  }
    		}
    		if(lastY - e.getY() < 0){
    			transTemp.rotate(Math.PI/90.0);
    			if(bucket.getBody().contains(pInv)){
 	    		   //bottle.reset();
 	    		   System.out.println("You grabbed bottle!");
    			}
    		}
    	  
      }else{
    	  selected = NONE;
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
      selectedRect = null;
   }

  public void mouseMoved(MouseEvent e){}
  public void mouseClicked(MouseEvent e){
    if(e.getClickCount() == 2){
      System.out.println("double click");
      AffineTransform trans = new AffineTransform(new double[] {-1.0,0.0,0.0,1.0});
      objectTransform.concatenate(trans); 
      rotated = !rotated;
      repaint();
    }
  }
  public void mouseExited(MouseEvent e){}
  public void mouseEntered(MouseEvent e){}

  @Override
  public void keyPressed(KeyEvent e) {
  	System.out.println(e.getKeyChar());  
  	System.out.println("slected: "+selected);
  	  if(selected == SCALE_ARM){
  		  AffineTransform transTemp = scalarArm.getTrans();
  		  AffineTransform limitTemp = scalarArm.getChild().getTrans();
  		  if(e.getKeyChar() == 'l'){
          System.out.println("Pressed l");
  			  transTemp.scale(1.05,1);
  			  limitTemp.scale(.95, 1);
  		  }
  		  else if(e.getKeyChar() == 'k'){
  			  transTemp.scale(.95,1);
  			  limitTemp.scale(1.05, 1);
  		  }
  		  repaint();
  	  }	
  	
  }

  @Override
  public void keyReleased(KeyEvent e) {
  	// TODO Auto-generated method stub
  	
  }

  @Override
  public void keyTyped(KeyEvent e) {
  	// TODO Auto-generated method stub
  	
  }
   
}


