package backend;

public class ParallelLinesException extends IllegalStateException {
    private static final long serialVersionUID = 359123247403088984L;
    public final int x[] = new int[4], y[] = new int[4];
    public ParallelLinesException(int x0, int y0, int x1, int y1,
	    int x2, int y2, int x3, int y3) {
	this.x[0] = x0;
	this.x[1] = x1;
	this.x[2] = x2;
	this.x[3] = x3;
	this.y[0] = y0;
	this.y[1] = y1;
	this.y[2] = y2;
	this.y[3] = y3;
    }
}
