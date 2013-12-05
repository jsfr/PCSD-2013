package Lock;

import java.util.Set;

/*
 * A simple locking interface.
 */
public interface Lock {
	public enum LockType {
		READ, WRITE;
	}
	
	/*
	 * Take the lock.
	 */
	public void get(LockType l, Timestamp timestamp) throws LockException;
	
	/*
	 * Release the lock. Should only be used, if current thread has the lock.
	 * Otherwise the lock will be release for some other 
	 */
	public void release();
	
	/*
	 * Tries to take the lock.
	 * If lock is taken, returns false, otherwise takes the lock and returns true.
	 */
	//public boolean tryGet(LockType l, Timestamp timestamp);
	
	/*
	 * Get all locks of a given a set. 
	 * 
	 */
	public void getAllLocks(Set<Tuple<Lock,LockType>> locks, Timestamp timestamp) throws LockException;
}
