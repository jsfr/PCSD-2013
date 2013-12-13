package com.acertainbookstore.client.workloads;

public class AggregateResult {
	private int workers;
	private double throughput;
	private double latency;
	private double succRatio;
	private double customerXactRatio;
	
	public AggregateResult(int workers, double throughput, double latency, double succRatio, double customerXactRatio) {
		this.workers = workers;
		this.throughput = throughput;
		this.latency = latency;
		this.succRatio = succRatio;
		this.customerXactRatio = customerXactRatio;
	}

	public int getWorkers() {
		return workers;
	}

	public void setWorkers(int workers) {
		this.workers = workers;
	}

	public double getThroughput() {
		return throughput;
	}

	public void setThroughput(double throughput) {
		this.throughput = throughput;
	}

	public double getLatency() {
		return latency;
	}

	public void setLatency(double latency) {
		this.latency = latency;
	}
	
    public double getsuccRatio() {
        return succRatio;
    }

    public void setsuccRatio(double succRatio) {
        this.succRatio = succRatio;
    }
    
    public double getcustomerXactRatio() {
        return customerXactRatio;
    }

    public void setcustomerXactRatio(double customerXactRatio) {
        this.customerXactRatio = customerXactRatio;
    }
}
