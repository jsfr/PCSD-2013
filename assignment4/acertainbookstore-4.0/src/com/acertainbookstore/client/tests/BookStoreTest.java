package com.acertainbookstore.client.tests;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.acertainbookstore.business.Book;
import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.client.ReplicationAwareBookStoreHTTPProxy;
import com.acertainbookstore.client.ReplicationAwareStockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreException;

/**
 * Test class to test the BookStore interface
 * 
 */
public class BookStoreTest {

	private static StockManager storeManager;
	private static BookStore client;
	
    @BeforeClass
    public static void setUpBeforeClass() {
        try {
            storeManager = new ReplicationAwareStockManagerHTTPProxy();
            client = new ReplicationAwareBookStoreHTTPProxy();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

	/**
	 * Here we want to test buyBooks functionality
	 * 
	 * 1. First we add a book with 5 copies.
	 * 
	 * 2. We buy two copies of this book.
	 * 
	 * 3. We check that after buying a copy number of copies is reduced to 3.
	 * 
	 * 4. We also try to buy non-existing books/ invalid ISBN and check that
	 * appropriate exceptions are thrown.
	 * 
	 * 5. We also try to buy 5 copies of the book and check that it is not
	 * possible to buy and the appropriate exception is thrown
	 */
	@Test
	public void testBuyBooks() {
		Integer testISBN = 300;
		Integer numCpies = 5;
		int buyCopies = 2;

		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(testISBN, "Book Name",
				"Author Name", (float) 100, numCpies, 0, 0, 0, false));
		try {
			storeManager.addBooks(booksToAdd);
		} catch (BookStoreException e) {
			e.printStackTrace();
			fail();
		}

		Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
		List<StockBook> listBooks = null;
		booksToBuy.add(new BookCopy(testISBN, buyCopies));
		try {
			client.buyBooks(booksToBuy);
			listBooks = storeManager.getBooks();
		} catch (BookStoreException e) {
			e.printStackTrace();
			fail();
		}
		for (StockBook b : listBooks) {
			if (b.getISBN() == testISBN) {
				assertTrue("Num copies  after buying one copy",
						b.getNumCopies() == numCpies - buyCopies);
				break;
			}
		}

		Boolean invalidISBNExceptionThrown = false;
		booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(-1, 1));
		try {
			client.buyBooks(booksToBuy);
		} catch (BookStoreException e) {
			invalidISBNExceptionThrown = true;
		}
		assertTrue(invalidISBNExceptionThrown);
		List<StockBook> currentList = null;
		try {
			currentList = storeManager.getBooks();
			assertTrue(currentList.equals(listBooks));
		} catch (BookStoreException e) {
			e.printStackTrace();
			fail();
		}

		Boolean nonExistingISBNExceptionThrown = false;
		booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(100000, 10));
		try {
			client.buyBooks(booksToBuy);
		} catch (BookStoreException e) {
			nonExistingISBNExceptionThrown = true;
		}
		assertTrue(nonExistingISBNExceptionThrown);
		try {
			currentList = storeManager.getBooks();
			assertTrue(currentList.equals(listBooks));
		} catch (BookStoreException e) {
			e.printStackTrace();
			fail();
		}

		Boolean cannotBuyExceptionThrown = false;
		booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(testISBN, numCpies));
		try {
			client.buyBooks(booksToBuy);
		} catch (BookStoreException e) {
			cannotBuyExceptionThrown = true;
		}
		assertTrue(cannotBuyExceptionThrown);
		try {
			currentList = storeManager.getBooks();
			assertTrue(currentList.equals(listBooks));
		} catch (BookStoreException e) {
			e.printStackTrace();
			fail();
		}
	}

	/**
	 * 
	 * Here we want to test getBooks(List<Integer> ISBbList)
	 * 
	 * 1. We add a book with ISBN = testISBN.
	 * 
	 * 2. We try to retrieve this book by executing getBooks with a list
	 * containing testISBN.
	 * 
	 * 3. We also test that getBooks executed with incorrect arguments throws
	 * exception.
	 * 
	 */
	@Test
	public void testGetBooks() {
		Integer testISBN = 400;
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(testISBN, "Book Name",
				"Book Author", (float) 100, 5, 0, 0, 0, false));
		try {
			storeManager.addBooks(booksToAdd);
		} catch (BookStoreException e) {
			e.printStackTrace();
		}

		Set<Integer> ISBNList = new HashSet<Integer>();
		ISBNList.add(testISBN);
		List<Book> books = null;
		try {
			books = client.getBooks(ISBNList);
		} catch (BookStoreException e) {
			e.printStackTrace();
			fail();
		}
		for (Book b : books) {
			if (b.getISBN() == testISBN) {
				assertTrue("Book ISBN", b.getISBN() == testISBN);
				assertTrue("Book Price", b.getPrice() == 100);
			}
		}
		List<StockBook> listBooks = null;
		try {
			listBooks = storeManager.getBooks();
		} catch (BookStoreException e) {
			e.printStackTrace();
			fail();
		}

		Boolean exceptionThrown = false;
		ISBNList = new HashSet<Integer>();
		ISBNList.add(-1);
		try {
			books = client.getBooks(ISBNList);
		} catch (BookStoreException e) {
			exceptionThrown = true;
		}
		assertTrue(exceptionThrown);
		List<StockBook> currentList = null;
		try {
			currentList = storeManager.getBooks();
			assertTrue(currentList.equals(listBooks));
		} catch (BookStoreException e) {
			e.printStackTrace();
			fail();
		}

		exceptionThrown = false;
		ISBNList = new HashSet<Integer>();
		ISBNList.add(10000000);
		try {
			books = client.getBooks(ISBNList);
		} catch (BookStoreException e) {
			exceptionThrown = true;
		}
		assertTrue(exceptionThrown);
		try {
			currentList = storeManager.getBooks();
			assertTrue(currentList.equals(listBooks));
		} catch (BookStoreException e) {
			e.printStackTrace();
			fail();
		}
	}

	@AfterClass
	public static void tearDownAfterClass() {
		((ReplicationAwareBookStoreHTTPProxy) client).stop();
		((ReplicationAwareStockManagerHTTPProxy) storeManager).stop();
	}

}
