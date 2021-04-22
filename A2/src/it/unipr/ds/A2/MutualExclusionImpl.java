package it.unipr.ds.A2;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * APIs for mutual exclusion with centralized algorithm (implementation)
 */
public class MutualExclusionImpl extends UnicastRemoteObject implements MutualExclusion {
	private static final long serialVersionUID = 1L;

	public Node node;

	public MutualExclusionImpl(Node node) throws RemoteException {
		this.node = node;
	}

	@Override
	public Node getNode() throws RemoteException {
		return this.node;
	}

	@Override
	public void requestMsg(MutualExclusion me) throws RemoteException {
		Message m = new Message(Message.InvokedMethod.REQUEST, me);
		this.node.msgQueue.add(m);
	}

	@Override
	public void grantMsg(MutualExclusion me) throws RemoteException {
		Message m = new Message(Message.InvokedMethod.GRANT, me);
		this.node.msgQueue.add(m);
	}

	@Override
	public void freeMsg(MutualExclusion me) throws RemoteException {
		Message m = new Message(Message.InvokedMethod.FREE, me);
		this.node.msgQueue.add(m);
	}

	
}
