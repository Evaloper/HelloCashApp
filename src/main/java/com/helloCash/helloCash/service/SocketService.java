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
                    out.println("Type your registered phone number:ACT e.g \"08012345678:ACT:PIN\" to Validate Phone number and set pin");

                    String inputLine;
                    String phoneNumber = null;

                    while ((inputLine = in.readLine()) != null) {
                        System.out.println("Received: " + inputLine);

                        String response = requestProcessor.processRequest(inputLine);
                        if (response.contains("Welcome") || response.contains("User already activated")) {
//                            response += "\nMenu:\n1. Transfer\n2. Check balance\n3. Buy airtime or data";
                            out.println(response);

                            while ((inputLine = in.readLine()) != null) {
                                String menuResponse = requestProcessor.processRequest(phoneNumber + ":" + inputLine);
                                out.println(menuResponse);
                            }
                        } else {
                            out.println(response);
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