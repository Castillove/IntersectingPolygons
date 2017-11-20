package backend;
import java.awt.Color;

import com.sun.istack.internal.NotNull;
import com.upokecenter.numbers.ERational;

public class Point {
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
	result = prime * result + ((c == null) ? 0 : c.hashCode());
	result = prime * result + ((x == null) ? 0 : x.hashCode());
	result = prime * result + ((y == null) ? 0 : y.hashCode());
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
	return(this.x.equals(that.x) && this.c.equals(that.c)
		&& this.y.equals(that.y));
    }
}
