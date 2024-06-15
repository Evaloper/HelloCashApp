package com.helloCash.helloCash.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

@Service
public class SocketService implements Runnable {

    @Autowired
    private RequestProcessor requestProcessor;

    private Map<String, String> activationState = new HashMap<>();

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(8082)) {
            System.out.println("Listening on port 8082...");

            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                     PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true)) {

                    String inputLine;
                    String phoneNumber = null;

                    while ((inputLine = in.readLine()) != null) {
                        System.out.println("Received: " + inputLine);

                        if (activationState.containsKey(phoneNumber)) {
                            // Process the PIN input
                            String response = requestProcessor.processRequest(phoneNumber + ":" + inputLine);
                            out.println(response);
                            if (response.contains("User activated successfully.")) {
                                activationState.remove(phoneNumber);
                                out.println("Menu:");
                                out.println("1. Transfer");
                                out.println("2. Check balance");
                                out.println("3. Buy airtime or data");
                                // Continue to listen for further commands
                            }
                        } else {
                            // Process the initial activation command
                            String[] parts = inputLine.split(":");
                            phoneNumber = parts[0];
                            String response = requestProcessor.processRequest(inputLine);
                            out.println(response);
                            if (response.contains("Please enter your 4-digit PIN:")) {
                                activationState.put(phoneNumber, "awaiting_pin");
                            }
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
