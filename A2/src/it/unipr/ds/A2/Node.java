package it.unipr.ds.A2;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

public class Node {

	/**
	* FIRST: creates the registry
	* IDLE: waits for coordination msg
	* CANDIDATE: wants to become coordinator, so waits for an OK from higher nodes
	* REQUESTER: asks the coordinator to access the resource
	* WAITER: waits for the coordinator to grant the access to the resource
	* DEAD: keeps tossing a coin until it becomes running again 
	*/
	private enum State {
		FIRST, IDLE, CANDIDATE, COORDINATOR, REQUESTER, WAITER, DEAD;
	}

	public int id;
	private State state;

	// The two remote services
	private Registry registry;
	private Election election;

	// The list of total nodes currently in the system
	private List<Election> totalNodes;

	// Constant string, useful when we lookup for other nodes
	private final static String ELECTION_STRING = "Election-";

	public Node(int id, String nodeType) throws RemoteException {
		this.id = id;

		// First node: creates the registry and set its state to IDLE
		if (nodeType.equals("f")) {
			System.out.println("First node");

			this.registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
			this.state = State.IDLE;
		}

		// Intermediate node: gets the registry and set its state to IDLE
		else if (nodeType.equals("i")) {
			System.out.println("Intermediate node");
			this.registry = LocateRegistry.getRegistry();
			this.state = State.IDLE;
		}

		// Last node: gets the registry and set its state to CANDIDATE
		// It doesn't even start a Bully execution, because he is obviously the coordinator
		else if (nodeType.equals("l")) {
			System.out.println("Last node");

			this.registry = LocateRegistry.getRegistry();
			this.state = State.CANDIDATE;
		}

		else {
			System.out.println("Not supported");
			System.exit(1);
		}

		System.out.println("ID: " + this.id);

		election = new ElectionImpl(this.id);
		totalNodes = new ArrayList<>();

		registry.rebind(ELECTION_STRING + id, this.election);
	}

	private void start() throws AccessException, RemoteException, NotBoundException, InterruptedException {
		while (true) {
			System.out.println(this.state);
			this.totalNodes = getAllNodes(id);

			switch (this.state) {
			case IDLE:
				idle();
				break;
			case CANDIDATE:
				candidate();
				break;
			case COORDINATOR:
				coordinator();
				break;
			case REQUESTER:
				requester();
				break;
			case WAITER:
				waiter();
				break;
			default:
			}

			// Sleep it's totally useless once we implement a sort of synchronization mechaism
			// e.g.: a blocking queue of messages
			Thread.sleep(3000);
		}
	}

	private void idle() throws InterruptedException, RemoteException {
		System.out.println("Waiting for a coordination message");
	}

	private void candidate() throws AccessException, RemoteException {
		// Send to every node with higher ID an election message
		int sentMessages = 0;
		for (Election e : totalNodes) {
			if (e.getId() > this.id) {
				System.out.println("Sending election message to " + e.getId());
				e.electionMsg(this.election);
				++sentMessages;
			}
		}

		// Trivial case:
		// I'm the node with the highest id, I have no other node to send an election msg, so I'm the coordinator
		if (sentMessages == 0) {
			for (Election e : totalNodes) {
				System.out.println("Sending coordination message to " + e.getId());
				e.coordinationMsg(this.election);
			}

			System.out.println("I'm the new coordinator");
			this.state = State.COORDINATOR;
		}

		// General case:
		// There are other nodes with higher id
		// So, we must wait for all their OK messages
		else {
			// ...
		}
	}

	private void coordinator() {
	}

	private void requester() {
	}

	private void waiter() {
	}

	/**
	 * Method that retrieves all the nodes currently registered to the registry, except for the current one
	 * @param id The id of the current node that requests the list of all nodes
	 * @return The list of all nodes, except the one that invoked the method
	 * @throws RemoteException
	 * @throws NotBoundException
	 */
	public List<Election> getAllNodes(final int id) throws RemoteException, NotBoundException {
		String[] nodes = registry.list();

		List<Election> nodesList = new ArrayList<>();

		for (String nodeName : nodes) {
			if (nodeName.startsWith(ELECTION_STRING)) {
				int nodeId = Integer.parseInt(nodeName.substring(ELECTION_STRING.length()));
				if (nodeId != id)
					nodesList.add((Election) registry.lookup(nodeName));
			}
		}

		return nodesList;
	}

	public static void main(String[] args) throws RemoteException, NotBoundException, InterruptedException {
		if (args.length != 2) {
			System.out.println("Usage: java Node <NODE_ID> <NODE_TYPE>");
			System.exit(1);
		}

		int id = Integer.parseInt(args[0]);
		String nodeType = args[1];

		new Node(id, nodeType).start();
	}

}
