package com.helloCash.helloCash.service;


import com.helloCash.helloCash.integration.BankIntegrationService;
import com.helloCash.helloCash.model.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class RequestProcessor {

    @Autowired
    private BankIntegrationService bankIntegrationService;

    @Autowired
    private UserService userService;

    private Map<String, String> userStates = new HashMap<>();

    public String processRequest(String request) {
        String[] parts = request.split(":");
        String phoneNumber = parts[0];
        String action = parts.length > 1 ? parts[1].toUpperCase() : "";

        if (action.equals("ACTIVATE")) {
            boolean isValid = bankIntegrationService.validatePhoneNumberWithBank(phoneNumber);
            if (isValid) {
                userStates.put(phoneNumber, "AWAITING_PIN");
                return "Phone number validated. Please enter your 4-digit PIN:";
            } else {
                return "Phone number not registered with bank.";
            }
        } else if (userStates.get(phoneNumber) != null && userStates.get(phoneNumber).equals("AWAITING_PIN")) {
            if (parts[1].matches("\\d{4}")) {
                userService.saveUser(new UserEntity(phoneNumber, parts[1]));
                userStates.remove(phoneNumber);
                return "User activated successfully.";
            } else {
                return "Invalid PIN format. Please enter a 4-digit PIN:";
            }
        } else {
            return "Invalid request format.";
        }
    }
}

