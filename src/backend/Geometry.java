package backend;

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
}
