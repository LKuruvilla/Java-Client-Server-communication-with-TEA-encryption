/** **************************************************************************************
 *Programmer: Lovin Kuruvilla
 *
 *Course: CSCI 5531
 *
 *Date: 04/14/2018
 *
 *Assignment: Program 3
 *
 *Environment: Windows with JDK installed and connected to network
 *
 *Files Included: userList.txt, receiverList.txt
 *
 *Purpose: To create a client and server like program using socket that connects to a client
 *         and to a server. The relay server will connect a client after authentication and
 *         server name verification. The relay server will act as a middle party between the
 *	  client and server to transmit messages. All of the message transmissions are
 *         encrypted and decrypted using Tiny Encryption Algorithm and user defined key.
 *
 *Input: A port address to listen to client. client authentication is done using the
 *       userList.txt contents and server connection is established using information from
 *       receiverList.txt
 *
 *Preconditions: Key is received from user before sending and receiving messages
 *
 *Output: A connection is established to client and to server.
 *
 *Postconditions: sockets created for both client and server are closed.
 *
 *Algorithm:
 *	Use the command line arguments to create client object and listen for incoming connection
 *	Create an array list to store server-connected objects
 *       Use key from user to encrypt and decrypt later messages
 *	While the client is not authenticated
 *		create a scanner object to read from userList.txt file
 *		receive username from client
 *		check against userList.txt file
 *		if username not found
 *			send non confirmation message to client
 *			restart while loop
 *		else if username is found
 *			receive password from client
 *			check password against file contents
 *				if password is correct
 *					client is authenticated
 *					exit while loop
 *				else restart while loop
 *	While the client is not connected to server.
 *		create a scanner object to read from receiverList.txt file
 *		receive receiver name from client
 *		check against receiverList.txt file
 *		if match found
 *			check against array list to see if already connected to server
 *			if already connected
 *				send confirmation back to client
 *				exit while loop
 *			else create server object
 *				 connect to server
 *				 add server object to array list
 *				 send confirmation to client
 *				 exit while loop
 *		else send client not found
 *			 restart while loop
 *	While the client has not issued a close message
 *		receive string from client
 *		send string to server
 *	close client sockets
 *	close server sockets
 *	end of program.
 *************************************************************************************** */


//import libraries
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

//declaration of interface for the relay server
interface relayServerInterface {

    void send(String a);

    String recieve();
}

//client class implements interface described above
//to interact with sender
class client implements relayServerInterface {

    //Client fields
    private int port;
    private String ipaddress;
    private ServerSocket S_Socket;
    private Socket ClientSocket;
    private DataInputStream fromClient;
    private DataOutputStream toClient;
    private int[] key = null; //used to store key
    private char[] text = null; //used to store encrypted and decrypted text

    //declration of getters and setters
    public void setkey(String k) {
        key = new int[4];
        key[0] = (int) k.charAt(0);
        key[1] = (int) k.charAt(1);
        key[2] = (int) k.charAt(2);
        key[3] = (int) k.charAt(3);

    }

    public Socket getClientSocket() {
        return ClientSocket;
    }

