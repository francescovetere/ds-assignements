package it.unipr.ds.A2;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * APIs for election with Bully algorithm
 */
public interface Election extends Remote {
	/**
	 * Return this node's id
	 * @return this node's id
	 * @throws RemoteException
	 */
	int getId() throws RemoteException;

	/**
	 * Receives an election message from node associated with Election object e
	 * @param e the Election object of the node which sent this message
	 * @throws RemoteException
	 */
	void electionMsg(final Election e) throws RemoteException;

	/**
	* Receives an OK message from node associated with Election object e
	* @param e the Election object of the node which sent this message
	* @throws RemoteException
	*/
	void okMsg(final Election e) throws RemoteException;

	/**
	* Receives a coordination message from node associated with Election object e
	* @param e the Election object of the node which sent this message
	* @throws RemoteException
	*/
	void coordinationMsg(final Election e) throws RemoteException;
}
