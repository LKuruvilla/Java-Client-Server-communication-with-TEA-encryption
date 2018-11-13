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
 *Files Included: none
 *
 *Purpose: To create a client-like program using socket that connects to a relay server.
 *         The relay server will then communicate with the receiver server. The program
 *         will send strings after being authenticated with the relay server and connecting
 *         the receiver server. The program will then print the longest common substring
 *         received from the relay server.All of the message transmissions are
 *         encrypted and decrypted using Tiny Encryption Algorithm and user defined key.
 *
 *Input: Relay server IP address and port address to create socket, username and password
 *       for user authentication, receiver name for connecting to receiver and user inputted
 *       strings for processing.
 *
 *Preconditions: Key is received from user before sending and receiving messages
 *
 *Output: A longest common substring is received from relay server and printed to screen.
 *
 *Postconditions: socket created is closed.
 *
 *Algorithm:
 *	Use the command line arguments to create client object and connect to relay server
 *	While the user is not authenticated
 *		Ask the user for username
 *		Send username to relay server
 *		Receive confirmation from relay server
 *		if username found
 *			ask user for password
 *			send password to relay server
 *			receive confirmation from relay server
 *			if password correct
 *				user is authenticated
 *				exit while loop
 *			else restart while loop
 *		else restart while loop
 *	While the receiver name is not found
 *		Ask the user for receiver name
 *		send the receiver name to relay server
 *		if receiver name exists
 *			if connection exists
 *				print connected to server
 *				exit while loop
 *			else enter receiver name again to connect
 *				 send to relay server
 *				 exit while loop
 *		else print receiver name found
 *			 restart while loop
 *	While user still wants to enter string
 *		receive and store string
 *	Transmit the string to relay server
 *	Receive processed output and print to screen
 *	While the user does not want to exit
 *		Take string input from user
 *		transmit the string to relay server
 *		Receive processed output and print to screen
 *	close sockets
 *	end of program.
 *
 *************************************************************************************** */


//import libraries
import java.net.*;
import java.io.*;
import java.util.Scanner;

//declaration of interface
interface senderInterface {

    void send(String a);

    String recieve();
}

//class sender implements above interface
class sender implements senderInterface {

    //class fields
    private int port;
    private String ipaddress;
    private DataInputStream in;
    private DataOutputStream out;
    private Socket relaysocket;
    private int[] key = null;//used to store key for encrypt/decrypt
    private char[] text = null; //used to store encrypted and decrypted text

    //declaration setters and getters
    public void setkey(String k) {
        key = new int[4];
        key[0] = (int) k.charAt(0);
        key[1] = (int) k.charAt(1);
        key[2] = (int) k.charAt(2);
        key[3] = (int) k.charAt(3);

    }

    public Socket getRelaysocket() {
        return relaysocket;
    }

