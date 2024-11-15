 /* Client FTP program
 *
 * NOTE: Starting homework #2, add more comments here describing the overall function
 * performed by server ftp program
 * This includes, the list of ftp commands processed by server ftp.
 *
 */

#include <stdio.h>
#include <unistd.h>

#include <stdlib.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <netdb.h>
#include <string.h>


#define SERVER_FTP_PORT 2050
#define DATA_FTP_PORT 2051

/* Error and OK codes */
#define OK 0
#define ER_INVALID_HOST_NAME -1
#define ER_CREATE_SOCKET_FAILED -2
#define ER_BIND_FAILED -3
#define ER_CONNECT_FAILED -4
#define ER_SEND_FAILED -5
#define ER_RECEIVE_FAILED -6

/* Function prototypes */

int clntConnect(char *serverName, int *s); 
int sendMessage(int s, char *msg, int msgSize); 
int receiveMessage(int s, char *buffer, int bufferSize, int *msgSize); 
int svcInitServer(int *s);

/* List of all global variables */

char userCmd[1024];	/* user typed ftp command line read from keyboard */
char cmd[1024];		/* ftp command extracted from userCmd */
char argument[1024];	/* argument extracted from userCmd */
char replyMsg[1024];    /* buffer to receive reply message from server */
char temp[1024];

/*
 * main
 *
 * Function connects to the ftp server using clntConnect function.
 * Reads one ftp command in one line from the keyboard into userCmd array.
 * Sends the user command to the server.
 * Receive reply message from the server.
 * On receiving reply to QUIT ftp command from the server,
 * close the control connection socket and exit from main
 *
 * Parameters
 * argc		- Count of number of arguments passed to main (input)
 * argv  	- Array of pointer to input parameters to main (input)
 *		   It is not required to pass any parameter to main
 *		   Can use it if needed.
 *
 * Return status
 *	OK	- Successful execution until QUIT command from client 
 *	N	- Failed status, value of N depends on the function called or cmd processed
 */
int main(int argc, char *argv[]) {

	/* List of local varibale */

	int ccSocket; /* Control connection socket - to be used in all client communication */
	int dcSocket; /* data connection socket - to be used to transfer files between hosts*/
	int msgSize; /* size of the reply message received from the server */
	int status; /* Variable to hold status of strcmp, stores integer. If variable has 0, then it can indicate success to program, else not */
	int lSocket; /* holds socket connection info */
	int ccPort; /* Store port number */
	int bytesread = 600; /* store number of bytes while reading file in send and recv cmds */
	char buffer[100]; /* amount of bytes of a file */
	FILE *fp; /* create file pointer to work with files in program */

	/*
	 * NOTE: without \n at the end of format string in printf,
         * UNIX will buffer (not flush)
	 * output to display and you will not see it on monitor.
 	 */
	printf("Started execution of client ftp\n");

	/* Connect to client ftp*/
	printf("Calling clntConnect to connect to the server\n");	/* changed text */

	status = clntConnect("127.0.0.1", &ccSocket); \

	if (status != 0) {
		printf("Connection to server failed, exiting main.\n");
		return (status);
	}

	status = svcInitServer(&lSocket);

	if (status != 0) { 
		return (status);
	}

	/* 
	 * Read an ftp command with argument, if any, in one line from user into userCmd.
	 * Copy ftp command part into ftpCmd and the argument into arg array.
 	 * Send the line read (both ftp cmd part and the argument part) in userCmd to server.
	 * Receive reply message from the server.
	 * until quit command is typed by the user.
	 */

	do {
		printf("my ftp> "); /* prompt user to enter cmd here */
		fgets(userCmd, sizeof(userCmd), stdin); /* This statement must be replaced in homework #2 */
						/* to read the command from the user. Use gets or readln function */
		userCmd[strcspn(userCmd, "\n")] = 0; 

		strcpy(temp, userCmd);
		/* Separate command and argument from userCmd */
		char *cmd = strtok(temp, " "); /* Modify in Homework 2.  Use strtok function */
		char *argument = strtok(NULL, " "); /* Modify in Homework 2.  Use strtok function */

		/* send the userCmd to the server */
		status = sendMessage(ccSocket, userCmd, strlen(userCmd) + 1);
		if (status != OK) {
			break;
		}

		int userCheck = 0;  
		int passCheck = 0;  

		if (strcmp(cmd, "user") == 0) {
			if (argument == NULL) { 
				userCheck = 1;  
			}
		}
		else if (strcmp(cmd, "pass") == 0) {
			if (argument == NULL) { 
				passCheck = 1;  
			}
		}

		/* Receive reply message from the the server */
		status = receiveMessage(ccSocket, replyMsg, sizeof(replyMsg), &msgSize);
		if (status != OK) {
			break;
		}
	} while ((strcmp(userCmd, "quit") != 0) && (strcmp(userCmd, "bye") != 0));

	printf("Closing control connection\n");
	close(ccSocket); /* close control connection socket */
	close(lSocket); 
	printf("Exiting client main \n");

	return (status);
}

