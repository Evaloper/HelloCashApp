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
import java.util.logging.Logger;

import static java.lang.System.out;


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
    private final Logger logger = Logger.getLogger(RequestProcessor.class.getName());

    private String lastActivatedPhoneNumber = null;

    public String processRequest(String request) {
        String[] parts = request.split(":");
        String phoneNumber;
        String action = parts.length > 1 ? parts[1].toUpperCase() : "";

        if (parts.length > 0 && !parts[0].isEmpty()) {
            phoneNumber = parts[0];
        } else {
            phoneNumber = lastActivatedPhoneNumber;
        }

        if (phoneNumber == null) {
            return "Invalid request format. Phone number is required.";
        }

        logger.info("Processing request: " + request + " for phone number: " + phoneNumber);
        logger.info("Current state: " + userStates.getOrDefault(phoneNumber, "None"));

        if (action.equals("CANCEL")) {
            return handleCancelCommand(phoneNumber);
        }

        if (action.equals("ACT") && parts.length == 3) {
            return handleActivation(parts, phoneNumber);
        } else if (action.equals("CANCEL")) {
            return handleCancelCommand(phoneNumber);
        } else if (action.equals("MENU")) {
            return handleMenu(phoneNumber);
        } else if (userStates.containsKey(phoneNumber)) {
            return handleUserStates(phoneNumber, action, parts);
        } else if (parts.length == 1) { // Added to handle direct option selection
            return handleMenuSelection(phoneNumber, action);
        } else {
            return "Invalid request format.";
        }
    }

    private String handleActivation(String[] parts, String phoneNumber) {
        String pin = parts[2];
        UserEntity existingUser = userRepository.findByPhoneNumber(phoneNumber);
        if (existingUser != null) {
            userStates.put(phoneNumber, "AWAITING_MENU");
            lastActivatedPhoneNumber = phoneNumber; // Track the last activated phone number
            return "User already activated. Please select an option: 1. Transfer 2. Check Balance 3. Buy Airtime or Data";
        }

        boolean isValid = bankIntegrationService.validatePhoneNumberWithBank(phoneNumber);
        out.println("Phone validation result: " + isValid); // Debug the validation result
        if (isValid) {
            String hashedPin = passwordEncoder.encode(pin);
            userService.saveUser(new UserEntity(phoneNumber, hashedPin));
            NameAccountResponse accountDetails = bankIntegrationService.getAccountDetails(phoneNumber);
            userStates.put(phoneNumber, "AWAITING_MENU");
            lastActivatedPhoneNumber = phoneNumber; // Track the last activated phone number
            return "Welcome " + accountDetails.getFirstName() + " " + accountDetails.getLastName() +
                    ". Your HelloCash app has been activated successfully. Here is your account number " +
                    accountDetails.getAccountNumber() + " to your chosen bank. You can now select an option: 1. Transfer 2. Check Balance 3. Buy Airtime or Data";
        } else {
            return "Phone number not registered with bank.";
        }
    }


    private String handleMenu(String phoneNumber) {
        userStates.put(phoneNumber, "AWAITING_MENU");
        return "Please select an option: 1. Transfer 2. Check Balance 3. Buy Airtime or Data";
    }


    private String handleUserStates(String phoneNumber, String action, String[] parts) {
        switch (userStates.get(phoneNumber)) {
            case "MENU_TEXT":
                return "Type 'Menu' to continue with other transactions.";
            case "AWAITING_MENU":
                return handleMenuSelection(phoneNumber, action);
            case "TRANSFER_INITIATED":
                return handleTransfer(phoneNumber, action);
            case "TRANSFER_CONFIRM":
                return confirmTransfer(phoneNumber, action);
            case "AIRTIME_INITIATED":
                return handleBuyAirtime(phoneNumber, action);
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
        NameAccountResponse accountDetails = bankIntegrationService.getAccountDetails(phoneNumber);
        if (accountDetails == null || accountDetails.getAccountNumber() == null) {
            return "Unable to fetch account details. Please try again.";
        }

        String accountNumber = accountDetails.getAccountNumber();
        BigDecimal balance = bankIntegrationService.getBalance(accountNumber, phoneNumber).getAccountBalance();
        return "Your current balance is: " + balance + " .Type Menu and follow the prompt to continue the session or type exit to exit the session";
    }

    private String handleBuyAirtime(String phoneNumber, String action) {
        String[] details = action.split(",");
        if (details.length != 2) {
            return "Invalid format. Please enter the phone number and amount separated by a comma.";
        }

//        phoneNumber = details[0];
        BigDecimal amount = new BigDecimal(details[0]);
        String pin = details[1];

        UserEntity user = userRepository.findByPhoneNumber(phoneNumber);
        if (user == null || !passwordEncoder.matches(pin, user.getPin())) {
            return "Invalid PIN. Airtime purchase cancelled.";
        }

        boolean success = bankIntegrationService.buyAirtimeForSelf(amount, pin);
        if (success) {
            userStates.put(phoneNumber, "AWAITING_MENU");
            return "Airtime purchased successfully for " + phoneNumber + ". Type MENU to return to the main menu.";
        } else {
            return "Airtime purchase failed. Please try again.";
        }
    }

    private String handleTransfer(String phoneNumber, String action) {
        String[] details = action.split(",");
        if (details.length != 2) {
            return "Invalid format. Please enter the destination account number and amount separated by a comma.";
        }

        String destinationAccountNumber = details[0];
        BigDecimal amount = new BigDecimal(details[1]);

        NameAccountResponse destinationAccountDetails = bankIntegrationService.getNameDetails(destinationAccountNumber);
        if (destinationAccountDetails == null || destinationAccountDetails.getAccountNumber() == null) {
            return "Invalid destination account number. Please try again.";
        }

        transferDetails.put(phoneNumber, destinationAccountNumber + "," + amount);
        userStates.put(phoneNumber, "TRANSFER_CONFIRM");

        return "You are about to transfer " + amount + " to account " + destinationAccountDetails.getFirstName() + " " + destinationAccountDetails.getLastName() + ". Please confirm by typing your PIN.";
    }

    private String confirmTransfer(String phoneNumber, String pin) {
        UserEntity user = userRepository.findByPhoneNumber(phoneNumber);
        if (user == null || !passwordEncoder.matches(pin, user.getPin())) {
            userStates.put(phoneNumber, "AWAITING_MENU");
            return "Invalid PIN. Transfer cancelled. Please select an option: 1. Transfer 2. Check Balance 3. Buy Airtime or Data";
        }

        String[] details = transferDetails.get(phoneNumber).split(",");
        String sourceAccountNumber = bankIntegrationService.getAccountDetails(phoneNumber).getAccountNumber();
        String destinationAccountNumber = details[0];
        BigDecimal amount = new BigDecimal(details[1]);

        boolean success = bankIntegrationService.transferFunds(sourceAccountNumber, destinationAccountNumber, amount);
        if (success) {
            transferDetails.remove(phoneNumber);
            userStates.put(phoneNumber, "AWAITING_MENU");
            return "Transfer successful. Type MENU to return to the main menu.";
        } else {
            return "Transfer failed. Please try again.";
        }
    }

    private String handleCancelCommand(String phoneNumber) {
        String state = userStates.getOrDefault(phoneNumber, "NONE");
        switch (state) {
            case "TRANSFER_INITIATED":
                transferDetails.remove(phoneNumber);
                userStates.put(phoneNumber, "MENU_TEXT");
                return "Transfer initiation cancelled. Type 'Menu' to continue with other transactions.";

            case "TRANSFER_CONFIRM":
                transferDetails.remove(phoneNumber);
                userStates.put(phoneNumber, "MENU_TEXT");
                return "Transfer confirmation cancelled. Type 'Menu' to continue with other transactions.";

            case "BUY_AIRTIME_INITIATED":
                transferDetails.remove(phoneNumber);
                userStates.put(phoneNumber, "AWAITING_MENU");
                return "Airtime purchase cancelled. Type 'Menu' to continue with other transactions.";

            case "BUY_DATA_INITIATED":
                transferDetails.remove(phoneNumber);
                userStates.put(phoneNumber, "AWAITING_MENU");
                return "Data purchase cancelled. Type 'Menu' to continue with other transactions.";

            default:
                return "There is no ongoing transaction to cancel.";
        }
    }


}
