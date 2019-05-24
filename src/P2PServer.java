import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;

import javax.imageio.ImageIO;

public class P2PServer {
	static final int statusCode200 = 200;
	static final int statusCode400 = 400; 
	static final int statusCode404 = 404; 
	static final int statusCode505 = 505;

	int initialPort;
	ServerSocket peerServerTCPSocket; 
	public static ArrayList<UniqueTCP> peerClientUniqueList = new ArrayList<UniqueTCP>();

	public P2PServer(int port) {
		initialPort = port;
		try {
			peerServerTCPSocket = new ServerSocket(initialPort);
			new Thread(mainTCPRunnable).start();
		}catch (IOException e) {
			System.out.println("Port Not Available");
		}
	}

	Runnable mainTCPRunnable = new Runnable() {
		public void run() {
			while (true) {
				String msg;	
				try {
					Socket getFromClient = peerServerTCPSocket.accept(); 
					DataInputStream in = new DataInputStream(getFromClient.getInputStream());
					msg = in.readUTF();
					Scanner scan = new Scanner(msg);
					scan.next();
					int port = findAvaliableUDPPort();
					peerClientUniqueList.add(new UniqueTCP(port));
					msg = statusCode200 + " " + port;
					DataOutputStream out = new DataOutputStream(getFromClient.getOutputStream());
					out.writeUTF(msg);
					getFromClient.close();
				} catch (IOException e) {
					e.printStackTrace();
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
				ServerSocket tryPort = new ServerSocket(portFind);
				done = true;
				tryPort.close();
				break;
			}catch (Exception e) {
				portFind++;
			}
		}
		port = portFind;
		return port;
	}
	
	public class UniqueTCP {
		ServerSocket unqTCPSock; 	
		Thread TCPThread;	 

		public UniqueTCP(int port) {
			try {
				unqTCPSock = new ServerSocket(port); 
				TCPThread = new Thread(TCPRunnable);
				TCPThread.start();
			}catch (IOException e) {
				e.printStackTrace();
			}
		}

		Runnable TCPRunnable = new Runnable() {
			public void run() {
				try {
					byte[] finalBytesArray = null;
					String msg, fileName, req, httpVersion, HTTPResponce, connection, contentType = ""; 		
					String timeString = getCurrentTime();	
					Socket socket = unqTCPSock.accept();					
					DataInputStream in = new DataInputStream(socket.getInputStream());
					msg = in.readUTF();
					Scanner scan = new Scanner(msg);
					req = scan.next();
					
					if (req.equals("DOWNLOAD"))
					{
						
						fileName = scan.next();
						httpVersion = scan.next(); 
						connection = "Close";
						contentType = "image/jpeg";
						
						fileName = fileName.substring(1); // Remove the '/'.
						File f = new File(fileName);
						
						String newFileName = "";
						String newFileName2 = "";
						newFileName = fileName.substring(0, fileName.indexOf(".jpeg"));
						newFileName2 = fileName.substring(0, fileName.indexOf(".jpeg"));
						newFileName += "---Trial---.jpeg";
						File isFileNameBad = new File(newFileName);
						isFileNameBad.createNewFile();
						isFileNameBad.delete();
						File f2 = new File(newFileName2 + ".jpg");
						f = new File(newFileName2 + ".jpg");


						double fileSizeBytes = f.length();
						String lastMod = getFileModifiedTime(f);
						HTTPResponce = createHTTPResponce(statusCode200, timeString, lastMod, "bytes", Integer.toString((int) fileSizeBytes), connection, contentType);
						byte[] httpToBytes = HTTPResponce.getBytes(Charset.forName("UTF-8"));
						byte[] fileToBytes = new byte[(int) f.length()];									
						FileInputStream fileIn = new FileInputStream(f);
						fileIn.read(fileToBytes);
						fileIn.close();

						finalBytesArray = new byte[httpToBytes.length + fileToBytes.length];
						System.arraycopy(httpToBytes, 0, finalBytesArray, 0, httpToBytes.length);
						System.arraycopy(fileToBytes, 0, finalBytesArray, httpToBytes.length, fileToBytes.length);

						//convert BufferedImage to byte array

						BufferedImage originalImage = ImageIO.read(f);
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						ImageIO.write(originalImage, "jpg", baos);
						baos.flush();
						fileToBytes = baos.toByteArray();
						baos.close();
					}

					
				 if (req.equals("GET")) {
						fileName = scan.next();
						httpVersion = scan.next(); 
						connection = "Close";
						contentType = "image/jpeg";
						
						if (httpVersion.equals("HTTP/1.1")) {
							fileName = fileName.substring(1);
							File f = new File(fileName);
							try {
								String newFN, newFN2 = "";
								newFN = fileName.substring(0, fileName.indexOf(".jpeg"));
								newFN2 = fileName.substring(0, fileName.indexOf(".jpeg"));
								newFN += "---Trial---.jpeg";
								File isFNBad = new File(newFN);
								isFNBad.createNewFile();
								isFNBad.delete();
								File f2 = new File(newFN2 + ".jpg");
								
								if (f2.exists())
									f = new File(newFN2 + ".jpg");
								
								if (f.exists()) {
									double fileSizeBytes = f.length();
									String lastMod = getFileModifiedTime(f);
									HTTPResponce = createHTTPResponce(statusCode200, timeString, lastMod, "bytes", Integer.toString((int) fileSizeBytes), connection, contentType);
									byte[] httpToBytes = HTTPResponce.getBytes(Charset.forName("UTF-8"));
									byte[] fileToBytes = new byte[(int) f.length()];									
									FileInputStream fileIn = new FileInputStream(f);
									fileIn.read(fileToBytes);
									fileIn.close();

									finalBytesArray = new byte[httpToBytes.length + fileToBytes.length];
									System.arraycopy(httpToBytes, 0, finalBytesArray, 0, httpToBytes.length);
									System.arraycopy(fileToBytes, 0, finalBytesArray, httpToBytes.length, fileToBytes.length);
								} else {
									HTTPResponce = createHTTPResponce(statusCode404, timeString, null, null, null, connection, null);
									finalBytesArray = HTTPResponce.getBytes(Charset.forName("UTF-8"));
								}
							} catch (Exception e) {
								HTTPResponce = createHTTPResponce(statusCode400, timeString, null, null, null, connection, null);
								finalBytesArray = HTTPResponce.getBytes(Charset.forName("UTF-8"));
							}
						} else {
							HTTPResponce = createHTTPResponce(statusCode505, timeString, null, null, null, connection, null);
							finalBytesArray = HTTPResponce.getBytes(Charset.forName("UTF-8"));
						}
					}
					OutputStream out = socket.getOutputStream();
					DataOutputStream dos = new DataOutputStream(out);
					dos.writeInt(finalBytesArray.length);
					dos.write(finalBytesArray, 0, finalBytesArray.length);
					socket.close();
					unqTCPSock.close();
					for (int i = 0; i < P2PServer.peerClientUniqueList.size(); i++) {
						if (P2PServer.peerClientUniqueList.get(i).equals(this)) {
							P2PServer.peerClientUniqueList.remove(i);
							TCPThread.stop();
							break;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};

		public String getCurrentTime() {
			Date d = new Date();
			Scanner s = new Scanner(d.toString());
			String dayName = s.next();
			String month = s.next();
			String dateNumber = s.next();
			DateFormat timeFormat = new SimpleDateFormat("yyyy HH:mm:ss");
			Date time = new Date();
			String timeString = dayName + ", " + dateNumber + " " + month + " " + timeFormat.format(time) + " GMT";
			return timeString;
		}

		public String getFileModifiedTime(File f) {
			Date date = new Date(f.lastModified());
			Scanner scan = new Scanner(date.toString());
			String dayName = scan.next();
			String month = scan.next();
			String dateNumber = scan.next();
			DateFormat timeFormat = new SimpleDateFormat("yyyy HH:mm:ss");
			Date time = new Date(f.lastModified());
			String timeString = dayName + ", " + dateNumber + " " + month + " " + timeFormat.format(time) + " GMT";
			return timeString;
		}

		public String createHTTPResponce(int code, String currDate, String fileModDate, String acceptRange, String length, String connection, String contentType) {
			String str = "";
			switch(code){
			case statusCode200:
				str += "HTTP/1.1 " + code + " " + "OK\r\n";
				str += "Connection: " + connection + "\r\n";
				str += "Date: " + currDate + "\r\n";
				str += "Last-Modified: " + fileModDate + "\r\n";
				str += "Accept-Ranges: " + acceptRange + "\r\n";
				str += "Content-Length: " + length + "\r\n";
				str += "Content-Type: " + contentType + "\r\n\r\n";
				break;
			case statusCode400:
				str += "HTTP/1.1 " + code + " " + "Bad Request\r\n";
				break;
			case statusCode404:
				str += "HTTP/1.1 " + code + " " + "Not Found\r\n";
				break;
			case statusCode505:
				str += "HTTP/1.1 " + code + " " + "HTTP Version Not Supported\r\n";
				break;
			}
			return str;
		}
	}
}