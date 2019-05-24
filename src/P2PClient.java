import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import javax.swing.JOptionPane;

public class P2PClient {
	static String server1IP;   
	static int server1Port, peerServerPort;    

	public static void main(String[] args) {
		peerServerPort = Integer.parseInt(JOptionPane.showInputDialog("Client Server Port Number"));
		server1IP = JOptionPane.showInputDialog("Ip address for first server in DHT");	
		server1Port = Integer.parseInt(JOptionPane.showInputDialog("DHT First server Port Number"));  
		new Thread(mainRunnable).start();
	}

	static Runnable mainRunnable = new Runnable() {
		public void run() {
			System.out.println("Client Starting");
			Client peerClient = new Client(server1IP, server1Port,peerServerPort);
			new P2PServer(peerServerPort);
			Scanner scannerIn = new Scanner(System.in); 
			String userInput; 

			while (true) {
				System.out.println("Your Inputs: U=Upload, Q=Query For Content, D=Download, E=Exit");
				System.out.print("Enter: ");
				userInput = scannerIn.next();
				
				System.out.print("Enter File Name: ");
				String fileName = scannerIn.next();

				int calculatedServerID = 0;
				for (int i = 0; i < fileName.length(); i++) {
					calculatedServerID += (int) fileName.charAt(i);
				}
				calculatedServerID = calculatedServerID % 2;
				try {
					if (userInput.equalsIgnoreCase("U")) {
						peerClient.uploadData(calculatedServerID, fileName);
					} else if (userInput.equalsIgnoreCase("Q")) {
						peerClient.query(calculatedServerID, fileName);
					}else if (userInput.equalsIgnoreCase("D")) {
						peerClient.downloadData(calculatedServerID, fileName);
					} else if (userInput.equalsIgnoreCase("E")){
						peerClient.exit();
					}else{
						System.out.println("Invalid Input.");
					}
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
	}; 

	public static class Client {
		int peerServerPort;	
		String[] fileName;	
		int[] serverPortNumbers = new int[4];
		String[] serverIPs = new String[4];	
		DatagramSocket clientUDPSocket;		

		public Client(String server1IP, int server1Port, int peerServerPort) {
			this.peerServerPort = peerServerPort;
			this.serverIPs[0] = server1IP; 
			this.serverPortNumbers[0] = server1Port; 

			try {
				clientUDPSocket = new DatagramSocket();
				init();
			}catch (Exception e) { e.printStackTrace();} 
		}

		public void init() throws Exception {
			String message;		
			String statusCode;	
			sendDataToServer("GET ALL IP", serverIPs[0], serverPortNumbers[0]);
			message = receiveDataFromServer();
			Scanner scan = new Scanner(message);
			statusCode = scan.next();
			scan.next();
			scan.next();
			scan.next();
			scan.next();
			scan.next();

			if (statusCode.equals("200")) {
				System.out.println("FROM SERVER -> Client Initilized To Server");
			}
			for (int i = 0; i < 2; i++) {
				serverIPs[i] = scan.next();
				serverPortNumbers[i] = Integer.parseInt(scan.next());
			}
		}

		public void uploadData(int id, String fileName) throws Exception {
			String statusCode;
			String message = "UPLOAD " + fileName + " " + InetAddress.getLocalHost().getHostAddress() + " Padding";
			sendDataToServer(message, serverIPs[id], serverPortNumbers[id]); 
			message = receiveDataFromServer();
			Scanner scan = new Scanner(message);
			statusCode = scan.next();
			if (statusCode.equals("200")) {
				System.out.println("FROM SERVER -> File Added To DHT");
			}
		}

		public void downloadData(int id, String fileName) throws Exception {
			String clientToContactIP;
			String statusCode; // HTTP status code.
			String message = "DOWNLOAD " + fileName + " Padding";
			sendDataToServer(message, serverIPs[id], serverPortNumbers[id]); // Send the message "QUERY" to the appropriate server.
			message = receiveDataFromServer();
			Scanner scan = new Scanner(message);
			statusCode = scan.next();

			if (statusCode.equals("404")) {
				System.out.println("FROM SERVER -> Content Not Found");
			} 

			else if (statusCode.equals("200")) {
				System.out.println("FROM SERVER -> Content Found, IP given ");
				scan = new Scanner(message);
				scan.next(); // status code
				clientToContactIP = scan.next(); // The IP of the client who has the file.


				String HTTPRequest = createHTTPRequest("DOWNLOAD", fileName, "Close", InetAddress.getByName(clientToContactIP).getHostName(), "image/jpeg", "en-us");
				message = connectToPeerServer("OPEN " + fileName, clientToContactIP, peerServerPort); // Connect to the server of the client who has the file.
				scan = new Scanner(message);
				statusCode = scan.next();
				int newPort = scan.nextInt();

				if (statusCode.equals("200")) {
					System.out.println("FROM PEER SERVER -> New Connection Open On Port " + newPort);
					System.out.println("--File Transfer-- START\n" + HTTPRequest + "--File Transfer--END\n");
					connectToUniqueServer(fileName, HTTPRequest, clientToContactIP, newPort);
				}
			} 
		}

		public void exit() throws Exception {
			byte[] receiveData = new byte[1024];
			String statusCode; 
			String message = "EXIT " + serverPortNumbers[0] + " " + serverPortNumbers[1] + " " + serverPortNumbers[2] + " " + serverPortNumbers[3] + " Padding";
			sendDataToServer(message, serverIPs[0], serverPortNumbers[0]);
			message = receiveDataFromServer();
			clientUDPSocket.close();
			Scanner scan = new Scanner(message);
			statusCode = scan.next();

			if (statusCode.equals("200")) {
				System.out.println("FROM SERVER -> All contents removed sucessfully");
			}
			System.exit(0); 
		}

		public void query(int id, String fileName) throws Exception {
			String clientToContactIP;
			String statusCode; 
			String message = "QUERY " + fileName + " Padding";
			sendDataToServer(message, serverIPs[id], serverPortNumbers[id]); 
			message = receiveDataFromServer();
			Scanner scan = new Scanner(message);
			statusCode = scan.next();

			if (statusCode.equals("404")) {
				System.out.println("FROM SERVER -> Content Not Found");
			} else if (statusCode.equals("200")) {
				System.out.println("FROM SERVER -> Content Found, IP given ");
				scan = new Scanner(message);
				scan.next(); 
				clientToContactIP = scan.next(); 
				String HTTPRequest = createHTTPRequest("GET", fileName, "Close", InetAddress.getByName(clientToContactIP).getHostName(), "image/jpeg", "en-us");
				message = connectToPeerServer("OPEN " + fileName, clientToContactIP, peerServerPort); 
				scan = new Scanner(message);
				statusCode = scan.next();
				int newPort = scan.nextInt();

				if (statusCode.equals("200")) {
					System.out.println("FROM PEER SERVER -> New Connection Open On Port " + newPort);
					System.out.println("--HTTP Request Sent to Server-- START\n" + HTTPRequest + "--HTTP Request Sent to Server--END\n");
					//connectToUniqueServer(fileName, HTTPRequest, clientToContactIP, newPort);
				}
			} 
		}

		public void sendDataToServer(String message, String serverIP, int serverPort)throws IOException {
			byte[] sendData = message.getBytes();
			InetAddress internetAddress = InetAddress.getByName(serverIP); 
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, internetAddress, serverPort);
			clientUDPSocket.send(sendPacket);
		}

		public String receiveDataFromServer() throws IOException {
			byte[] receiveData = new byte[1024];
			DatagramPacket recievePacket = new DatagramPacket(receiveData, receiveData.length);
			clientUDPSocket.receive(recievePacket);
			return new String(recievePacket.getData());
		}

		public String connectToPeerServer(String message, String ip, int port) throws UnknownHostException, IOException {
			Socket connectToPeerServer = new Socket(ip, port); 
			OutputStream outToServer = connectToPeerServer.getOutputStream();
			DataOutputStream out = new DataOutputStream(outToServer);
			out.writeUTF(message);
			DataInputStream in = new DataInputStream(connectToPeerServer.getInputStream());
			message = in.readUTF();
			connectToPeerServer.close(); 
			return message;
		}

		public void connectToUniqueServer(String fileName, String httpRequest, String ip, int port) throws UnknownHostException, IOException {
			Socket connectToUniqueServer = new Socket(ip, port); 
			OutputStream outToServer = connectToUniqueServer.getOutputStream();
			DataOutputStream out = new DataOutputStream(outToServer);
			out.writeUTF(httpRequest);
			InputStream in = connectToUniqueServer.getInputStream();
			DataInputStream dis = new DataInputStream(in);
			int len = dis.readInt();
			byte[] data = new byte[len];
			if (len > 0) {
				dis.readFully(data);
			}
			connectToUniqueServer.close();	 
			String s = new String(data);
			Scanner scan = new Scanner(s);
			String responceStaus = scan.nextLine() + "\r\n";
			String temp;
			if (responceStaus.contains("HTTP/1.1 200 OK")) {
				responceStaus = getHTTPResponse(scan, responceStaus);
				File outputfile = new File(fileName + ".jpeg");
				int fileSize = data.length - responceStaus.getBytes().length;
				byte[] backToBytes = new byte[fileSize];

				for (int i = responceStaus.getBytes().length; i < data.length; i++) {
					backToBytes[i - responceStaus.getBytes().length] = data[i];
				}

				FileOutputStream fos = new FileOutputStream(outputfile);
				fos.write(backToBytes);
				fos.close(); 
			} 
			else {
				responceStaus = getHTTPResponse(scan, responceStaus);
			}
			System.out.println("--HTTP Responce Got From Server-- START\n" + responceStaus + "--HTTP Responce Got From Server--END\n");
		}


		public String getHTTPResponse(Scanner scan, String rep) {
			String temp;
			while (scan.hasNext()) {
				temp = scan.nextLine() + "\r\n";
				rep += temp;
				if (temp.equals("\r\n")) {
					break;
				}
			}
			return rep;
		}

		public String createHTTPRequest(String request, String object, String connection, String host, String acceptType, String acceptLan) {
			String req = "";
			req += request + " /" + object + ".jpeg" + " HTTP/1.1\r\n";
			req += "Host: " + host + "\r\n";
			req += "Connection: " + connection + "\r\n";
			req += "Accept: " + acceptType + "\r\n";
			req += "Accept-Language: " + acceptLan + "\r\n\r\n";
			return req;
		}
	}
}
