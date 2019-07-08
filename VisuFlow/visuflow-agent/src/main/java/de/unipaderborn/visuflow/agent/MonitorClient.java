package de.unipaderborn.visuflow.agent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MonitorClient {

	private static MonitorClient instance = new MonitorClient();
	private Socket socket;
	private InputStream in;
	private OutputStream out;
	private DataOutputStream writer;
	private DataInputStream reader;
	private BlockingQueue<String[]> queue = new LinkedBlockingQueue<>();
	private Thread sendThread;
	boolean running = true;

	private MonitorClient() {}
	public static MonitorClient getInstance() {
		return instance;
	}

	public void connect() throws UnknownHostException, IOException {
		socket = new Socket("localhost", 6543);
		out = socket.getOutputStream();
		in = socket.getInputStream();
		writer = new DataOutputStream(out);
		reader = new DataInputStream(in);
	}

	public void start() {
		sendThread = new Thread() {
			@Override
			public void run() {
				while(running) {
					try {
						String[] msg = queue.take();
						if(msg[0].toString().equals("UNIT_UPDATE")) {
						writer.writeUTF(msg[0]); // msg type
						writer.writeUTF(msg[1]); // fqn
						writer.writeUTF(msg[2]); // inset
						writer.writeUTF(msg[3]); // outset
						writer.writeUTF(msg[4].toString()); // unit type
						writer.flush();
						} else {
							writer.writeUTF(msg[0].toString());
							writer.writeUTF(msg[1].toString());
							writer.writeUTF(msg[2].toString());
							writer.flush();
						}
					} catch (InterruptedException ie) {
						if(running) {
							// if state is running and this threads gets interrupted, we don't
							// have a graceful shutdown!
							System.err.println("Interrupted while still sending messages from queue");
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		};
		sendThread.setName("MonitorClient Async Message Dispatcher");
		sendThread.start();
	}

	public void send(String[] msg) throws IOException {
		writer.writeUTF(msg[0]); // msg type
		writer.writeUTF(msg[1]); // fqn
		writer.writeUTF(msg[2]); // inset
		writer.writeUTF(msg[3]); // outset
		writer.writeUTF(msg[4].toString()); // unit type
	}

	public void sendAsync(String fqn, String inset, String outset, String unitType) throws IOException, InterruptedException {
		String[] msg = new String[5];
	//	System.out.println("in sendasync "+fqn+" ,"+inset+" ,"+outset);
		msg[0] = "UNIT_UPDATE";
		msg[1] = fqn;
		msg[2] = inset;
		msg[3] = outset;
		msg[4] = unitType;
		queue.put(msg);
	}

	public void sendAsyncClasses(String fileName, String text) throws IOException, InterruptedException {
		String[] msg = new String[3];
		msg[0] = "jimpleFile";
		msg[1] = fileName;
		msg[2] = text;
		queue.put(msg);
	}

	public String readResponse() throws IOException {
		return reader.readUTF();
	}

	public void close() throws IOException, InterruptedException {
		queue.add(new String[] {"CLOSE", "", "", "", ""});
		String response = readResponse();
		if(!response.equals("OK")) {
			System.err.println("Server didn't respond with OK to CLOSE");
		}

		// shutting down gracefully
		// FIXME ?!? if the queue is not empty, we might loose some results,
		// because they are not sent. we might have to think about a better
		// solution to close the client
		running = false;
		sendThread.interrupt();
		writer.flush();
		socket.close();
	}
}
