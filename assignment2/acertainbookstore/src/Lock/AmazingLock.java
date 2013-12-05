package Lock;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import com.acertainbookstore.utils.BookStoreException;

public class AmazingLock implements Lock {
	private enum State {
		READ, WRITE, FREE;
	}
	State lock;
	private Queue<Tuple> queue;
	Long lastTimestamp;
	int numberOfUsers;
	
	public AmazingLock() {
		lock = State.FREE;
		queue = new LinkedList<Tuple>();
		numberOfUsers = 0;
		lastTimestamp = 0L;
	}
	
	@Override
	public synchronized void get(LockType lt, Timestamp timestamp) throws LockException {
		State newLock = getLockType(lt);
		switch (lock) {
			case FREE:	if(!queue.isEmpty()){
							joinQueue(newLock, timestamp);
						}//else break to take
						break;
			case READ:	if(!queue.isEmpty() || lt != LockType.READ){
			                joinQueue(newLock, timestamp);
						}//else break to take
						break;
			case WRITE:	joinQueue(newLock, timestamp);
						break;
		}
		//Default take the lock and increase number of users.
		lock = newLock;
		numberOfUsers++;
		return;
	}
	
	@Override
	public synchronized void release() {
		numberOfUsers--; //Decrease the number of users.
		if(numberOfUsers == 0 && queue.isEmpty()){
			lock = State.FREE;
		} else {
			do{
				queue.poll().notify();
			} while(!queue.isEmpty() && queue.peek().left() == State.READ);
		}
	}
	
/*
	@Override
	public synchronized boolean tryGet(LockType lt){
		if(numberOfUsers == 0 && queue.isEmpty() && lock == State.FREE){
			lock = getLockType(lt);
			numberOfUsers++;
			return true;
		}
		return false;
	}
*/
	
	/*
	 * Get all locks of the set. If not possible to get all immediately,
	 * release all locks and wait for the taken lock to be free. Then try again.
	 * @see com.acertainbookstore.business.Lock#getAllLocks(java.util.Set)
	 */
	@Override
	public void getAllLocks(Set<Tuple<Lock,LockType>> locks, Timestamp timestamp) 
	        throws LockException {
		Set<Lock> takenLocks = new HashSet<Lock>();
		try{
    		for(Tuple<Lock,LockType> t : locks){
    			Lock l = t.left();
    			LockType lt = t.right();
    			l.get(lt, timestamp);
    		}
		} catch(LockException e){ // Remember to release all taken locks.
		    releaseLocks(takenLocks);
		    throw e;
		}
		//Use tryGet, otherwise we cannot release already taken locks.
	}

	/*
	 * Join the queue, then wait.
	 * Return to get()-method when awaken (through notify), to take the lock.
	 */
	private synchronized void joinQueue(State s, Timestamp timestamp) 
	        throws LockException{
		if(lastTimestamp < timestamp.get()){
		    queue.add(new Tuple(s, this));
		} else {
		    throw new LockException();
		}
		try {
			this.wait(); //sleep until awakened
		} catch (InterruptedException e) {
			e.printStackTrace(); //TODO??
		}
		//return to get() to take the lock
	}
	/*
	 * Converts LockType-type to State-type.
	 */
	private State getLockType(LockType lt){
		switch(lt){
			case READ: 	return State.READ;
			case WRITE: return State.WRITE;
			default:	return State.FREE; //Should never happen.
		}
	}
	
	private void releaseLocks(Set<Lock> locks){
		for(Lock l : locks){
			l.release();
		}
	}
	
	/*
	 * Triple class for the queue.
	 */
/*
	class Triple<X, Y, Z> {
	  private final X x;
	  private final Y y;
	  private final Z z;
	  public Triple(X x, Y y, Z z) {
	    this.x = x;
	    this.y = y;
	    this.z = z;
	  }
	  public X left(){
		  return x;
	  }
	  public Y middle(){
		  return y;
	  }
	  public Z right(){
          return z;
      }
	} 
*/
}
