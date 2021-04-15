package it.unipr.ds.A2;

import java.rmi.RemoteException;

public class Utility {
	public static void electionLogic(Election from, Election to) throws RemoteException {
		// We respond to the sender with an ok
		from.okMsg(to);
		
		

		// if (m.getService().getId() > this.id) {
		// 	this.state = State.IDLE;
		// } else {
		// 	reset();

		// 	this.state = State.CANDIDATE;

		// 	printSen(Type.CANBE, m.getService().getId());

		// 	((Election) m.getService()).canBe(this.election);
		// }
	}

	public static void coordinationLogic(Election from, Node to) {
		to.state = Node.State.REQUESTER;
	}

	public static void okLogic(Election from, Node to) {
	}
}