    public void setClientSocket(Socket ClientSocket) {
        this.ClientSocket = ClientSocket;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getIpaddress() {
        return ipaddress;
    }

    public void setIpaddress(String ipaddress) {
        this.ipaddress = ipaddress;
    }

    //constructor that takes port as an argument
    public client(String b) {
        try {
            setPort(Integer.parseInt(b));
            S_Socket = new ServerSocket(getPort());

        } catch (IOException ex) {
            System.out.println("Server socket failed " + ex.getMessage());

        }

        System.out.println("ServerSocket created... waiting for incoming connection.");

        try {
            ClientSocket = S_Socket.accept();
            fromClient = new DataInputStream(ClientSocket.getInputStream());
            toClient = new DataOutputStream(ClientSocket.getOutputStream());
            System.out.println("Succesfully created client sock and attached streams");
        } catch (IOException ex) {
            System.out.println("Failed to connect with client socket");
        }

    }

    //Method used to encrypt using the key defined by user
    private char[] encrypt(char[] text, int[] k) {
        for (int m = 0; m < text.length; m += 2) {

            int temp = m + 1;

            char y = text[m], z = text[temp];
            int delta = 0x9e3779b9, sum = 0;
            int n;
            //System.out.println(k[0] + " "+k[1]);
            for (n = 0; n < 32; n++) {
                sum += delta;
                y += ((z << 4) + k[0]) ^ (z + sum) ^ ((z >> 5) + k[1]);
                z += ((y << 4) + k[2]) ^ (y + sum) ^ ((y >> 5) + k[3]);
            }
            text[m] = y;
            text[temp] = z;
        }
        return text;
    }

    //Method used to decrypt using the key defined by user
    public char[] decrypt(char[] text, int[] k) {
        for (int m = 0; m < text.length; m += 2) {

            int temp = m + 1;
            char y = text[m], z = text[temp];
            int delta = 0x9e3779b9, sum = 0xC6EF3720;
            int n;
            //System.out.println(k[0] + " "+k[1]);

            for (n = 0; n < 32; n++) {
                z -= ((y << 4) + k[2]) ^ (y + sum) ^ ((y >> 5) + k[3]);
                y -= ((z << 4) + k[0]) ^ (z + sum) ^ ((z >> 5) + k[1]);

                sum -= delta;

            }
            text[m] = y;
            text[temp] = z;
        }
        return text;
    }

    @Override
    //method to send data to client after encrypting
    public void send(String a) {
        //conversion to char array first
        text = new char[128];
        for (int i = 0; i < a.length(); i++) {
            text[i] = a.charAt(i);
        }

        try {
            a = String.copyValueOf(encrypt(this.text, this.key));
            //encrypted message is now sent
            toClient.writeUTF(a);
            text = null;
        } catch (IOException ex) {
            System.out.println("Failed to send");
        }
    }

    @Override
    //method to receive data from client after encrypting
    public String recieve() {
        String temp = null;
        try {   //clear out char array first by making a new one
            text = new char[128];

            temp = fromClient.readUTF();
            for (int i = 0; i < temp.length(); i++) {
                text[i] = temp.charAt(i);
            }
            //String received is loaded to char array 

            this.text = decrypt(this.text, this.key);
            temp = String.copyValueOf(this.text);
            //decrypted and processed before returning string
            temp = temp.trim();

        } catch (IOException ex) {
            System.out.println("Error reading data input " + ex.getMessage());
        }
        return temp;
    }
}

//class server implements the interface above to interact with
//receiver
class server implements relayServerInterface {   //class fields 

    private int port;
    private String ipaddress;
    private Socket ServerSocket;
    private DataInputStream fromServer;
    private DataOutputStream toServer;
    private int[] key = null; //used to store key
    private char[] text = null;//used to store text

    public Socket getServerSocket() {
        return ServerSocket;
    }

