package it.unipr.ds.A1;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

public class MasterThread implements Runnable {
	private static final long SLEEPTIME = 200;

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

//		while (true) {
			try {
				Object obj = is.readObject();

				if (obj instanceof String) {
					String registrationString = (String) obj;
					Random rand = new Random();
					//Maximum number of nodes
					int N_MAX = 10;

					System.out.println("Master received request to register from " + registrationString);
					
					// Split the string received to obtain id, addr and port of node
					//String[] registrationStringSplitted = registrationString.split(";");
					//int nodeId = Integer.parseInt(registrationStringSplitted[0]);
					//String nodeAddrAndPort = registrationStringSplitted[1];
					int nodeId = rand.nextInt(N_MAX);
					
					// Now, we add the <key, val> pair to the master's HashMap of nodes
					Map<Integer, String> nodes = this.master.getNodes();
					
					// Randomly generate the nodeId for last registered Node
					for (int i : nodes.keySet()) {						
						do {
							nodeId = rand.nextInt(N_MAX);
						} while(nodeId == i);
					}
					
					nodes.put(nodeId, registrationString);
							
//					System.out.println("Current list of registered nodes:");
//					nodes.forEach((k,v) -> System.out.println("<" + k + "; " + v + ">"));
					
					Thread.sleep(SLEEPTIME);

					if (os == null) {
						os = new ObjectOutputStream(new BufferedOutputStream(this.nodeSocket.getOutputStream()));
					}
					
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(0);
			}
//		}
	}
}
