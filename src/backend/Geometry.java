package backend;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import com.upokecenter.numbers.ERational;

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
    
    public static <T extends Point> List<T> lowerHull(Set<T> input) {
	List<T> ans = new ArrayList<T>();	
	PriorityQueue<T> pull = new PriorityQueue<T>(input);
	ans.set(0, pull.poll());
	int i = 0;
	while(!pull.isEmpty()) {
	    T p = pull.poll();
	    ans.add(p);
	    i++;
	    while(i > 2 && Geometry.isLeftTurn(ans.get(i - 2), ans.get(i - 1), p) < 0)
		ans.remove(--i);
	}
	return(ans);
    }
}
