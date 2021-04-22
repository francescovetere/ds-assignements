package it.unipr.ds.A2;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * APIs for election with Bully algorithm (implementation)
 */
public class ElectionImpl extends UnicastRemoteObject implements Election {
	private static final long serialVersionUID = 1L;

	public Node node;

	public ElectionImpl(Node node) throws RemoteException {
		this.node = node;
	}

	@Override
	public Node getNode() throws RemoteException {
		return this.node;
	}

	@Override
	public void electionMsg(Election e) throws RemoteException {
		Message m = new Message(Message.InvokedMethod.ELECTION, e);
		this.node.msgQueue.add(m);
	}

	@Override
	public void okMsg(Election e) throws RemoteException {
		Message m = new Message(Message.InvokedMethod.OK, e);
		this.node.msgQueue.add(m);
	}

	@Override
	public void coordinationMsg(Election e) throws RemoteException {
		Message m = new Message(Message.InvokedMethod.COORDINATION, e);
		this.node.msgQueue.add(m);
	}
}
