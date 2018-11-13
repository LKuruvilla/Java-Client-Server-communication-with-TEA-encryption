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
 *Purpose: To server like program using socket that connects to a relay server. The
 *         server will receive string from relay server and compute the longest
 *         common substring and send it back to relay server.All of the message
 *         transmissions are encrypted and decrypted using Tiny Encryption Algorithm
 *         and user defined key.
 *
 *Input: A port address to listen to the relay server.
 *
 *Preconditions: Key is received from user before sending and receiving messages
 *
 *Output: A connection is established to the relay server.
 *
 *Postconditions: Sockets connected to relay server is closed.
 *
 *Algorithm:
 *	Create relay server object using command line argument and listen for incoming connection
 *	Accept connection
 *	While connection to relay server is open
 *		receive string from relay server
 *		split the string using space as delimiter
 *		for each string in array
 *			compare two strings at a time
 *			take longest common substring
 *			compare the substring with next string and record the longest common substring
 *		send the string to relay server.
 *	close the relay server socket
 *	end of program.
 *************************************************************************************** */


//import libraries
import java.net.*;
import java.io.*;
import java.util.Scanner;

//declaration of interface
interface recieverInterface {

    void send(String a);

    String recieve();
}

class reciever implements recieverInterface {

    //declaration of class fields
    private ServerSocket SSocket = null;
    private int port;
    private Socket relaySocket = null;
    private DataInputStream in;
    private DataOutputStream out;
    private int[] key = null; //to store key for encrypt and decrypt
    private char[] text = null; //to store text that is encryped and decrypted

    public void setkey(String k) {
        key = new int[4];
        key[0] = (int) k.charAt(0);
        key[1] = (int) k.charAt(1);
        key[2] = (int) k.charAt(2);
        key[3] = (int) k.charAt(3);

    }

    public ServerSocket getSSocket() {
        return SSocket;
    }

    public void setSSocket(ServerSocket SSocket) {
        this.SSocket = SSocket;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    //method to encrypt before sending it to relay server
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

    //method to decrypt before receiving from relay server
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
    //method to send data to relay server after encrypting
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

    @Override
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

    //constructor that takes the port number as inputs to create server socket
    public reciever(String a) {
        setPort(Integer.parseInt(a));
        try {
            SSocket = new ServerSocket(getPort());
            System.out.println("Server is running");
            relaySocket = SSocket.accept();
            System.out.println("Connection accepted");

        } catch (IOException ex) {
            System.out.println("Failed to set up server socket. " + ex.getMessage());
        }

        try {
            in = new DataInputStream(relaySocket.getInputStream());
            out = new DataOutputStream(relaySocket.getOutputStream());
        } catch (IOException ex) {
            System.out.println("Failed to set up input and output streams " + ex.getMessage());
        }

    }
}

//declaration of public class
public class recievers {

    //declaration of main class
    public static void main(String[] args) {

        //create a server object
        reciever relay = new reciever(args[0]);

        //set key for encrypting and decrypting
        relay.setkey(args[1]);

        //initiate transmision 
        System.out.println("Starting transmission now");

        //set indicators to check for close connection message
        String val = "open";
        String close = "close";

        //if close message has not been received yet, process strings
        while (val.equals(close) == false) {
            //return string after processing and print it
            String str = transmit(relay);
            System.out.println("Sent data");

            //send string
            relay.send(str);

            //check message for close indication
            val = relay.recieve();
            System.out.println(val);
        }

        //close message has been recieved 
        System.out.println("closing now");
        try {
            //let relay server know reciever is closing before shutting down
            //and then close the reciever socket
            relay.send(close);
            System.out.println("Reciever has closed now");
            relay.getSSocket().close();
        } catch (IOException ex) {
            System.out.println("Relay socket failed to close");
        }
        // TODO code application logic here

    }

    public static String transmit(reciever s) /**
     * *********************************************************************************
     * Purpose: To receive string from relay server and to send to process
     * function
     *
     * Parameters: relay server object
     *
     * Action: Receives string from relay server and sends it to be processed
     *
     ***********************************************************************************
     */
    {

        String temp = s.recieve();
        System.out.println("Transmission: " + temp);

        String x = process(temp);
        return x;
    }

    public static String process(String s) /**
     * *********************************************************************************
     * Purpose: Splits the string and sends each string to the lcg method to
     * obtain longest common substring and returns it to previous function.
     *
     * Parameters: String received from relay server
     *
     * Action: Splits string and sends it to lcg method. returns the string from
     * lcg method
     *
     ***********************************************************************************
     */
    {
        System.out.println("Processing the string " + s + " now");
        String[] store = s.split(" ");

        String currenttemp = null;

        for (int i = 0; i < store.length; i++) {
            if (i == 0) {
                currenttemp = lcg(store[i], store[1]);
            }
            currenttemp = lcg(store[i], currenttemp);
        }
        return currenttemp;

    }

    public static String lcg(String S1, String S2) /**
     * *********************************************************************************
     * Purpose: To compare two strings and return the common substring
     *
     * Parameters: Two strings to compare
     *
     * Action: Compares the two strings for a common subsequence and returns it.
     *
     ***********************************************************************************
     */
    {
        int Start = 0;
        int Max = 0;
        for (int i = 0; i < S1.length(); i++) {
            for (int j = 0; j < S2.length(); j++) {
                int x = 0;
                while (S1.charAt(i + x) == S2.charAt(j + x)) {
                    x++;
                    if (((i + x) >= S1.length()) || ((j + x) >= S2.length())) {
                        break;
                    }
                }
                if (x > Max) {
                    Max = x;
                    Start = i;
                }
            }
        }
        return S1.substring(Start, (Start + Max));
    }

}
