package backend;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
    public static int isLeftTurn(Point p, Point q, Point r) {
	ERational a = q.x.Subtract(p.x).Multiply(r.y.Subtract(p.y));
	ERational b = q.y.Subtract(p.y).Multiply(r.x.Subtract(p.x));
	return(a.compareTo(b));
    }
    public static List<Point> hull(Set<Point> input) {
	List<Point> ans = new ArrayList<Point>();
	ans.set(0, Collections.min(input));
	List<Point> others = new ArrayList<Point>(input);
	others.remove(ans.get(0));
	others.sort(new Comparator<Point>() {
	    Point p = ans.get(0);
	    @Override
	    public int compare(Point o1, Point o2) {
		int i = Geometry.isLeftTurn(p, o2, o1);
		if(i != 0)
		    return(i);
		return(o1.compareTo(o2));
	    }
	});
	int i = 0;
	for(Point p : others) {
	    ans.add(p);
	    i++;
	    while(i > 2 && Geometry.isLeftTurn(ans.get(i - 2), ans.get(i - 1), p) < 0)
		ans.remove(--i);
	}
	return(ans);
    }
}
