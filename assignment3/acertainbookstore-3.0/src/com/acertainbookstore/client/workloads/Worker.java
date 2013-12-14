/**
 * 
 */
package com.acertainbookstore.client.workloads;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

import com.acertainbookstore.business.Book;
import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreException;

/**
 * 
 * Worker represents the workload runner which runs the workloads with
 * parameters using WorkloadConfiguration and then reports the results
 * 
 */
public class Worker implements Callable<WorkerRunResult> {
	private WorkloadConfiguration configuration = null;
	private StockManager stockManager = null; 
	private BookStore bookStore = null;
	private BookSetGenerator bookSetGenerator = null;
	private int numSuccessfulFrequentBookStoreInteraction = 0;
	private int numTotalFrequentBookStoreInteraction = 0;

	public Worker(WorkloadConfiguration config) {
		logger.addHandler(new ConsoleHandler());
		configuration = config;
		stockManager = configuration.getStockManager();
		bookSetGenerator = configuration.getBookSetGenerator();
		bookStore = configuration.getBookStore();
	}

	/**
	 * Run the appropriate interaction while trying to maintain the configured
	 * distributions
	 * 
	 * Updates the counts of total runs and successful runs for customer
	 * interaction
	 * 
	 * @param chooseInteraction
	 * @return
	 */
	private boolean runInteraction(float chooseInteraction) {
		logger.finest("Running interaction");
		try {
			if (chooseInteraction < configuration
					.getPercentRareStockManagerInteraction()) {
				runRareStockManagerInteraction();
			} else if (chooseInteraction < configuration
					.getPercentFrequentStockManagerInteraction()) {
				runFrequentStockManagerInteraction();
			} else {
				numTotalFrequentBookStoreInteraction++;
				runFrequentBookStoreInteraction();
				numSuccessfulFrequentBookStoreInteraction++;
			}
		} catch (BookStoreException ex) {
			ex.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Run the workloads trying to respect the distributions of the interactions
	 * and return result in the end
	 */
	public WorkerRunResult call() throws Exception {
		int count = 1;
		long startTimeInNanoSecs = 0;
		long endTimeInNanoSecs = 0;
		int successfulInteractions = 0;
		long timeForRunsInNanoSecs = 0;

		Random rand = new Random();
		float chooseInteraction;

		// Perform the warmup runs
		while (count++ <= configuration.getWarmUpRuns()) {
			chooseInteraction = rand.nextFloat() * 100f;
			runInteraction(chooseInteraction);
		}

		count = 1;
		numTotalFrequentBookStoreInteraction = 0;
		numSuccessfulFrequentBookStoreInteraction = 0;

		// Perform the actual runs
		startTimeInNanoSecs = System.nanoTime();
		while (count++ <= configuration.getNumActualRuns()) {
			chooseInteraction = rand.nextFloat() * 100f;
			if (runInteraction(chooseInteraction)) {
				successfulInteractions++;
			}
		}
		endTimeInNanoSecs = System.nanoTime();
		timeForRunsInNanoSecs += (endTimeInNanoSecs - startTimeInNanoSecs);
		return new WorkerRunResult(successfulInteractions,
				timeForRunsInNanoSecs, configuration.getNumActualRuns(),
				numSuccessfulFrequentBookStoreInteraction,
				numTotalFrequentBookStoreInteraction);
	}

	/**
	 * Runs the new stock acquisition interaction
	 * 
	 * @throws BookStoreException
	 */
	private void runRareStockManagerInteraction() throws BookStoreException {
		List<StockBook> books = stockManager.getBooks();
		Set<StockBook> booksToAdd = bookSetGenerator.nextSetOfStockBooks(configuration.getNumBooksToAdd());
		for(StockBook book : books) {
			if(booksToAdd.contains(book.getISBN())) {
				booksToAdd.remove(book);
			}
		}
		
		stockManager.addBooks(booksToAdd);
	}

	/**
	 * Runs the stock replenishment interaction
	 * 
	 * @throws BookStoreException
	 */
	private void runFrequentStockManagerInteraction() throws BookStoreException {
		List<StockBook> books = stockManager.getBooks();
		Set<BookCopy> booksToCopy = new HashSet<BookCopy>();
		List<StockBook> refillBooks = new ArrayList<StockBook>(); 
		
		if (books.size() < configuration.getNumBooksToRefill()) {
		    refillBooks.addAll(books);
		} else {
		    List<StockBook> subBooks = books.subList(0, configuration.getNumBooksToRefill());
		    refillBooks.addAll(subBooks);
		    books.removeAll(subBooks);
		    for(StockBook book : books) {
		        StockBook tmpBook = refillBooks.get(0);
		        for (StockBook rBook : refillBooks) {
		            if (rBook.getNumCopies() > tmpBook.getNumCopies()) {
		                tmpBook = rBook;
		            }
		        }
		        if (book.getNumCopies() < tmpBook.getNumCopies()) {
		            refillBooks.remove(tmpBook);
		            refillBooks.add(book);
		        }
		    }
		}
		for(StockBook book : refillBooks) {
			BookCopy copy = new BookCopy(book.getISBN(),configuration.getNumAddCopies());
			booksToCopy.add(copy);
		}
		stockManager.addCopies(booksToCopy);		
	}

	/**
	 * Runs the customer interaction
	 * 
	 * @throws BookStoreException
	 */
	private void runFrequentBookStoreInteraction() throws BookStoreException {
		List<Book> editorPicks = bookStore.getEditorPicks(configuration.getNumEditorPicksToGet());
		Set<Integer> isbns = new HashSet<Integer>();
		for(Book book : editorPicks) {
			isbns.add(book.getISBN());
		}
		Set<Integer> isbnsToBuy = bookSetGenerator.sampleFromSetOfISBNs(isbns, configuration.getNumBooksToBuy());
		Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
		for(Integer isbn : isbnsToBuy) {
			BookCopy copy = new BookCopy(isbn, configuration.getNumBookToBuy());
			booksToBuy.add(copy);
		}
		bookStore.buyBooks(booksToBuy);
	}
	private static Logger logger = Logger.getLogger(Worker.class.getName());
}
