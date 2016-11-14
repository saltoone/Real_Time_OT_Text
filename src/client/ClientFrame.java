
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import main.Message;
import main.Settings;

/**
 * the class that creates GUI for the client side.
 */
public class ClientFrame extends javax.swing.JFrame implements ActionListener {

    private boolean connected;
    private Client client;

    // the default values
    private int portNumber;
    private String hostName = Settings.DefaultHost;
    private String guestText = Settings.DefaultUserName;
    private String loginText = "Enter your username below";
    private String messageTxt = "Enter your message below";
    private String editingAllowedText = "Editing is enabled.";
    private int screenWidth = Settings.DefaultScreenWidth;
    private int screenHeight = Settings.DefaultScreenHeight;
    private int screenXpos;
    private int screenYpos;

    private boolean accessAllowed;

    private final static String TITLE = "Client of Real-time Group Text Editor";

    /**
     * main class that start the client GUI
     */
    public static void main(String[] args) {
        new ClientFrame(Settings.DefaultHost, Settings.DefaultPort, Settings.DefaultScreenWidth, 0);
    }

    /**
     * receives host name and port number
     *
     * @param host
     * @param port
     */
    public ClientFrame(String host, int port) {
        super(TITLE);
        Toolkit toolkit = getToolkit();
        Dimension dimension = toolkit.getScreenSize();
        screenXpos = (int) dimension.getWidth() - screenWidth;
        screenYpos = 0;// (int)dimension.getHeight() - screenHeight;

        setup(host, port, screenXpos, screenYpos);
    }

    public ClientFrame(String host, int port, int x, int y) {
        super(TITLE);
        setup(host, port, x, y);
    }

    private void setup(String host, int port, int screenXpos, int screenYpos) {
        initComponents();

        serverHostField.setText(host);

        portNumberField.setText("" + port);
        userInputField.setText(guestText);
        userInputField.setBackground(Color.WHITE);

        chatTextArea.setText("You have entered the Chat room\n");
        chatTextArea.setFont(Settings.DefaultFont);
        chatTextArea.setEditable(false);

        textEditArea.setFont(Settings.DefaultFont);
        textEditArea.addKeyListener(keyListener);

        loginBtn.addActionListener(this);

        logoutBtn.addActionListener(this);
        logoutBtn.setEnabled(false); // user has to login before being able to
        // logout
        whoIsInBtn.addActionListener(this);
        whoIsInBtn.setEnabled(false);

        setLocation(screenXpos, screenYpos);

        setVisible(true);
        userInputField.requestFocus();
    }

    /**
     * called by the Client to append text in the chat TextArea
     *
     * @param str
     */
    public void append(String str) {
        chatTextArea.append(str);
        chatTextArea.setCaretPosition(chatTextArea.getText().length() - 1);
    }

    /**
     * called by the Client to append edited text in the
     *
     * @param str
     */
    public void appendEdit(Message msg) {
        activeUserLabel.setText(msg.getSender() + " is editing... Others cannot edit now.");
        if (client.getUsername().equals(msg.getSender())) {
            return;
        }
        // disable the access of others
        setAccessAllowed(false);

        if (msg.getTypedChar() == '\n') {
            textEditArea.insert(msg.getTypedChar() + "", msg.getCaretPos() - 1);
        } else if (msg.getTypedChar() == '\b') { // backspace
            if (0 <= msg.getCaretPos() && msg.getCaretPos() < textEditArea.getText().length()) {
                String startText = textEditArea.getText().substring(0, msg.getCaretPos());
                String endText = textEditArea.getText().substring(msg.getCaretPos() + 1, textEditArea.getText().length());
                textEditArea.setText(startText + endText);
            }
        } else {
            textEditArea.insert(msg.getTypedChar() + "", msg.getCaretPos());
        }
    }

    /**
     * called by the GUI if the connection failed resets the buttons, label, and
     * textfield
     */
    public void connectionFailed() {
        loginBtn.setEnabled(true);
        logoutBtn.setEnabled(false);
        whoIsInBtn.setEnabled(false);
        userInputLabel.setText(loginText);
        userInputField.setText(guestText);
        // reset port number and host name as a construction time
        portNumberField.setText("" + portNumber);
        serverHostField.setText(serverHostField.getText());
        // let the user change them
        serverHostField.setEditable(false);
        portNumberField.setEditable(false);
        userInputField.removeActionListener(this);
        connected = false;
    }

