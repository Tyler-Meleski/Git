/* 
 * server FTP program
 *   Groups Name: Lucas Ferreira & Tyler Meleski 
 * NOTE: Starting homework #2, add more comments here describing the overall function
 * performed by server ftp program
 * This includes, the list of ftp commands processed by server ftp.
 *
 */

#include <sys/types.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <netdb.h>
#include <string.h>

#define SERVER_FTP_PORT 4032
#define CONTROL_CONNECTION_FTP_PORT 4002

/* Error and OK codes */
#define OK 0
#define ER_INVALID_HOST_NAME -1
#define ER_CREATE_SOCKET_FAILED -2
#define ER_BIND_FAILED -3
#define ER_CONNECT_FAILED -4
#define ER_SEND_FAILED -5
#define ER_RECEIVE_FAILED -6
#define ER_ACCEPT_FAILED -7


/* Function prototypes */

int svcInitServer(int *s);
int sendMessage (int s, char *msg, int  msgSize);
int receiveMessage(int s, char *buffer, int  bufferSize, int *msgSize);


/* List of all global variables */

char userCmd[1024];	/* user typed ftp command line received from client */
char cmd[1024];		/* ftp command (without argument) extracted from userCmd */
char argument[1024];	/* argument (without ftp command) extracted from userCmd */
char replyMsg[1024];       /* buffer to send reply message to client */
char temp[1024]; /* temp char array (string) to hold userCmd before broken into 2 parts */
int userVar; /* Hold a temp variable value */
static FILE *fp; /* a global variable where its pointer for files

/*
 * main
 *
 * Function to listen for connection request from client
 * Receive ftp command one at a time from client
 * Process received command
 * Send a reply message to the client after processing the command with staus of
 * performing (completing) the command
 * On receiving QUIT ftp command, send reply to client and then close all sockets
 *
 * Parameters
 * argc		- Count of number of arguments passed to main (input)
 * argv  	- Array of pointer to input parameters to main (input)
 *		   It is not required to pass any parameter to main
 *		   Can use it if needed.
 *
 * Return status
 *	0			- Successful execution until QUIT command from client 
 *	ER_ACCEPT_FAILED	- Accepting client connection request failed
 *	N			- Failed stauts, value of N depends on the command processed
 */

