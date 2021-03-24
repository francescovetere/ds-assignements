package it.unipr.ds.A1;

import java.io.Serializable;

/**
 * Class that defines the structure of a Message
 */
public final class Message implements Serializable { 
	private static final long serialVersionUID = 1L;

	private int senderID;
	private int messageID;
	
	public Message(int senderID, int messageID) {
		this.senderID = senderID;
		this.messageID = messageID;
	}
	
	public int getSenderID() {
		return senderID;
	}
	
	public void setMessageID(int messageID) {
		this.messageID = messageID;
	}
	
	public int getMessageID() {
		return messageID;
	}
	
	public void setSenderID(int senderID) {
		this.senderID = senderID;
	}
}
