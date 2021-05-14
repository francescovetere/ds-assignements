package it.unipr.ds.A3;

import java.io.Serializable;

import javax.jms.Queue;

/**
 * Class that defines a generic request made by a client
 */
public class Request implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * Type of the request
	 */
	public enum Type {
		READ, WRITE, RELEASE
	}

	private int senderID;
	private Type type;
	private Queue queue;

	/**
	 * Class constructor
	 * @param senderID Identifier of the sender of the request
	 * @param type Type of the request
	 * @param queue Queue of the sender (useful to tell the recipient where to respond)
	 */
	public Request(int senderID, Type type, Queue queue) {
		this.senderID = senderID;
		this.type = type;
		this.queue = queue;
	}

	/**
	 * senderID getter
	 * @return this.senderID
	 */
	public int getSenderID() {
		return this.senderID;
	}

	/**
	 * type getter
	 * @return this.type
	 */
	public Type getType() {
		return this.type;
	}

	/**
	 * queue getter
	 * @return this.queue
	 */
	public Queue getQueue() {
		return this.queue;
	}
}
