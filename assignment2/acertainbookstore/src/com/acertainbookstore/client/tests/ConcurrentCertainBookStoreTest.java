package com.acertainbookstore.client.tests;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.ConcurrentCertainBookStore;
import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreException;

public class ConcurrentCertainBookStoreTest {
	private static boolean localTest = true; 
	private static StockManager storeManager;
	private static BookStore client;

	@BeforeClass
	public static void setUpBeforeClass() {
		try {
			if (localTest) {
				storeManager = ConcurrentCertainBookStore.getInstance();
				client = ConcurrentCertainBookStore.getInstance();
			} else {
				storeManager = new StockManagerHTTPProxy(
						"http://localhost:8081/stock");
				client = new BookStoreHTTPProxy("http://localhost:8081");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Interface for the assertions given to threadTest.
	 */
	private interface TestAssert{
		/**
		 * Contains the assertion
		 */
		public void asserts();
	}
	/*
	 * Starts threads of the given list of runnables, runs them 
	 */
	private void threadTest(Runnable[] ts, TestAssert ta, int k) {
		ArrayList<Thread> tList = new ArrayList<Thread>();
		if(k < 0){
			return;
		}
		for(int i=0; i<k;i++){
			for(Runnable t : ts){
				Thread tmpT = new Thread(t);
				tmpT.start();
				tList.add(tmpT);
			}
			for(Thread t : tList){
				try {
					t.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
					fail();
				}
			}
			ta.asserts();
		}
	}
	
	/*
	 * Test 1 of the assignment.
	 */
	@Test
	public void test1() {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		final Integer testISBN = 300;
		final int numCpies = 10; //TODO:Could be done random for each iteration.
		final ImmutableStockBook book = new ImmutableStockBook(testISBN, "Book Name",
				"Author Name", (float) 100, numCpies, 0, 0, 0, false);
		//Add the books to work on.
		booksToAdd.add(book);
		// BookStore thread
		class C1 implements Runnable{
			Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
			@Override
			public void run() {
				booksToBuy.add(new BookCopy(testISBN, numCpies));
				try{
					client.buyBooks(booksToBuy);
				} catch (BookStoreException e) {
					e.printStackTrace();
					fail();
				}
			}
		}
		// StockManager thread
		class C2 implements Runnable{
			Set<BookCopy> booksToCopy = new HashSet<BookCopy>();
			@Override
			public void run() {
				try{
					booksToCopy.add(new BookCopy(testISBN, numCpies));
					storeManager.addCopies(booksToCopy); 
				} catch (BookStoreException e) {
					e.printStackTrace();
					fail();
				}
				
			}
			
		}
		class Assertion implements TestAssert {
			//List<StockBook> booksInStore = new ArrayList<StockBook>();
			
			@Override
			public void asserts() {
				try{
					List<StockBook> booksInStore = storeManager.getBooks();
					int i = booksInStore.indexOf(book);
					assertTrue("Book not found, should not happen!", i >= 0);
					assertEquals(numCpies,  booksInStore.get(i).getNumCopies());
				} catch (BookStoreException e) {
					e.printStackTrace();
					fail();
				}
			}
		}
		
		Assertion a = new Assertion();
		Runnable[] ts = {new C1(), new C2()};
		threadTest(ts, a, 100);
	}
	
	/*
	 * Test 2 of the assignment.
	 */
	@Test
	public void test2() {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		final Integer testISBN1 = 200;
		final Integer testISBN2 = 201;
		final Integer testISBN3 = 202;
		final int numCpies = 3; //initial number of copies
		final ImmutableStockBook book1 = new ImmutableStockBook(testISBN1, "Star Wars IV",
				"George Lucas", (float) 100, numCpies, 0, 0, 0, false);
		final ImmutableStockBook book2 = new ImmutableStockBook(testISBN2, "Star Wars V",
				"George Lucas", (float) 100, numCpies, 0, 0, 0, false);
		final ImmutableStockBook book3 = new ImmutableStockBook(testISBN3, "Star Wars VI",
				"George Lucas", (float) 100, numCpies, 0, 0, 0, false);
		//Add the book to work on.
		booksToAdd.add(book1);
		booksToAdd.add(book2);
		booksToAdd.add(book3);
		// C1 thread. Buys a specific set of books and then replenish it.
		class C1 implements Runnable{
			Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
			Set<BookCopy> booksToCopy = new HashSet<BookCopy>();
			boolean isDone = false;
			@Override
			public void run() {
				//First buy books
				try{
					booksToBuy.add(new BookCopy(testISBN1, 1));
					booksToBuy.add(new BookCopy(testISBN2, 1));
					booksToBuy.add(new BookCopy(testISBN3, 1));
					client.buyBooks(booksToBuy);
				} catch (BookStoreException e) {
					e.printStackTrace();
					fail();
				}
				//Then replenish them
				try{
					booksToCopy.add(new BookCopy(testISBN1, 1));
					booksToCopy.add(new BookCopy(testISBN2, 1));
					booksToCopy.add(new BookCopy(testISBN3, 1));
					storeManager.addCopies(booksToCopy); 
				} catch (BookStoreException e) {
					e.printStackTrace();
					fail();
				}
				isDone= true;
			}
			
		}
		// C2 thread. Watches snapshots of bookstore to check for consistent all-or-nothing.
		class C2 implements Runnable{
			C1 c1; //Hack to see if first thread is done.
			public C2(C1 c1) {
				this.c1 = c1;
			}
			
			@Override
			public void run() {
				try{
					List<StockBook> booksInStore = storeManager.getBooks();
					int currNumCpies;
					int tmpNumCpies;
					while(!c1.isDone){
						tmpNumCpies = booksInStore.get(0).getNumCopies();
						if(tmpNumCpies == numCpies){
							currNumCpies = numCpies;
						} else {
							currNumCpies = tmpNumCpies;
						}
						for(StockBook b : booksInStore){
							assertTrue(b.getNumCopies() == currNumCpies);
						}
					}
				} catch (BookStoreException e) {
					e.printStackTrace();
					fail();
				}
			}
			
		}
		class Assertion implements TestAssert {
			//List<StockBook> booksInStore = new ArrayList<StockBook>();
			
			@Override
			public void asserts() {
				return; //This is not used
			}
		}
		Assertion a = new Assertion();
		C1 c1 = new C1();
		C2 c2 = new C2(c1);
		Runnable[] ts = {c1, c2};
		threadTest(ts, a, 100);
	}
	
	
	@AfterClass
	public static void tearDownAfterClass() {
		if (!localTest) {
			((BookStoreHTTPProxy) client).stop();
			((StockManagerHTTPProxy) storeManager).stop();
		}
	}

}


