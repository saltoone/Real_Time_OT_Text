package server;

import java.awt.Color;
import java.awt.Toolkit;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;

import main.Message;
import main.Settings;
import main.Test;

/**
 * The server that can be run both as a console application or a GUI
 */
public class Server {

    private static int uniqueId; // unique ID for each connection
    private ArrayList<ClientThread> clientsList; // list of clients
    private ServerFrame serverFrame; // GUI of server

    private int port; // the port number to listen for connection
    private boolean listeningForClients; // the boolean that will be turned off
    // to stop the server

    /**
     * server constructor that receive the port to listen to for connection as
     * paramete
     */
    public Server(int port) {
        this(port, null);
    }

    public Server(int port, ServerFrame serverGui) {
        this.serverFrame = serverGui; // if it is null no GUI is created for
        // server
        this.port = port;
        clientsList = new ArrayList<ClientThread>(); // keep a list of client
        // threads
    }

    /**
     * creates socket server and wait for connection requests
     */
    public void start() throws BadLocationException {
        listeningForClients = true;
        try {
            ServerSocket serverSocket = new ServerSocket(port); // the socket
            // used by the
            // server
            while (listeningForClients) { // infinite loop to listen for
                // connections
                displayImportantInfo("Server listening for Clients on port " + port + ".");
                Socket socket = serverSocket.accept(); // accept connection
                if (!listeningForClients) { // if server is stoped
                    break;
                }
                // make a thread of the client socket and save it to the list
                // then start it
                ClientThread thread = new ClientThread(socket);
                clientsList.add(thread);
                thread.start();
            }
            // close the server socket
            try {
                serverSocket.close();
                for (int i = 0; i < clientsList.size(); ++i) {
                    ClientThread clientThread = clientsList.get(i);
                    try {
                        clientThread.inputStream.close();
                        clientThread.outputStream.close();
                        clientThread.socket.close();
                    } catch (IOException ioE) {
                        System.out.println("Error closing the server socket.");
                    }
                }
            } catch (Exception e) {
                displayError("Error closing the server and clients: " + e);
            }
        } catch (IOException e) {
            String msg = Settings.DateFormat.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
            displaySuccess(msg);
        }
    }

    private void info(String msg) {
        display(msg, Color.lightGray);
    }

    private void displayError(String msg) {
        display(msg, Color.RED);
    }

    private void displaySuccess(String msg) {
        display(msg, Color.GREEN);
    }

    private void displayImportantInfo(String msg) {
        display(msg, Color.BLUE);

    }

    /**
     * used by the GUI to stop the server
     */
    protected void stop() {
        listeningForClients = false;
        // connect to myself as Client to exit statement
        try {
            new Socket(Settings.DefaultHost, port);
        } catch (Exception e) {
            System.out.println("Error creating socket");
        }
    }

    /**
     * Display an event (not a message) to the console or the GUI
     *
     * @param color
     */
    private void display(String msg, Color color) {
        String time = Settings.DateFormat.format(new Date()) + " " + msg;
        if (serverFrame == null) {
            System.out.println(time);
        } else {
            serverFrame.appendEvent(time + "\n", color);
        }
    }

    /**
     * broadcasts a message to all Clients
     */
    private synchronized void broadcastMsg(String message) throws BadLocationException {
        // add a time stamp (of format HH:mm:ss) to the message
        String time = Settings.DateFormat.format(new Date());
        String msgStr = time + " " + message + "\n";
        // display message GUI
        if (serverFrame == null) {
            System.out.print(msgStr);
        } else {
            
            serverFrame.appendRoom(msgStr); // append in the room window
        }

        // loop through the client list in reverse order and remove clients that
        // are disconnected
        for (int i = clientsList.size(); --i >= 0;) {
            ClientThread clientThread = clientsList.get(i);
            // try to write to the Client if it fails remove it from the list
            if (!clientThread.writeMsg(new Message(Message.MESSAGE, msgStr))) {
                clientsList.remove(i);
                displayImportantInfo("Disconnected Client " + clientThread.username + " removed from list.");
            }
        }
    }

