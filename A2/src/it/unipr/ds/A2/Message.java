package it.unipr.ds.A2;

import java.io.Serializable;
import java.rmi.RemoteException;

public class Message implements Serializable {
	private static final long serialVersionUID = 1L;

	public enum InvokedMethod {
		ELECTION, OK, COORDINATION;
	}

	private InvokedMethod invokedMethod;

	private Election remoteNode;

	public Message(InvokedMethod invokedMethod, Election remoteNode) {
		this.invokedMethod = invokedMethod;
		this.remoteNode = remoteNode;
	}

	public InvokedMethod getInvokedMethod() {
		return this.invokedMethod;
	}

	public Node getRemoteNode() throws RemoteException {
		return this.remoteNode.getNode();
	}

	@Override
	public String toString() {
		String invokedMethodString = null;

		switch (invokedMethod) {
		case ELECTION:
			invokedMethodString = "ELECTION";
			break;
		case OK:
			invokedMethodString = "OK";
			break;
		case COORDINATION:
			invokedMethodString = "COORDINATION";
			break;
		default:
			break;
		}

		int remoteNodeId = -1;
		try {
			remoteNodeId = this.remoteNode.getNode().getId();
		} catch(RemoteException e) {
			e.printStackTrace();
		}

		return "invokedMethod: " + invokedMethodString + " remoteNode: " + remoteNodeId;
	}
}
