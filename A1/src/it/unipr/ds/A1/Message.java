package it.unipr.ds.A1;

import java.io.Serializable;

/**
 * Class that defines the structure of a Message
 */
public final class Message implements Serializable { 
	private static final long serialVersionUID = 1L;

	private String senderID;
	private int messageID;
	
	public Message(String senderID, int messageID) {
		this.senderID = senderID;
		this.messageID = messageID;
	}
	
	public String getSenderID() {
		return senderID;
	}
	
	public int getMessageID() {
		return messageID;
	}
}
