package backend;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class Polylines {
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private final List<IPoint> blue = new ArrayList(), green = new ArrayList();
    public void addPoint(boolean blue, int x, int y)
	    throws CollinearPointException, VerticalPointsException {
	IPoint p = new IPoint(x, y, blue? Color.BLUE : Color.GREEN);
	if(blue)
	    this.blue.add(p);
	else
	    this.green.add(p);
	checkOK(blue, blue? this.blue.size() - 1 : this.green.size() - 1);
    }
    public void removePoint(boolean blue)
	    throws CollinearPointException, VerticalPointsException {
	if(blue)
	    this.blue.remove(this.blue.size() - 1);
	else
	    this.green.remove(this.green.size() - 1);
	checkOK(blue, 0);
    }
    public void movePoint(boolean blue, int which, int x, int y) {
	int sz = (blue? this.blue : this.green).size();
	which = ((which % sz) + sz) % sz;
	IPoint p = new IPoint(x, y, blue? Color.BLUE : Color.GREEN);
	if(blue)
	    this.blue.set(which, p);
	else
	    this.green.set(which, p);
	checkOK(blue, which);
    }
    private void checkOK(boolean blue, int which)
	    throws CollinearPointException, VerticalPointsException {
	List<IPoint> as = blue? this.blue : this.green,
		bs = blue? this.green : this.blue;
	IPoint prev = as.get((which + as.size() - 1) % as.size());
	IPoint cur = as.get(which);
	IPoint next = as.get((which + 1) % as.size());
	if(prev.ix == cur.ix)
	    throw new VerticalPointsException(cur.ix, cur.iy, prev.iy);
	if(next.ix == cur.ix)
	    throw new VerticalPointsException(cur.ix, cur.iy, next.iy);
	Line prl = Geometry.connection(prev, cur, null);
	Line nxl = Geometry.connection(cur, next, null);
	IPoint pb = bs.get(bs.size() - 1);
	for(IPoint b : bs) {
	    if(Geometry.incident(b, prl))
		throw new CollinearPointException(
			prev.ix, prev.iy,
			cur.ix, cur.iy,
			b.ix, b.iy);
	    if(Geometry.incident(b, nxl))
		throw new CollinearPointException(
			next.ix, next.iy,
			cur.ix, cur.iy,
			b.ix, b.iy);
	    Line bl = Geometry.connection(pb, b, null);
	    if(Geometry.incident(cur, bl))
		throw new CollinearPointException(
			pb.ix, pb.iy,
			cur.ix, cur.iy,
			b.ix, b.iy);
	    if(prl.m.equals(bl.m))
		throw new ParallelLinesException(
			prev.ix, prev.iy,
			cur.ix, cur.iy,
			b.ix, b.iy,
			pb.ix, pb.iy);
	    if(nxl.m.equals(bl.m))
		throw new ParallelLinesException(
			next.ix, next.iy,
			cur.ix, cur.iy,
			b.ix, b.iy,
			pb.ix, pb.iy);
	    pb = b;
	}
    }
}
