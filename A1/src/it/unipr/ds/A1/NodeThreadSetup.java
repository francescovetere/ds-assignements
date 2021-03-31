package it.unipr.ds.A1;

import java.net.Socket;

/**
 * Class that handles the reception of new messages in the setup phase
 */
public class NodeThreadSetup implements Runnable {
	private Socket nodeSocket; // this thread's socket towards a Node that sent a setup message

	public NodeThreadSetup(final Socket nodeSocket) {
		this.nodeSocket = nodeSocket;
	}

	@Override
	public void run() {
		Object obj = Utility.receive(nodeSocket);
		
		if (obj instanceof Message) {
			Message msg = (Message) obj;

			System.out.println("**Received setup message: " + msg.getMessageID() + " from node " + msg.getSenderID());
		}
	}
}
