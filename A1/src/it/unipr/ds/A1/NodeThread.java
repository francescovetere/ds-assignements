package it.unipr.ds.A1;

import java.io.ObjectInputStream;
import java.net.Socket;

/**
 * !!! N.B.: Questa classe, al momento, si può tranquillamente eliminare
 */
public class NodeThread implements Runnable {

	// private Node node; // reference to the Master node
	private Socket nodeSocket; // this thread's socket towards a Node that sent a Message

	public NodeThread(Node node, Socket nodeSocket) {
		this.node = node;
		this.nodeSocket = nodeSocket;
	}

	@Override
	public void run() {
		while (true) {
			ObjectInputStream is = null;

			try {
				is = new ObjectInputStream(nodeSocket.getInputStream());
				
				Object obj = is.readObject();

				if (obj instanceof Message) {
					Message msg = (Message) obj;

					System.out.println("**Received message: " + msg.getMessageID() + " from node " + msg.getSenderID());

					Thread.sleep(1000);
				}

				// TODO: Bisognerebbe chiamare node.close() a un certo punto (?)
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(0);
			}
		}
	}

}
