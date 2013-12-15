/**
 * 
 */
package com.acertainbookstore.client.workloads;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
	
	static boolean localTest = true;
	static String serverAddress = "http://localhost:8081";
	

	private static int intArg(String[] args, int argNum) throws IndexOutOfBoundsException {
		return Integer.parseInt(args[argNum]);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		
		int numConcurrentWorkloadThreadsStep = 1;
		int numConcurrentWorkloadThreadsMax = 30;
		int numRunsPerStep = 10;
		
		try {
			numConcurrentWorkloadThreadsMax = intArg(args,0);
			numRunsPerStep = intArg(args, 1);
			numConcurrentWorkloadThreadsMax = intArg(args, 2);
			
		} catch (IndexOutOfBoundsException e) {
			//Just ignore this
		}
		String fname = "benchmark.remote.dat";
		if(localTest) {
			fname = "benchmark.local.dat";
		}
		FileHandler logFile = new FileHandler(fname);
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
				sb.append('\t');
                sb.append(result.getsuccRatio());
                sb.append('\t');
                sb.append(result.getcustomerXactRatio());
				sb.append('\n');
				return sb.toString();
			}
			
			public String getHead(Handler h) {
				return "workers\tthroughput (succXact/s)\tlatency (s)\tsuccRatio\tcustomerXactRatio\n";
			}
			
		};
		logFile.setFormatter(datFormatter);
		benchmarkLogger.addHandler(logFile);
		
		runTests(numConcurrentWorkloadThreadsStep, numConcurrentWorkloadThreadsMax, numRunsPerStep);
	}
	
	public static void runTests(int step, int max, int runs) throws Exception {
		List<WorkerRunResult> workerRunResults;
		List<Future<WorkerRunResult>> runResults;
		ExecutorService exec;
		WorkloadConfiguration config;
		Worker workerTask;
		
		//Do everything we did before
		//Run max/steps runs, or something like that
		for(int j = step; 
				j < max+1; 
				j = j + step) {
			//Run the test numRunsPerStep times
			for (int k = 0; k < runs; k++) {
				//We want the same number of workers for all tests in this step
				exec = Executors
						.newFixedThreadPool(j);
						//Re-initialize the bookstore for each run
						initializeBookStoreData(serverAddress, localTest);
						
						consoleLogger.info("Running with " + j + " workers, run #" + (k+1));
						
						workerRunResults = new ArrayList<WorkerRunResult>();
						
						runResults = new ArrayList<Future<WorkerRunResult>>();
			
						for (int i = 0; i < j; i++) {
							consoleLogger.info("Adding worker");
							// The server address is ignored if localTest is true
							config = new WorkloadConfiguration(
									serverAddress, localTest);
							workerTask = new Worker(config);
							// Keep the futures to wait for the result from the thread
							runResults.add(exec.submit(workerTask));
						}
						
			
						// Get the results from the threads using the futures returned
						for (Future<WorkerRunResult> futureRunResult : runResults) {
							
							WorkerRunResult runResult = futureRunResult.get(); // blocking call
							workerRunResults.add(runResult);
						}

						exec.awaitTermination(100, TimeUnit.MILLISECONDS); // shutdown the executor
						reportMetric(workerRunResults);
						exec = null;
					}
				}
		consoleLogger.info("Benchmarking complete");
	}

	/**
	 * Computes the metrics and prints them
	 * 
	 * @param workerRunResults
	 */
	public static void reportMetric(List<WorkerRunResult> workerRunResults) {
		try {
				double aggregateThroughput = 0D;
				long totalLatency = 0L;
				double averageLatency = 0D;
				double succRatio = 0D;
				double customerXactRatio = 0D;
				int numWorkers = workerRunResults.size();
				double sumInteractions = 0D;
				double sumSuccInteractions = 0D;
				double sumAllInteractions = 0D;
				
				for(WorkerRunResult result : workerRunResults) {
					 int interactions = result.getSuccessfulFrequentBookStoreInteractionRuns();
					 double time = result.getElapsedTimeInNanoSecs()/1E9;
					 
					 aggregateThroughput += interactions/time;
					 totalLatency += result.getElapsedTimeInNanoSecs();
					 sumInteractions += result.getTotalFrequentBookStoreInteractionRuns();
					 sumSuccInteractions += result.getSuccessfulFrequentBookStoreInteractionRuns();
					 sumAllInteractions += result.getSuccessfulInteractions();
				}
				
				averageLatency = (totalLatency/numWorkers)/1E9;
				succRatio = sumSuccInteractions / sumInteractions;
				customerXactRatio = sumSuccInteractions / sumAllInteractions;
				
				Object[] parameters = {new AggregateResult(numWorkers, aggregateThroughput, averageLatency, succRatio, customerXactRatio)};
				LogRecord record = new LogRecord(Level.INFO, "Added result");
				record.setParameters(parameters);
				benchmarkLogger.log(record);
		} catch (SecurityException e) {
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
		consoleLogger.info("Initializing");
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
		
		stockManager.dropBooks();
		// Initialization of books
		BookSetGenerator bookSetGenerator = new BookSetGenerator();
		Set<StockBook> books = bookSetGenerator.nextSetOfStockBooks(500);
		
		stockManager.addBooks(books);
		
		consoleLogger.info("Finished initializing books");
		// Finished initialization, stop the clients if not localTest
		if (!localTest) {
			((BookStoreHTTPProxy) bookStore).stop();
			((StockManagerHTTPProxy) stockManager).stop();
		}

	}
	private static Logger consoleLogger = Logger.getLogger(CertainWorkload.class.getName() + "Console");
	private static Logger benchmarkLogger = Logger.getLogger(CertainWorkload.class.getName());
}
