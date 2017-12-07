package IntersectingPolygons_GUI;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/*
 * Screen (Panel) type for getting the polygons. 
 * Both visualizes and stores what the user has input in.
 * */
public class IP_PaintPanel extends JPanel{
	ArrayList<Point> prevPaintedPoints;
	ArrayList<Point> paintedPoints;
	Color polygonColor;
	int index, caseNum;
	boolean showPoints;
	
	/*[CONSTRUCTOR] IP_PaintPanel(Color c, ArrayList<Point> p):
	 * Initializes the panel's state
	 * 		Color used to determine what color the polygon will be represented by
	 * 		ArrayList used for the second polygon to easily get to the previous polygon's 
	 * 			points to visualize.
	 * */
	public IP_PaintPanel(Color c, ArrayList<Point> p){
		polygonColor = (c != null ? c : Color.RED);
		prevPaintedPoints = (p != null ? p : null);
		paintedPoints = new ArrayList<Point>();
		index = 0;
		caseNum = (p != null ? 2 : 0);
		showPoints = true;
		
		if(caseNum == 2){
			System.out.println("PRINT PREVIOUS POLYGON");
			repaint();
		}
		
		addMouseListener(new MouseAdapter(){
			public void mousePressed(MouseEvent e){
				if(SwingUtilities.isRightMouseButton(e)){	//On right clicks, switches between showing points of the polygon or showing lines
					caseNum = (prevPaintedPoints == null ? 3 : 2);
					System.out.println("SWITCH VIEW");
					showPoints = !showPoints;
					repaint();
				}else if(showPoints){	//On left clicks, a point is being checked and stored
					caseNum = 1;
					storePoint(e.getX(), e.getY());
				}
			}
		});
	}
	
	/*Translates a point from the normal coordinate plane to the java coordinate plane;
	 * (Unused so disabled)
	 * private Point transformPoint(Point p){
		//System.out.println("width " + getWidth()/2 + " and height " + getHeight()/2);
		int newX = (getWidth()/2 - p.x) * -1;
		int newY = getHeight()/2 - p.y;
		Point newPt = new Point(newX, newY);
		return newPt;
	}*/
	
	/* storePoint(int x, int y):
	 * [Checks the points for any errors- yet to be implemented]
	 * Stores the given coordinates into a Point, an then visualized*/
	private void storePoint(int x, int y){
		paintedPoints.add(new Point(x,y)); 
		System.out.println("ADDED POINT " + index + 
				" (" + paintedPoints.get(index).x  + "," 
				+ paintedPoints.get(index).y + ")");
		repaint(x, y, 5, 5);		
	}
	
	/* drawNewPoint(Graphics2D g2d, ArrayList<Point> currPG):
	 * Called on to draw the last point in the stored list of points onto the panel
	 * */
	private void drawNewPoint(Graphics2D g2d, ArrayList<Point> currPG){
		g2d.setColor(polygonColor);
		g2d.fillOval(currPG.get(index).x, currPG.get(index).y, 5, 5);	
		index++;
	}
	
	/* drawPolygon(Graphics2D g2d, ArrayList<Point> currPG, boolean isPoints):
	 * Called on to either draw the point representation, or the line representation, of the polygon
	 * */
	private void drawPolygon(Graphics2D g2d, ArrayList<Point> currPG, boolean isPoints){
		if(!currPG.isEmpty()){
			if(isPoints){
				for(Point p : currPG){
					g2d.drawOval(p.x, p.y, 5, 5);
					g2d.fillOval(p.x, p.y, 5, 5);
				}
			}else {
				g2d.setStroke(new BasicStroke(5f)); //Note that strokes are done in the order the points are given
				for(int i=0; i<currPG.size(); i++){
					int nextPt = (i == currPG.size()-1 ? 0 : i+1);
					g2d.drawLine(currPG.get(i).x,
							currPG.get(i).y,
							currPG.get(nextPt).x,
							currPG.get(nextPt).y);
				}
			}
		}
	}
	
	/*[GET] getPointList():
	 * Returns the points started as a list of backend.Point points
	 * */
	public ArrayList<backend.Point> getPointList(){
		ArrayList<backend.Point> pointList = new ArrayList<backend.Point>();
		for(Point p : paintedPoints){
			backend.Point newPT = new backend.IPoint(p.x, p.y, polygonColor);
			pointList.add(newPT);
		}
		return pointList;
	}
	
	/*[GET] getPaintedPoints():
	 * Returns the list of painted points.
	 * */
	public ArrayList<Point> getPaintedPoints(){
		return paintedPoints;
	}
	
	/* paintComponent(Graphics g):
	 * Responsible for all visualizations; called on every time repaint() called
	 * What/ how much to draw is dependent on the clicks:
	 * */
	public void paintComponent(Graphics g){
		Graphics2D g2d = (Graphics2D) g; 
		super.paintComponent(g2d); 
				
		switch(caseNum){
			case 1: drawNewPoint(g2d, paintedPoints);	//Left click: adding a new point
					caseNum = 0;
					break;
			case 2:	g2d.setColor(Color.BLUE);	//Paints the polygon (line view) from the previous polygon
					drawPolygon(g2d, prevPaintedPoints, false);
					caseNum = 0;
					//Note no break! ON purpose because full-window drawing
			case 3: g2d.setColor(polygonColor);		//Right click: showing a different view of the polygon
					drawPolygon(g2d, paintedPoints, showPoints);
					caseNum = 0;
					break;
				
		}
		
		
	}
	
}
