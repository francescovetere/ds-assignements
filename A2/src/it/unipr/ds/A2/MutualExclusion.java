package it.unipr.ds.A2;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * APIs for election with Bully algorithm
 */
public interface MutualExclusion extends Remote {
	/**
	* Return this node's reference
	* @return this node's reference
	* @throws RemoteException
	*/
	Node getNode() throws RemoteException;

	/**
	 * Receives a request message from node associated with MutualExclusion object me
	 * @param me the MutualExclusion object of the node which sent this message
	 * @throws RemoteException
	 */
	void requestMsg(final MutualExclusion me) throws RemoteException;

	/**
	 * Receives a grant message from node (coordinator) associated with MutualExclusion object me
	 * @param me the MutualExclusion object of the node (coordinator) which sent this message
	 * @throws RemoteException
	 */
	void grantMsg(final MutualExclusion me) throws RemoteException;

	/**
	* Receives a free message from node associated with MutualExclusion object me
	* @param me the MutualExclusion object of the node which sent this message
	* @throws RemoteException
	*/
	void freeMsg(final MutualExclusion me) throws RemoteException;
}
