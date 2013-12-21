 /**
 * 
 */
package com.acertainbookstore.server;

import java.util.logging.Logger;


/**
 * Starts the slave bookstore HTTP server.
 */
public class SlaveBookStoreHTTPServer {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int port = 8082;
		if(args.length > 0) {
			System.out.print("Got: " + args[0]);
			port = Integer.parseInt(args[0]);
		}
		SlaveBookStoreHTTPMessageHandler handler = new SlaveBookStoreHTTPMessageHandler();
		Logger.getLogger(SlaveBookStoreHTTPServer.class.getName()).info("Starting slave at port: " + port);
		if (BookStoreHTTPServerUtility.createServer(port, handler)) {
			;
		}
	}

}
