package it.unipr.ds.A1;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.Random;

public class MasterThread implements Runnable {

	private Master master;     // reference to the Master node
	private Socket nodeSocket; // this thread's socket towards a Node that requested a registration
	
	public MasterThread(final Master master, final Socket nodeSocket) {
		this.master = master;
		this.nodeSocket = nodeSocket;
	}

	@Override
	public void run() {
		ObjectInputStream is = null;
		ObjectOutputStream os = null;

		try {
			is = new ObjectInputStream(new BufferedInputStream(this.nodeSocket.getInputStream()));
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		try {
			Object obj = is.readObject();

			if (obj instanceof String) {
				String registrationString = (String) obj;
				
				Random rand = new Random();
				
				//Maximum number of nodes
				int N_MAX = 10;

				System.out.println("Master received request to register from " + registrationString);
					
				// Split the string received to obtain id, addr and port of node
				/* 
				   String[] registrationStringSplitted = registrationString.split(";");
				   int nodeId = Integer.parseInt(registrationStringSplitted[0]);
				   String nodeAddrAndPort = registrationStringSplitted[1];
				*/
					
				// Now, we add the <key, val> pair to the master's HashMap of nodes
				Map<Integer, String> nodes = this.master.getNodes();
					
				// First, we randomly generate the nodeId for last registered Node
					
				int nodeId = rand.nextInt(N_MAX);
				// TODO 
				// E se invece a nodeId fosse semplicemente assegnato nodes.size() ? Ovvero:
//				int nodeId = nodes.size();
					// Potrebbero forse esserci problemi di concorrenza, ma in quel caso allora potremmo usare un AtomicInteger?
					
				for (int i : nodes.keySet()) {						
					do {
						nodeId = rand.nextInt(N_MAX);
//						nodeId = nodes.size();
					} while(nodeId == i);
				}
					
				nodes.put(nodeId, registrationString);
				
				// We wait on the pool object, until the Master will wake up this and all other threads in the pool
				// That is, when the admin will enter the termination string
				synchronized(this.master.getPool()) {
					this.master.getPool().wait();
				}
				
				if (os == null) {
					os = new ObjectOutputStream(new BufferedOutputStream(this.nodeSocket.getOutputStream()));
				}
				
				// Now, we send the whole map of nodes to each node
				os.writeObject(nodes);
				os.flush();
					
			}
		} catch (Exception e) {
				e.printStackTrace();
				System.exit(0);
		}
		
	}
}
