package backend;

public class CollinearPointException extends IllegalStateException {
    private static final long serialVersionUID = 4195483775917790892L;
    public final int x[] = new int[3], y[] = new int[3];
    public CollinearPointException(int x0, int y0, int x1, int y1,
	    int x2, int y2) {
	this.x[0] = x0;
	this.x[1] = x1;
	this.x[2] = x2;
	this.y[0] = y0;
	this.y[1] = y1;
	this.y[2] = y2;
    }
}
