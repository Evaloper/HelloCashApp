package com.helloCash.helloCash.service;

import com.helloCash.helloCash.integration.BankIntegrationService;
import com.helloCash.helloCash.model.NameAccountResponse;
import com.helloCash.helloCash.model.UserEntity;
import com.helloCash.helloCash.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class RequestProcessor {

    @Autowired
    private BankIntegrationService bankIntegrationService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    private Map<String, String> userStates = new HashMap<>();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public String processRequest(String request) {
        String[] parts = request.split(":");
        String phoneNumber = parts[0];
        String action = parts.length > 1 ? parts[1].toUpperCase() : "";

        if (action.equals("ACT") && parts.length == 3) {
            String pin = parts[2];
            // Check if the user already exists
            UserEntity existingUser = userRepository.findByPhoneNumber(phoneNumber);
            if (existingUser != null) {
                return "User already activated. Please select an option: 1. Transfer 2. Check Balance 3. Buy Airtime or Data";
            }

            boolean isValid = bankIntegrationService.validatePhoneNumberWithBank(phoneNumber);
            if (isValid) {
                String hashedPin = passwordEncoder.encode(pin);
                userService.saveUser(new UserEntity(phoneNumber, hashedPin));
                NameAccountResponse accountDetails = bankIntegrationService.getAccountDetails(phoneNumber);
                return "Welcome " + accountDetails.getFirstName() + " " + accountDetails.getLastName() +
                        ". Your HelloCash app has been activated successfully. Here is your account number " +
                        accountDetails.getAccountNumber() + " to your chosen bank. Please select an option: 1. Transfer 2. Check balance 3. Buy airtime or data";
            } else {
                return "Phone number not registered with bank.";
            }
        } else if (userStates.get(phoneNumber) != null && userStates.get(phoneNumber).equals("AWAITING_MENU")) {
            switch (action) {
                case "1":
                    // Implement transfer logic
                    return "Transfer service is not implemented yet.";
                case "2":
                    // Implement check balance logic
                    return "Check balance service is not implemented yet.";
                case "3":
                    // Implement buy airtime logic
                    return "Buy airtime service is not implemented yet.";
                default:
                    return "Invalid option. Please select an option: \n1. Transfer \n2. Check Balance \n3. Buy Airtime or Data";
            }
        } else {
            return "Invalid request format.";
        }
    }
}