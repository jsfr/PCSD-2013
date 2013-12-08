package com.acertainbookstore.business;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.Stack;

import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.lock.MultiGranularityLock;
import com.acertainbookstore.lock.MultiGranularityLock.LockType;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;
import com.acertainbookstore.utils.BookStoreUtility;
import com.acertainbookstore.utils.Tuple;

public class ConcurrentCertainBookStore implements BookStore, StockManager {
	private static ConcurrentCertainBookStore singleInstance;
	private static Map<Integer, BookStoreBook> bookMap;
	//One lock for each book
	private static Map<Integer, MultiGranularityLock> bookMapLocks;
	//Lock for the entire bookMap
	private static MultiGranularityLock bookMapLock;
	
	private ConcurrentCertainBookStore() {
		// TODO Auto-generated constructor stub
	}

	public synchronized static ConcurrentCertainBookStore getInstance() {
		if (singleInstance != null) {
			return singleInstance;
		} else {
			singleInstance = new ConcurrentCertainBookStore();
			bookMap = new HashMap<Integer, BookStoreBook>();
			bookMapLocks = new HashMap<Integer, MultiGranularityLock>();
			bookMapLock = new MultiGranularityLock();
		}
		return singleInstance;
	}
	
	private void releaseLocks(Stack<Tuple<MultiGranularityLock, LockType>> locks) {
	    while(!locks.empty()) {
		    Tuple<MultiGranularityLock, LockType> t = locks.pop();
		    MultiGranularityLock lock = t.left();
		    LockType lt = t.right();
		    lock.release(lt);
		}
	}

	public void addBooks(Set<StockBook> bookSet)
			throws BookStoreException {
		
		bookMapLock.getExclusive(); //KISS
		
		if (bookSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}
		// Check if all are there
		for (StockBook book : bookSet) {
			int ISBN = book.getISBN();
			String bookTitle = book.getTitle();
			String bookAuthor = book.getAuthor();
			int noCopies = book.getNumCopies();
			float bookPrice = book.getPrice();
			if (BookStoreUtility.isInvalidISBN(ISBN)
					|| BookStoreUtility.isEmpty(bookTitle)
					|| BookStoreUtility.isEmpty(bookAuthor)
					|| BookStoreUtility.isInvalidNoCopies(noCopies)
					|| bookPrice < 0.0) {

				bookMapLock.release(LockType.X);
				throw new BookStoreException(BookStoreConstants.BOOK
						+ book.toString() + BookStoreConstants.INVALID);
			} else if (bookMap.containsKey(ISBN)) {

				bookMapLock.release(LockType.X);
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN
						+ BookStoreConstants.DUPLICATED);
			}
			
		}
		
		for (StockBook book : bookSet) {
			int ISBN = book.getISBN();
			bookMap.put(ISBN, new BookStoreBook(book));
			
			//We already have exclusive lock on everything >:D
			MultiGranularityLock lock = new MultiGranularityLock();
			bookMapLocks.put(ISBN, lock);
		}
		
