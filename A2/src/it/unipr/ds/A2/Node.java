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

	// The queue of nodes waiting to access the resource
	// obviously, this queue has only meaning if this node is the current coordinator
	public BlockingQueue<MutualExclusion> waitingNodes;

	// The state in which this current node is
	private State state;

	// The global RMI registry
	private Registry registry;

	// The two remote services exposed by this node
	private Election election;
	private MutualExclusion mutualExclusion;

	// Reference to the current coordinator
	private MutualExclusion currentCoordinator;

	// Constant strings, useful when we lookup for other nodes
	private final static String ELECTION_STRING = "Election-";
	private final static String MUTUAL_EXCLUSION_STRING = "MutualExclusion-";

	// Timeout between two while iterations, just to slow down the execution
	private int TIMEOUT_WHILE = 3000;

	// Probability of dying
	private static final double H = 0.01;
	// Probability of resuscitate
	private static final double K = 0.05;

	// Useful counter that manages timeouts
	private int attempts = 0;

	// Node is waiting for a coordination message
	private static final int IDLEATTEMPTS = 30;
	// Node is waiting for becoming the coordinator
	private static final int CANDIDATEATTEMPTS = 10;
	// Node is waiting for a grant message
	private static final int WAITERATTEMPTS = 10;
	// Coordinator is waiting for a free message
	private static final int COORDINATORATTEMPTS = 3;

	// Critical section variables
	private boolean resourceAvailable = true;
	private Node inCriticalSection = null;

	// Random generator
	private Random random;

	// Toggle for printing verbose output
	private boolean verbose = true;

	public Node(final int id, final String nodeType) throws RemoteException {
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
		// It doesn't even start a Bully execution, because he is obviously the
		// coordinator
		else if (nodeType.equals("l")) {
			System.out.println("Last node");

			this.registry = LocateRegistry.getRegistry();
			this.state = State.CANDIDATE;

			// Trick for avoiding the first election and directly set this node as coordinator
			this.attempts = CANDIDATEATTEMPTS;
		}

		else {
			System.out.println("Not supported");
			System.exit(1);
		}

		System.out.println("ID: " + this.id);

		msgQueue = new LinkedBlockingQueue<>();
		waitingNodes = new LinkedBlockingQueue<>();

		election = new ElectionImpl(this);
		mutualExclusion = new MutualExclusionImpl(this);

		registry.rebind(ELECTION_STRING + id, this.election);
		registry.rebind(MUTUAL_EXCLUSION_STRING + id, this.mutualExclusion);
	}

	private void start() throws AccessException, RemoteException, NotBoundException, InterruptedException {
		while (true) {
			// Just to slow down the execution
			Thread.sleep(TIMEOUT_WHILE);

			if (verbose) {
				System.out.println();
				System.out.print("\tMessage queue: [ ");
				for (Message m : msgQueue) {
					System.out.print(m.getRemoteNode().id + ":" + m.getInvokedMethod() + " ");
				}
				System.out.println("]");
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

				sendElection();
			}
		}
	}

	private void idle() throws InterruptedException, RemoteException, NotBoundException {
		System.out.println("Waiting for a coordination message");

		Message msg = msgQueue.poll();

		if (msg == null) {
			++this.attempts;

			if (this.attempts > IDLEATTEMPTS) {
				clearState();

				System.out.println("***I recognized that coordinator is dead, so I start an election***");

				// for (Election e : getAllCandidates(this.id)) {
				// 	e.electionMsg(this.election);
				// }

				sendElection();
			}
		}

		else {
			checkMessage(msg);
		}
	}

	private void candidate() throws AccessException, RemoteException, InterruptedException, NotBoundException {
		System.out.println("Candidating...");

		Message msg = msgQueue.poll();

		if (msg == null) {
			++this.attempts;
			if (this.attempts > CANDIDATEATTEMPTS) {
				clearState();

				this.state = State.COORDINATOR;

				System.out.println("***I'm the new coordinator***");

				for (Election e : getAllNodes(this.id)) {
					e.coordinationMsg(this.election);
				}
			}
		}

		else {
			checkMessage(msg);
		}
	}

	private void coordinator() throws AccessException, RemoteException, InterruptedException, NotBoundException {
		System.out.println("Managing the resource...");
		System.out.println("In critical section: " + ((inCriticalSection != null) ? inCriticalSection.id : "None"));

		Message msg = msgQueue.poll();

		if (msg == null) {
			++this.attempts;

			if (this.attempts > COORDINATORATTEMPTS) {
				this.attempts = 0;

				System.out.println("***I recognized that requester is dead, so I free the resource***");

				freeResource();
			}
		}

		else {
			checkMessage(msg);
		}

		// If there is at least one Node waiting for the resource, and the resource's available, 
		// grant access and set 'resourceAvailable' to false
		if (resourceAvailable && (waitingNodes.size() > 0)) {
			MutualExclusion nextRequester = waitingNodes.take();
			nextRequester.grantMsg(this.mutualExclusion);

			resourceAvailable = false;
			inCriticalSection = nextRequester.getNode();
		}

	}

	private void requester() throws RemoteException, InterruptedException {
		// Wait up to 5 seconds before submitting a new request to the coordinator
		Thread.sleep(random.nextInt(6) * 1000);

		System.out.println("Requesting the resource to the coordinator...");

		this.currentCoordinator.requestMsg(this.mutualExclusion);
		this.state = State.WAITER;
	}

	private void waiter() throws AccessException, RemoteException, InterruptedException, NotBoundException {
		System.out.println("Waiting for the coordinator to give me access to the resource...");

		Message msg = msgQueue.poll();

		if (msg == null) {
			++this.attempts;

			if (this.attempts > WAITERATTEMPTS) {
				this.clearState();

				System.out.println("***I recognized that coordinator is dead, so I candidate myself***");

				this.state = State.CANDIDATE;

				// for (Election e : getAllCandidates(this.id)) {
				// 	e.electionMsg(this.election);
				// }
				sendElection();
			}
		}

		else {
			checkMessage(msg);
		}
	}

	/**
	 * Method that retrieves all the nodes currently registered to the registry,
	 * except for the current one
	 * 
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
	 * 
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
	 * Method for taking the correct decision after extracting a message from the
	 * FIFO message queue
	 * 
	 * @param msg The message extracted from the FIFO message queue
	 * @throws AccessException
	 * @throws RemoteException
	 * @throws InterruptedException
	 * @throws NotBoundException
	 */
	private void checkMessage(final Message msg)
			throws AccessException, RemoteException, InterruptedException, NotBoundException {

		if (msg == null)
			return;

		switch (msg.getInvokedMethod()) {
		case COORDINATION:
			// If we are idle and we receive a coordination message, then we can start
			// asking the coordinator for the resource
			System.out.println("Received a coordination message from " + msg.getRemoteNode().getId());

			this.attempts = 0;

			// If I received a coordination message from a higher node, I recognize this new
			// node as the new coordinator
			if (msg.getRemoteNode().getId() > this.id) {
				this.currentCoordinator = msg.getRemoteNode().mutualExclusion;
				this.state = State.REQUESTER;
			}

			// Else I received one from a lower node, in this case I resend a coordination
			// message
			else {
				for (Election e : getAllNodes(this.id)) {
					System.out.println("Sending coordination message to " + e.getNode().getId());
					e.coordinationMsg(this.election);
				}

			}
			break;

		case ELECTION:
			// If we receive an election request, we answer ok to the sender
			// and we candidate ourselves to become the new coordinator
			System.out.println("Received an election message from " + msg.getRemoteNode().getId());

			this.attempts = 0;

			// If I received an election request from a lower node, I answer OK to it and I
			// set myself to candidate
			if (msg.getRemoteNode().getId() < this.id) {
				clearState();

				System.out.println("Answering ok to " + msg.getRemoteNode().getId());

				this.state = State.CANDIDATE;

				msg.getRemoteNode().election.okMsg(this.election);

				sendElection();
			}

			// Else, if the election request was sent by a higher node, I ignore it and I
			// wait again for a coordinator
			else {
				this.state = State.IDLE;
			}
			break;

		case OK:
			// If arrives an OK message, we return to IDLE state waiting for the new
			// coordinator
			System.out.println("Received an OK message from " + msg.getRemoteNode().getId());

			this.attempts = 0;

			// If I received an OK message from a higher node, I cannot be coordinator
			// anymore
			if (msg.getRemoteNode().getId() > this.id) {
				this.state = State.IDLE;
			}

			// Else, if the OK message was sent by a lower node, I ignore it
			break;

		case REQUEST:
			System.out.println("Received a request message from " + msg.getRemoteNode().getId());

			waitingNodes.add(msg.getRemoteNode().mutualExclusion);
			break;
		case GRANT:
			System.out.println("Received a grant message from " + msg.getRemoteNode().getId());

			this.attempts = 0;

			useResource();

			this.currentCoordinator.freeMsg(this.mutualExclusion);
			this.state = State.REQUESTER;
			break;

		case FREE:
			System.out.println("Received a free message from " + msg.getRemoteNode().getId());

			freeResource();
			break;

		}
	}

	/**
	 * Ensures that a node clears its internal state before restarting
	 */
	private void clearState() {
		this.msgQueue.clear();
		this.waitingNodes.clear();
		this.attempts = 0;
	}

	/**
	 * Method runned by the current node in the critical section
	 * @throws RemoteException
	 * @throws InterruptedException
	 */
	private void useResource() throws RemoteException, InterruptedException {
		// do something with a hypothetic resource...
		System.out.println("***I'm working with the resource!***");

		for (int i = 0; i < 100000; ++i)
			random.nextInt();
	}

	/**
	 * Method called by the coordinator in order to free the resource
	 * @throws RemoteException
	 * @throws InterruptedException
	 */
	private void freeResource() throws RemoteException, InterruptedException {
		resourceAvailable = true;
		inCriticalSection = null;
	}

	/**
	 * Method used to send and Election message to all the possible candidates
	 * @throws NotBoundException
	 * @throws RemoteException
	 */
	private void sendElection() throws RemoteException, NotBoundException {
		List<Election> candidates = getAllCandidates(this.id);

		for (Election e : candidates) {
			e.electionMsg(this.election);
			System.out.println("Election message sent to " + e.getNode().getId() + "\n");
		}

	}

	/**
	 * This node's id
	 * @return id
	 */
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
