package it.unipr.ds.A1;

import java.io.Serializable;
/**
 * Class that defines the structure of a Statistics object
 * This object will be sent by a Node at the end of its multicast communication, 
 * and contains some statistical information about the messages exhanged  
 */
public class Statistics implements Serializable {
	private static final long serialVersionUID = -314405121338778299L;

	public int nodeID;

	public double totTime;
	public double avgTime;

	public int numSent;
	public int numResent;
	public int numReceived;
	public int numLost;

	@Override
	public String toString() {
		return "nodeID: " + nodeID + "\n" + "totTime[ms]: " + totTime + "\n" + "avgTime[ms]: " + avgTime + "\n"
				+ "numSent: " + numSent + "\n" + "numResent: " + numResent + "\n" + "numReceived: " + numReceived + "\n"
				+ "numLost: " + numLost + "\n";
	}
}
