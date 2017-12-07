package IntersectingPolygons_GUI;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import backend.Geometry;
import backend.Line;
import backend.Point;

public class IP_PaintConvexHull extends JPanel{
	
	ArrayList<Line> blueLines;
	ArrayList<Line> greenLines;
	
	public IP_PaintConvexHull(ArrayList<Point> bluePoints, ArrayList<Point> greenPoints){
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
		
		repaint();		
	}
	
	private ArrayList<Line> makePolygonLines(List<Point> pointList){
		ArrayList<Line> polygon = new ArrayList<Line>();
		for(int i=1; i<pointList.size(); i++){	
			Point p1 = pointList.get(i-1);
			Point p2 = pointList.get(i);
			polygon.add(Geometry.connection(p1, p2, null));
			if(i==pointList.size()-1){
				p1 = pointList.get(0);
				p2 = pointList.get(i);
				polygon.add(Geometry.connection(p1, p2, null));
			}
		}
		System.out.println("Number of lines stored " + polygon.size());
		return polygon;
	}
	
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
		
		g2d.scale(1, -1);
		
	}
}
