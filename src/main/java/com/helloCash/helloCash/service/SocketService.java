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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SocketService implements Runnable {

    @Autowired
    private RequestProcessor requestProcessor;

    private Map<String, String> activationState = new HashMap<>();
    private final List<PrintWriter> clientWriters = new CopyOnWriteArrayList<>();

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(8082)) {
            System.out.println("Listening on port 8082...");

            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                     PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true)) {
                    out.println("Type your registered phone number:ACT e.g \"08012345678:ACT:PIN\" to Validate Phone number and set pin");
                    clientWriters.add(out);

                    String inputLine;
                    String phoneNumber = null;

                    while ((inputLine = in.readLine()) != null) {
                        System.out.println("Received: " + inputLine);

                        if (phoneNumber == null) {
                            phoneNumber = inputLine.split(":")[0];
                        }

                        String response = requestProcessor.processRequest(inputLine);
                        out.println(response);

                        if (response.contains("Please select an option")) {
                            while ((inputLine = in.readLine()) != null) {
                                String menuResponse = requestProcessor.processRequest(phoneNumber + ":" + inputLine);
                                out.println(menuResponse);
//                                if (menuResponse.contains("Please select an option") || menuResponse.contains("Type MENU")) {
//                                    break;
//                                }
                            }
                        } else if (response.contains("Please confirm by typing your PIN")) {
                            inputLine = in.readLine();
                            String confirmationResponse = requestProcessor.processRequest(phoneNumber + ":" + inputLine);
                            out.println(confirmationResponse);
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
    public void broadcastMessage(String message) {
        for (PrintWriter writer : clientWriters) {
            writer.println(message);
        }
    }
}
