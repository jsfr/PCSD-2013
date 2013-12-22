package com.acertainbookstore.business;

import java.util.concurrent.Callable;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.io.ByteArrayBuffer;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import com.acertainbookstore.client.BookStoreClientConstants;
import com.acertainbookstore.utils.BookStoreException;
import com.acertainbookstore.utils.BookStoreUtility;

/**
 * CertainBookStoreReplicationTask performs replication to a slave server. It
 * returns the result of the replication on completion using ReplicationResult
 */
public class CertainBookStoreReplicationTask implements
		Callable<ReplicationResult> {

	private String server;
	private ReplicationRequest request;
	private HttpClient client;
	
	public CertainBookStoreReplicationTask(String server, ReplicationRequest request) {
		this.server = server;
		this.request = request;
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
		try {
			client.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public ReplicationResult call() throws Exception {
		String dataSetXmlString = BookStoreUtility.serializeObjectToXMLString(request.getDataSet());
		
		Buffer requestContent = new ByteArrayBuffer(dataSetXmlString);

		ContentExchange exchange = new ContentExchange();
		String urlString = this.server
				+ request.getMessageType();
		
		
		exchange.setMethod("POST");
		exchange.setURL(urlString);
		exchange.setRequestContent(requestContent);
		ReplicationResult retval = new ReplicationResult(this.server, true);
		
		try {
			BookStoreUtility.SendAndRecv(this.client, exchange);
		} catch (BookStoreException e) {
			retval.setReplicationSuccessful(false);
		}
		
		return retval;
	}

}
