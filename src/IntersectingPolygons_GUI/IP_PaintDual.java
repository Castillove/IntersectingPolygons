package IntersectingPolygons_GUI;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
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

    private static final long serialVersionUID = 2533222926501067150L;
	ArrayList<Line> blueLines;
	ArrayList<Line> greenLines;
	ArrayList<Line> lowerHullLines;
	ArrayList<Point> blueDual;
	ArrayList<Point> greenDual;
	ArrayList<Line> lowerHullDual;
	Set<Point> allPts;
	List<Point> lowerHull, upperHull, intersection, blueConvexHull, greenConvexHull;
	
	/*[CONSTRUCTOR]: IP_PaintDual(ArrayList<Point> bluePoints, ArrayList<Point> greenPoints)
	 * Coordinates the calculations necessary to get the intersection
	 * Strings used for ease of debugging purposes, please do not remove
	 * */
	public IP_PaintDual(ArrayList<Point> bluePoints, ArrayList<Point> greenPoints){
		
		/*Step 1: compute convex hull for the polygons' convex hulls_________________________________________ 
		Set<Point> blueSet = new HashSet<Point>();
		blueSet.addAll(bluePoints);
		Set<Point> greenSet = new HashSet<Point>();
		greenSet.addAll(greenPoints);
		
		List<Point>blueLowerHull = Geometry.lowerHull(blueSet);
		for(Point p : blueSet)
			Geometry.spin(p);
		List<Point>blueUpperHull = Geometry.lowerHull(blueSet);
		for(Point p : blueUpperHull)
			Geometry.spin(p);
		blueConvexHull = new ArrayList<Point>();
		blueConvexHull.addAll(blueLowerHull);
		blueConvexHull.addAll(blueUpperHull);
		System.out.println("Number of points in the lower hull: " + blueLowerHull.size());
		System.out.println("Number of points in the upper hull: " + blueUpperHull.size());
		System.out.println("Number of points in the convex hull: " + blueConvexHull.size());
		
		List<Point>greenLowerHull = Geometry.lowerHull(greenSet);
		for(Point p : greenSet)
			Geometry.spin(p);
		List<Point>greenUpperHull = Geometry.lowerHull(greenSet);
		for(Point p : greenUpperHull)
			Geometry.spin(p);
		greenConvexHull = new ArrayList<Point>();
		greenConvexHull.addAll(greenLowerHull);
		greenConvexHull.addAll(greenUpperHull);
		System.out.println("Number of points in the lower hull: " + greenLowerHull.size());
		System.out.println("Number of points in the upper hull: " + greenUpperHull.size());
		System.out.println("Number of points in the convex hull: " + greenConvexHull.size());
		____________________________________________________________________________________________________*/
		/*Step 2: Get the line representation of these convex hulls___________________________________________*/
		blueLines = makePolygonLines(bluePoints);
		greenLines = makePolygonLines(greenPoints);
		/*___________________________________________________________________________________________________*/
		/*Step 3: Dualize these lines into points*/
		blueDual = makeDualPoints(blueLines);
		greenDual = makeDualPoints(greenLines);
		/*for(Point p : blueDual)
			System.out.println("(" + p.x.ToInt32Checked() + "," + p.y.ToInt32Checked() + ")");
		for(Point p : greenDual)
			System.out.println("(" + p.x.ToInt32Checked() + "," + p.y.ToInt32Checked() + ")");*/
		/*____________________________________________________________________________________________________*/
		/*Step 4: Take the lower hull of these points*/
		allPts = new HashSet<Point>();
		allPts.addAll(blueDual);
		allPts.addAll(greenDual);
		
		lowerHull = Geometry.lowerHull(allPts);
		
		Set<Point> tempset = new HashSet<Point>();
		for(Point p : allPts){
			p = Geometry.spin(p);
			tempset.add(p);
		}
		for(Point p : tempset)
			System.out.println("(" + p.x.ToInt32Checked() + "," + p.y.ToInt32Checked() + ")");
		System.out.println();
		upperHull = Geometry.lowerHull(tempset);
		tempset.clear();
		for(Point p : upperHull){
			p = Geometry.spin(p);
			tempset.add(p);
		}
		for(Point p : tempset)
			System.out.println("(" + p.x.ToInt32Checked() + "," + p.y.ToInt32Checked() + ")");
		//lowerHullLines = makePolygonLines(lowerHull);
		
		intersection = new ArrayList<Point>();
		intersection.addAll(lowerHull);
		intersection.addAll(tempset);
		/*____________________________________________________________________________________________________*/
		/*Bonus: project back to the primal
		 * dualize all lower hull points(turns into lines)
		 * display these lines
		 * */
		ArrayList<Point> lh = new ArrayList<Point>();
		lh.addAll(intersection);
		lowerHullDual = makeDualLines(lh);
		
		//System.out.println("Number of lines: " + (blueLines.size() + greenLines.size()));
		System.out.println("Number of points in blue dual: " + blueDual.size());
		System.out.println("Number of point in green dual: "+ greenDual.size());
		//System.out.println("Points in dual: " + allPts.size());
		System.out.println("Points in intersection: " + intersection.size());
		for(Point p : intersection)
			System.out.println("(" + p.x.ToInt32Checked() + "," + p.y.ToInt32Checked() + ")");
		
		repaint();
	}
	/* getLeftmost(Set<Point> pointSet):
	 * Returns the leftmost point of the set
	 * 
	private Point getLeftmost(ArrayList<Point> pointList){	
		Point leftmost = pointList.get(0);
		
		for(Point p : pointList){
			if (p.x.compareTo(leftmost.x) < 1)
				leftmost = p;
		}
		return leftmost;
	}
	
	/* separatePoints(Set<Point> pointSet):
	 * Separates the given point set by comparing y coordinate values with respect to the leftmost point
	 * 
	private Set<Point> separatePoints(ArrayList<Point> pointList){
		Set<Point> lowerPoints = new HashSet<Point>();
		Point leftmost = getLeftmost(pointList);
		for(Point p : pointList){
			if (p.y.compareTo(leftmost.y) < 1){
				lowerPoints.add(p);
				System.out.println("(" + p.x.ToInt32Checked() + "," + p.y.ToInt32Checked() + ")");
			}
		}
		return lowerPoints;
	}*/
	
	/* makePolygonLines(Set<Point> pointSet):
	 * Takes in a list of points, and outputs the corresponding lines
	 * */
	private ArrayList<Line> makePolygonLines(List<Point> pointList){
		ArrayList<Line> polygon = new ArrayList<Line>();
<<<<<<< HEAD
		for(int i=1; i<pointList.size(); i++){	
			Point p1 = pointList.get(i-1);
			Point p2 = pointList.get(i);
			polygon.add(Geometry.connection(p1, p2, null));
			if(i==pointList.size()-1){
				p1 = pointList.get(0);
				p2 = pointList.get(i);
				polygon.add(Geometry.connection(p1, p2, null));
			}
=======
		Iterator<Point> setItr = pointSet.iterator();
		Point prev = null;
		Point current = null;
		Point first = (Point) setItr.next();
		while(setItr.hasNext()){
			prev = (polygon.isEmpty() ? first : current);
			current = (Point) setItr.next();
			polygon.add(Geometry.connection(prev, current, null));
>>>>>>> branch 'master' of https://github.com/Castillove/IntersectingPolygons.git
		}
		System.out.println("Number of lines stored " + polygon.size());
		return polygon;
	}
	
	/*
	 * makeDual(ArrayList<Line> lineList):
	 * Dualizes the list of lines into a list of points
	 * */

	private ArrayList<Point> makeDualPoints(ArrayList<Line> lineList){
		ArrayList<Point> dualPts = new ArrayList<Point>();
		for(Line ln : lineList){
			dualPts.add(Geometry.dualize(ln));
		}
		return dualPts;
	}
	
	private ArrayList<Line> makeDualLines(ArrayList<Point> pointList){
		ArrayList<Line> dualLns = new ArrayList<Line>();
		for(Point p : pointList){
			dualLns.add(Geometry.dualize(p));
		}
		return dualLns;
		
	}
	
	/* paintComponent(Graphics g):
	 * Responsible for all visualizations; called on every time repaint() called
	 * What/ how much to draw is dependent on the clicks:*/
	
	public void paintComponent(Graphics g){
		Graphics2D g2d = (Graphics2D) g; 
		super.paintComponent(g2d); 
		
		g2d.translate(getWidth()/2, getHeight()/2);		// Translating the coordinate plane to one where the origin is in the center
		g2d.scale(0.5,-0.5);	//x increases right, y increases up
					
		/*Test: 
		 * [x]Paint the lines of the primal lower hulls
		 * [x]Paint the dual of these lines
		 * [x]Paint the dual lower hull
		 */
		g2d.setColor(Color.BLUE);	
		for(Line ln : blueLines)
			g2d.draw(new Line2D.Double(getWidth(), getWidth()*ln.m.ToDouble()+ln.b.ToDouble(),
					getWidth()*-1, -1*getWidth()*ln.m.ToDouble()+ln.b.ToDouble()));
		g2d.setColor(Color.GREEN);
		for(Line ln : greenLines)
			g2d.draw(new Line2D.Double(getWidth(), getWidth()*ln.m.ToDouble()+ln.b.ToDouble(),
					getWidth()*-1, -1*getWidth()*ln.m.ToDouble()+ln.b.ToDouble()));
		
		g2d.setColor(Color.BLUE);	
		for(Point p : blueDual)
			g2d.draw(new Ellipse2D.Double(p.x.ToDouble(), p.y.ToDouble(), 30.0, 30.0));
		g2d.setColor(Color.GREEN);
		for(Point p : greenDual)
			g2d.draw(new Ellipse2D.Double(p.x.ToDouble(), p.y.ToDouble(), 30.0, 30.0));
		
		/*for(Line ln : lowerHullLines)
			g2d.drawLine(-getWidth(), -getWidth()*ln.m.ToInt32Checked()+ln.b.ToInt32Checked(),
					getWidth(), getWidth()*ln.m.ToInt32Checked()+ln.b.ToInt32Checked());
		
		/*Painting the lowerHull*/
		g2d.setColor(Color.PINK);	
		for(Point p : intersection)
			g2d.draw(new Ellipse2D.Double(p.x.ToDouble(), p.y.ToDouble(), 30.0, 30.0));
				
		g2d.setColor(Color.PINK);
		for(Line ln : lowerHullDual)
			g2d.draw(new Line2D.Double(getWidth(), getWidth()*ln.m.ToDouble()+ln.b.ToDouble(),
					getWidth()*-1, -1*getWidth()*ln.m.ToDouble()+ln.b.ToDouble()));
		
		g2d.scale(1, -1);
	}

	
	
}
