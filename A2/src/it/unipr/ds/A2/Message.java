package it.unipr.ds.A2;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
/**
 * Class that implements a generic message exchanged by the nodes in the system
 */
public class Message implements Serializable {
	private static final long serialVersionUID = 1L;

	public enum InvokedMethod {
		ELECTION, OK, COORDINATION, // election
		REQUEST, GRANT, FREE; // mutual exclusion	
	}

	private InvokedMethod invokedMethod;

	private Remote remoteNode;

	public Message(InvokedMethod invokedMethod, Remote remoteNode) {
		this.invokedMethod = invokedMethod;
		this.remoteNode = remoteNode;
	}

	public InvokedMethod getInvokedMethod() {
		return this.invokedMethod;
	}

	public Node getRemoteNode() throws RemoteException {
		if(remoteNode instanceof Election) {
			return ((Election) remoteNode).getNode();
		}

		else if(remoteNode instanceof MutualExclusion) {
			return ((MutualExclusion) remoteNode).getNode();
		}

		else return null;
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
		case REQUEST:
			invokedMethodString = "REQUEST";
			break;
		case GRANT:
			invokedMethodString = "GRANT";
			break;
		case FREE:
			invokedMethodString = "FREE";
			break;
		default:
		}

		int remoteNodeId = -1;
		try {
			remoteNodeId = this.getRemoteNode().getId();
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		return "invokedMethod: " + invokedMethodString + " remoteNode: " + remoteNodeId;
	}
}
