package com.acertainbookstore.client.workloads;

public class AggregateResult {
	private int workers;
	private float throughput;
	private float latency;
	
	public AggregateResult(int workers, float throughput, float latency) {
		this.workers = workers;
		this.throughput = throughput;
		this.latency = latency;
	}

	public int getWorkers() {
		return workers;
	}

	public void setWorkers(int workers) {
		this.workers = workers;
	}

	public float getThroughput() {
		return throughput;
	}

	public void setThroughput(float throughput) {
		this.throughput = throughput;
	}

	public float getLatency() {
		return latency;
	}

	public void setLatency(float latency) {
		this.latency = latency;
	}
}