    public void setRelaysocket(Socket relaysocket) {
        this.relaysocket = relaysocket;
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

    //method to send data to relayserver after encrypting
    public void send(String a) {
        //conversion to char array first
        text = new char[128];
        for (int i = 0; i < a.length(); i++) {
            text[i] = a.charAt(i);
        }

        try {
            a = String.copyValueOf(encrypt(this.text, this.key));
            //encrypted message is now sent
            out.writeUTF(a);
            text = null;
        } catch (IOException ex) {
            System.out.println("Failed to send");
        }

    }

    //method to read data from relayserver after encrypting
    public String recieve() {
        String temp = null;
        try {   //clear out char array first by making a new one
            text = new char[128];

            temp = in.readUTF();
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

    //constructor that takes ipaddress and port number as inputs for sockets
    public sender(String a, String b) {

        setPort(Integer.parseInt(b));
        setIpaddress(a);
        System.out.println(b + " " + a);
        try {
            relaysocket = new Socket(ipaddress, port);
            in = new DataInputStream(relaysocket.getInputStream());
            out = new DataOutputStream(relaysocket.getOutputStream());

        } catch (Exception e) {
            System.out.println("Error creating socket " + e.getMessage());
        }

        System.out.println("Socket created with input and outputsreams");

    }

}

//declaration of public class
public class senders {

    //declaration of main class
    public static void main(String[] args) {
        Scanner x = new Scanner(System.in);

        //create a new object to connect to relay server
        sender relayserver = new sender(args[0], args[1]);

        //setting key for client to encrypt/decrypt messages
        relayserver.setkey(args[2]);

        //loop to authenticate client
        boolean var = false;
        while (var == false) {
            var = authentication(relayserver, x);
        };

        //method to process reciever name
        recieverlist(relayserver, x);

        //begin transmission of messages after connection
        System.out.println("Starting transmission now");
        transmit(relayserver, x);

        //loop to quit or continue processing strings
        int choice = 1;
        System.out.println("Do you want to send any more strings? enter 1 to quit or 0 to continue");

        while (choice == 1) {
            choice = x.nextInt();
            switch (choice) {
                case 0: {
                    //keep processing strings
                    transmit(relayserver, x);
                    break;

                }
                case 1: {
                    //initiate closing procedures
                    relayserver.send("close");

                    try {   //confirm relayserver socket is closing
                        if (relayserver.recieve().equals("close")) {

                            //close sender socket    
                            System.out.println("Relay server has closed.. closing sender now");
                            relayserver.getRelaysocket().close();
                        }
                    } catch (IOException ex) {
                        System.out.println("Client socket failed to close");
                    }
                    System.out.println("closed");
                    return;

                }

            }
        }

    }

    public static void transmit(sender c, Scanner x) /**
     * *********************************************************************************
     * Purpose: To send user inputted strings from client server to relay
     * server.To receive back results from relay server and to print that
     * output.
     *
     * Parameters: Client object containing socket connected to server and a
     * scanner object that reads input from keyboard.
     *
     * Action: A user inputted string is transmitted to the relay server using
     * the socket. A longest common substring is received from server and
     * printed to screen.
     *
     ***********************************************************************************
     */
    {
        //get user input
        System.out.println("Please enter different type strings");
        String store = x.nextLine();
        int i = 0;
        String temp;
        System.out.println("Do you have anymore strings to enter? Enter 0 for yes and 1 for no");
        while (i == 0) {
            i = x.nextInt();

            switch (i) {
                case 0: {
                    //concatnate any remaining user strings before sending
                    System.out.println("Please enter the remaining strings now");
                    temp = x.nextLine();
                    store.concat(" " + temp);
                    System.out.println("Do you have anymore strings to enter? Enter 0 for yes and 1 for no");
                    break;
                }
                case 1: {
                    //send to relay server
                    System.out.println("Sending strings now!");
                    c.send(store);
                    break;

                }
            }

        }
        //receive and print the longest substring from relayserver
        System.out.println("The longest substring was " + c.recieve());

    }

    public static void recieverlist(sender c, Scanner x) /**
     * *********************************************************************************
     * Purpose: To get receiver name from user and send it to relay server
     *
     * Parameters: Client object containing socket connected to relay server and
     * a scanner object that reads input from keyboard.
     *
     * Action: Receiver name is send to relay server and connection is
     * established to receiver server.
     *
     ***********************************************************************************
     */
    {
        while (true) {
            System.out.println("Please enter the name of the reciever:");
            String temp = x.nextLine();
            c.send(temp);//send name of receiver to relayserver

            String t = c.recieve();

            //check to see if connection already exists
            if (t.equals("match")) {
                System.out.println("There is already an existing connection to reciever");
                return;
            } //if not create a new connection to correct receiver
            else if (t.equals("mismatch")) {
                System.out.println("reciever exists, but no connection has been established yet");
                System.out.println("Please enter the receiever name again to establish connection");
                c.send(temp = x.nextLine());
                return;
            } //exit as receiver name was not found
            else {
                System.out.println("reciever name not found");
            }
        }
    }

    public static boolean authentication(sender relayserver, Scanner x) /**
     * *********************************************************************************
     * Purpose: To get user authenticated from client server
     *
     * Parameters: Client object containing socket connected to relay server.
     *
     * Action: User is authenticated from relay server and a true statement is
     * returned.
     *
     ***********************************************************************************
     */
    {

        String confirm = "match";
        String temp = null;

        System.out.println("Please enter your username: ");

        //send username to relayserver
        String username = x.nextLine();
        relayserver.send(username);

        //check for confirmation
        temp = relayserver.recieve();

        //if username not found
        if (temp.equals(confirm) == false) {
            System.out.println("No username found..please authenticate again");
            return false;
        }
        //else enter password and send it to relayserver 
        System.out.println("Username found.");
        System.out.println("Please enter your password: ");
        String password = x.nextLine();
        relayserver.send(password);

        //check for client authentication
        temp = relayserver.recieve();

        if (temp.equals(confirm) == true) {
            System.out.println("User authenticated.");

            return true;
        }

        //if not authenticated exit and reenter loop
        return false;

    }

}
