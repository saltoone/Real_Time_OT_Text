package main;


import java.awt.Color;
import java.io.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Highlighter;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import main.Test;

/**
 * This class defines different types of messages that will be exchanged between
 * the Clients and the Server.
 */

public class Message implements Serializable {
        private Highlighter.HighlightPainter redPainter;
	private static final long serialVersionUID = 1L;
       // private Highlighter.HighlightPainter painter;
	// different types of message that clients can send
	public static final int WHOISIN = 0; // to receive the list of the users
											// connected
	public static final int MESSAGE = 1; // a text message
	public static final int LOGOUT = 2; // to disconnect from the Server
	public static final int EDIT = 3; // to edit text in the group editing area
	public static final int CHANGEID = 4; // to change the id in case the same
											// id is
	// chosen
	public static final int UNLOCK = 5; // to unlock the users and allow editing
										// access
	public static final int COPYALL = 6; // to copy the recent text area text
											// from server to client

	private int type;
	private String message;
	private int caretPos;
	private String sender;
	private char typedChar;
        Object textEditArea;

      //   Test t = new Test();
	public Message(int type, String message) throws BadLocationException {
		this.type = type;
              // System.out.println("here");
//                String d ="d";
//                StyledDocument document = new DefaultStyledDocument();
//                SimpleAttributeSet attributes = new SimpleAttributeSet();
//                attributes = new SimpleAttributeSet();
//                attributes.addAttribute(StyleConstants.CharacterConstants.Bold, Boolean.FALSE);
//                attributes.addAttribute(StyleConstants.CharacterConstants.Italic, Boolean.FALSE);
//                attributes.addAttribute(StyleConstants.CharacterConstants.Foreground, Color.BLUE);
//                document.insertString(document.getLength(), message, attributes);
		this.message = message;
//                        painter = new DefaultHighlighter.DefaultHighlightPainter(Color.ORANGE);
//
//                painter = new DefaultHighlighter.DefaultHighlightPainter(Color.ORANGE);
//                            textEditArea.getHighlighter().addHighlight(startIndex, endIndex, painter);

//                if(sender=="a"){
//                   System.out.println("here");
//                }
	}

    public Message(Highlighter.HighlightPainter redPainter, int type, String message, int caretPos, String sender, char typedChar) {
        this.redPainter = redPainter;
        this.type = type;
        this.message = message;
        this.caretPos = caretPos;
        this.sender = sender;
        this.typedChar = typedChar;
        
    }

    public Message(Highlighter.HighlightPainter redPainter) {
        this.redPainter = redPainter;

    }


	public Message(int type, String message, String sender, int caretPos, char typedChar) {
		this.type = type;
		this.message = message;
		this.caretPos = caretPos;
		this.sender = sender;
		this.typedChar = typedChar;
              // new Test.findLastNonWordChar(sender,message);
               

                
        }

    Message() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


	public int getType() {
		return type;
	}


	public String getMessage() {
            
		return message;
	}


	public int getCaretPos() {
		return caretPos;
	}


	public String getSender() {
		return sender;
	}


	public char getTypedChar() {
		return typedChar;
	}

//    private void find(String sender,String message) {
//                t.findLastNonWordChar(sender,message);
//    }
}