int main(int argc,char *argv[])
{
	/* List of local varibale */

	int msgSize;        /* Size of msg received in octets (bytes) */
	int listenSocket;   /* listening server ftp socket for client connect request */
	int ccSocket;        /* Control connection socket - to be used in all client communication */
	int status;
	FILE *fp; /* helps with ls pwd in the code for the client side */
	char limit[100];
	bool checkUser = false; /* boolean variable to check if user has entered username, set to false until user enters correct ftp username */
	bool checkPass = false; /* boolean varible to check if the password.

	/*
	 * NOTE: without \n at the end of format string in printf,
         * UNIX will buffer (not flush)
	 * output to display and you will not see it on monitor.
	*/
	printf("Started execution of server ftp\n");


	 /*initialize server ftp*/
	printf("Initialize ftp server\n");	/* changed text */

	status = svcInitServer(&listenSocket);
	if(status != 0)
	{
		printf("Exiting server ftp due to svcInitServer returned error\n");
		exit(status);
	}


	printf("ftp server is waiting to accept connection\n");

	/* wait until connection request comes from client ftp */
	ccSocket = accept(listenSocket, NULL, NULL);

	printf("Came out of accept() function \n");

	if(ccSocket < 0)
	{
		perror("cannot accept connection:");
		printf("Server ftp is terminating after closing listen socket.\n");
		close(listenSocket);  /* close listen socket */
		return (ER_ACCEPT_FAILED);  // error exist
	}

	printf("Connected to client, calling receiveMsg to get ftp cmd from client \n");


	/* Receive and process ftp commands from client until quit command.
 	 * On receiving quit command, send reply to client and 
         * then close the control connection socket "ccSocket". 
	 */
	do
	{
	    /* Receive client ftp commands until */
 	    status=receiveMessage(ccSocket, userCmd, sizeof(userCmd), &msgSize);
	    if(status < 0)
	    {
		printf("Receive message failed. Closing control connection \n");
		printf("Server ftp is terminating.\n");
		break;
	    }


/*
 * Starting Homework#2 program to process all ftp commandsmust be added here.
 * See Homework#2 for list of ftp commands to implement.
 */
 /* These  two arrays store the name users and their password, as in initializer list notation, these arrays work with user and pass cmds */
		char *users[10] = { "Lucas", "Tyler", "lferreira", "tmeleski", "ferreira", "meleski",
				"water", "mouse", "quartz", "chappie", };

		char *pass[20] = { "Computer", "televison", "friday", "admin",
				"framingham1", "password", "root", "pizza", "","qwerty123" };
 
		 * /*This block of code has the user login with password. Test for cmd to be pass, then compare argument of user cmd to pass array location userVar (which is the location of the username entered and found),
		 * search for password in array, must match the password at array 
		 */
)	    /* Separate command and argument from userCmd */
	    strcpy(cmd, userCmd);  /* Modify in Homework 2.  Use strtok function */
	    strcpy(argument, "");  /* Modify in Homework 2.  Use strtok function */
		 char *whatCMD = strtok(temp, " "); 
		 char *whatArg = strtok(NULL, " ")
	    
		if (strcmp(cmd, "user") == 0)
		{
			for (int x = 0; x < usersLength - 2; x++) 
			{
				if (strcmp(argument, users[x]) == 0) 
				{
					strcpy(replyMsg, "ftp Username correct\n200 cmd OK\n");
					userVar = x;
					checkUser = true;
					
					break;
				}
			}

			if (checkUser == false || (strcmp(argument, "") == 0)) 
			{
				strcpy(replyMsg, "Invalid ftp username\nUsername not found\n");
				printf("Invalid ftp username\nUsername not found\n");
			}
		}			
		else if (strcmp(cmd, "pass") == 0) 
		{  /*If the cmd alone is equal to 'pass' enter conditional statement */
			
			if (checkUser == false) 
			{
				strcpy(replyMsg,"Please enter a username first \n");
			} 
			else if (strcmp(argument, pass[userVar]) == 0) 
			{
				strcpy(replyMsg,"Password correct\nLogin successful\n200 cmd OK\n");
				checkPass = true;
			} 
			else if (checkPass == false) 
			{
				strcpy(replyMsg,"Invalid password for the user\nLogin failed. Please enter username and password.\n");
			}
		}
 	     /* ftp server sends only one reply message to the client for 
	     * each command received in this implementation.
	     */
	    strcpy(replyMsg,"200 cmd okay\n");  /* Should have appropriate reply msg starting HW2 */
	    status=sendMessage(ccSocket,replyMsg,strlen(replyMsg) + 1);	/* Added 1 to include NULL character in */
				/* the reply string strlen does not count NULL character */
    if(status < 0)
	    {
			printf("Receive message has failed. Closing server connection" ;
			printf("Server ftp is closing. \n");
		break;  /* exit while loop */
	    }
	}
  /* This block of code will only work if the user was able to login with their password. */
	if(checkUser = true && checkPass== true;)
	{
		if (strcmp(cmd, "mkdir") == 0) {
				cmdCheck = true;
				status = system(userCmd);
				if (status == 0) 
				{
					strcpy(replyMsg, "200 cmd OK\n");
				}
				else 
				{
					strcpy(replyMsg, "500 invalid syntax\nCommand Failed\n");
				}
			}

			/* This block tests the rmdir cmd, check cmd, then do a system call to perform action
			 * and send replymsg to client if successful
			 * cmd test 7, example 'ls', 'rmdir abc', 'ls'
			 * Jon Petani implemented this command
			 */
			else if (strcmp(cmd, "rmdir") == 0) 
			{
				cmdCheck = true;
				status = system(userCmd);
				if (status == 0) 
				{
					strcpy(replyMsg, "200 cmd OK\n");
				}
				else 
				{
					strcpy(replyMsg, "500 invalid syntax\nCommand Failed\n");
				}
			}
			else if (strcmp(cmd, "dir") == 0) {
				cmdCheck = true;
				status = system("dir > diroutput.txt");
				if ((strcmp(userCmd, "dir") == 0) && status == 0) {
					fp = fopen("diroutput.txt", "r");
					bytesread = fread(replyMsg, 1, 1024, fp);
					replyMsg[bytesread] = '\0';	// store null terminator
					remove("diroutput.txt");
					fclose(fp);
					strcat(replyMsg, "\n200 cmd OK\n");
				} else {
					strcpy(replyMsg, "500 invalid syntax\nCommand Failed\n");
				}
			}

			/*
			 * This block tests the cd cmd, check cmd, then do a chdir call to perform actions (move to different dir)
			 * and send replymsg to client if successful
			 * use chdir() which performs a system call to change current working directory
			 * cmd test 8, example 'pwd', 'cd ..', 'pwd'
			 * Jon Petani implemented this command
			 */
			else if (strcmp(cmd, "cd") == 0) {
				cmdCheck = true;
				status = chdir(argument);
				if (status == 0) {
					strcpy(replyMsg, "200 cmd OK\n");
				} else {
					strcpy(replyMsg, "500 invalid syntax\nCommand Failed\n");
				}
			}

			/*
			 * This block tests the rm cmd, check cmd, then do a system call to perform action
			 * and send replymsg to client if successful
			 * cmd test 9, example 'ls', (use touch to create a file before running this ftp program), 'rm file1', 'ls'
			 * Brian Perel implemented this command
			 */
			else if (strcmp(cmd, "rm") == 0) {
				cmdCheck = true;
				status = system(userCmd);
				if (status == 0) {
					strcpy(replyMsg, "200 cmd OK\n");
				} else {
					strcpy(replyMsg, "500 invalid syntax\nCommand Failed\n");
				}
			}

			/*
			 * mv cmd can be used to rename or move files
			 * rename (mv) = 'mv old-filename new-filename'
			 * move (mv) = 'mv filename destination-directory'
			 */
			else if (strcmp(cmd, "mv") == 0) {
				cmdCheck = true;
				status = system(userCmd);
				if (status == 0) {
					strcpy(replyMsg, "200 cmd OK\n");
				} else {
					strcpy(replyMsg, "500 invalid syntax\nCommand Failed\n");
				}
			}

			/*
			 * This block tests the pwd cmd, check cmd, then do a system call to perform action
			 * in which, pwd > pwdoutput.txt stores the content of pwd into the txt file
			 * open the txt file and read the file content to terminal while sending replymsg back to client
			 * remove the txt file once operation is finished (content is read to replyMsg and sent)
			 * cmd test 10, example 'pwd'
			 * Jon Petani implemented this command
			 */
			else if (strcmp(cmd, "pwd") == 0) {
				cmdCheck = true;
				status = system("pwd > pwdoutput.txt");
				if ((strcmp(userCmd, "pwd") == 0) && status == 0) {
					fp = fopen("pwdoutput.txt", "r");
					bytesread = fread(replyMsg, 1, 1024, fp);
					replyMsg[bytesread] = '\0';	// store null terminator
					remove("pwdoutput.txt");
					fclose(fp);
					strcat(replyMsg, "\n200 cmd OK\n");
				} else {
					strcpy(replyMsg, "500 invalid syntax\nCommand Failed\n");
				}
			}

			/*
			 * This block tests the ls cmd, check cmd, then do a system call to perform action
			 * in which ls > lsoutput.txt stores the content of ls into the txt file
			 * open the txt file and read the content to terminal while sending replymsg back to client
			 * remove the txt file once operation is finished (content is read to replyMsg and sent)
			 * cmd test 11, 'ls'
			 * Jon Petani implemented this command
			 */
			else if (strcmp(cmd, "ls") == 0) {
				cmdCheck = true;
				status = system("ls > lsoutput.txt");
				if ((strcmp(userCmd, "ls") == 0) && status == 0) {
					fp = fopen("lsoutput.txt", "r");
					bytesread = fread(replyMsg, 1, 1024, fp);
					replyMsg[bytesread] = '\0';	// store null terminator
					remove("lsoutput.txt"); // remove the txt file after content is read
					fclose(fp);
					strcat(replyMsg, "\n200 cmd OK\n");
				} else {
					strcpy(replyMsg, "500 invalid syntax\nCommand Failed\n");
				}
			}
		
		if(
			strcpy(replyMsg,"Invalid command");
		
		
	}
		
	/*quit command */	
	while(strcmp(cmd, "quit") != 0);

	printf("Closing control connection socket.\n");
	close (ccSocket);  /* Close client control connection socket */

	printf("Closing listen socket.\n");
	close(listenSocket);  /*close listen socket */

	printf("Existing from server ftp main \n");

	return (status);
}

7
/*
 * svcInitServer
 *
 * Function to create a socket and to listen for connection request from client
 *    using the created listen socket.
 *
 * Parameters
 * s		- Socket to listen for connection request (output)
 *
 * Return status
 *	OK			- Successfully created listen socket and listening
 *	ER_CREATE_SOCKET_FAILED	- socket creation failed
 */

int svcInitServer (
	int *s 		/*Listen socket number returned from this function */
	)
{
	int sock;
	struct sockaddr_in svcAddr;
	int qlen;

	/*create a socket endpoint */
	if( (sock=socket(AF_INET, SOCK_STREAM,0)) <0)
	{
		perror("cannot create socket");
		return(ER_CREATE_SOCKET_FAILED);
	}

	/*initialize memory of svcAddr structure to zero. */
	memset((char *)&svcAddr,0, sizeof(svcAddr));

	/* initialize svcAddr to have server IP address and server listen port#. */
	svcAddr.sin_family = AF_INET;
	svcAddr.sin_addr.s_addr=htonl(INADDR_ANY);  /* server IP address */
	svcAddr.sin_port=htons(SERVER_FTP_PORT);    /* server listen port # */

	/* bind (associate) the listen socket number with server IP and port#.
	 * bind is a socket interface function 
	 */
	if(bind(sock,(struct sockaddr *)&svcAddr,sizeof(svcAddr))<0)
	{
		perror("cannot bind");
		close(sock);
		return(ER_BIND_FAILED);	/* bind failed */
	}

	/* 
	 * Set listen queue length to 1 outstanding connection request.
	 * This allows 1 outstanding connect request from client to wait
	 * while processing current connection request, which takes time.
	 * It prevents connection request to fail and client to think server is down
	 * when in fact server is running and busy processing connection request.
	 */
	qlen=1; 


	/* 
	 * Listen for connection request to come from client ftp.
	 * This is a non-blocking socket interface function call, 
	 * meaning, server ftp execution does not block by the 'listen' funcgtion call.
	 * Call returns right away so that server can do whatever it wants.
	 * The TCP transport layer will continuously listen for request and
	 * accept it on behalf of server ftp when the connection requests comes.
	 */

	listen(sock,qlen);  /* socket interface function call */

	/* Store listen socket number to be returned in output parameter 's' */
	*s=sock;

	return(OK); /*successful return */
}


/*
 * sendMessage
 *
 * Function to send specified number of octet (bytes) to client ftp
 *
 * Parameters
 * s		- Socket to be used to send msg to client (input)
 * msg  	- Pointer to character arrary containing msg to be sent (input)
 * msgSize	- Number of bytes, including NULL, in the msg to be sent to client (input)
 *
 * Return status
 *	OK		- Msg successfully sent
 *	ER_SEND_FAILED	- Sending msg failed
 */

int sendMessage(
	int    s,	/* socket to be used to send msg to client */
	char   *msg, 	/* buffer having the message data */
	int    msgSize 	/* size of the message/data in bytes */
	)
{
	int i;


	/* Print the message to be sent byte by byte as character */
	for(i=0; i<msgSize; i++)
	{
		printf("%c",msg[i]);
	}
	printf("\n");

	if((send(s, msg, msgSize, 0)) < 0) /* socket interface call to transmit */
	{
		perror("unable to send ");
		return(ER_SEND_FAILED);
	}

	return(OK); /* successful send */
}


/*
 * receiveMessage
 *
 * Function to receive message from client ftp
 *
 * Parameters
 * s		- Socket to be used to receive msg from client (input)
 * buffer  	- Pointer to character arrary to store received msg (input/output)
 * bufferSize	- Maximum size of the array, "buffer" in octent/byte (input)
 *		    This is the maximum number of bytes that will be stored in buffer
 * msgSize	- Actual # of bytes received and stored in buffer in octet/byes (output)
 *
 * Return status
 *	OK			- Msg successfully received
 *	R_RECEIVE_FAILED	- Receiving msg failed
 */


int receiveMessage (
	int s, 		/* socket */
	char *buffer, 	/* buffer to store received msg */
	int bufferSize, /* how large the buffer is in octet */
	int *msgSize 	/* size of the received msg in octet */
	)
{
	int i;

	*msgSize=recv(s,buffer,bufferSize,0); /* socket interface call to receive msg */

	if(*msgSize<0)
	{
		perror("unable to receive");
		return(ER_RECEIVE_FAILED);
	}

	/* Print the received msg byte by byte */
	for(i=0;i<*msgSize;i++)
	{
		printf("%c", buffer[i]);
	}
	printf("\n");

	return (OK);
}


