import java.awt.*;
import java.awt.geom.*;
import java.util.*;

public class MyRectangle extends Rectangle2D.Float { 
   
   private static final Color GREEN = Color.GREEN;
   private static final Color ORANGE = Color.ORANGE;
   private final static BasicStroke wideStroke = new BasicStroke(5.0f);
   private static final int COORD_LENGTH = 5;

   private Color color;
   private AffineTransform trans  = new AffineTransform();
   private MyRectangle parent;
   private ArrayList<MyRectangle> children;

   
   private static boolean debug = true;

   public static void setDebug(boolean b) {
      debug = b;
      System.out.println("debug called");
   }

   /*
    * The constructor of MyRectangle assigns the following fields that
    * define the rectangle geometry and bare appearance. 
    * - An AffineTransform trans that changes the coordinate system
    * of the Graphics2D context prior to draw the shape
    * - A width, a height and a color and
    * - A parent (the root has parent==null)
    * Notice: without AffineTransform trans it is impossible to draw 
    * a rotated rectangle.
    *
    * The AffineTransform of trans of a MyRectangle object defines 
    *    its local coordinate system in relation to the one of its parent.
    * A traversal from parent to children encodes the hierarchical formation,
    * that permits the hierarchical modelling.
    * 
    * The rectangle shape is drawn from left top corner (0,0) AFTER 
    * transforming by all the AffineTransformations down to the leaf 
    * (i.e. the AffineTransform trans of parent at each level).
    * When a parent changes the orientation of its local coordinate system 
    * with a rotation
    * - its local AffineTransform is not the identity matrix, neither just 
    * a translation transformation,
    * - its children follow it since they use parent transformations to
    * proceed their rendering.
    * Notice: Differently of the starting code you will not always
    * want the coordinate system by which the transformation is applied
    * to be at the left corner of a rectangle.
    * Important reference for transformations :
    * http://java.sun.com/docs/books/tutorial/2d/display/transforming.html
    * Link to update
    */
   public MyRectangle(AffineTransform t, int w, int h, Color c, MyRectangle p) {
      super(0, 0, w, h);
      trans = t;
      color = c;
      parent = p;
      children = new ArrayList<MyRectangle>();
   }
   
   public void addChild(MyRectangle r) {
      children.add(r);
   }
   public AffineTransform getTrans(){
	   return trans;
   }

  /* public MyRectangle getChild(int x, int y){
   		/*for(int i = 0; i < children.size(); i++){
   			if(children.get(i).contains(x,y)){
   				return children.get(i);
   			}
   		}
   		return children.get(0);
   }*/
   public MyRectangle getChild(){
  		/*for(int i = 0; i < children.size(); i++){
  			if(children.get(i).contains(x,y)){
  				return children.get(i);
  			}
  		}*/
  		return children.get(0);
  }


   public void paint(Graphics2D g2) {
      
      AffineTransform saveAT = g2.getTransform();
      g2.transform(trans);


      g2.setColor(color);
      g2.fill(this);
     
      /*
       * For debugging purpose: 
       * to know where you are/what the transformation do, 
       * the anchor point at the crossing of local coord axis is drawn.
       * (Can be made different than the (x,y) left corner location of
       * the rectangle, if you wish.)
       */
      if (debug) {
         // left top corner of rectangle is (0, 0) (uncomment below to see)
         int CS_x = (int)getX();  
         int CS_y = (int)getY();
         //System.out.println("rect top left corner " + CS_x + " "+ CS_y);

         g2.setStroke(wideStroke);
         // y-axis in green
         g2.setColor(GREEN);
         g2.drawLine(CS_x, CS_y+COORD_LENGTH, CS_x, CS_y-COORD_LENGTH);
         // x-axis in orange 
         g2.setColor(ORANGE);
         g2.drawLine(CS_x+COORD_LENGTH, CS_y, CS_x-COORD_LENGTH, CS_y);
      }

      /*
       * Draw the children linked to that shape
       */ 
      
      Iterator<MyRectangle> it = children.iterator();
      while (it.hasNext()) {
         MyRectangle r = it.next();
         r.paint(g2);
      }

      g2.setTransform(saveAT);
      
   }
}

   
