package it.unipr.ds.A2;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Node {
	/**
	* FIRST: creates the registry
	* IDLE: waits for coordination msg
	* CANDIDATE: wants to become coordinator, so waits for an OK from bigger nodes
	* REQUESTER: asks the coordinator to access the resource
	* WAITER: waits for the coordinator to grant the access to the resource
	* DEAD: keeps spinning a coin until it becomes running again 
	*/
	private enum State {
		FIRST, IDLE, CANDIDATE, COORDINATOR, REQUESTER, WAITER, DEAD;
	}

	private static int id;
	private State state;

	private Registry registry;

	public Node(String node_type) throws RemoteException {
		// First node: creates the registry and set its state to IDLE
		if (node_type.equals("f")) {
			System.out.println("First node");

			this.registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
			this.state = State.IDLE;
		}

		// Intermediate node: gets the registry and set its state to IDLE
		else if (node_type.equals("i")) {
			System.out.println("Intermediate node");

			this.registry = LocateRegistry.getRegistry();
			this.state = State.IDLE;
		}

		// Last node: gets the registry and set its state to CANDIDATE
		// It doesn't even start a Bully execution, because he is obviously the coordinator
		else if (node_type.equals("l")) {
			System.out.println("Last node");

			this.registry = LocateRegistry.getRegistry();
			this.state = State.CANDIDATE;
		}

		else {
			System.out.println("Not supported");
			System.exit(1);
		}

		System.out.println("ID: " + id);
	}

	private void start() {
		while(true) {

		}
	}

	public static void main(String[] args) throws RemoteException {
		if (args.length != 2) {
			System.out.println("Usage: java Node <NODE_ID> <NODE_TYPE>");
			System.exit(1);
		}

		id = Integer.parseInt(args[0]);
		String node_type = args[1];

		new Node(node_type).start();
	}

}
