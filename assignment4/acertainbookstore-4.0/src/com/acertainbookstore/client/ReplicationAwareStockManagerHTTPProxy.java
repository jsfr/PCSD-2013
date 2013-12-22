/**
 * 
 */
package com.acertainbookstore.client;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.io.ByteArrayBuffer;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.BookEditorPick;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;
import com.acertainbookstore.utils.BookStoreMessageTag;
import com.acertainbookstore.utils.BookStoreProxyUtility;
import com.acertainbookstore.utils.BookStoreResult;
import com.acertainbookstore.utils.BookStoreUtility;

/**
 * 
 * ReplicationAwareStockManagerHTTPProxy implements the client level synchronous
 * CertainBookStore API declared in the BookStore class. It keeps retrying the
 * API until a consistent reply is returned from the replicas.
 * 
 */
public class ReplicationAwareStockManagerHTTPProxy implements StockManager {
	private HttpClient client;
	private List<String> slaveAddresses = new ArrayList<String>();
	private String masterAddress;
	//Kick somebodys ass for this
	//private String filePath = "/universe/pcsd/acertainbookstore/src/proxy.properties";
	private String filePath = null;
	private long snapshotId = 0;
	private int baton = 0;
	private int masterPoints = 0;

	/**
	 * Initialize the client object
	 */
	public ReplicationAwareStockManagerHTTPProxy() throws Exception {
		initializeReplicationAwareMappings();
		client = new HttpClient();
		client.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
		client.setMaxConnectionsPerAddress(BookStoreClientConstants.CLIENT_MAX_CONNECTION_ADDRESS); // max
																									// concurrent
																									// connections
																									// to
																									// every
																									// address
		client.setThreadPool(new QueuedThreadPool(
				BookStoreClientConstants.CLIENT_MAX_THREADSPOOL_THREADS)); // max
																			// threads
		client.setTimeout(BookStoreClientConstants.CLIENT_MAX_TIMEOUT_MILLISECS); // seconds
																					// timeout;
																					// if
																					// no
																					// server
																					// reply,
																					// the
																					// request
																					// expires
		client.start();
	}

	private void initializeReplicationAwareMappings() throws IOException {
		
		this.masterAddress = BookStoreProxyUtility.getMasterAddress();
		
		if (!this.masterAddress.endsWith("/stock")) {
			this.masterAddress = new String(this.masterAddress + "/stock");
		}

		List<String> slaveAddresses = BookStoreProxyUtility.getSlaveAddresses();
		for(String slave : slaveAddresses) {

			if (!slave.endsWith("/stock")) {
				slave = new String(slave + "/stock");
			}
			this.slaveAddresses.add(slave);
		}
	}

	public String getReplicaAddress() {
		int numSlaves = slaveAddresses.size();
		int limit = numSlaves+1;
		
		baton = baton++ % limit;
		
		if(baton == numSlaves) {
			if(masterPoints > 0) {
				masterPoints--;
				return getReplicaAddress();
			}
			return masterAddress;
		} else {
			return slaveAddresses.get(baton);
		}
	}

	public String getMasterServerAddress() {
		return masterAddress;
	}

	public void addBooks(Set<StockBook> bookSet) throws BookStoreException {
	    System.out.println("addBooks");
		String listBooksxmlString = BookStoreUtility
				.serializeObjectToXMLString(bookSet);
		Buffer requestContent = new ByteArrayBuffer(listBooksxmlString);

		BookStoreResult result = null;

		ContentExchange exchange = new ContentExchange();
		masterPoints++;
		String urlString = getMasterServerAddress() + "/"
				+ BookStoreMessageTag.ADDBOOKS;
		exchange.setMethod("POST");
		exchange.setURL(urlString);
		exchange.setRequestContent(requestContent);
		result = BookStoreUtility.SendAndRecv(this.client, exchange);
		this.setSnapshotId(result.getSnapshotId());
	}

	public void addCopies(Set<BookCopy> bookCopiesSet)
			throws BookStoreException {
	    System.out.println("addCopies");
		String listBookCopiesxmlString = BookStoreUtility
				.serializeObjectToXMLString(bookCopiesSet);
		Buffer requestContent = new ByteArrayBuffer(listBookCopiesxmlString);
		BookStoreResult result = null;

		ContentExchange exchange = new ContentExchange();
		masterPoints++;
		String urlString = getMasterServerAddress() + "/"
				+ BookStoreMessageTag.ADDCOPIES;
		exchange.setMethod("POST");
		exchange.setURL(urlString);
		exchange.setRequestContent(requestContent);
		result = BookStoreUtility.SendAndRecv(this.client, exchange);
		this.setSnapshotId(result.getSnapshotId());
	}

	@SuppressWarnings("unchecked")
	public List<StockBook> getBooks() throws BookStoreException {
	    System.out.println("getBooks");
		BookStoreResult result = null;
		do {
			ContentExchange exchange = new ContentExchange();
			String urlString = getReplicaAddress() + "/"
					+ BookStoreMessageTag.LISTBOOKS;

			exchange.setURL(urlString);
			result = BookStoreUtility.SendAndRecv(this.client, exchange);
//	        System.out.println("This.snapshotid:" + Double.toString(this.snapshotId));
//	        System.out.println("result.snapshotid:" + Double.toString(result.getSnapshotId()));
		} while (result.getSnapshotId() < this.getSnapshotId());
		System.out.println("out of loop");
		this.setSnapshotId(result.getSnapshotId());
		return (List<StockBook>) result.getResultList();
	}

	public void updateEditorPicks(Set<BookEditorPick> editorPicksValues)
			throws BookStoreException {
	    System.out.println("updateEditorPicks");
		String xmlStringEditorPicksValues = BookStoreUtility
				.serializeObjectToXMLString(editorPicksValues);
		Buffer requestContent = new ByteArrayBuffer(xmlStringEditorPicksValues);

		BookStoreResult result = null;
		ContentExchange exchange = new ContentExchange();
		masterPoints++;
		String urlString = getMasterServerAddress() + "/"
				+ BookStoreMessageTag.UPDATEEDITORPICKS + "?";
		exchange.setMethod("POST");
		exchange.setURL(urlString);
		exchange.setRequestContent(requestContent);
		result = BookStoreUtility.SendAndRecv(this.client, exchange);
		this.setSnapshotId(result.getSnapshotId());
	}

	public long getSnapshotId() {
		return snapshotId;
	}

	public void setSnapshotId(long snapshotId) {
		this.snapshotId = snapshotId;
	}

	public void stop() {
		try {
			client.stop();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public List<StockBook> getBooksInDemand() throws BookStoreException {
		// TODO Auto-generated method stub
		throw new BookStoreException();
	}

}
