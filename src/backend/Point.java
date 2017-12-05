package backend;
import java.awt.Color;

import com.sun.istack.internal.NotNull;
import com.upokecenter.numbers.ERational;

public class Point implements Comparable<Point> {
    public final @NotNull ERational x, y;
    public final @NotNull Color c;
    public Point(ERational x, ERational y, Color c) {
	this.x = x == null? ERational.Create(0, 1) : x;
	this.y = y == null? ERational.Create(0, 1) : y;
	this.c = c == null? Color.BLACK : c;
    }
    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((y == null) ? 0 : y.hashCode());
	result = prime * result + ((x == null) ? 0 : x.hashCode());
	return result;
    }
    @Override
    public boolean equals(Object obj) {
	if(this == obj)
	    return true;
	if(obj == null)
	    return false;
	if(getClass() != obj.getClass())
	    return false;
	Point that = (Point) obj;
	return(this.x.equals(that.x) 
		&& this.y.equals(that.y));
    }
    @Override
    public int compareTo(Point o) {
	int a = x.compareTo(o.x);
	if(a != 0)
	    return(a);
	return(y.compareTo(o.y));
    }
}
