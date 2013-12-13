/**
 * 
 */
package com.acertainbookstore.client.workloads;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.acertainbookstore.business.CertainBookStore;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;

/**
 * 
 * CertainWorkload class runs the workloads by different workers concurrently.
 * It configures the environment for the workers using WorkloadConfiguration
 * objects and reports the metrics
 * 
 */
public class CertainWorkload {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		int numConcurrentWorkloadThreads = 10;
		String serverAddress = "http://localhost:8081";
		boolean localTest = true;
		List<WorkerRunResult> workerRunResults = new ArrayList<WorkerRunResult>();
		List<Future<WorkerRunResult>> runResults = new ArrayList<Future<WorkerRunResult>>();

		initializeBookStoreData(serverAddress, localTest);

		ExecutorService exec = Executors
				.newFixedThreadPool(numConcurrentWorkloadThreads);

		for (int i = 0; i < numConcurrentWorkloadThreads; i++) {
			// The server address is ignored if localTest is true
			WorkloadConfiguration config = new WorkloadConfiguration(
					serverAddress, localTest);
			Worker workerTask = new Worker(config);
			// Keep the futures to wait for the result from the thread
			runResults.add(exec.submit(workerTask));
		}

		// Get the results from the threads using the futures returned
		for (Future<WorkerRunResult> futureRunResult : runResults) {
			WorkerRunResult runResult = futureRunResult.get(); // blocking call
			workerRunResults.add(runResult);
		}

		exec.shutdownNow(); // shutdown the executor
		reportMetric(workerRunResults);
	}

	/**
	 * Computes the metrics and prints them
	 * 
	 * @param workerRunResults
	 */
	public static void reportMetric(List<WorkerRunResult> workerRunResults) {
		// TODO: You should aggregate metrics and output them for plotting here
		try {
			ConsoleHandler logFile = new ConsoleHandler();//FileHandler("log.txt");
			logger.addHandler(logFile);
			float aggregateThroughput = 0F;
			//Long totalLatency = 0L;
			long totalLatency = 0L;
			float averageLatency = 0F;
			int numWorkers = workerRunResults.size();
			
			for(WorkerRunResult result : workerRunResults) {
				 int interactions = result.getSuccessfulFrequentBookStoreInteractionRuns();
				 long time = result.getElapsedTimeInNanoSecs();
				 aggregateThroughput += interactions/time;
				 
				 totalLatency += result.getElapsedTimeInNanoSecs();
			}
			
			averageLatency = totalLatency/numWorkers;
			LogRecord record = new LogRecord(Level.INFO, null);
			Formatter datFormatter = new Formatter() {

				@Override
				public String format(LogRecord record) {
					
					AggregateResult result = (AggregateResult) record.getParameters()[0];
					StringBuffer sb = new StringBuffer(1000);
					sb.append(result.getWorkers());
					
					sb.append('\t');
					sb.append(result.getThroughput());
					sb.append('\t');
					sb.append(result.getLatency());
					sb.append('\n');
					return sb.toString();
				}
				
				public String getHead(Handler h) {
					return "workers\throughput\tlatency";
				}
				
			};
			logFile.setFormatter(datFormatter);
			Object[] parameters = {new AggregateResult(numWorkers, aggregateThroughput, averageLatency)};
			record.setParameters(parameters);
			logger.log(record);
			
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Generate the data in bookstore before the workload interactions are run
	 * 
	 * Ignores the serverAddress if its a localTest
	 * 
	 * @param serverAddress
	 * @param localTest
	 * @throws Exception
	 */
	public static void initializeBookStoreData(String serverAddress,
			boolean localTest) throws Exception {
		BookStore bookStore = null;
		StockManager stockManager = null;
		// Initialize the RPC interfaces if its not a localTest
		if (localTest) {
			stockManager = CertainBookStore.getInstance();
			bookStore = CertainBookStore.getInstance();
		} else {
			stockManager = new StockManagerHTTPProxy(serverAddress + "/stock");
			bookStore = new BookStoreHTTPProxy(serverAddress);
		}

		BookSetGenerator bookSetGenerator = new BookSetGenerator();
		Set<StockBook> books = bookSetGenerator.nextSetOfStockBooks(9001);
		stockManager.addBooks(books);
		
		// Finished initialization, stop the clients if not localTest
		if (!localTest) {
			((BookStoreHTTPProxy) bookStore).stop();
			((StockManagerHTTPProxy) stockManager).stop();
		}

	}
	private static Logger logger = Logger.getLogger(CertainWorkload.class.getName());
}
