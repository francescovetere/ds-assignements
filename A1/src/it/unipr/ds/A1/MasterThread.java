package it.unipr.ds.A1;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MasterThread implements Runnable {

	private Master master; // reference to the Master node
	private Socket nodeSocket; // this thread's socket towards a Node that requested a registration
	private AtomicInteger atom = new AtomicInteger();


	public MasterThread(final Master master, final Socket nodeSocket) {
		this.master = master;
		this.nodeSocket = nodeSocket;
	}

	@Override
	public void run() {
	
		while(true) {
			ObjectOutputStream os = null;
			ObjectInputStream is = null;

			try {
				// Object obj = null;

				is = new ObjectInputStream(this.nodeSocket.getInputStream());

				Object obj = is.readObject();
	
				if (obj instanceof String) {
					String registrationString = (String) obj;
	
					System.out.println("Master received request to register from " + registrationString);
	
					// Now, we add the <key, val> pair to the master's HashMap of nodes
					Map<Integer, String> nodes = this.master.getNodes();
	
					// Set the Id of the Node with the current size of Nodes list
					int nodeId = nodes.size();
	
					nodes.put(nodeId, registrationString);
	
					// We wait on the pool object, until the Master will wake up this and all other
					// threads in the pool
					// That is, when the admin will enter the termination string
					synchronized (this.master.getPool()) {
						this.master.getPool().wait();
					}
	
					if (os == null) {
						os = new ObjectOutputStream(new BufferedOutputStream(this.nodeSocket.getOutputStream()));
					}
	
					// Now, we send the whole map of nodes to each node
					os.writeObject(nodes);
					os.flush();
	
				}
				else {
					// TODO: Wait for all the statistical results
					// is = new ObjectInputStream((this.nodeSocket.getInputStream()));
					// is.readObject();
	
					// if (!(o instanceof Statistics)) {
					// System.out.println("Not a Stat");
					// System.exit(-1);
					// }

					System.out.println("**Received termination message from one Node --> " + obj);
					System.out.println(this.atom.getAndIncrement());

					synchronized (this.master.getPool()) {
						this.master.getPool().wait();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(0);
			}

		}


	}
}
