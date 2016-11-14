package main;

import client.ClientFrame;
import server.ServerFrame;

public class Main {

    /**
     * @param args
     */
    public static void main(String[] args) {

        new ServerFrame(Settings.DefaultPort);
        new ClientFrame(Settings.DefaultHost, Settings.DefaultPort, 2 * Settings.DefaultScreenWidth, 0);
        new ClientFrame(Settings.DefaultHost, Settings.DefaultPort, Settings.DefaultScreenWidth, Settings.DefaultScreenHeight);
    }

}
