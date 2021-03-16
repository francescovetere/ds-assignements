package it.unipr.ds.A1;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Random;

/**
 * 
 * Class that manages a single thread of execution inside a Node object
 */
public class NodeThread implements Runnable {
	private static final long SLEEPTIME = 200;

	private Node node;
	private Socket socket;

	public NodeThread(final Node n, final Socket c) {
		this.node = n;
		this.socket = c;
	}

	@Override
	public void run() {
		ObjectInputStream is = null;
		ObjectOutputStream os = null;

		try {
			is = new ObjectInputStream(new BufferedInputStream(this.socket.getInputStream()));
		} catch (Exception e) {
			e.printStackTrace();

			return;
		}

		while (true) {
			try {
				Object i = is.readObject();

				if (i instanceof Message) {
					Message m = (Message) i;

					Thread.sleep(SLEEPTIME);

					if (os == null) {
						os = new ObjectOutputStream(new BufferedOutputStream(this.socket.getOutputStream()));
					}

					Message rs = new Message("1.1.1.1", 0);

					os.writeObject(rs);
					os.flush();
					
//					// Messaggio di chiusura
//					if (rs.getValue() == 0) {
//						// Se questo thread è l'ultimo, chiude anche la socket del main thread del server
//						if (this.server.getPool().getActiveCount() == 1) {	
//							this.server.close();
//						}
//
//						this.socket.close(); // in ogni caso, chiude sempre la socket di questo thread
//
//						return;
//					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(0);
			}
		}
	}
}
