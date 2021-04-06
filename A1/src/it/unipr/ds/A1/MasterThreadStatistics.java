package it.unipr.ds.A1;

import java.net.Socket;

/**
 * Class that handles the reception of final node statistics
 */
public class MasterThreadStatistics implements Runnable {
	private Socket nodeSocket; // this thread's socket towards a Node that sent its statistics
	private Master master;     // reference to the Master

	public MasterThreadStatistics(final Master master, final Socket nodeSocket) {
		this.master = master;
		this.nodeSocket = nodeSocket;
	}

	@Override
	public void run() {
		Object obj = Utility.receive(this.nodeSocket);

		if (obj instanceof Statistics) {
			Statistics statistics = (Statistics) obj;
			System.out.println("**Received termination message from Node " + statistics.nodeID);

			master.statsMap.put(statistics.nodeID, statistics);
		}
	}
}
