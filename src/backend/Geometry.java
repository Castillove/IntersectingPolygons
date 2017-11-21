package backend;

import java.awt.Color;
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
}
