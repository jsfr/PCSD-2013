package Lock;
/*
 * Tuple class used by the Lock interface and for queues.
 * As found on http://stackoverflow.com/questions/2670982/using-tuples-in-java
 * but with edited/added functionality.
 */

public class Tuple<X, Y> {
    private final X x; 
    private final Y y; 
    public Tuple(X x, Y y) { 
      this.x = x; 
      this.y = y; 
    }
    public X left(){
        return x;
	}
	public Y right(){
	  return y;
	}
}
