package backend;
import com.sun.istack.internal.NotNull;
import com.upokecenter.numbers.ERational;
import java.awt.Color;

public class Line {
    public final @NotNull ERational m, b;
    public final @NotNull Color c;
    public Line(ERational m, ERational b, Color c) {
	this.m = m == null? ERational.Create(0, 1) : m;
	this.b = b == null? ERational.Create(0, 1) : b;
	this.c = c == null? Color.BLACK : c;
    }
    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((b == null) ? 0 : b.hashCode());
	result = prime * result + ((c == null) ? 0 : c.hashCode());
	result = prime * result + ((m == null) ? 0 : m.hashCode());
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
	Line that = (Line)obj;
	return(this.b.equals(that.b) && this.c.equals(that.c)
		&& this.m.equals(that.m));
    }

}
