
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Scanner;

import javax.swing.JOptionPane;


public class DirectoryServer {




	public static void main (String[] args)
	{

		int port = Integer.parseInt(JOptionPane.showInputDialog("Enter port number for main server"));       
		int serverID = Integer.parseInt(JOptionPane.showInputDialog("Enter Server ID")); 	  
		int sucessorPort = Integer.parseInt(JOptionPane.showInputDialog("Enter port number for sucessor server"));   
		String sucessorIP = JOptionPane.showInputDialog("Enter ip address for sucessor server");			  
		Server server = new Server(port, serverID, sucessorPort, sucessorIP);								

	}

	public static class Server {
		//HTTP codes
		final int statusCode200 = 200; // HTTP OK message
		final int statusCode400 = 400; // HTTP Bad Request message
		final int statusCode404 = 404; // HTTP Not Found message
		final int statusCode505 = 505; // HTTP Version Not Supported message

		//initialize stuffs
		int initialPort; 			
		int myPortNumber; 			
		int myServerID; 			
		String myIP;				
		int mySuccessorPortNumber; 	
		String mySuccessorIP; 		
		int mySuccessorServerID; 	

		Thread mainTCPThread; 
		Thread mainUDPThread;

		ServerSocket serverTCPSocket; 	
		DatagramSocket serverUDPSocket; 

		ArrayList<UniqueUDP> clientUniqueUDPList = new ArrayList<UniqueUDP>(); 

		public static Hashtable<String, String> contentList = new Hashtable<String, String>(); 

		public Server(int port, int serverID, int successorPortNumber, String successorIP) {
			
			this.myPortNumber = port;
			this.myServerID = serverID;
			this.mySuccessorPortNumber = successorPortNumber;
			this.mySuccessorIP = successorIP;
			this.mySuccessorServerID = myServerID == 2 ? 1 : myServerID + 1;
			this.initialPort = port + 1;

			try {
		
				this.myIP = InetAddress.getLocalHost().getHostName();
			} 
			
			catch (Exception error) {
				System.out.println("No IP address found...");
			}

			System.out.println("Server will now begin to start......");
			System.out.println("	-IP: " + myIP + "\n	-Port: " + myPortNumber
					+ "\n	-SeverID: " + myServerID + "\n	-Successor's Port: "
					+ mySuccessorPortNumber + "\n	-Successor's IP: "
					+ mySuccessorIP + "\n	-Successor's Server ID: "
					+ mySuccessorServerID);
			try {
				serverTCPSocket = new ServerSocket(myPortNumber);
				if (serverID == 1)
					serverUDPSocket = new DatagramSocket(myPortNumber);

				mainTCPThread = new Thread(mainTCPRunnable);
				mainUDPThread = new Thread(mainUDPRunnable);
				mainTCPThread.start(); 
				mainUDPThread.start(); 
			} 
			catch (Exception error) {
				System.out.println("Port entered is not avaliable, please enter a valid port number");
			}
		}