/*
 * svcInitServer
 *
 * Function
 *
 * Parameters
 * 
 * s	-
 *
 *		   
 *
 * Return status
 *	OK	- Successful execution until QUIT command from client 
 */
int svcInitServer(int *s) {

	int sock, qlen;
	struct sockaddr_in svcAddr;

	if ((sock = socket(AF_INET, SOCK_STREAM, 0)) < 0) {
		perror("cannot create socket");
		return (ER_CREATE_SOCKET_FAILED);
	}

	/* initialize memory of svcAddr structure to zero. */
	memset((char*) &svcAddr, 0, sizeof(svcAddr));

	svcAddr.sin_family = AF_INET;
	svcAddr.sin_addr.s_addr = htonl(INADDR_ANY); 
	svcAddr.sin_port = htons(DATA_FTP_PORT); 

	/* bind listen socket to the address and port */
	if (bind(sock, (struct sockaddr*)&svcAddr, sizeof(svcAddr)) < 0) {
		perror("bind failed");
		close(sock);
		return (ER_BIND_FAILED);
	}

	/* listen for incoming connection */
	if (listen(sock, 5) < 0) {
		perror("listen failed");
		close(sock);
		return (ER_CREATE_SOCKET_FAILED);
	}

	*s = sock; // Assign the socket to the provided pointer
	return OK;
}

/*
 * clntConnect
 *
 * Function
 *
 * Parameters
 * serverName	-
 * s		-
 *		   
 *
 * Return status
 *	OK			- Successful execution until QUIT command from client
 *	ER_CONNECT_FAILED;	-
 */
int clntConnect(char *serverName, int *s) {

	struct sockaddr_in serverAddr;
	struct hostent *server;
	int sock;

	if ((sock = socket(AF_INET, SOCK_STREAM, 0)) < 0) {
		perror("cannot create socket");
		return ER_CREATE_SOCKET_FAILED;
	}

	server = gethostbyname(serverName);
	if (server == NULL) {
		fprintf(stderr, "No such host: %s\n", serverName);
		close(sock);
		return ER_INVALID_HOST_NAME;
	}

	memset((char*)&serverAddr, 0, sizeof(serverAddr));
	serverAddr.sin_family = AF_INET;
	memcpy((char*)&serverAddr.sin_addr.s_addr, (char*)server->h_addr, server->h_length);
	serverAddr.sin_port = htons(SERVER_FTP_PORT); 

	if (connect(sock, (struct sockaddr*)&serverAddr, sizeof(serverAddr)) < 0) {
		perror("connect failed");
		close(sock);
		return ER_CONNECT_FAILED;
	}

	*s = sock; 
	return OK;
}

int sendMessage(int s, char *msg, int msgSize) {

	int bytesSent = 0;

	while (bytesSent < msgSize) {
		int n = send(s, msg + bytesSent, msgSize - bytesSent, 0);
		if (n < 0) {
			perror("message unable to be sent");
			return ER_SEND_FAILED;
		}
		bytesSent += n;
	}

	return OK;
}

int receiveMessage(int s, char *buffer, int bufferSize, int *msgSize) {

	int bytesReceived = 0;

	while (bytesReceived < bufferSize) {
		int n = recv(s, buffer + bytesReceived, bufferSize - bytesReceived, 0);
		if (n < 0) {
			perror("message unable to be received");
			return ER_RECEIVE_FAILED;
		}
		bytesReceived += n;

		
		if (n == 0) {
			break;
		}
	}

	*msgSize = bytesReceived; 
	return OK;
}
