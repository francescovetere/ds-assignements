package it.unipr.ds.A3;

import java.io.Serializable;

/**
 * Class that defines a generic request made by a client
 */
public class Request implements Serializable {
	private static final long serialVersionUID = 1L;

	public enum Type {
		READ, WRITE, RELEASE
	}

	private int senderID;
	private Type type;

	public Request(int senderID, Type type) {
		this.senderID = senderID;
		this.type = type;
	}

	public int getSenderID() {
		return this.senderID;
	}

	public Type getType() {
		return this.type;
	}
}