		bookMapLock.release(LockType.X);
		return;
	}

	public void addCopies(Set<BookCopy> bookCopiesSet)
			throws BookStoreException {
		int ISBN, numCopies;
		Stack<Tuple<MultiGranularityLock,LockType>> locks = new Stack<Tuple<MultiGranularityLock,LockType>>();
		
		bookMapLock.intendExclusive();
		locks.push(new Tuple<>(bookMapLock, LockType.IX));
		
		if (bookCopiesSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		for (BookCopy bookCopy : bookCopiesSet) {
			ISBN = bookCopy.getISBN();
			numCopies = bookCopy.getNumCopies();
			if (BookStoreUtility.isInvalidISBN(ISBN)) {
				releaseLocks(locks);
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN
						+ BookStoreConstants.INVALID);
			}
			if (!bookMap.containsKey(ISBN)) {
				releaseLocks(locks);
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN
						+ BookStoreConstants.INVALID);
			}
			if (BookStoreUtility.isInvalidNoCopies(numCopies)) {
				releaseLocks(locks);
				throw new BookStoreException(BookStoreConstants.NUM_COPIES
						+ numCopies + BookStoreConstants.INVALID);
			}
			
			MultiGranularityLock lock = bookMapLocks.get(ISBN);
			lock.getExclusive();
			locks.push(new Tuple<>(lock, LockType.X));
		}

		BookStoreBook book;
		// Update the number of copies
		for (BookCopy bookCopy : bookCopiesSet) {
			ISBN = bookCopy.getISBN();
			numCopies = bookCopy.getNumCopies();
			book = bookMap.get(ISBN);
			book.addCopies(numCopies);
		}
		
		releaseLocks(locks);
	}

	public List<StockBook> getBooks() {
		List<StockBook> listBooks = new ArrayList<StockBook>();
		
		bookMapLock.getShared();
		Collection<BookStoreBook> bookMapValues = bookMap.values();
		bookMapLock.release(LockType.S);
		
		for (BookStoreBook book : bookMapValues) {
			listBooks.add(book.immutableStockBook());
		}
		return listBooks;
	}

	public void updateEditorPicks(Set<BookEditorPick> editorPicks)
			throws BookStoreException {
		
	    Stack<Tuple<MultiGranularityLock,LockType>> locks = new Stack<Tuple<MultiGranularityLock,LockType>>();
		
		// Check that all ISBNs that we add/remove are there first.
		if (editorPicks == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		bookMapLock.intendExclusive();
		locks.push(new Tuple<>(bookMapLock, LockType.IX));
		
		int ISBNVal;

		for (BookEditorPick editorPickArg : editorPicks) {
			ISBNVal = editorPickArg.getISBN();
			if (BookStoreUtility.isInvalidISBN(ISBNVal)) {
				releaseLocks(locks);
				throw new BookStoreException(BookStoreConstants.ISBN + ISBNVal
						+ BookStoreConstants.INVALID);
			}
			if (!bookMap.containsKey(ISBNVal)) {
				releaseLocks(locks);
				throw new BookStoreException(BookStoreConstants.ISBN + ISBNVal
						+ BookStoreConstants.NOT_AVAILABLE);
			}
			
			MultiGranularityLock lock = bookMapLocks.get(ISBNVal);
			lock.getExclusive();
			locks.push(new Tuple<>(lock, LockType.X));
		}

		for (BookEditorPick editorPickArg : editorPicks) {
			bookMap.get(editorPickArg.getISBN()).setEditorPick(
					editorPickArg.isEditorPick());
		}
		
		releaseLocks(locks);
		return;
	}

	public void buyBooks(Set<BookCopy> bookCopiesToBuy)
			throws BookStoreException {
		
	    Stack<Tuple<MultiGranularityLock,LockType>> locks = new Stack<Tuple<MultiGranularityLock,LockType>>();
		
		if (bookCopiesToBuy == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}
		
		bookMapLock.intendExclusive();
		locks.push(new Tuple<>(bookMapLock, LockType.IX));
		
		// Check that all ISBNs that we buy are there first.
		int ISBN;
		BookStoreBook book;
		Boolean saleMiss = false;
		for (BookCopy bookCopyToBuy : bookCopiesToBuy) {
			ISBN = bookCopyToBuy.getISBN();
			if (BookStoreUtility.isInvalidISBN(ISBN)) {
				releaseLocks(locks);
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN
						+ BookStoreConstants.INVALID);
			}
			if (!bookMap.containsKey(ISBN)) {
				releaseLocks(locks);
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN
						+ BookStoreConstants.NOT_AVAILABLE);
			}
			book = bookMap.get(ISBN);
			if (!book.areCopiesInStore(bookCopyToBuy.getNumCopies())) {
				book.addSaleMiss(); // If we cannot sell the copies of the book
									// its a miss 
				saleMiss = true;
			}

			MultiGranularityLock lock = bookMapLocks.get(ISBN);
			lock.getExclusive();
			locks.push(new Tuple<>(lock, LockType.X));
		}

		// We throw exception now since we want to see how many books in the
		// order incurred misses which is used by books in demand
		if (saleMiss) {
			releaseLocks(locks);
			throw new BookStoreException(BookStoreConstants.BOOK
					+ BookStoreConstants.NOT_AVAILABLE);
		}

		// Then make purchase
		for (BookCopy bookCopyToBuy : bookCopiesToBuy) {
			book = bookMap.get(bookCopyToBuy.getISBN());
			book.buyCopies(bookCopyToBuy.getNumCopies());
		}
		
		releaseLocks(locks);
		return;
	}

	public List<Book> getBooks(Set<Integer> isbnSet)
			throws BookStoreException {
		if (isbnSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}
		
		Stack<Tuple<MultiGranularityLock,LockType>> locks = new Stack<Tuple<MultiGranularityLock,LockType>>();

		bookMapLock.intendShared();
		locks.push(new Tuple<>(bookMapLock, LockType.IS));
		
		// Check that all ISBNs that we rate are there first.
		for (Integer ISBN : isbnSet) {
			if (BookStoreUtility.isInvalidISBN(ISBN)) {
				releaseLocks(locks);
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN
						+ BookStoreConstants.INVALID);
			}
			if (!bookMap.containsKey(ISBN)) {
				releaseLocks(locks);
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN
						+ BookStoreConstants.NOT_AVAILABLE);
			}

			MultiGranularityLock lock = bookMapLocks.get(ISBN);
			lock.getShared();
			locks.push(new Tuple<>(lock, LockType.S));
		}

		List<Book> listBooks = new ArrayList<Book>();

		// Get the books
		for (Integer ISBN : isbnSet) {
			listBooks.add(bookMap.get(ISBN).immutableBook());
		}
		
		releaseLocks(locks);
		
		return listBooks;
	}


	public List<Book> getEditorPicks(int numBooks)
			throws BookStoreException {
		if (numBooks < 0) {
			throw new BookStoreException("numBooks = " + numBooks
					+ ", but it must be positive");
		}

		bookMapLock.getShared();
		
		List<BookStoreBook> listAllEditorPicks = new ArrayList<BookStoreBook>();
		List<Book> listEditorPicks = new ArrayList<Book>();
		BookStoreBook book;
		
		// Get all books that are editor picks
		for(BookStoreBook b : bookMap.values()) {
			if (b.isEditorPick()) {
				listAllEditorPicks.add(b);
			}
		}
		
		bookMapLock.release(LockType.S);
		
		// Find numBooks random indices of books that will be picked
		Random rand = new Random();
		Set<Integer> tobePicked = new HashSet<Integer>();
		int rangePicks = listAllEditorPicks.size();
		if (rangePicks < numBooks) {
			throw new BookStoreException("Only " + rangePicks
					+ " editor picks are available.");
		}
		int randNum;
		while (tobePicked.size() < numBooks) {
			randNum = rand.nextInt(rangePicks);
			tobePicked.add(randNum);
		}

		// Get the numBooks random books
		for (Integer index : tobePicked) {
			book = listAllEditorPicks.get(index);
			listEditorPicks.add(book.immutableBook());
		}
		return listEditorPicks;

	}

	@Override
	public List<Book> getTopRatedBooks(int numBooks) throws BookStoreException {
		// TODO Auto-generated method stub
		throw new BookStoreException();
	}

	@Override
	public List<StockBook> getBooksInDemand() throws BookStoreException {
		// TODO Auto-generated method stub
		throw new BookStoreException();
	}

	@Override
	public void rateBooks(Set<BookRating> bookRating) throws BookStoreException {
		// TODO Auto-generated method stub
		throw new BookStoreException();
	}

	

}
