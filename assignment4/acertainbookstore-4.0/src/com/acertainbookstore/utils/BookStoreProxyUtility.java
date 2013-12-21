package com.acertainbookstore.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public abstract class BookStoreProxyUtility {
	private static String filePath = "/Users/rel/Private/PCSD-2013/assignment4/acertainbookstore-4.0/src/proxy.properties";
	
	
	public static String getMasterAddress() throws FileNotFoundException, IOException {
		Properties props = new Properties();
		
		String masterAddress;

		props.load(new FileInputStream(filePath));
		
		masterAddress = props
				.getProperty(BookStoreConstants.KEY_MASTER);
		if (!masterAddress.toLowerCase().startsWith("http://")) {
			masterAddress = new String("http://" + masterAddress);
		}
		return masterAddress;
	}
	
	public static List<String> getSlaveAddresses() throws FileNotFoundException, IOException {
		Properties props = new Properties();
		List<String> retval = new ArrayList<String>();
		
		props.load(new FileInputStream(filePath));
		
		String slaveAddresses = props
				.getProperty(BookStoreConstants.KEY_SLAVE);
		
		for (String slave : slaveAddresses
				.split(BookStoreConstants.SPLIT_SLAVE_REGEX)) {
			if (!slave.toLowerCase().startsWith("http://")) {
				slave = new String("http://" + slave);
			}
			retval.add(slave);
		}
		return retval;
	}

	
}
