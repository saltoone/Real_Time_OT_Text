package main;

import java.awt.Font;
import java.text.SimpleDateFormat;

public class Settings {

    public final static String DefaultHost = "localhost";
    public final static String DefaultUserName = "Guest";//"Anonymous";
    public final static int DefaultPort = 8080;
    public final static int DefaultScreenWidth = 450;
    public final static int DefaultScreenHeight = 600;
    public final static Font DefaultFont = new Font("Arial", Font.BOLD, 14);
    public final static SimpleDateFormat DateFormat = new SimpleDateFormat("HH:mm:ss"); // date format for displaying time as hh:mm:ss
    public final static String DefaultEditAreaText = "This is the group text editing area\n";
    public static final int TextAreaWidth = 50;
    public static final int TextAreaHeight = 50;
    public static int MiliSecondsDelayForLocking = 150; // 1000 mili-seconds equal 1 second

}