    public void setServerSocket(Socket ServerSocket) {
        this.ServerSocket = ServerSocket;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getIpaddress() {
        return ipaddress;
    }

    public void setIpaddress(String ipaddress) {
        this.ipaddress = ipaddress;
    }

    public void setkey(String k) {
        key = new int[4];
        key[0] = (int) k.charAt(0);
        key[1] = (int) k.charAt(1);
        key[2] = (int) k.charAt(2);
        key[3] = (int) k.charAt(3);

    }

    //server constructor that takes ipaddress and port number as inputs
    public server(String ip, String port) {
        setPort(Integer.parseInt(port));
        setIpaddress(ip);

        try {
            ServerSocket = new Socket(ip, getPort());
            fromServer = new DataInputStream(ServerSocket.getInputStream());
            toServer = new DataOutputStream(ServerSocket.getOutputStream());
        } catch (IOException e) {
            System.out.println("Failed to make server port " + e.getMessage());
        }

        System.out.println("Connected to server successfully");

    }

    //method to encrypt the messages using user-defined key
    private char[] encrypt(char[] text, int[] k) {
        for (int m = 0; m < text.length; m += 2) {

            int temp = m + 1;

            char y = text[m], z = text[temp];
            int delta = 0x9e3779b9, sum = 0;
            int n;
            //System.out.println(k[0] + " "+k[1]);
            for (n = 0; n < 32; n++) {
                sum += delta;
                y += ((z << 4) + k[0]) ^ (z + sum) ^ ((z >> 5) + k[1]);
                z += ((y << 4) + k[2]) ^ (y + sum) ^ ((y >> 5) + k[3]);
            }
            text[m] = y;
            text[temp] = z;
        }
        return text;
    }

    //method to decrypt the messages using user-defined key
    public char[] decrypt(char[] text, int[] k) {
        for (int m = 0; m < text.length; m += 2) {

            int temp = m + 1;
            char y = text[m], z = text[temp];
            int delta = 0x9e3779b9, sum = 0xC6EF3720;
            int n;
            //System.out.println(k[0] + " "+k[1]);

            for (n = 0; n < 32; n++) {
                z -= ((y << 4) + k[2]) ^ (y + sum) ^ ((y >> 5) + k[3]);
                y -= ((z << 4) + k[0]) ^ (z + sum) ^ ((z >> 5) + k[1]);

                sum -= delta;

            }
            text[m] = y;
            text[temp] = z;
        }
        return text;
    }

    @Override
    //method to send data to server after encrypting
    public void send(String a) {
        //conversion to char array first
        text = new char[128];
        for (int i = 0; i < a.length(); i++) {
            text[i] = a.charAt(i);
        }

        try {
            a = String.copyValueOf(encrypt(this.text, this.key));
            //encrypted message is now sent
            toServer.writeUTF(a);
            text = null;
        } catch (IOException ex) {
            System.out.println("Failed to send");
        }
    }

    @Override
    //method to receive data from server after encrypting
    public String recieve() {
        String temp = null;
        try {   //clear out char array first by making a new one
            text = new char[128];

            temp = fromServer.readUTF();
            for (int i = 0; i < temp.length(); i++) {
                text[i] = temp.charAt(i);
            }
            //String received is loaded to char array 

            this.text = decrypt(this.text, this.key);
            temp = String.copyValueOf(this.text);
            //decrypted and processed before returning string
            temp = temp.trim();

        } catch (IOException ex) {
            System.out.println("Error reading data input " + ex.getMessage());
        }
        return temp;
    }

}

//declaration of public class
public class relayserver {

    //declaration of main class
    public static void main(String[] args) {

        client Client = new client(args[0]);//client

        //setting key for client to encrypt/decrypt messages
        Client.setkey(args[1]);

        //array list to hold all current server connections
        List<client> serverlist = new ArrayList<client>();

        //loop to authenticate client
        boolean var = false;
        while (var == false) {
            var = authenticator(Client);
        };
        System.out.println("Client has been authenticated");

        //returns the actual server sender wants to connect to
        server current_server = reciever(Client, serverlist);

        //setting key for server to encrypt/decrypt messages
        current_server.setkey(args[1]);

        System.out.println("Starting transmission now \n\n");

        //indicators to keep or to close current connection
        String val = "open";
        String close = "close";

        //loop to check for "close" message from sender
        while (val.equals(close) == false) {
            transmit(Client, current_server);
            val = Client.recieve();
        }

        try {
            //send close message to receiver and wait till server confirms
            //before closing connection
            current_server.send(close);
            if (current_server.recieve().equals(close)) {
                System.out.println("Reciever closed.. now closing relay server");

                current_server.getServerSocket().close();
            }

        } catch (IOException ex) {
            System.out.println("Server socket failed to close");
        }

        try {   //confirm close to sender before closing connection
            Client.send(close);
            System.out.println("Relay server is closed");
            Client.getClientSocket().close();

        } catch (IOException ex) {
            System.out.println("Client socket failed to close");
        }

    }

