package client;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;

import main.Message;
import main.Settings;

/**
 * The Client object can be run as either console or a GUI
 */
public class Client {

    private ObjectInputStream inputStream; // input stream for reading from the
    // socket
    private ObjectOutputStream outputStream; // output stream for writing on the
    // socket
    private Socket socket;

    private ClientFrame clientGui;

    private String serverAddress;
    private String username;
    private int port;

    /**
     * called by console mode defining server, port and username
     */
    public Client(String server, int port, String username) {
        // which calls the common constructor with the GUI set to null
        this(server, port, username, null);
    }

    /**
     * called by GUI
     */
    public Client(String server, int port, String username, ClientFrame clientFrame) {
        this.serverAddress = server;
        this.port = port;
        this.username = username;
        this.clientGui = clientFrame;
    }

    public boolean start() {
        // connect to the server
        try {
            socket = new Socket(serverAddress, port);
        } catch (Exception ec) {
            display("Error connectiong to server:" + ec);
            return false;
        }

        String msg = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort();
        display(msg);

        try { // Creating both Data Stream
            inputStream = new ObjectInputStream(socket.getInputStream());
            outputStream = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException eIO) {
            display("Exception creating new Input/output Streams: " + eIO);
            return false;
        }

        // creates the Thread to listen from the server
        new ListenFromServer().start();

        // Send username to the server. This is the only message that is sent as
        // a String. All other messages will be ChatMessage objects
        try {
            outputStream.writeObject(username);
        } catch (IOException e) {
            display("Exception doing login : " + e);
            disconnect();
            return false;
        }
        return true;
    }

    /**
     * displays the message to the console or the GUI
     */
    private void display(String msg) {
        if (clientGui == null) {
            System.out.println(msg); // print in console mode
        } else {
            clientGui.append(msg + "\n"); // append to the ClientGUI JTextArea
        }
    }

    /**
     * sends a message to the server
     */
    public void sendMessage(Message msg) {
        try {
            outputStream.writeObject(msg);
        } catch (IOException e) {
            display("Error writing to the server: " + e);
        }
    }

    /**
     * disconnects the client and close streams
     */
    private void disconnect() {
        try {
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (Exception e) {
        } // not much else I can do

        try {
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (Exception e) {
        } // not much else I can do

        try {
            if (socket != null) {
                socket.close();
            }
        } catch (Exception e) {
        } // not much else I can do

        // inform the GUI
        if (clientGui != null) {
            clientGui.connectionFailed();
        }

    }

    public String getUsername() {
        return username;
    }

    /**
     * main method to start the client
     */
    public static void main(String[] args) throws BadLocationException {

        int portNumber = Settings.DefaultPort;
        String serverAddress = Settings.DefaultHost;
        String userName = Settings.DefaultUserName;

        // depending of the number of arguments provided we fall through
        switch (args.length) {
            // > javac Client username portNumber serverAddr
            case 3:
                serverAddress = args[2];
            // > javac Client username portNumber
            case 2:
                try {
                    portNumber = Integer.parseInt(args[1]);
                } catch (Exception e) {
                    System.out.println("Invalid port number.");
                    System.out.println("Usage is: > java Client [username] [portNumber] [serverAddress]");
                    return;
                }
            // > javac Client username
            case 1:
                userName = args[0];
            // > java Client
            case 0:
                break;
            // invalid number of arguments
            default:
                System.out.println("Usage is: > java Client [username] [portNumber] {serverAddress]");
                return;
        }
        // create the Client object
        Client client = new Client(serverAddress, portNumber, userName);
        // test if we can start the connection to the Server if it failed
        // nothing we can do
        if (!client.start()) {
            return;
        }
        // wait for messages from user
        Scanner scan = new Scanner(System.in);
        while (true) { // loop forever for message from the user
            System.out.print("> ");
            String msg = scan.nextLine(); // read message from user
            
            if (msg.equalsIgnoreCase("LOGOUT")) {try {
                // logout if message is LOGOUT
                client.sendMessage(new Message(Message.LOGOUT, ""));
                } catch (BadLocationException ex) {
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                }
                break; // break to do the disconnect
            } else if (msg.equalsIgnoreCase("WHOISIN")) {
                try {
                    client.sendMessage(new Message(Message.WHOISIN, ""));
                } catch (BadLocationException ex) {
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else { // default to ordinary message
                client.sendMessage(new Message(Message.MESSAGE, msg));
            }
        }
        client.disconnect();
    }

    int showOpenDialog(ClientFrame aThis) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    File getSelectedFile() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * waits for the message from the server and append them to the JTextArea if
     * there is a GUI otherwise prints it in console mode
     */
    public class ListenFromServer extends Thread {

        public void run() {
            while (true) {
                try {
                    Message msg = (Message) inputStream.readObject();
                    // if console mode print the message and add back the prompt
                    if (clientGui == null) {
                        System.out.println(msg.getMessage());
                        System.out.print("> ");
                    } else if (msg.getType() == Message.EDIT) {
                        clientGui.appendEdit(msg);
                    } else if (msg.getType() == Message.CHANGEID) {
                        if (!msg.getMessage().equals(username)) {
                            display("Server changed username to " + msg.getMessage());
                            username = msg.getMessage();
                        } else {
                            display("Server accepted your username.");
                        }
                    } else if (msg.getType() == Message.UNLOCK) {
                        clientGui.setAccessAllowed(true);
                    } else if (msg.getType() == Message.COPYALL) {
                        clientGui.updateEditAreaText(msg.getMessage());
                    } else {
                        clientGui.append(msg.getMessage());
                    }
                } catch (IOException e) {
                    display("Server has close the connection: " + e);
                    if (clientGui != null) {
                        clientGui.connectionFailed();
                    }
                    break;
                } // can't happen with a String object but need the catch anyhow
                catch (ClassNotFoundException e2) {
                }
            }
        }
    }
}
