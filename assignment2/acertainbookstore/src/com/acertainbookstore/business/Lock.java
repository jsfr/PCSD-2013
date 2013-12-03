package com.acertainbookstore.business;

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
	public void get(LockType t);
	
	/*
	 * Release the lock. Should only be used, if current thread has the lock.
	 * Otherwise the lock will be release for some other 
	 */
	public void release();
	
	/*
	 * Tries to take the lock.
	 * If lock is taken, returns false, otherwise takes the lock and returns true.
	 */
	public boolean tryGet(LockType t);
	
	/*
	 * Given a set of locks, try to get all. If not possible, release all taken 
	 * locks and try again.
	 * Can be implemented to wait for the taken lock and retry after this is taken.
	 * Starvation is a problem in this case though.
	 */
	public void getAllLocks(Set<Lock> locks);
}
