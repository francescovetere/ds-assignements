package it.unipr.ds.A2;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * APIs for election with Bully algorithm
 */
public class ElectionImpl extends UnicastRemoteObject implements Election {
	private static final long serialVersionUID = 1L;

	private int id;

	public ElectionImpl(int id) throws RemoteException {
		this.id = id;
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

	}
}
