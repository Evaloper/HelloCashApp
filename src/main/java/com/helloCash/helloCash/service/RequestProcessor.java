package com.helloCash.helloCash.service;

import com.helloCash.helloCash.integration.BankIntegrationService;
import com.helloCash.helloCash.payload.response.NameAccountResponse;
import com.helloCash.helloCash.model.UserEntity;
import com.helloCash.helloCash.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
    private Map<String, String> transferDetails = new HashMap<>();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public String processRequest(String request) {
        String[] parts = request.split(":");
        String phoneNumber = parts[0];
        String action = parts.length > 1 ? parts[1].toUpperCase() : "";

        if (action.equals("ACT") && parts.length == 3) {
            return handleActivation(parts, phoneNumber);
        } else if (userStates.containsKey(phoneNumber)) {
            return handleUserStates(phoneNumber, action, parts);
        } else {
            return "Invalid request format.";
        }
    }

    private String handleActivation(String[] parts, String phoneNumber) {
        String pin = parts[2];
        UserEntity existingUser = userRepository.findByPhoneNumber(phoneNumber);
        if (existingUser != null) {
            userStates.put(phoneNumber, "AWAITING_MENU");
            return "User already activated. Please select an option: 1. Transfer 2. Check Balance 3. Buy Airtime or Data";
        }

        boolean isValid = bankIntegrationService.validatePhoneNumberWithBank(phoneNumber);
        if (isValid) {
            String hashedPin = passwordEncoder.encode(pin);
            userService.saveUser(new UserEntity(phoneNumber, hashedPin));
            NameAccountResponse accountDetails = bankIntegrationService.getAccountDetails(phoneNumber);
            userStates.put(phoneNumber, "AWAITING_MENU");
            return "Welcome " + accountDetails.getFirstName() + " " + accountDetails.getLastName() +
                    ". Your HelloCash app has been activated successfully. Here is your account number " +
                    accountDetails.getAccountNumber() + " to your chosen bank. Please select an option: 1. Transfer 2. Check balance 3. Buy airtime or data";
        } else {
            return "Phone number not registered with bank.";
        }
    }

    private String handleUserStates(String phoneNumber, String action, String[] parts) {
        switch (userStates.get(phoneNumber)) {
            case "AWAITING_MENU":
                return handleMenuSelection(phoneNumber, action);
//            case "TRANSFER_INITIATED":
//                return handleTransfer(phoneNumber, action); // Changed to handle action directly
//            case "TRANSFER_CONFIRM":
//                return confirmTransfer(phoneNumber, action); // New state for confirming transfer
            case "AIRTIME_INITIATED":
                return handleBuyAirtime(phoneNumber, action); // Changed to handle action directly
            default:
                return "Invalid state.";
        }
    }

    private String handleMenuSelection(String phoneNumber, String action) {
        switch (action) {
            case "1":
                userStates.put(phoneNumber, "TRANSFER_INITIATED");
                return "Enter the destination account number and amount separated by a comma, e.g., accountNumber,amount";
            case "2":
                return handleCheckBalance(phoneNumber);
            case "3":
                userStates.put(phoneNumber, "AIRTIME_INITIATED");
                return "Enter the phone number to top-up and amount separated by a comma, e.g., phoneNumber,amount";
            default:
                return "Invalid option. Please select an option: 1. Transfer 2. Check Balance 3. Buy Airtime or Data";
        }
    }


    private String handleCheckBalance(String phoneNumber) {
        // Fetch account details using the phone number
        NameAccountResponse accountDetails = bankIntegrationService.getAccountDetails(phoneNumber);
        if (accountDetails == null || accountDetails.getAccountNumber() == null) {
            return "Unable to fetch account details. Please try again.";
        }

        // Fetch balance using the account number
        String accountNumber = accountDetails.getAccountNumber();
        BigDecimal balance = bankIntegrationService.getBalance(accountNumber).getAccountBalance();
        return "Your current balance is: " + balance;
    }

    private String handleBuyAirtime(String phoneNumber, String pin) {
        return "Airtime purchased successfully";
    }
}