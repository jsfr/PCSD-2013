package com.acertainbookstore.business;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class AmazingLock implements Lock {
	private enum State {
		READ, WRITE, FREE;
	}
	State lock;
	private Queue<Tuple> queue;
	int numberOfUsers;
	
	public AmazingLock() {
		lock = State.FREE;
		queue = new LinkedList<Tuple>();
		numberOfUsers = 0;
	}
	
	@Override
	public synchronized void get(LockType lt) {
		State newLock = getLockType(lt);
		switch (lock) {
			case FREE:	if(!queue.isEmpty()){
							joinQueue(newLock);
						}//else break to take
						break;
			case READ:	if(!queue.isEmpty() || lt != LockType.READ){
							joinQueue(newLock);
						}//else break to take
						break;
			case WRITE:	joinQueue(newLock);
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
	

	@Override
	public boolean tryGet(LockType t){
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * Get all locks of the set. If not possible to get all immediately,
	 * release all locks and wait for the taken lock to be free. Then try again.
	 * @see com.acertainbookstore.business.Lock#getAllLocks(java.util.Set)
	 */
	@Override
	public void getAllLocks(Set<Lock> locks) {
		// TODO Auto-generated method stub
		//Use tryGet, otherwise we cannot release already taken locks.
		
	}
	
	/*
	 * Join the queue, then wait.
	 * Return to get()-method when awaken (through notify), to take the lock.
	 */
	private synchronized void joinQueue(State s){
		Tuple tuple = new Tuple(s, this);
		queue.add(tuple);
		//Join the queue.
		try {
			this.wait();
		} catch (InterruptedException e) {
			e.printStackTrace(); //TODO??
		}
		//return to get() to take the lock
		return;
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
	
	/*
	 * Tuple class for the queue.
	 * As found on http://stackoverflow.com/questions/2670982/using-tuples-in-java
	 * but with edited/added functionality.
	 */
	class Tuple<X, Y> { 
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
	
}