    /**
     * action perfored if Button or JTextField is clicked
     */
    public void actionPerformed(ActionEvent e) {
        Object o = e.getSource();
        if (o == logoutBtn) { try {
            // if it is the Logout button
            client.sendMessage(new Message(Message.LOGOUT, ""));
            } catch (BadLocationException ex) {
                Logger.getLogger(ClientFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
            return;
        }
        if (o == whoIsInBtn) { try {
            // if it the who is in button
            client.sendMessage(new Message(Message.WHOISIN, ""));
            return;
            } catch (BadLocationException ex) {
                Logger.getLogger(ClientFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        // if it's not the buttons then it is the text field, need to send the
        // message
        if (connected) {
            try {
                client.sendMessage(new Message(Message.MESSAGE, userInputField.getText()));
            } catch (BadLocationException ex) {
                Logger.getLogger(ClientFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
            userInputField.setText("");
            return;
        }
        // if it's none of the above then the login button is clicked to
        // establish a connection
        if (o == loginBtn) {
            String username = userInputField.getText().trim();
            if (username.length() == 0) { // if username is empty then ignore
                // the action
                append("Please enter your username \n");
                return;
            }

            String server = serverHostField.getText().trim();
            if (server.length() == 0) { // if host name is empty
                append("Please enter the host address \n");
                return;
            }

            String portNumber = portNumberField.getText().trim();
            if (portNumber.length() == 0) { // if port number is not entered
                append("Please enter the port number \n");
                return;
            }
            int port = 0;
            try {
                port = Integer.parseInt(portNumber);
            } catch (NumberFormatException en) {
                append("Please enter a number for the port number \n");
                return;
            }

            // create a new Client object with GUI and test if it can be started
            client = new Client(server, port, username, this);
            if (!client.start()) {
                append("Cannot start the Client \n");
                return;
            }
            // the client is started successfully
            userInputField.setText("");
            userInputLabel.setText(messageTxt);
            connected = true;

            // disable login button and enable the other 2 buttons
            loginBtn.setEnabled(false);
            logoutBtn.setEnabled(true);
            whoIsInBtn.setEnabled(true);
            // disable the Server and Port JTextField
            serverHostField.setEditable(false);
            portNumberField.setEditable(false);

            // add action listener for when the user enters a message
            userInputField.addActionListener(this);
        }

    }

    public boolean isAccessAllowed() {
        return accessAllowed;
    }

    /**
     * enables or disables the access for editing the access is allowed at the
     * start for all the users as soon as the first person starts typing the
     * access i restricted to him/her for an amount of time.
     *
     * @param accessAllowed
     */
    public void setAccessAllowed(boolean accessAllowed) {
        this.accessAllowed = accessAllowed;
        textEditArea.setEditable(accessAllowed);
        textEditArea.setFocusable(accessAllowed);
        if (accessAllowed) {
            activeUserLabel.setText(editingAllowedText);
        }
    }

    public void updateEditAreaText(String text) {
        textEditArea.setText(text);
    }

    private KeyListener keyListener = new KeyListener() {

        public void keyTyped(KeyEvent keyEvent) {
            // printIt("Typed", keyEvent);
            if (textEditArea.isFocusOwner()) {
                char keyPressed = keyEvent.getKeyChar();
                int keyCode = keyEvent.getKeyCode();
                // String keyName = KeyEvent.getKeyText(keyCode);
                String message = keyPressed + "";// "keyPressed "+keyPressed + "
                
                // keyName: "+keyName;
                int caretPos = textEditArea.getCaretPosition();
                client.sendMessage(new Message(Message.EDIT, message, client.getUsername(), caretPos, keyPressed));
            }
        }

        public void keyPressed(KeyEvent keyEvent) {
            
        }

        public void keyReleased(KeyEvent keyEvent) {
            
        }

        private void printIt(String title, KeyEvent keyEvent) {
            int keyCode = keyEvent.getKeyCode();
            String keyText = KeyEvent.getKeyText(keyCode);
            System.out.println(title + " : " + keyText);
        }
    };

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        serverHostField = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        portNumberField = new javax.swing.JTextField();
        userInputLabel = new javax.swing.JLabel();
        userInputField = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        chatTextArea = new javax.swing.JTextArea();
        loginBtn = new javax.swing.JButton();
        logoutBtn = new javax.swing.JButton();
        whoIsInBtn = new javax.swing.JButton();
        textEditAreaScrollPane = new javax.swing.JScrollPane();
        textEditArea = new javax.swing.JTextArea();
        activeUserLabel = new javax.swing.JLabel();
        jToolBar1 = new javax.swing.JToolBar();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Client of RealTime Text Editor");

        jLabel1.setText("Server Address");

        serverHostField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                serverHostFieldActionPerformed(evt);
            }
        });

        jLabel2.setText("Port Number");

        userInputLabel.setText("Enter a Username");

        jScrollPane1.setBorder(javax.swing.BorderFactory.createTitledBorder("Chat Area"));

        chatTextArea.setColumns(20);
        chatTextArea.setRows(5);
        jScrollPane1.setViewportView(chatTextArea);

        loginBtn.setText("Login");

        logoutBtn.setText("Logout");

        whoIsInBtn.setText("Who is in");

        textEditAreaScrollPane.setBorder(javax.swing.BorderFactory.createTitledBorder("Text Editing Area"));

        textEditArea.setBackground(new java.awt.Color(204, 255, 255));
        textEditArea.setColumns(20);
        textEditArea.setRows(5);
        textEditAreaScrollPane.setViewportView(textEditArea);

        jToolBar1.setRollover(true);

        jMenu1.setText("File");

        jMenuItem1.setText("Open");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem1);

        jMenuItem2.setText("Save");
        jMenu1.add(jMenuItem2);

        jMenuBar1.add(jMenu1);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(22, 22, 22)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                .addComponent(userInputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 131, Short.MAX_VALUE)
                                .addGap(103, 103, 103))
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel1)
                                    .addComponent(jLabel2))
                                .addGap(28, 28, 28)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(serverHostField, javax.swing.GroupLayout.DEFAULT_SIZE, 113, Short.MAX_VALUE)
                                    .addComponent(portNumberField)))
                            .addComponent(userInputField)))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(loginBtn)
                        .addGap(18, 18, 18)
                        .addComponent(logoutBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(whoIsInBtn))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jScrollPane1))
                    .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 307, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(activeUserLabel)
                        .addContainerGap(303, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(textEditAreaScrollPane))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(textEditAreaScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 324, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(activeUserLabel)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(serverHostField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2)
                            .addComponent(portNumberField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(userInputLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 10, Short.MAX_VALUE)
                        .addComponent(userInputField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(loginBtn)
                            .addComponent(logoutBtn)
                            .addComponent(whoIsInBtn))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 193, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void serverHostFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_serverHostFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_serverHostFieldActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed

        JFileChooser jFileChooser =new JFileChooser();
        StringBuffer buffer;
         buffer = new StringBuffer();
        int result= jFileChooser.showOpenDialog(this);
        if(result==JFileChooser.APPROVE_OPTION)
    {
        try {
            FileReader reader;
            reader = null;
            File file=jFileChooser.getSelectedFile();
            reader = new FileReader(file);
            int i=1;
            while(i!=-1)
            {
                i=reader.read();
                char ch=(char) i;
                buffer.append(ch);

            }

            textEditArea.setText(buffer.toString());
            updateEditAreaText(buffer.toString());
        } catch (FileNotFoundException ex) {
            //Logger.getLogger(MainForm.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
          //  Logger.getLogger(MainForm.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
        // TODO add your handling code here:
    }//GEN-LAST:event_jMenuItem1ActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel activeUserLabel;
    private javax.swing.JTextArea chatTextArea;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JButton loginBtn;
    private javax.swing.JButton logoutBtn;
    private javax.swing.JTextField portNumberField;
    private javax.swing.JTextField serverHostField;
    public javax.swing.JTextArea textEditArea;
    private javax.swing.JScrollPane textEditAreaScrollPane;
    private javax.swing.JTextField userInputField;
    private javax.swing.JLabel userInputLabel;
    private javax.swing.JButton whoIsInBtn;
    // End of variables declaration//GEN-END:variables

}