		//TCP thread.
		Runnable mainTCPRunnable = new Runnable() {
			public void run() {
				System.out.println("TCP Thread Starting...");
				while (true) {
					String message;
					try {
						Socket getFromPredecessor = serverTCPSocket.accept();
						DataInputStream in = new DataInputStream(getFromPredecessor.getInputStream());
						message = in.readUTF();
						System.out.println("FROM PREDECESSOR SERVER -> " + message);

						
						if (myServerID == 1 && message.contains("GET ALL IP")) {
							String[] information = init(message);
							String newMessage = statusCode200 + " " + message + " Padding";
							System.out.println("TO CLIENT -> " + newMessage + "\n");
							sendDataToClient(newMessage, information[0], Integer.parseInt(information[1]));
						} 

						
						else if (message.contains("GET ALL IP")) {
							String[] information = init(message);
							int uniquePort;
							uniquePort = findAvaliableUDPPort();
							clientUniqueUDPList.add(new UniqueUDP(information[0], uniquePort, mySuccessorIP, mySuccessorPortNumber));

							message += " " + myIP + " " + uniquePort;
							System.out.println("TO SUCCESSOR -> " + message + "\n");
							sendToSuccessorServer(message);
						} 

						
						else if (myServerID == 1 && message.contains("EXIT")) {
							String[] information = exit(message);
							message = statusCode200 + " Padding";
							System.out.println("TO CLIENT -> " + message + "\n");
							sendDataToClient(message, information[0], Integer.parseInt(information[1]));
						} 

					
						else if (message.contains("EXIT")) {
							exit(message);
							System.out.println("TO SUCCESSOR -> " + message + "\n");
							sendToSuccessorServer(message);
						}
						getFromPredecessor.close();
					}
					catch (Exception error) {
				
					}
				}
			}
		};

		
		Runnable mainUDPRunnable = new Runnable() {
			public void run() {
				System.out.println("Main UDP Thread Starting...");
				while (true) {
					String message;
					byte[] receiveData = new byte[1024];
					try {
						DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
						serverUDPSocket.receive(receivePacket); 
						message = new String(receivePacket.getData());
						System.out.println("FROM CLIENT -> " + message);

					
						if (message.contains("GET ALL IP")) {
							int uniquePort;
							uniquePort = findAvaliableUDPPort();
							clientUniqueUDPList.add(new UniqueUDP(receivePacket.getAddress().getHostAddress(), uniquePort, mySuccessorIP, mySuccessorPortNumber));
							message = "GET ALL IP " + receivePacket.getPort() + " " + receivePacket.getAddress().getHostAddress() + " " + myIP + " " + uniquePort;
							System.out.println("TO SUCCESSOR SERVER -> " + message);
							sendToSuccessorServer(message);
						}
					} 
					catch (Exception error) {
						
					}
				}
			}
		};

	
		public int findAvaliableUDPPort() {
			int port = 0;
			int portFind = initialPort;
			boolean done = false;
			while (done == false) {

				
				try {
					DatagramSocket tryPort = new DatagramSocket(portFind);
					done = true;
					tryPort.close();
					break;
				} 
				
				catch (SocketException e) {
					portFind++;
				}
			}
			port = portFind;
			return port; 
		}

		
		public void sendToSuccessorServer(String message) throws UnknownHostException, IOException {
			Socket connectToSuccessor = new Socket(mySuccessorIP, mySuccessorPortNumber); 
			OutputStream outToServer = connectToSuccessor.getOutputStream();
			DataOutputStream out = new DataOutputStream(outToServer);
			out.writeUTF(message);		
			connectToSuccessor.close(); 
		}

	
		public void sendDataToClient(String theMessage, String clientIP, int clientPort) throws IOException {
			byte[] sendData = new byte[1024]; 
			sendData = theMessage.getBytes(); 
			InetAddress ip = InetAddress.getByName(clientIP); 
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, clientPort); 
			serverUDPSocket.send(sendPacket); 
		}

		
		public String[] exit(String message) {
			Scanner scan = new Scanner(message);
			scan.next(); 

			String clientIP = scan.next(); 
			int clientPort = scan.nextInt(); 
			int port = 0;					

			for (int i = 0; i < myServerID; i++) {
				port = scan.nextInt();
			}

			for (int i = 0; i < clientUniqueUDPList.size(); i++) {
				if (clientUniqueUDPList.get(i).clientIP.equals(clientIP)
						&& clientUniqueUDPList.get(i).uniquePort == port) {
					clientUniqueUDPList.get(i).kill();
					clientUniqueUDPList.remove(i);
					break;
				}
			}

			Enumeration em = contentList.keys();
			while (em.hasMoreElements()) {
				String key = (String) em.nextElement();
				if (contentList.get(key).equals(clientIP))
					contentList.remove(key);
			}
			return new String[] { clientIP, clientPort + "" };
		}

		
		public String[] init(String message) {
			Scanner scan = new Scanner(message);
			scan.next();
			scan.next();
			scan.next();
			int clientPort = scan.nextInt();
			String clientIP = scan.next();
			return new String[] { clientIP, clientPort + "" };
		}
	}

	public static class UniqueUDP {
		final int statusCode200 = 200;
		final int statusCode400 = 400; 
		final int statusCode404 = 404; 
		final int statusCode505 = 505; 
		
		int uniquePort;	 
		String clientIP; 
		Thread uniqueThread;  
		DatagramSocket uniqueUDPSocket; 
		String mySuccessorIP;  
		int mySuccessorPortNumber; 

	
		public UniqueUDP(String clientIP, int port, String mySuccessorIP, int mySuccessorPortNumber) {
			this.clientIP = clientIP;
			this.mySuccessorIP = mySuccessorIP;
			this.mySuccessorPortNumber = mySuccessorPortNumber;
			uniquePort = port;
			try {
				
				uniqueUDPSocket = new DatagramSocket(port);
			} 
			catch (SocketException e) {
				System.out.println("Port not avaliable.");
			}
			uniqueThread = new Thread(UniqueRunnable);
			uniqueThread.start();
		}

		
		Runnable UniqueRunnable = new Runnable() {
			public void run() {
				while (true) {
					String message;
					byte[] receiveData = new byte[1024];
					try {
						DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length); // UDP Packet.
						uniqueUDPSocket.receive(receivePacket);
						message = new String(receivePacket.getData());
						System.out.println("FROM CLIENT -> " + message);

				
						if (message.contains("UPLOAD")) {
							Scanner scan = new Scanner(message);
							System.out.println("YARRRRRRR");
							scan.next();
							String fileName = scan.next(); 
							System.out.println("TO CLIENT -> " + statusCode200 + " Padding" + "\n");
							sendDataToClient(statusCode200 + " Padding", clientIP, receivePacket.getPort()); 
							Server.contentList.put(fileName, clientIP); 
						} 

						else if (message.contains("QUERY")) {
							Scanner scan = new Scanner(message);
							scan.next(); 
							String fileName = scan.next(); 

							String ip = Server.contentList.get(fileName);
							
							if (ip == null) {
								System.out.println("TO CLIENT -> " + statusCode404 + " Padding" + "\n");
								sendDataToClient(statusCode404 + " Padding", clientIP, receivePacket.getPort());
							} 
							
							else {
								System.out.println("TO CLIENT -> " + statusCode200 + " " + ip + " Padding" + "\n");
								sendDataToClient(statusCode200 + " " + ip + " Padding", clientIP, receivePacket.getPort());
							}
						} 
						
				
						else if (message.contains("DOWNLOAD")) {
							Scanner scan = new Scanner(message);
							scan.next(); 
							String fileName = scan.next();

							String ip = Server.contentList.get(fileName);
							
							if (ip == null) {
								System.out.println("TO CLIENT -> " + statusCode404 + " Padding" + "\n");
								sendDataToClient(statusCode404 + " Padding", clientIP, receivePacket.getPort());
							} 
						
							else {
								System.out.println("TO CLIENT -> " + statusCode200 + " " + ip + " Padding" + "\n");
								sendDataToClient(statusCode200 + " " + ip + " Padding", clientIP, receivePacket.getPort());
							}

						        
						       
						} 
						

					
						else if (message.contains("EXIT")) {
							Scanner scan = new Scanner(message);
							message = scan.next() + " " + clientIP + " " + receivePacket.getPort();
							for (int i = 0; i < 4; i++)
								message += " " + scan.next();
							System.out.println("TO SUCCESSOR SERVER -> " + message);
							sendToSuccessorServer(message);
						}
					} 
					catch (Exception error) {
						// aint catching nothing
					}
				}
			}
		};

		
		public void kill() {
			uniqueThread.stop();
			uniqueUDPSocket.close();
		}

		
		public void sendToSuccessorServer(String message) throws UnknownHostException, IOException {
			Socket connectToSuccessor = new Socket(mySuccessorIP, mySuccessorPortNumber); 
			OutputStream outToServer = connectToSuccessor.getOutputStream();
			DataOutputStream out = new DataOutputStream(outToServer);
			out.writeUTF(message); 
			connectToSuccessor.close(); 
		}

		
		public void sendDataToClient(String theMessage, String clientIP, int clientPort) throws IOException {
			byte[] sendData = new byte[1024];
			sendData = theMessage.getBytes();
			InetAddress ip = InetAddress.getByName(clientIP);	
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, clientPort); 
			uniqueUDPSocket.send(sendPacket); 
		}
	}
}
