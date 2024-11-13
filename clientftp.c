/* Client FTP program
 *
 * NOTE: Starting homework #2, add more comments here describing the overall function
 * performed by server ftp program
 * This includes, the list of ftp commands processed by server ftp.
 *
 */

#include <stdio.h>
#include <unistd.h>

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

/* List of all global variables */

char userCmd[1024]; /* user typed ftp command line read from keyboard */
char cmd[1024]; /* ftp command extracted from userCmd */
char argument[1024]; /* argument extracted from userCmd */
char replyMsg[1024]; /* buffer to receive reply message from server */
char hold[1024]; /* char array will hold a copy of the userCmd array for division purposes */

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
int main(
	int argc,
	char *argv[]
	) 
{
	/* List of local varibale */
	
	int ccSocket; /* Control connection socket - to be used in all client communication */
	int dcSocket; 
	int msgSize; /* size of the reply message received from the server */
	int status;  
	int lSocket;
	int ccPort; 
	bool users = false; 
	bool passes = false; 
	char buffer[100]; 
	FILE *fp; 
	
	/*
	 * NOTE: without \n at the end of format string in printf,
	 * UNIX will buffer (not flush)
	 * output to display and you will not see it on monitor.
	 */
	printf("Started execution of client ftp\n");

	/* Connect to client ftp*/
	printf("Calling clntConnect to connect to the server\n");	/* changed text */

	status = clntConnect("127.0.0.1", &ccSocket); 
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
		printf("my ftp> "); 
		gets(userCmd); 
				/* to read the command from the user. Use gets or readln function */
		 /* Separate command and argument from userCmd */
		strcpy(hold, userCmd);
		char *cmd = strtok(hold, " ");
		char *argument = strtok(NULL, " ");

		/* send the userCmd to the server */
		status = sendMessage(ccSocket, userCmd, strlen(userCmd) + 1);
		if (status != OK) {
			break;
		}

		if (strcmp(cmd, "user") == 0) {
			if (strcmp(userCmd, "user") != 0) { /* if no argument is provided, then unsuccessful try, since user needs argument - username */
				users = true;
			}
		}

		else if (strcmp(cmd, "pass") == 0) {
			if (strcmp(userCmd, "pass") != 0) { 
				passes = true;
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
} /* end main() */

int svcInitServer(int *s) {

	int sock, qlen;
	struct sockaddr_in svcAddr;

	if ((sock = socket(AF_INET, SOCK_STREAM, 0)) < 0) {
		perror("cannot create socket");
		return (ER_CREATE_SOCKET_FAILED);
	}

	memset((char*) &svcAddr, 0, sizeof(svcAddr));
	
	svcAddr.sin_family = AF_INET;
	svcAddr.sin_addr.s_addr = htonl(INADDR_ANY); 
	svcAddr.sin_port = htons(DATA_CONNECTION_FTP_PORT); 

	if (bind(sock, (struct sockaddr*) &svcAddr, sizeof(svcAddr)) < 0) {
		perror("cannot bind");
		close(sock);
		return (ER_BIND_FAILED); /* bind failed */
	}

	qlen = 1;

	listen(sock, qlen); /* socket interface function call */

	*s = sock;

	return (OK); 
}

int clntConnect(char *serverName, int *s) {

	int sock; 

	struct sockaddr_in clientAddress; 
	struct sockaddr_in serverAddress;
	struct hostent *serverIPstructure; 

	if ((serverIPstructure = gethostbyname(serverName)) == NULL) {
		printf("%s is unknown server. \n", serverName);
		return (ER_INVALID_HOST_NAME); 
	}

	if ((sock = socket(AF_INET, SOCK_STREAM, 0)) < 0) {
		perror("cannot create socket ");
		return (ER_CREATE_SOCKET_FAILED); 
	}

	memset((char*) &clientAddress, 0, sizeof(clientAddress));

	clientAddress.sin_family = AF_INET; 
	clientAddress.sin_addr.s_addr = htonl(INADDR_ANY); 
	clientAddress.sin_port = 0; 
	
	if (bind(sock, (struct sockaddr*) &clientAddress, sizeof(clientAddress)) < 0) {
		perror("cannot bind");
		close(sock);
		return (ER_BIND_FAILED); 
	}

	memset((char*) &serverAddress, 0, sizeof(serverAddress));

	serverAddress.sin_family = AF_INET;
	memcpy((char*) &serverAddress.sin_addr, serverIPstructure -> h_addr,
			serverIPstructure -> h_length);
	serverAddress.sin_port = htons(CONTROL_CONNECTION_FTP_PORT);

	if (connect(sock, (struct sockaddr*) &serverAddress, sizeof(serverAddress))
			< 0) {
		perror("Cannot connect to server");
		close(sock); 
		return (ER_CONNECT_FAILED); 
	}

	*s = sock;

	return (OK); 
}


int clntExtractReplyCode(char *buffer, int *replyCode) {
	sscanf(buffer, "%d", replyCode);
	return (OK);
}
