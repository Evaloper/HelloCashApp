package com.helloCash.helloCash.model;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class SocketClient {

    public static void main(String[] args) {
        String hostName = "localhost";
        int portNumber = 8082;

        try (Socket socket = new Socket(hostName, portNumber);
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in))) {

            String fromServer;
            String fromUser;

            while ((fromServer = in.readLine()) != null) {
                System.out.println(fromServer);


                if (fromServer.contains("Type your registered phone number:ACT e.g \"08012345678:ACT:PIN\" to Validate Phone number and set pin")) {
                    fromUser = stdIn.readLine();
                    if (fromUser != null) {
                        out.println(fromUser);
                    }
                } else if (fromServer.contains("Menu:")) {
                    System.out.println(fromServer);
                    while ((fromServer = in.readLine()) != null) {
                        System.out.println(fromServer);
                        System.out.print("Client: ");
                        fromUser = stdIn.readLine();
                        if (fromUser != null) {
                            System.out.println("Sending to server: " + fromUser);
                            out.println(fromUser);
                        }
                    }
                } else {
                    // Handle other responses
                    System.out.print("Client: ");
                    fromUser = stdIn.readLine();
                    if (fromUser != null) {
                        System.out.println("Sending to server: " + fromUser);
                        out.println(fromUser);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
