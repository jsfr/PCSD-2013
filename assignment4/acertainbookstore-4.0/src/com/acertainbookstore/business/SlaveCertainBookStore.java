package com.acertainbookstore.business;

import java.util.Set;

import com.acertainbookstore.interfaces.ReplicatedReadOnlyBookStore;
import com.acertainbookstore.interfaces.ReplicatedReadOnlyStockManager;
import com.acertainbookstore.utils.BookStoreException;
import com.acertainbookstore.utils.BookStoreResult;

/**
 * SlaveCertainBookStore is a wrapper over the CertainBookStore class and
 * supports the ReplicatedReadOnlyBookStore and ReplicatedReadOnlyStockManager
 * interfaces
 * 
 * This class must also handle replication requests sent by the master
 * 
 * Designed using the singleton design pattern
 * 
 */
public class SlaveCertainBookStore implements ReplicatedReadOnlyBookStore,
		ReplicatedReadOnlyStockManager {
	private CertainBookStore bookStore = null;
	private static SlaveCertainBookStore instance = null;
	private long snapshotId = 0;

	private SlaveCertainBookStore() {
		bookStore = CertainBookStore.getInstance();
	}

	public synchronized static SlaveCertainBookStore getInstance() {
		if (instance == null) {
			instance = new SlaveCertainBookStore();
		}
		return instance;
	}

	public synchronized BookStoreResult getBooks() throws BookStoreException {
		BookStoreResult result = new BookStoreResult(bookStore.getBooks(),
				snapshotId);
		return result;
	}

	public synchronized BookStoreResult getBooksInDemand()
			throws BookStoreException {
		throw new BookStoreException();
	}

	public synchronized BookStoreResult getBooks(Set<Integer> ISBNList)
			throws BookStoreException {
		BookStoreResult result = new BookStoreResult(
				bookStore.getBooks(ISBNList), snapshotId);
		return result;
	}

	public synchronized BookStoreResult getTopRatedBooks(int numBooks)
			throws BookStoreException {
		throw new BookStoreException();
	}

	public synchronized BookStoreResult getEditorPicks(int numBooks)
			throws BookStoreException {
		BookStoreResult result = new BookStoreResult(
				bookStore.getEditorPicks(numBooks), snapshotId);
		return result;
	}
	
	public synchronized void addBooks(Set<StockBook> bookSet)
            throws BookStoreException {
	    System.out.println("addCopies in slave");
	    bookStore.addBooks(bookSet); // If this fails it will throw an exception
        snapshotId++;
        System.out.println("this" + snapshotId);
	    return;
    }
	
	public synchronized void addCopies(Set<BookCopy> bookCopiesSet)
            throws BookStoreException {
	    bookStore.addCopies(bookCopiesSet); // If this fails it will throw an exception
        snapshotId++;
	    return;
	}
	
	public synchronized void updateEditorPicks(
            Set<BookEditorPick> editorPicks) 
                    throws BookStoreException {
	    bookStore.updateEditorPicks(editorPicks);
	    snapshotId++;
        return;
    }
	
	public synchronized void buyBooks(Set<BookCopy> bookCopiesToBuy)
            throws BookStoreException {
	    bookStore.buyBooks(bookCopiesToBuy);
	    snapshotId++;
        return;
    }

}
