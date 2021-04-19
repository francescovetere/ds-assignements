package it.unipr.ds.A2;

import java.io.Serializable;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.lang.model.util.ElementScanner6;

public class Node implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	* IDLE: waits for coordination msg
	* CANDIDATE: wants to become coordinator, so send an election msg to higher nodes
	* COORDINATOR: send coordination msg to all other nodes, and manages the resource from now on
	* REQUESTER: asks the coordinator to access the resource
	* WAITER: waits for the coordinator to grant the access to the resource
	* DEAD: flips a coin until it becomes running again
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

	// Constant string, useful when we lookup for other nodes
	private final static String ELECTION_STRING = "Election-";

	private int TIMEOUT_OK = 1000; // Timeout for receiving an OK answer after we send an election request
	private int TIMEOUT_WHILE = 500; // Timeout between two while iterations

	private static final double H = 0.03;
	private static final double K = 0.05;

	private Random random;

	// Toggle for printing verbose output
	private boolean verbose = true;

	public Node(int id, String nodeType) throws RemoteException {
		this.id = id;
		this.random = new Random();

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

		// The Election object will need the reference to Node
		election = new ElectionImpl(this);

		registry.rebind(ELECTION_STRING + id, this.election);
	}

	private void start() throws AccessException, RemoteException, NotBoundException, InterruptedException {
		if (this.state == State.CANDIDATE) {
			candidate();
		}

		while (true) {
			// Sleep it's totally useless once we implement a sort of synchronization mechaism
			// e.g.: a blocking queue of messages
			Thread.sleep(TIMEOUT_WHILE);

			if (verbose) {
				System.out.println("\tState: " + this.state);
				System.out.println("\tMessage queue:");
				for (Message m : msgQueue) {
					System.out.println("\t" + m);
				}
			}

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

			double flippedCoin = this.random.nextDouble();
			// System.out.println("Flipped coin: " + flippedCoin);

			// I'm alive and I die 
			if ((this.state != State.DEAD) && (flippedCoin < H)) {
				System.out.println("\t\t***Node is dead***");
				this.state = State.DEAD;
			}

			// I'm dead and I return alive
			else if ((this.state == State.DEAD) && (flippedCoin < K)) {
				System.out.println("\t\t***Node has restarted***");
				// In any case, if I'm turned back alive, I reset my internal state
				this.clearState();

				this.state = State.CANDIDATE;
				// List<Election> candidates = getAllCandidates(this.id);

				// //There's no candidate, so I'm automatically the new Coordinator 
				// if (candidates.size() == 0) {
				// 	this.state = State.COORDINATOR;

				// 	for (Election e : getAllNodes(this.id)) {
				// 		System.out.println("Sending coordination message to " + e.getNode().getId());
				// 		e.coordinationMsg(this.election);
				// 	}

				// } else {
				// 	this.state = State.CANDIDATE;

				// 	for (Election e : getAllNodes(this.id)) {
				// 		if (e.getNode().getId() > this.id) {
				// 			System.out.println("Sending election message to " + e.getNode().getId());
				// 			e.electionMsg(this.election);
				// 		}
				// 	}
				// }

			}
		}
	}

	private void idle() throws InterruptedException, RemoteException, NotBoundException {
		System.out.println("Waiting for a coordination message");

		Message msg = msgQueue.poll(); // TODO: take() ?

		// Make a check on the type of Message and handle the change of State 
		checkQueue(msg);
	}

	private void candidate() throws AccessException, RemoteException, InterruptedException, NotBoundException {
		// Send to every node with higher ID an election message
		for (Election e : getAllCandidates(this.id)) {
			// if (e.getNode().getId() > this.id) {
				System.out.println("Sending election message to " + e.getNode().getId());
				e.electionMsg(this.election);
			// }
		}

		// If there are other nodes with higher id and one of them answers OK, we cannot be coordinators
		// But, if no one answer in a certain timeout (e.g.: because I'm the node with the highest id) we can be the coordinators
		Thread.sleep(TIMEOUT_OK);

		Message msg = msgQueue.poll();

		// boolean arrivesOK = msg.getInvokedMethod().equals(Message.InvokedMethod.OK);

		// If we didn't receive any message at all, or we received something different from OK, then we can be coordinators
		if (msg==null || !msg.getInvokedMethod().equals(Message.InvokedMethod.OK)) {
			for (Election e : getAllNodes(this.id)) {
				System.out.println("Sending coordination message to " + e.getNode().getId());
				e.coordinationMsg(this.election);
			}

			clearState();

			System.out.println("I'm the new coordinator");
			this.state = State.COORDINATOR;
		}
		// If arrives an OK message, we return to IDLE state waiting for the new coordinator
		else if (msg.getInvokedMethod().equals(Message.InvokedMethod.OK)) {
			this.state = State.IDLE;
		}

	}

	private void coordinator() throws AccessException, RemoteException, InterruptedException, NotBoundException {
		System.out.println("TODO: Manage the resource...");

		Message msg = msgQueue.take(); // TODO: take() ?

		checkQueue(msg);
	}

	private void requester() throws AccessException, RemoteException, InterruptedException, NotBoundException {
		System.out.println("TODO: Requesting the resource to the coordinator...");

		Message msg = msgQueue.poll(); // TODO: take() ?

		// Make a check on the type of Message and handle the change of State 
		checkQueue(msg);
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

	/**
	 * Method that retrieves all the possibly candidates nodes
	 * @param id The id of the current node that requests the list of candidates
	 * @return The list of all nodes, except the one that invoked the method
	 * @throws RemoteException
	 * @throws NotBoundException
	 */
	public List<Election> getAllCandidates(final int id) throws RemoteException, NotBoundException {
		String[] nodes = registry.list();

		List<Election> nodesList = new ArrayList<>();

		for (String nodeName : nodes) {
			if (nodeName.startsWith(ELECTION_STRING)) {
				int nodeId = Integer.parseInt(nodeName.substring(ELECTION_STRING.length()));
				if (nodeId > id)
					nodesList.add((Election) registry.lookup(nodeName));
			}
		}

		return nodesList;
	}

	/**
	 * Method for taking the correct decision after extracting a message from the message queue,
	 * being in a certain state
	 * 
	 * @param state The current state
	 * @param msg The message extracted from the FIFO message queue
	 * @throws AccessException
	 * @throws RemoteException
	 * @throws InterruptedException
	 * @throws NotBoundException
	 */
	public void checkQueue(Message msg) throws AccessException, RemoteException, InterruptedException, NotBoundException {

		if (msg == null) {
			return;
		}

		// If we are idle and we receive a coordination message, then we can start asking the coordinator for the resource
		if (msg.getInvokedMethod().equals(Message.InvokedMethod.COORDINATION)) {
			System.out.println("Received a coordination message from " + msg.getRemoteNode().getId());

			// If I received a coordination message from a higher node, I recognize this new node as the new coordinator
			if (msg.getRemoteNode().getId() > this.id) {
				this.state = State.REQUESTER;
			}
			// Else I received one from a lower node, in this case I resend a coordination message
			else {
				// TODO
				System.out.println("\n\n\n*** SOMETHING IS WRONG ***\n\n\n");

				for (Election e : getAllNodes(this.id)) {
					System.out.println("Sending coordination message to " + e.getNode().getId());
					e.coordinationMsg(this.election);
				}

			}
		}

		// If we receive an election request, we answer ok to the sender
		// and we candidate ourselves to become the new coordinator
		else if (msg.getInvokedMethod().equals(Message.InvokedMethod.ELECTION)) {
			System.out.println("Received an election message from " + msg.getRemoteNode().getId());

			// If I received an election request from a lower node, I answer OK to it and I set myself to candidate
			if (msg.getRemoteNode().getId() < this.id) {
				System.out.println("Answering ok to " + msg.getRemoteNode().getId());

				this.state = State.CANDIDATE;

				msg.getRemoteNode().election.okMsg(this.election);
			}

			// Else, if the election request was sent by a higher node, I ignore it and I wait again for a coordinator
			else {
				this.state = State.IDLE;
			}
		}

		// If arrives an OK message, we return to IDLE state waiting for the new coordinator
		else if (msg.getInvokedMethod().equals(Message.InvokedMethod.OK)) {
			System.out.println("Received an OK message from " + msg.getRemoteNode().getId());

			// If I received an OK message from a higher node, I cannot be coordinator anymore
			if (msg.getRemoteNode().getId() > this.id) {
				this.state = State.IDLE;
			}

			// Else, if the OK message was sent by a lower node, I ignore it
		}

	}

	/**
	 * Ensures that a node clears its internal state before restarting
	 */
	private void clearState() {
		this.msgQueue.clear();
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
