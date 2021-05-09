package it.unipr.ds.A3;

import java.io.Serializable;

import javax.jms.Queue;

/**
 * Class that defines a generic request made by a client
 */
public class Request implements Serializable {
	private static final long serialVersionUID = 1L;

	public enum Type {
		READ, WRITE, RELEASE, VOTE
	}

	private int senderID;
	private Type type;
	private Queue queue;

	public Request(int senderID, Type type, Queue queue) {
		this.senderID = senderID;
		this.type = type;
		this.queue = queue;
	}

	public int getSenderID() {
		return this.senderID;
	}

	public Type getType() {
		return this.type;
	}

	public Queue getQueue() {
		return this.queue;
	}
}
