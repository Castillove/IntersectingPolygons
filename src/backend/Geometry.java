package backend;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import com.upokecenter.numbers.ERational;

/*
 * Class representing the geometry functions used in the project
 * 
 * Geometry(): false constructor
 * dualize(Line l): dualizes the given line into a point
 * dualize(Point p): dualizes the given point into a line
 * incident(Point p, Line l): returns true if p is on the line l
 * connection(Point p, Point q, Color c): returns a line based on p and q
 * isLeftTurn(Point p, Point q, Point r): compares relation of given points to determine whether added the point results in a left turn
 * spin(Point p): returns the point's opposite (-x, -y)
 * spin(Line p): shifts the line to have a y-intercept of -b
 * lowerHull(Set<Point> input): returnd the list of points that make up the lower hull
 * */
public class Geometry {
	private Geometry() {
		assert false : "Don't construct this";
	}

	public static Point dualize(Line l) {
		return new Point(l.m, l.b.Negate(), l.c);
	}

	public static Line dualize(Point p) {
		return new Line(p.x, p.y.Negate(), p.c);
	}

	public static boolean incident(Point p, Line l) {
		return(p.x.Multiply(l.m).Add(l.b).Subtract(p.y).isZero());
	}

	public static Line connection(Point p, Point q, Color c) {
		ERational m = p.y.Subtract(q.y).Divide(p.x.Subtract(q.x));
		ERational b = p.y.Subtract(p.x.Multiply(m));
		return new Line(m, b, c);
	}

	// Assumes x increases to right, y increases up.
	public static int isLeftTurn(Point p, Point q, Point r) {
		ERational a = q.x.Subtract(p.x).Multiply(r.y.Subtract(p.y));
		ERational b = q.y.Subtract(p.y).Multiply(r.x.Subtract(p.x));
		return(b.compareTo(a));
	}

	public static Point spin(Point p) {
		return new Point(p.x.Negate(), p.y.Negate(), p.c);
	}

	public static Line spin(Line p) {
		return new Line(p.m, p.b.Negate(), p.c);
	}

	public static List<Point> lowerHull(Set<Point> input) {
		List<Point> ans = new ArrayList<Point>();	
		PriorityQueue<Point> pull = new PriorityQueue<Point>(input);
		//ans.set(0, pull.poll());
		int i = 0;
		while(!pull.isEmpty()) {
			Point p = pull.poll();
			ans.add(p);
			i++;
			while(i > 2 && Geometry.isLeftTurn(ans.get(i - 2), ans.get(i - 1), p) < 0)
				ans.remove(--i);
		}
		return(ans);
	}
}
