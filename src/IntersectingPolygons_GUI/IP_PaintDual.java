package IntersectingPolygons_GUI;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JPanel;

import backend.Point;
import backend.Line;
import backend.Geometry;

/*
 * Screen (Panel) type for showing the intersection via projective geometry
 * (***May separate into different frames in the future)
 * Visualizing said concepts.
 * */
public class IP_PaintDual extends JPanel{
	
	ArrayList<Line> blueLines;
	ArrayList<Line> greenLines;
	ArrayList<Point> blueDual;
	ArrayList<Point> greenDual;
	Set<Point> allPts;
	List<Point> lowerHull, blueLowerHull, greenLowerHull;
	
	/*[CONSTRUCTOR]: IP_PaintDual(ArrayList<Point> bluePoints, ArrayList<Point> greenPoints)
	 * Coordinates the calculations necessary to get the intersection
	 * Strings used for ease of debugging purposes, please do not remove
	 * */
	public IP_PaintDual(ArrayList<Point> bluePoints, ArrayList<Point> greenPoints){
		
		/*Step 1: compute convex hull for the polygons' convex hulls */
		Set<Point> blueSet = new HashSet<Point>();
		blueSet.addAll(bluePoints);
		blueLowerHull = Geometry.lowerHull(blueSet);
		
		Set<Point> greenSet = new HashSet<Point>();
		greenSet.addAll(greenPoints);
		greenLowerHull = Geometry.lowerHull(greenSet);
		
		/*Step 2: Get the line representation of these convex hulls*/
		blueLines = makePolygonLines(blueSet);
		greenLines = makePolygonLines(greenSet);
		
		/*Step 3: Dualize these lines into points*/
		blueDual = makeDual(blueLines);
		greenDual = makeDual(greenLines);
		
		/*Step 4: Take the lower hull of these points*/
		allPts = new HashSet<Point>();
		allPts.addAll(blueDual);
		allPts.addAll(greenDual);
		lowerHull = Geometry.lowerHull(allPts);
		
		System.out.println("Number of lines: " + (blueLines.size() + greenLines.size()));
		System.out.println("Number of points in blue dual: " + blueDual.size());
		System.out.println("Number of point in green dual: "+ greenDual.size());
		System.out.println("Points in dual: " + allPts.size());
		System.out.println("Points in lowerhull: " + lowerHull.size());
		for(Point p : lowerHull)
			System.out.println("(" + p.x.ToInt32Checked() + "," + p.y.ToInt32Checked() + ")");
		
		repaint();
	}	
	
	/* makePolygonLines(Set<Point> pointSet):
	 * Takes in a set of points, and outputs the corresponding lines
	 * */
	private ArrayList<Line> makePolygonLines(Set<Point> pointSet){
		ArrayList<Line> polygon = new ArrayList<Line>();
		Iterator<Point> setItr = pointSet.iterator();
		Point prev = null;
		Point current = null;
		Point first = (Point) setItr.next();
		int index = 0;
		while(setItr.hasNext()){
			prev = (polygon.isEmpty() ? first : current);
			current = (Point) setItr.next();
			polygon.add(Geometry.connection(prev, current, null));
			index++;
		}
		polygon.add(Geometry.connection(current, first, null));
		return polygon;
	}
	
	/*
	 * makeDual(ArrayList<Line> lineList):
	 * Dualizes the list of lines into a list of points
	 * */

	private ArrayList<Point> makeDual(ArrayList<Line> lineList){
		ArrayList<Point> dualPts = new ArrayList<Point>();
		for(Line ln : lineList){
			dualPts.add(Geometry.dualize(ln));
		}
		return dualPts;
	}
	
	/* paintComponent(Graphics g):
	 * Responsible for all visualizations; called on every time repaint() called
	 * What/ how much to draw is dependent on the clicks:*/
	
	public void paintComponent(Graphics g){
		Graphics2D g2d = (Graphics2D) g; 
		super.paintComponent(g2d); 
		
		g2d.translate(getWidth()/2, getHeight()/2);		// Translating the coordinate plane to one where the origin is in the center
		g2d.scale(0.1,-0.1);	// Zooms out the coordinate plane; x increases right, y increases up
		
		/*Test: painted dual points of the convex hulls
		 * (Commented out: painting for the convex hull (lines)*/
		g2d.setColor(Color.BLUE);	
		for(Point p : blueDual)
			g2d.fillOval(p.x.ToInt32Checked(), p.y.ToInt32Checked(), 50, 50);
			//g2d.drawLine(0, 0*ln.m.ToInt32Checked()+ln.b.ToInt32Checked(),
				//	getWidth(), getWidth()*ln.m.ToInt32Checked()+ln.b.ToInt32Checked());
		
		g2d.setColor(Color.GREEN);
		for(Point p : greenDual)
			g2d.fillOval(p.x.ToInt32Checked(), p.y.ToInt32Checked(), 50, 50);
				
			
		/*Painting the lowerHull*/
		g2d.setColor(Color.PINK);
		for(int i=0; i<lowerHull.size(); i++){
				Point pt1 = lowerHull.get(i);
				Point pt2 = (i == lowerHull.size()-1 ? lowerHull.get(0) : lowerHull.get(i));
				g2d.drawLine(pt1.x.ToInt32Checked(),
						pt1.y.ToInt32Checked(),
						pt2.x.ToInt32Checked(),	
						pt2.y.ToInt32Checked());
			}
	}

	
	
}
