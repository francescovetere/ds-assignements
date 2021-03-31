package it.unipr.ds.A1;

import java.net.Socket;
import java.util.Map;
/**
 * Class that handles the reception of new node registrations
 */
public class MasterThreadRegistration implements Runnable {

	private Master master; // reference to the Master node
	private Socket nodeSocket; // this thread's socket towards a Node that requested a registration

	public MasterThreadRegistration(final Master master, final Socket nodeSocket) {
		this.master = master;
		this.nodeSocket = nodeSocket;
	}

	@Override
	public void run() {
		Object obj = Utility.receive(nodeSocket);

		if (obj instanceof String) {
			String registrationString = (String) obj;

			System.out.println("Master received request to register from " + registrationString);

			// Now, we add the <key, val> pair to the master's HashMap of nodes
			Map<Integer, String> nodes = this.master.getNodes();

			// Set the Id of the Node with the current size of Nodes list
			int nodeId = nodes.size();

			nodes.put(nodeId, registrationString);

			// We wait on the pool object, until the Master will wake up this and all other threads in the pool
			// That is, when the admin will enter the termination string
			try {
				synchronized (this.master.getPool()) {
					this.master.getPool().wait();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			Utility.send(nodeSocket, nodes);
		}

	}
}
