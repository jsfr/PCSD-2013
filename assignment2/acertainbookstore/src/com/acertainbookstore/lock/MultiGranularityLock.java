package com.acertainbookstore.lock;

import java.util.ArrayDeque;
import java.util.Queue;

public class MultiGranularityLock {
    public enum LockType {
        IX, IS, S, X;
    }

    private int ix;
    private int is;
    private int s;
    private int x;
    private Queue<LockType> queue;

    public MultiGranularityLock() {
        ix = 0;
        is = 0;
        s = 0;
        x = 0;
        queue = new ArrayDeque<LockType>();
    }

    public void getExclusive() {
        synchronized(queue) {
            while (x + s + ix + is > 0) {
                LockType lt = LockType.X;
                synchronized(lt) {
                    System.out.println("getExclusive in synchronized");
                    queue.add(lt);
                    try {
                        lt.wait();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
    
                System.out.println("getExclusive take lock");
                x = 1; // Take the exclusive lock;
            }
        }
    }

    public void getShared() {
        synchronized(queue) {
            while (x + ix > 0) {
                LockType lt = LockType.S;
                synchronized(lt) {
                    System.out.println("getShared in synchronized");
                    queue.add(lt);
                    try {
                        lt.wait();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
    
                System.out.println("getShared take lock");
                s++; // Increment the number of shared locks
            }
        }
    }

    public void intendExclusive() {
        synchronized(queue) {
            while (x + s > 0) {
                LockType lt = LockType.IX;
                synchronized(lt) {
                    System.out.println("intendExclusive in synchronized");
                    queue.add(lt);
                    try {
                        lt.wait();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
    
                System.out.println("intendExclusive take lock");
                ix++; // Increment the number of intend exclusive locks;
            }
        }
    }

    public void intendShared() {
        synchronized(queue) {
            while (x > 0) {
                LockType lt = LockType.IS;
                synchronized(lt) {
                    System.out.println("intendShared in synchronized");
                    queue.add(lt);
                    try {
                        lt.wait();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }

            System.out.println("intendShared take lock");
            is++; // Increment the number of intend shared locks;
        }
    }

    public void release(LockType lt) {
        synchronized(queue) {
            System.out.println("release before release");
            switch (lt) {
            case X:
                if (x > 0) {
                    System.out.println("Release exclusive");
                    x = 0; // Release the exclusive lock
                }
                break;
            case S:
                if (s > 0) {
                    System.out.println("Release exclusive");
                    s--; // Release one shared lock
                }
                break;
            case IX:
                if (ix > 0) {
    
                    System.out.println("Release intendExclusive");
                    ix--; // Release one intend exclusive lock
                }
                break;
            case IS:
                if (is > 0) {
                    System.out.println("Release intendShared");
                    is--; // Release one intend shared lock
                }
                break;
            }
        
            boolean done = false;
            while (!queue.isEmpty() && !done) {
                LockType next = queue.peek(); // Look at first element, but leave it as we might not wake any thread yet
                    System.out.println("release in synchronized");
                    synchronized(next) {
                        switch (next) {
                        case X:
                            if (x + s + ix + is == 0) {
                                queue.poll().notify();
                            }
                            done = true;
                            break;
                        case S:
                            if (x + ix == 0) {
                                queue.poll().notify();
                            } else {
                                done = true;
                            }
                            break;
                        case IX:
                            if (x + s == 0) {
                                queue.poll().notify();
                            } else {
                                done = true;
                            }
                            break;
                        case IS:
                            if (x == 0) {
                                queue.poll().notify();
                            } else {
                                done = true;
                            }
                            break;
                        }
                    }
                }
            }
        }
}
