package it.unipr.ds.A2;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.BlockingQueue;

/**
 * APIs for election with Bully algorithm
 */
public class ElectionImpl extends UnicastRemoteObject implements Election {
	private static final long serialVersionUID = 1L;

	private int id;
	private BlockingQueue<Message> msgQueue;

	public ElectionImpl(int id, BlockingQueue<Message> msgQueue) throws RemoteException {
		this.id = id;
		this.msgQueue = msgQueue;
	}

	@Override
	public int getId() throws RemoteException {
		return this.id;
	}

	@Override
	public void electionMsg(Election e) throws RemoteException {
		
	}

	@Override
	public void okMsg(Election e) throws RemoteException {

	}

	@Override
	public void coordinationMsg(Election e) throws RemoteException {
		Message m = new Message(Message.InvokedMethod.COORDINATION, e);
		this.msgQueue.add(m);
		System.out.println("new size" + msgQueue.size());
	}
}
