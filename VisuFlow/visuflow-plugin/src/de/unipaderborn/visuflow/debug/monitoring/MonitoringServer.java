package de.unipaderborn.visuflow.debug.monitoring;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;

import de.unipaderborn.visuflow.Logger;
import de.unipaderborn.visuflow.Visuflow;
import de.unipaderborn.visuflow.builder.GlobalSettings;
import de.unipaderborn.visuflow.model.DataModel;
import de.unipaderborn.visuflow.model.VFUnit;
import de.unipaderborn.visuflow.model.impl.EventDatabase;
import de.unipaderborn.visuflow.util.ServiceUtil;

public class MonitoringServer {

	private ServerSocket serverSocket;
	private Socket clientSocket;
	private Thread t;
	private boolean running = true;
	private DataModel dataModel = ServiceUtil.getService(DataModel.class);
	private EventDatabase eventDatabase = EventDatabase.getInstance();
	private Logger logger = Visuflow.getDefault().getLogger();
	private Lock lock = new ReentrantLock();

	public void start() {
		logger.info("Monitoring server starting");
		logger.info("Server launcher setting lock");
		lock.lock();
		t = new Thread() {
			@Override
			public void run() {
				try {
					logger.info("Monitoring server setting lock");
					lock.lock();
					serverSocket = new ServerSocket(6543);
					logger.info("Monitoring server unlock");
					lock.unlock();
					clientSocket = serverSocket.accept();

					ObjectInputStream objIn = new ObjectInputStream(clientSocket.getInputStream());
					ObjectOutputStream objOut = new ObjectOutputStream(clientSocket.getOutputStream());

					while(running) {
						String msgType = objIn.readUTF();
						if(msgType.equals("CLOSE")) {
							logger.info("Client closed the connection");
							objOut.writeUTF("OK");
							objOut.flush();
							MonitoringServer.this.stop();
						} else if(msgType.equals("UNIT_UPDATE")) {
							String unitFqn = objIn.readUTF();
							String inSet = objIn.readUTF();
							String outSet = objIn.readUTF();
							String unitType = objIn.readUTF();
							VFUnit unit = dataModel.getVFUnit(unitFqn);
							if(unit != null) {
								dataModel.setInSet(unitFqn, "in", inSet, unitType);
								dataModel.setOutSet(unitFqn, "out", outSet, unitType);
								dataModel.setCurrentUnit(unit);
								
								if(inSet.equals(outSet)) {
									eventDatabase.addEvent(unitFqn, false, null, false, null);
								} else {
									List<String> inSetList = parseSet(inSet);
									List<String> outSetList = parseSet(outSet);
									Iterator<String> iterateInSet = inSetList.iterator();
									// remove all coincident data-flow facts
									while(iterateInSet.hasNext()) {
										String currentInItem = iterateInSet.next();
										Iterator<String> iterateOutSet = outSetList.iterator();
										while(iterateOutSet.hasNext()) {
											String currentOutItem = iterateOutSet.next();
											if(currentInItem.equals(currentOutItem)) {
												iterateInSet.remove();
												iterateOutSet.remove();
												break;
											}
										}
									}
									eventDatabase.addEvent(unitFqn, !outSetList.isEmpty(), outSetList, !inSetList.isEmpty(), inSetList);
								}
							}
						} else if(msgType.equals("jimpleFile")) {
							String fileName = objIn.readUTF();
							String fileTxt = objIn.readUTF();
							IFolder outputFolder = ResourcesPlugin.getWorkspace().getRoot().getProject(GlobalSettings.get("AnalysisProject")).getFolder("sootOutput");
							Path file = Paths.get(outputFolder.getLocation().toOSString() + File.separator + fileName);
							Files.write(file, fileTxt.getBytes("utf-8"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
							outputFolder.refreshLocal(IResource.DEPTH_INFINITE, null);

						}
					}
				} catch (EOFException e) {
					logger.info("No more data. The client probably closed the connection");
				} catch (IOException e) {
					logger.error("Monitoring server threw an exception", e);
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		t.setDaemon(true);
		t.setName("Analysis Monitoring Server");
		t.start();
		logger.info("Server launcher unlock");
		lock.unlock();
	}

	public boolean waitForServer(int millis) {
		logger.info("Delegate setting lock");
		try {
			Thread.sleep(1000); // give it a second to start
			lock.tryLock(millis, TimeUnit.MILLISECONDS);
			lock.unlock();
			return true;
		} catch (InterruptedException e) {
			logger.error("Couldn't wait for server to start", e);
			return false;
		}
	}

	public void stop() {
		logger.info("Monitoring server stopping");
		running = false;
		t.interrupt();

		if(clientSocket != null && !clientSocket.isClosed()) {
			try {
				clientSocket.close();
			} catch (IOException e) {
				logger.error("Couldn't close monitoring server connection", e);
			}
		}
		if(serverSocket != null && !serverSocket.isClosed()) {
			try {
				serverSocket.close();
			} catch (IOException e) {
				logger.error("Couldn't close monitoring server connection", e);
			}
		}
	}
	
	/**
	 * Parses the String of data flow facts into a list of data flow facts
	 * @param originalSet The received set as String, created by Soot
	 * @return The formated into data-flow facts
	 */
	private List<String> parseSet(String originalSet){
		originalSet = originalSet.length() > 1 ? originalSet.substring(1, originalSet.length()-1) : originalSet;
		List<String> result = new ArrayList<String>(Arrays.asList(originalSet.split(", ")));
		return result;
	}
}
