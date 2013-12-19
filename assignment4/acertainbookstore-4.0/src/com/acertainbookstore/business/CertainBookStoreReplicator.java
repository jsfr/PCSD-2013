package com.acertainbookstore.business;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.acertainbookstore.interfaces.Replicator;

/**
 * CertainBookStoreReplicator is used to replicate updates to slaves
 * concurrently.
 */
public class CertainBookStoreReplicator implements Replicator {

	ExecutorService exec;
	
	public CertainBookStoreReplicator(int maxReplicatorThreads) {
		// TODO:Implement this constructor
		exec = Executors.newFixedThreadPool(maxReplicatorThreads);
	}

	public List<Future<ReplicationResult>> replicate(Set<String> slaveServers,
			ReplicationRequest request) {
		
		List<Future<ReplicationResult>> results = new ArrayList<Future<ReplicationResult>>();
		
		for(String server : slaveServers) {
			CertainBookStoreReplicationTask task = new CertainBookStoreReplicationTask(server, request);
			results.add(exec.submit(task));
		}
		
		return results;
	}

}
