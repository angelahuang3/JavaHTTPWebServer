import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.StringTokenizer;

// Each Client Connection will be processed in a Thread
public class JavaHTTPServer implements Runnable{ 
	
	static final File WEB_ROOT = new File(".");
	static final String DEFAULT_FILE = "index.html";
	static final String FILE_NOT_FOUND = "404.html";
	static final String FORBIDDEN = "403.html";
	static final String BAD_REQUEST = "400.html";
	static final String METHOD_NOT_SUPPORTED = "501.html";
	// port to listen connection
	//	static final int PORT = 8080;
	
	// verbose mode
	static final boolean verbose = true;
	
	// Client Connection via Socket Class
	private Socket socket;
	
	public JavaHTTPServer(Socket c) {
		socket = c;
	}
	
	public static void main(String[] args) {
		try {
			if (args.length != 4) {
				System.out.println("Please enter 4 arguments!");
				System.exit(0);
			}
			// listen to the new connections from incoming client
			int port = Integer.parseInt(args[3]);
			ServerSocket server_socket = new ServerSocket(port);
			System.out.println("Server is running.\nListening for connections on port : " + port + "...\n");
			
			// create loop to process HTTP request infinitely
			while (true) {
				// accept() Waits for incoming client/TCP connection request
				JavaHTTPServer myServer = new JavaHTTPServer(server_socket.accept());
				
				if (verbose) {
					System.out.println("Connecton opened. (" + new Date() + ")");
				}
				// req processes the HTTP request message.
				// Create a new thread to handle each request
				Thread thread = new Thread(myServer);
				thread.start(); 
			}
			
		} catch (IOException e) {
			System.err.println("Server Connection error : " + e.getMessage());
		}
	}

	@Override
	public void run() {
		// we manage our particular client connection
		BufferedReader in = null; PrintWriter out = null; BufferedOutputStream dataOut = null;
		String fileRequested = null; String content = null;
		
		try {
			// we read characters from the client via input on the socket
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			// we get character output stream to client (for headers)
			out = new PrintWriter(socket.getOutputStream());
			// get binary output stream to client (for requested data)
			dataOut = new BufferedOutputStream(socket.getOutputStream());
			
			// get first line of the request from the client
			String input = in.readLine();
			// parse the request with a string tokenizer
			StringTokenizer parse = new StringTokenizer(input);
			String method = parse.nextToken().toUpperCase(); // get the HTTP method of the client
			// get file requested
			fileRequested = parse.nextToken().toLowerCase();
			
			// accept only GET and HEAD methods
			if (!method.equals("GET")  &&  !method.equals("HEAD")) {
				if (verbose) {
					System.out.println("501 Not Implemented : " + method + " method.");
				}
				
				// we return the not supported file to the client
				File file = new File(WEB_ROOT, METHOD_NOT_SUPPORTED);
				int fileLength = (int) file.length();
				String contentMimeType = "text/html";
				//read content to return to client
				byte[] fileData = readFileData(file, fileLength);
					
				// we send HTTP Headers with data to client
				out.println("HTTP/1.1 501 Not Implemented");
				out.println("Date: " + new Date());
				out.println("Content-type: " + contentMimeType);
				out.println("Content-length: " + fileLength);
				out.println("Connection: Keep-Alive");
				out.println("Keep-Alive: timeout=3");
				out.println("Access-Control-Allow-Origin: *");
				out.println(); // blank line between headers and content, very important !
				out.flush(); // flush character output stream buffer
				// file
				dataOut.write(fileData, 0, fileLength);
				dataOut.flush();
				
			} else {
				// GET or HEAD method
				if (fileRequested.endsWith("/")) {
					fileRequested += DEFAULT_FILE;
				}
				
				File file = new File(WEB_ROOT, fileRequested);
				int fileLength = (int) file.length();
				content = getContentType(fileRequested);
				
				
				if (file.exists() && !file.canRead()) {
					forbidden(out, dataOut, fileRequested);	//403 error handling
					return;
				}

				if (method.equals("GET")) { // GET method so we return content
					byte[] fileData = readFileData(file, fileLength);
					
					// send HTTP Headers
					out.println("HTTP/1.1 200 OK");
					out.println("Date: " + new Date());
					out.println("Content-type: " + content);
					out.println("Content-length: " + fileLength);
					out.println("Connection: Keep-Alive");
					out.println("Keep-Alive: timeout=3");
					out.println("Access-Control-Allow-Origin: *");
					out.println(); // blank line between headers and content
					out.flush(); // flush character output stream buffer
					
					dataOut.write(fileData, 0, fileLength);
					dataOut.flush();
				}
				
				if (verbose) {
					System.out.println("GET " + fileRequested);
				}
				
			}
			
		} catch (FileNotFoundException fnfe) {
			try {
				fileNotFound(out, dataOut, fileRequested);	//404 error handling
				
			} catch (IOException ioe) {
				System.err.println("Error with file not found exception : " + ioe.getMessage());
			}
			
		} catch (IOException ioe) {
			System.err.println("Server error : " + ioe);
		} finally {
			try {
				if(content != null && content.equals("text/plain")) {
					badRequest(out, dataOut, fileRequested); //400 error handling
				}
				in.close();
				out.close();
				dataOut.close();
				socket.close(); // we close socket connection
			} catch (Exception e) {
				System.err.println("Error closing stream : " + e.getMessage());
			} 
			
			if (verbose) {
				System.out.println("Connection closed.\n");
			}
		}
		
		
	}
	
