package it.unipr.ds.A1;

import java.io.BufferedInputStream;
import java.io.ObjectInputStream;
import java.net.Socket;

public class NodeThread implements Runnable {

	private Node node;  		// reference to the Master node
	private Socket nodeSocket;  // this thread's socket towards a Node that sent a Message
	
	public NodeThread(Node node, Socket nodeSocket) {
		this.node = node;
		this.nodeSocket = nodeSocket;
	}

	@Override
	public void run() {
		ObjectInputStream is = null;

		try {
			is = new ObjectInputStream(new BufferedInputStream(this.nodeSocket.getInputStream()));
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		try {
			Object obj = is.readObject();

			if (obj instanceof Message) {
				Message msg = (Message) obj;

				System.out.println("Received message: " + msg.getMessageID() + " from Node " + msg.getSenderID());
			}
			
			// TODO: Bisognerebbe chiamare node.close() a un certo punto (?)
		} catch (Exception e) {
				e.printStackTrace();
				System.exit(0);
		}
	}

}