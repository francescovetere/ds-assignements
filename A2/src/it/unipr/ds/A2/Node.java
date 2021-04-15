package it.unipr.ds.A2;

import java.io.Serializable;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Node implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	* IDLE: waits for coordination msg
	* CANDIDATE: wants to become coordinator, so send an election msg to higher nodes
	* COORDINATOR: send coordination msg to all other nodes, and manages the resource from now on
	* REQUESTER: asks the coordinator to access the resource
	* WAITER: waits for the coordinator to grant the access to the resource
	* DEAD: keeps tossing a coin until it becomes running again 
	*/
	public enum State {
		IDLE, CANDIDATE, COORDINATOR, REQUESTER, WAITER, DEAD;
	}

	// This node's id
	private int id;

	// The queue of received messages
	public BlockingQueue<Message> msgQueue;

	public State state;

	// The two remote services
	private Registry registry;
	private Election election;

	// The list of total nodes currently in the system
	private List<Election> totalNodes;

	// Constant string, useful when we lookup for other nodes
	private final static String ELECTION_STRING = "Election-";

	// Timeout for receiving an OK answer after we send an election request
	private int OK_TIMEOUT = 1000;

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

		msgQueue = new LinkedBlockingQueue<Message>();

		// The Election object will need two variables in order to reference the Node: its id and its msgQueue
		election = new ElectionImpl(this);

		totalNodes = new ArrayList<>();

		registry.rebind(ELECTION_STRING + id, this.election);
	}

	private void start() throws AccessException, RemoteException, NotBoundException, InterruptedException {
		this.totalNodes = getAllNodes(id);

		if(this.state == State.CANDIDATE) {
			candidate();
		}

		while (true) {
			System.out.println("\tState: " + this.state);
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

		Message msg = msgQueue.take();

		// If we are idle and we receive a coordination message, then we can start asking the coordinator for the resource
		if(msg.getInvokedMethod().equals(Message.InvokedMethod.COORDINATION)) {
			System.out.println("Received a coordination message from " + msg.getRemoteNode().getId());
			this.state = State.REQUESTER;
		}

		// If we are idle and we receive an election request, we answer ok to the sender
		// and we candidate ourselves to become the new coordinator
		else if(msg.getInvokedMethod().equals(Message.InvokedMethod.ELECTION)) {
			System.out.println("Received an election message from " + msg.getRemoteNode().getId());
			System.out.println("Answering ok to " + msg.getRemoteNode().getId());
			
			msg.getRemoteNode().election.okMsg(this.election);

			this.state = State.CANDIDATE;
		}
	}

	private void candidate() throws AccessException, RemoteException, InterruptedException {
		// Send to every node with higher ID an election message
		for (Election e : totalNodes) {
			if (e.getNode().getId() > this.id) {
				System.out.println("Sending election message to " + e.getNode().getId());
				e.electionMsg(this.election);
			}
		}

		// If there are other nodes with higher id and one of them answers OK, we cannot be coordinators
		// But, if no one answer in a certain timeout (e.g.: because I'm the node with the highest id) we can be the coordinators
		Thread.sleep(OK_TIMEOUT);

		Message msg = msgQueue.poll();

		// If we didn't receive any message at all, or we received something different from OK, then we can be coordinators
		if (msg == null || !msg.getInvokedMethod().equals(Message.InvokedMethod.OK)) {
			for (Election e : totalNodes) {
				System.out.println("Sending coordination message to " + e.getNode().getId());
				e.coordinationMsg(this.election);
			}

			System.out.println("I'm the new coordinator");
			this.state = State.COORDINATOR;
		}
	}

	private void coordinator() {
		System.out.println("TODO: Manage the resource...");
	}

	private void requester() {
		System.out.println("TODO: Requesting the resource to the coordinator...");
	}

	private void waiter() {
		System.out.println("TODO: Wait for the coordinator to give me access to the resource...");
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

	public int getId() {
		return this.id;
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