    /**
     * broadcasts edit messages to all Clients
     */
    private synchronized void broadcastEdit(Message editMsg) {
        // display message on console or GUI
        if (serverFrame == null) {
            System.out.print(editMsg);
        } else {
            serverFrame.appendEvent(editMsg); // append in the room window
            serverFrame.updateEditArea(editMsg);
        }
        // loop through the client list in reverse order and remove clients that
        // are disconnected
        for (int i = clientsList.size(); --i >= 0;) {
            ClientThread clientThread = clientsList.get(i);
            // try to write to the Client if it fails remove it from the list
            if (!clientThread.writeMsg(editMsg)) {
                clientsList.remove(i);
                displayImportantInfo("Disconnected Client " + clientThread.username + " removed from list.");
            }
        }
    }

    private synchronized void enableEditingForAll(Message msg) {
        for (int i = clientsList.size(); --i >= 0;) {
            ClientThread clientThread = clientsList.get(i);
            // try to write to the Client if it fails remove it from the list
            if (!clientThread.writeMsg(msg)) {
                clientsList.remove(i);
                displayImportantInfo("Disconnected Client " + clientThread.username + " removed from list.");
            }
        }
    }

    /**
     * removes a client who has logged out
     */
    private synchronized void remove(String id) {
        for (int i = 0; i < clientsList.size(); ++i) {
            ClientThread clientThread = clientsList.get(i);
            if (clientThread.username.equals(id)) {
                clientsList.remove(i);
                return;
            }
        }
    }

    /**
     * Main method to run the server as a console application: Open a console
     * window and type: java Server to run the java server with default port or
     * java Server portNumber
     */
    public static void main(String[] args) throws BadLocationException {
        // start server on the default port unless a PortNumber is specified
        int portNumber = Settings.DefaultPort;
        switch (args.length) {
            case 1:
                try {
                    portNumber = Integer.parseInt(args[0]);
                } catch (Exception e) {
                    System.out.println("Invalid port number.");
                    System.out.println("Usage is: > java Server [portNumber]");
                    return;
                }
            case 0:
                break;
            default:
                System.out.println("Usage is: > java Server [portNumber]");
                return;

        }
        // create a server object and start it
        Server server = new Server(portNumber);
        server.start();
    }

    /**
     * One instance of this thread will run for each client
     */
    private class ClientThread extends Thread {

        private Socket socket; // reference to the socket that client is
        // listening/talking to
        private ObjectInputStream inputStream;
        private ObjectOutputStream outputStream;
        private int userId; // a unique client id used for reconnection
        private String username; // client username which is a unique identifier
        // used for reconnection and find the user
        // object
        private Message chatMsg; // chat message
        private String dateConnected; // date of connection
        private Reminder reminder = null;

        public ClientThread(Socket socket) throws BadLocationException {
            userId = ++uniqueId;
            this.socket = socket;
            // Creating input and output Data Streams
            try {
                outputStream = new ObjectOutputStream(socket.getOutputStream()); // creating
                // chat
                // output
                // first
                inputStream = new ObjectInputStream(socket.getInputStream());
                username = (String) inputStream.readObject(); // reading the
                // username

                for (ClientThread aClientThread : clientsList) {
                    if (aClientThread.getUsername().equals(username)) {
                        displayError(username + " already exists. ");
                        this.username += "-" + userId;
                        displaySuccess("Changing the username to " + username);
                        break;
                    }
                }

                writeMsg(new Message(Message.CHANGEID, username));
                writeMsg(new Message(Message.COPYALL, serverFrame.getTextEditArea().getText()));
                displaySuccess(username + " is connected.");
                System.out.println("Creating Object Input/Output Streams in the client " + username + " thread");
            } catch (IOException e) {
                displayError("Error creating Input/output Streams: " + e);
                return;
            } catch (ClassNotFoundException e) {
                displayError("Class not found Exception: " + e);
                return;
            }
            dateConnected = new Date().toString() + "\n";
        }