	private byte[] readFileData(File file, int fileLength) throws IOException {
		FileInputStream fileIn = null;
		byte[] fileData = new byte[fileLength];
		
		try {
			fileIn = new FileInputStream(file);
			fileIn.read(fileData);
		} finally {
			if (fileIn != null) 
				fileIn.close();
		}
		
		return fileData;
	}
	
	// return supported MIME Types
	private String getContentType(String fileRequested) {
		if (fileRequested.endsWith(".htm")  ||  fileRequested.endsWith(".html")) {
			return "text/html";
		}else if (fileRequested.endsWith(".js")||fileRequested.endsWith("js")) {
			return "text/javascript";
		} else if (fileRequested.endsWith(".css")) {
			return "text/css";
		} else if (fileRequested.endsWith(".jpg") || fileRequested.endsWith(".jpeg")) {
			return "image/jpeg";
		} else if (fileRequested.endsWith(".txt")) {
			return "text/txt";
		} else if (fileRequested.endsWith(".png")) {
			return "image/png";
		} else if (fileRequested.endsWith(".gif")) {
			return "image/gif";
		} else if (fileRequested.endsWith(".class")) {
			return "application/octet-stream";
		} else {
			return "text/plain";
		}
	}
	
	private void fileNotFound(PrintWriter out, OutputStream dataOut, String fileRequested) throws IOException {
		File file = new File(WEB_ROOT, FILE_NOT_FOUND);
		int fileLength = (int) file.length();
		String content = "text/html";
		byte[] fileData = readFileData(file, fileLength);
		
		out.println("HTTP/1.1 404 File Not Found");
		out.println("Date: " + new Date());
		out.println("Content-type: " + content);
		out.println("Content-length: " + fileLength);
		out.println("Connection: Keep-Alive");
		out.println("Keep-Alive: timeout=3");
		out.println("Access-Control-Allow-Origin: *");
		out.println(); // blank line between headers and content]
		out.flush(); // flush character output stream buffer
		
		dataOut.write(fileData, 0, fileLength);
		dataOut.flush();
		
	}
	private void forbidden(PrintWriter out, OutputStream dataOut, String fileRequested) throws IOException {
		File file = new File(WEB_ROOT, FORBIDDEN);
		int fileLength = (int) file.length();
		String content = "text/html";
		byte[] fileData = readFileData(file, fileLength);
		
		out.println("HTTP/1.1 403 Forbidden");
		out.println("Date: " + new Date());
		out.println("Content-type: " + content);
		out.println("Content-length: " + fileLength);
		out.println("Connection: Keep-Alive");
		out.println("Keep-Alive: timeout=3");
		out.println("Access-Control-Allow-Origin: *");
		out.println(); // blank line between headers and content
		out.flush(); // flush character output stream buffer
		
		dataOut.write(fileData, 0, fileLength);
		dataOut.flush();
		
	}
	
	private void badRequest(PrintWriter out, OutputStream dataOut, String fileRequested) throws IOException {
		File file = new File(WEB_ROOT, BAD_REQUEST);
		int fileLength = (int) file.length();
		String content = "text/html";
		byte[] fileData = readFileData(file, fileLength);
		
		out.println("HTTP/1.1 400 Bad Request");
		out.println("Date: " + new Date());
		out.println("Content-type: " + content);
		out.println("Content-length: " + fileLength);
		out.println("Connection: Keep-Alive");
		out.println("Keep-Alive: timeout=3");
		out.println("Access-Control-Allow-Origin: *");
		out.println(); // blank line between headers and content
		out.flush(); // flush character output stream buffer
		
		dataOut.write(fileData, 0, fileLength);
		dataOut.flush();
		
//		if (verbose) {
//			System.out.println("File " + fileRequested + " bad request");
//		}
	}
	
}
