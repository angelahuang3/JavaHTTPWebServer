1. Name: Ching Yueh Huang

2. Assignment name: PA 1 - Build a functional Web Server

3. High-level description of the assignment on what this program does:
* The program is a self-built HTTP web server deal with HTTP requests and responses.
* Images of type jpeg/jpg, png, gif and files of type txt, HTML, js, CSS are supported independently. (support HTTP/1.0 protocol)
* Response header such as content-type, content-length, and date is generated when sending back.
* The application is multithreaded program. Once the connection is accepted, it will generate a new thread to handle the request.
* The server is defaulted with a keep-alive connection and determined with a timeout mechanism (support HTTP/1.1 protocol).

A brief explanation of the program:
* server_socket listens for incoming connections and takes port number.
* Inside the while loop we wait for a TCP connection infinitely.
	- A Socket object is created to wait for incoming clients and then process the HTTP request message
	- A thread is created to handle each request and then it will start a new thread
* Each request is handled in run() method of JavaHTTPServer class that implements Runnable interface.
* In JavaHTTPServer() method, we get the reference for socket input and output streams.
* We take the initial request line of the HTTP request message and extract the filename, which is then parsed and the type of content is determined.
* The requested resource is written to a buffer and displayed and the connection is closed
* Http 501,404,403,400,200 are handled
* Http 400 and 404 errors are returned

4. List of submitted files:
> DS_ChingYueh_PA1
	> JavaHTTPServer.java
	> index.html: includes files for index.html
	> other HTML, jpg, png, and TXT files used for testing
	> Script.pdf: all screenshots and their explanations

5. Instructions for running the program:
	1)	javac JavaHTTPServer.java
	2)	java JavaHTTPServer -document_root "./DS_ChingYueh_PA1" -port 8080

# open the browser and enter localhost:8080 or 127.0.0.1:8080

# open another terminal
1.	telnet localhost 8080
2.	get / HTTP/1.1
    