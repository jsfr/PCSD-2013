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

    public synchronized void getExclusive() {
        while (x + s + ix + is > 0) {
            LockType lt = LockType.X;
            queue.add(lt);
            try {
                lt.wait();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        x = 1; // Take the exclusive lock;
    }

    public synchronized void getShared() {
        while (x + ix > 0) {
            LockType lt = LockType.S;
            queue.add(lt);
            try {
                lt.wait();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        s++; // Increment the number of shared locks
    }

    public synchronized void intendExclusive() {
        while (x + s > 0) {
            LockType lt = LockType.IX;
            queue.add(lt);
            try {
                lt.wait();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        ix++; // Increment the number of intend exclusive locks;
    }

    public synchronized void intendShared() {
        while (x > 0) {
            LockType lt = LockType.IS;
            queue.add(lt);
            try {
                lt.wait();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        is++; // Increment the number of intend shared locks;
    }

    public synchronized void release(LockType lt) {
        switch (lt) {
        case X:
            if (x > 0) {
                x = 0; // Release the exclusive lock
            }
            break;
        case S:
            if (s > 0) {
                s--; // Release one shared lock
            }
            break;
        case IX:
            if (ix > 0) {
                ix--; // Release one intend exclusive lock
            }
            break;
        case IS:
            if (is > 0) {
                is--; // Release one intend shared lock
            }
            break;
        }
        
        boolean done = false;
        while (!queue.isEmpty() && !done) {
            LockType next = queue.peek(); // Look at first element, but leave it as we might not wake any thread yet
            
            switch (next) {
            case X:
                if (x + s + ix + is == 0) {
                    queue.poll().notify();
                }
                done = true;
            case S:
                if (x + ix == 0) {
                    queue.poll().notify();
                } else {
                    done = true;
                }
            case IX:
                if (x + s == 0) {
                    queue.poll().notify();
                } else {
                    done = true;
                }
            case IS:
                if (x == 0) {
                    queue.poll().notify();
                } else {
                    done = true;
                }
            }
        }
    }
}
