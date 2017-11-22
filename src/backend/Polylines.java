package backend;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Polylines {
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private final List<IPoint> blue = new ArrayList(), green = new ArrayList();
    private Set<Point> cans = null;
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
	    pb = b;
	    cans = null;
	}
    }
    public Set<Point> getIntersections() {
	if(cans != null)
	    return(cans);
	cans = new HashSet<Point>();
	List<Point> gcon = new ArrayList<Point>();
	for(int i = 0; i < green.size(); i++)
	    gcon.add(Geometry.dualize(Geometry.connection(green.get(i),
		    green.get((i + 1) % blue.size()), null)));
	for(int i = 0; i < blue.size(); i++) {
	    Point thisb = blue.get(i);
	    Point nextb = blue.get((i+1)%blue.size());
	    Point bcon = Geometry.dualize(Geometry.connection(thisb, nextb, null)); 
	    for(int j = 0; j < green.size(); j++) {
		Line l = Geometry.connection(bcon, gcon.get(j), null);
		if(l.m.IsInfinity())
		    break;
		if(l.m.compareTo(thisb.x) * l.m.compareTo(nextb.x) != -1)
		    break;
		if(l.m.compareTo(green.get(i).x) *
			l.m.compareTo(green.get((i+1)%green.size()).x) != -1)
		    break;
		cans.add(Geometry.dualize(l));
	    }
	}
	return(cans);
    }
}