    public static void transmit(client client, server server) /**
     * *********************************************************************************
     * Purpose: To send message back to back from client to server
     *
     * Parameters: Client and server object
     *
     * Action: Sends string received from client to server back to back
     *
     ***********************************************************************************
     */
    {
        //receive from sender and send to receiver

        server.send(client.recieve());

        //receive from reciever and send to client
        client.send(server.recieve());
    }

    public static server reciever(client toClient, List server_list) /**
     * *********************************************************************************
     * Purpose: To connect to server using receiver name received from client
     *
     * Parameters: Client object and array list holding server object (if any)
     *
     * Action: Checks for and establishes connection with server if the correct
     * receiver name is received from client. If new connection, creates server
     * object, connects to server and adds that server to array list and returns
     * the server object.
     *
     ***********************************************************************************
     */
    {
        boolean stat = false;

        while (stat == false) {
            //recieve client name from sender
            String R_nameClient = toClient.recieve();

            System.out.println("client name recieved is " + R_nameClient);

            String Rname = null, Rip = null, Rport = null;

            Scanner scan = null;

            //access file 
            try {
                File file = new File("receiverList.txt");
                RandomAccessFile rac = new RandomAccessFile(file, "r");
                scan = new Scanner(file);
            } catch (Exception e) {
                System.out.println("error reading recieverlist " + e.getMessage());
            }
            boolean status = true;

            //check file for reciever name
            while (scan.hasNext()) {
                Rname = scan.next();

                //found receiever name
                if (Rname.equals(R_nameClient)) {
                    Rip = scan.next();
                    Rport = scan.next();

                    System.out.println("reciever name: " + Rname + " + iP: " + Rip + " + port: " + Rport);

                    //check to see if connection already exists
                    for (Iterator it = server_list.iterator(); it.hasNext();) {
                        server s = (server) it.next();
                        System.out.println("Inside for loop " + s.getPort() + " " + s.getIpaddress());

                        if ((s.getIpaddress().equals(Rip)) && (Integer.toString(s.getPort()).equals(Rport))) {
                            System.out.println("Connections exists; proceed");
                            toClient.send("match");
                            stat = true;

                            return s;
                        }

                    }
                    //make a new connection 
                    System.out.println("No existing connection, establishing connection now");
                    toClient.send("mismatch");

                    String newreciever = toClient.recieve();

                    server x = new server(Rip, Rport);
                    server_list.add(x);

                    return x;
                } //receiver name is not found
                else if (scan.hasNext() == false) {
                    System.out.println("Reciever name not found");
                    toClient.send("mismatch");

                }
            }
        }

        server x = null;
        return x;

    }

    public static boolean authenticator(client Client) /**
     * *********************************************************************************
     * Purpose: To authenticate the client based on userList.txt contents
     *
     * Parameters: Client object
     *
     * Action: Authenticates the client based on username and password received
     * from client and verifying it against the userList.txt contents
    ***********************************************************************************
     */
    {
        System.out.println("Beginning authentication process");
        //recieve username from client
        String clientusername = Client.recieve();

        File file = null;
        Scanner scan = null;

        //get file access
        try {
            //opening file of type randomaccessfile
            file = new File("userList.txt");
            RandomAccessFile rac = new RandomAccessFile(file, "r");
            scan = new Scanner(file);
        } catch (Exception e) {
            System.out.println("File not found " + e.getMessage());
        }

        //variables to hold username and password values
        String temppassword = null, filepassword = null;

        //go through file
        while (scan.hasNext()) {
            String fileusername = scan.next();

            //check to see if username matches
            if (fileusername.equals(clientusername)) {
                filepassword = scan.next();
                Client.send("match");
                System.out.println(fileusername + " " + filepassword);

                temppassword = Client.recieve();

                //check to see if password matches
                if (filepassword.equals(temppassword)) {
                    Client.send("match");
                    System.out.println("Client authenticated");
                    return true;
                } else//username and password combination is wrong
                {
                    System.out.println("Username and password combination is wrong");
                    Client.send("mismatch");

                    return false;
                }

            } //username is not found in file
            else if (scan.hasNext() == false) {

                Client.send("mismatch");
                System.out.println("Username not found");
                return false;
            }

        }
        return false;
    }

}
