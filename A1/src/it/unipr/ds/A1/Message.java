package it.unipr.ds.A1;

import java.io.Serializable;

/**
 * Class that defines the structure of a Message
 */
public final class Message implements Serializable, Comparable<Message> {
	private static final long serialVersionUID = -8002716552973566762L;

	private int senderID;
	private int messageID;
	private String body;

	public Message(int senderID, int messageID, String body) {
		this.senderID = senderID;
		this.messageID = messageID;
		this.body = body;
	}

	public Message(int senderID, int messageID) {
		this(senderID, messageID, "<empty body>");
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

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	@Override
	public String toString() {
		return "--Message-- \n" + "MessageID: " + messageID + "\n" + "SenderID: " + senderID + "\n" + "Body: " + body
				+ "\n";
	}

	@Override
	public boolean equals(Object obj) {
		Message msg = (Message) obj;
		return this.senderID == msg.senderID && this.messageID == msg.messageID && this.body.equals(msg.body);
	}

	@Override
	public int compareTo(Message o) {
		Message other = (Message) o;

		if(this.messageID < other.messageID) return -1;
		else if(this.messageID < other.messageID) return 1;
		return 0;
	}

}