        /**
         * loops until user logs out
         */
        public void run() {
            boolean keepGoing = true;
            while (keepGoing) {
                // reading the message object
                try {
                    chatMsg = (Message) inputStream.readObject();
                } catch (IOException e) {
                    displayError(username + " is disconnected. ");
                    break;
                } catch (ClassNotFoundException e2) {
                    System.out.println("Class not found Exception: " + e2);
                    break;
                }
                String message = chatMsg.getMessage();
                // based on the type of message do the following
                switch (chatMsg.getType()) {

                    case Message.MESSAGE:
                {
                    try {
                        broadcastMsg(username + ": " + message);
                    } catch (BadLocationException ex) {
                        Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                        break;
                    case Message.LOGOUT:
                        displayImportantInfo(username + " logged out.");
                        keepGoing = false;
                        break;
                    case Message.WHOISIN:
                {
                    try {
                        writeMsg(new Message(Message.MESSAGE,
                                "List of the users connected at " + Settings.DateFormat.format(new Date()) + "\n"));
                    } catch (BadLocationException ex) {
                        Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                        // scan all the users connected
                        for (int i = 0; i < clientsList.size(); ++i) {
                            ClientThread clientThread = clientsList.get(i);
                    try {
                        writeMsg(new Message(Message.MESSAGE,
                                (i + 1) + ") " + clientThread.username + " since " + clientThread.dateConnected));
                    } catch (BadLocationException ex) {
                        Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                    }
                        }
                        break;
                    case Message.EDIT:

                        int miliSecondsDelay = Settings.MiliSecondsDelayForLocking;
                        broadcastEdit(chatMsg);

                        updateStatus(chatMsg.getSender() + " has typed. Wait " + miliSecondsDelay + " second.", true);

                        if (reminder != null) {
                            reminder.cancel();
                        }
                        reminder = new Reminder(miliSecondsDelay);
                        break;
                }
            }
            // remove client from the list of connected clients
            remove(username);
            close();
        }

        /**
         * close data streams and the socket connection
         */
        private void close() {

            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (Exception e) {
                System.out.println("Error closing the output stream.");
            }

            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception e) {
                System.out.println("Error closing the input stream.");
            }

            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (Exception e) {
                System.out.println("Error closing the Socket connection.");
            }
        }

        /**
         * Write a String to the Client output stream
         */
        private boolean writeMsg(Message msg) {
            if (!socket.isConnected()) { // if client is not connected close its
                // connections
                close();
                return false;
            }
            // if Client is still connected send the message to it by writing
            // the message to its stream
            try {
                outputStream.writeObject(msg);
            } catch (IOException e) {
                displayError("Error sending message to " + username);
                displayError(e.toString());
            }
            return true;
        }

        public String getUsername() {
            return username;
        }

    }

    private void updateStatus(String msg, boolean showTime) {
        String message = msg;
        if (showTime) {
            message = Settings.DateFormat.format(new Date()) + " " + msg;
        }
        if (serverFrame == null) {
            System.out.println(message);
        } else {
            serverFrame.updateStatus(message);
        }

    }

    /**
     * this class is a timer to temporarily block other users for a few moment if someone is editing.
     */
    public class Reminder {

        Timer timer;

        public Reminder(int miliSeconds) {
            timer = new Timer();
            timer.schedule(new RemindTask(), miliSeconds );
        }

        public class RemindTask extends TimerTask {

            public void run() {
                updateStatus("", false);
                try {
                    enableEditingForAll(new Message(Message.UNLOCK, ""));
                } catch (BadLocationException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
                Toolkit.getDefaultToolkit().beep();
                timer.cancel(); // Terminate the timer thread
            }
        }

        public void cancel() {
            timer.cancel();
        }
    }
}
