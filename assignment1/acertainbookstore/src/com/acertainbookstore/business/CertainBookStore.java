/**
 * 
 */
package com.acertainbookstore.business;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;

import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;
import com.acertainbookstore.utils.BookStoreUtility;

/**
 * CertainBookStore implements the bookstore and its functionality which is
 * defined in the BookStore
 * 
 * Designed using the singleTon design pattern so there is always just one
 * CertainBookStore object
 * 
 */
public class CertainBookStore implements BookStore, StockManager {
	private static CertainBookStore singleInstance;
	private static Map<Integer, BookStoreBook> bookMap;

	private CertainBookStore() {

	}

	public synchronized static CertainBookStore getInstance() {
		if (singleInstance != null) {
			return singleInstance;
		} else {
			singleInstance = new CertainBookStore();
			bookMap = new HashMap<Integer, BookStoreBook>();
		}
		return singleInstance;
	}

	public synchronized void addBooks(Set<StockBook> bookSet)
			throws BookStoreException {

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
				throw new BookStoreException(BookStoreConstants.BOOK
						+ book.toString() + BookStoreConstants.INVALID);
			} else if (bookMap.containsKey(ISBN)) {
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN
						+ BookStoreConstants.DUPLICATED);
			}
		}

		for (StockBook book : bookSet) {
			int ISBN = book.getISBN();
			bookMap.put(ISBN, new BookStoreBook(book));
		}
		return;
	}

	public synchronized void addCopies(Set<BookCopy> bookCopiesSet)
			throws BookStoreException {
		int ISBN, numCopies;

		if (bookCopiesSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		for (BookCopy bookCopy : bookCopiesSet) {
			ISBN = bookCopy.getISBN();
			numCopies = bookCopy.getNumCopies();
			if (BookStoreUtility.isInvalidISBN(ISBN))
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN
						+ BookStoreConstants.INVALID);
			if (!bookMap.containsKey(ISBN))
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN
						+ BookStoreConstants.INVALID);
			if (BookStoreUtility.isInvalidNoCopies(numCopies))
				throw new BookStoreException(BookStoreConstants.NUM_COPIES
						+ numCopies + BookStoreConstants.INVALID);

		}

		BookStoreBook book;
		// Update the number of copies
		for (BookCopy bookCopy : bookCopiesSet) {
			ISBN = bookCopy.getISBN();
			numCopies = bookCopy.getNumCopies();
			book = bookMap.get(ISBN);
			book.addCopies(numCopies);
		}
	}

	public synchronized List<StockBook> getBooks() {
		List<StockBook> listBooks = new ArrayList<StockBook>();
		Collection<BookStoreBook> bookMapValues = bookMap.values();
		for (BookStoreBook book : bookMapValues) {
			listBooks.add(book.immutableStockBook());
		}
		return listBooks;
	}

	public synchronized void updateEditorPicks(Set<BookEditorPick> editorPicks)
			throws BookStoreException {
		// Check that all ISBNs that we add/remove are there first.
		if (editorPicks == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		int ISBNVal;

		for (BookEditorPick editorPickArg : editorPicks) {
			ISBNVal = editorPickArg.getISBN();
			if (BookStoreUtility.isInvalidISBN(ISBNVal))
				throw new BookStoreException(BookStoreConstants.ISBN + ISBNVal
						+ BookStoreConstants.INVALID);
			if (!bookMap.containsKey(ISBNVal))
				throw new BookStoreException(BookStoreConstants.ISBN + ISBNVal
						+ BookStoreConstants.NOT_AVAILABLE);
		}

		for (BookEditorPick editorPickArg : editorPicks) {
			bookMap.get(editorPickArg.getISBN()).setEditorPick(
					editorPickArg.isEditorPick());
		}
		return;
	}

	public synchronized void buyBooks(Set<BookCopy> bookCopiesToBuy)
			throws BookStoreException {
		if (bookCopiesToBuy == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}
		// Check that all ISBNs that we buy are there first.
		int ISBN;
		BookStoreBook book;
		Boolean saleMiss = false;
		for (BookCopy bookCopyToBuy : bookCopiesToBuy) {
			ISBN = bookCopyToBuy.getISBN();
			if (BookStoreUtility.isInvalidISBN(ISBN))
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN
						+ BookStoreConstants.INVALID);
			if (!bookMap.containsKey(ISBN))
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN
						+ BookStoreConstants.NOT_AVAILABLE);
			book = bookMap.get(ISBN);
			if (!book.areCopiesInStore(bookCopyToBuy.getNumCopies())) {
				book.addSaleMiss(); // If we cannot sell the copies of the book
									// its a miss 
				saleMiss = true;
			}
		}

		// We throw exception now since we want to see how many books in the
		// order incurred misses which is used by books in demand
		if (saleMiss)
			throw new BookStoreException(BookStoreConstants.BOOK
					+ BookStoreConstants.NOT_AVAILABLE);

		// Then make purchase
		for (BookCopy bookCopyToBuy : bookCopiesToBuy) {
			book = bookMap.get(bookCopyToBuy.getISBN());
			book.buyCopies(bookCopyToBuy.getNumCopies());
		}
		return;
	}

	public synchronized List<Book> getBooks(Set<Integer> isbnSet)
			throws BookStoreException {
		if (isbnSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}
		// Check that all ISBNs that we rate are there first.
		for (Integer ISBN : isbnSet) {
			if (BookStoreUtility.isInvalidISBN(ISBN))
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN
						+ BookStoreConstants.INVALID);
			if (!bookMap.containsKey(ISBN))
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN
						+ BookStoreConstants.NOT_AVAILABLE);
		}

		List<Book> listBooks = new ArrayList<Book>();

		// Get the books
		for (Integer ISBN : isbnSet) {
			listBooks.add(bookMap.get(ISBN).immutableBook());
		}
		return listBooks;
	}


	public synchronized List<Book> getEditorPicks(int numBooks)
			throws BookStoreException {
		if (numBooks < 0) {
			throw new BookStoreException("numBooks = " + numBooks
					+ ", but it must be positive");
		}

		List<BookStoreBook> listAllEditorPicks = new ArrayList<BookStoreBook>();
		List<Book> listEditorPicks = new ArrayList<Book>();
		Iterator<Entry<Integer, BookStoreBook>> it = bookMap.entrySet()
				.iterator();
		BookStoreBook book;

		// Get all books that are editor picks
		while (it.hasNext()) {
			Entry<Integer, BookStoreBook> pair = (Entry<Integer, BookStoreBook>) it
					.next();
			book = (BookStoreBook) pair.getValue();
			if (book.isEditorPick()) {
				listAllEditorPicks.add(book);
			}
		}

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
	public synchronized List<Book> getTopRatedBooks(int numBooks) throws BookStoreException {
		//Check that it is a legal number
		if(numBooks > bookMap.size() || numBooks < 0) {
			throw new BookStoreException("The input: " + numBooks 
					+ BookStoreConstants.INVALID_PARAMS);
		}
		
		Comparator<BookStoreBook> comparator = new Comparator<BookStoreBook>() {
			@Override
			public int compare(BookStoreBook b1, BookStoreBook b2) {
				Float rating = b2.getAverageRating();
				return rating.compareTo(b1.getAverageRating());
			}
		};
		
		//Sorting agressively :)
		PriorityQueue<BookStoreBook> sorted = new PriorityQueue<BookStoreBook>(numBooks, comparator);
		for(BookStoreBook b : bookMap.values()) {
			sorted.add(b);
		}
		
		//We only want numBooks books
		List<Book> retval = new ArrayList<Book>();
		for(int i = 0; i < numBooks; i++) {
			retval.add(sorted.poll().immutableBook());
		}
		return retval;
	}

	@Override
	public synchronized List<StockBook> getBooksInDemand() throws BookStoreException {
		// TODO Auto-generated method stub
		
		return null;
	}

	@Override
	public void rateBooks(Set<BookRating> bookRating) throws BookStoreException {
		if (bookRating == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}
		// Check that all ISBNs that we buy are there first.
		int ISBN;
		BookStoreBook book;
		for (BookRating bookRate : bookRating) {
			ISBN = bookRate.getISBN();
			if (BookStoreUtility.isInvalidISBN(ISBN))
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN
						+ BookStoreConstants.INVALID);
			if (!bookMap.containsKey(ISBN))
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN
						+ BookStoreConstants.NOT_AVAILABLE);

			//Make sure the rating is within the correct bounds
			int rating = bookRate.getRating();
			if(rating < 0 || rating > 5)
				throw new BookStoreException(BookStoreConstants.RATING + rating
						+ BookStoreConstants.INVALID);
		}
		
		//Ok everything checks out, now let's do stuff :D
		synchronized(this) { //Accessing stuff, need to be careful here
			for(BookRating bookRate : bookRating) {
				book = bookMap.get(bookRate.getISBN());
				book.addRating(bookRate.getRating());
			}
		}
		return;
	}

}
