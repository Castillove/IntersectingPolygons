package backend;

public class VerticalPointsException extends IllegalStateException {
    private static final long serialVersionUID = 4171413569855140198L;
    public final int x, y0, y1;
    public VerticalPointsException(int x, int y0, int y1) {
	this.x = x;
	this.y0 = y0;
	this.y1 = y1;
    }
}
