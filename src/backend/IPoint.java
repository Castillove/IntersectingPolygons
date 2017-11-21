package backend;

import java.awt.Color;

import com.upokecenter.numbers.ERational;

public class IPoint extends Point {
    public final int ix, iy;
    public IPoint(int x, int y, Color c) {
	super(ERational.Create(x, 1), ERational.Create(y, 1), c);
	ix = x;
	iy = y;
    }
}
