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
    private PayStackService paystackService;

    @Autowired
    private UserService userService;

    @Autowired
    private BankService bankService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TwilioService twilioService;

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

//        if (action.equals("9")) {
//            return handleCancelCommand(phoneNumber);
//        }

        if (action.equals("ACT") && parts.length == 3) {
            return handleActivation(parts, phoneNumber);
        } else if (action.equals("9")) {
            return handleCancelCommand(phoneNumber);
        } else if (action.equals("0")) {
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
            String message = "User already activated. Please select an option: 1. Transfer to REF's Bank 2. Transfer to Other Banks 3. Check Balance 4. Buy Airtime or Data";
            twilioService.sendSms(phoneNumber, message);
            return message;
        }

        boolean isValid = bankIntegrationService.validatePhoneNumberWithBank(phoneNumber);
        out.println("Phone validation result: " + isValid); // Debug the validation result
        if (isValid) {
            String hashedPin = passwordEncoder.encode(pin);
            userService.saveUser(new UserEntity(phoneNumber, hashedPin));
            NameAccountResponse accountDetails = bankIntegrationService.getAccountDetails(phoneNumber);
            userStates.put(phoneNumber, "AWAITING_MENU");
            lastActivatedPhoneNumber = phoneNumber; // Track the last activated phone number

            String welcomeMessage = "Welcome " + accountDetails.getFirstName() + " " + accountDetails.getLastName() + " " + accountDetails.getOtherName() +
                    ". Your HelloCash app has been activated successfully. Here is your account number " +
                    accountDetails.getAccountNumber() + " to your chosen bank. You can now select an option: 1. Transfer to REF's Bank 2. Transfer to Other Banks 3. Check Balance 4. Buy Airtime or Data";

            twilioService.sendSms(phoneNumber, welcomeMessage);
            return welcomeMessage;
        } else {
            return "Phone number not registered with bank.";
        }
    }


    private String handleMenu(String phoneNumber) {
        userStates.put(phoneNumber, "AWAITING_MENU");
        return "Please select an option: 1. Transfer to REF's Bank 2. Transfer to Other Banks 3. Check Balance 4. Buy Airtime or Data";
    }


    private String handleUserStates(String phoneNumber, String action, String[] parts) {
        switch (userStates.get(phoneNumber)) {
            case "MENU_TEXT":
                return "Type \"0\" to return to the main menu";
            case "AWAITING_MENU":
                return handleMenuSelection(phoneNumber, action);
            case "TRANSFER_INITIATED":
                return handleTransfer(phoneNumber, action);
            case "TRANSFER_CONFIRM":
                return confirmTransfer(phoneNumber, action);
            case "TRANSFER_TO_OTHER_BANK_INITIATED":
                return handleTransferToOtherBank(phoneNumber, action);
            case "TRANSFER_TO_OTHER_BANK_CONFIRM":
                return confirmTransferToOtherBank(phoneNumber, action);
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
                String transferMessage = "Enter the destination account number and amount separated by a comma, e.g., accountNumber,amount. Type \"0\" to go back to main menu or type 9 to cancel.";
                twilioService.sendSms(phoneNumber, transferMessage);
                return transferMessage;
            case "2":
                userStates.put(phoneNumber, "TRANSFER_TO_OTHER_BANK_INITIATED");
                String message = "Enter the destination bank name, account number, and amount separated by commas, e.g., bankName,accountNumber,amount. Type \"0\" to go back to main menu or type 9 to cancel.";
                twilioService.sendSms(phoneNumber, message);
                return message;
            case "3":
                return handleCheckBalance(phoneNumber);
            case "4":
                userStates.put(phoneNumber, "AIRTIME_INITIATED");
                String vasMessage = "Enter the phone number to top-up and amount separated by a comma, e.g., phoneNumber,amount. Type \"0\" to go back to main menu or type 9 to cancel.";
                twilioService.sendSms(phoneNumber, vasMessage);
                return vasMessage;
            default:
                String invalidMessage = "Invalid option. Please select an option: 1. Transfer to REF's Bank 2. Transfer to Other Banks 3. Check Balance 4. Buy Airtime or Data";
                twilioService.sendSms(phoneNumber, invalidMessage);
                return invalidMessage;
        }
    }

    private String handleCheckBalance(String phoneNumber) {
        NameAccountResponse accountDetails = bankIntegrationService.getAccountDetails(phoneNumber);
        if (accountDetails == null || accountDetails.getAccountNumber() == null) {
            String message = "Unable to fetch account details. Please try again.";
            twilioService.sendSms(phoneNumber, message);
            return message ;
        }

        String accountNumber = accountDetails.getAccountNumber();
        BigDecimal balance = bankIntegrationService.getBalance(accountNumber, phoneNumber).getAccountBalance();
        String message = "Your current balance is: " + balance + " . Type \"0\" to return to the main menu";
        twilioService.sendSms(phoneNumber, message);
        return message;
    }

    private String handleBuyAirtime(String phoneNumber, String action) {
        String[] details = action.split(",");
        if (details.length != 2) {
            String message = "Invalid format. Please enter the phone number and amount separated by a comma. Type \"0\" to go back to main menu or type 9 to cancel.";
            twilioService.sendSms(phoneNumber, message);
            return message;
        }

//        phoneNumber = details[0];
        BigDecimal amount = new BigDecimal(details[0]);
        String pin = details[1];

        UserEntity user = userRepository.findByPhoneNumber(phoneNumber);
        if (user == null || !passwordEncoder.matches(pin, user.getPin())) {
            String message = "Invalid PIN. Airtime purchase cancelled. Type \"0\" to go back to main menu.";
            twilioService.sendSms(phoneNumber, message);
            return message;
        }

        boolean success = bankIntegrationService.buyAirtimeForSelf(amount, pin);
        if (success) {
            userStates.put(phoneNumber, "AWAITING_MENU");
            String message = "Airtime purchased successfully for " + phoneNumber + ". Type \"0\"  to return to the main menu.";
            twilioService.sendSms(phoneNumber, message);
            return message;
        } else {
            String message = "Airtime purchase failed. Please try again. Type \"0\" to go back to main menu.";
            twilioService.sendSms(phoneNumber, message);
            return message;
        }
    }

    private String handleTransfer(String phoneNumber, String action) {
        String[] details = action.split(",");
        if (details.length != 2) {
            String message = "Invalid format. Please enter the destination account number and amount separated by a comma. Type \"0\" to go back to main menu or type 9 to cancel";
            twilioService.sendSms(phoneNumber, message);
            return message;
        }

        String destinationAccountNumber = details[0];
        BigDecimal amount = new BigDecimal(details[1]);

        NameAccountResponse destinationAccountDetails = bankIntegrationService.getNameDetails(destinationAccountNumber);
        if (destinationAccountDetails == null || destinationAccountDetails.getAccountNumber() == null) {
            String message = "Invalid destination account number. Please try again.";
            twilioService.sendSms(phoneNumber, message);
            return message;
        }

        transferDetails.put(phoneNumber, destinationAccountNumber + "," + amount);
        userStates.put(phoneNumber, "TRANSFER_CONFIRM");
        String message = "You are about to transfer " + amount + " to account " + destinationAccountDetails.getFirstName() + " " + destinationAccountDetails.getLastName() + ". Please confirm by typing your PIN or type 9 to cancel.";
        twilioService.sendSms(phoneNumber, message);
        return message;
    }

    private String confirmTransfer(String phoneNumber, String pin) {

        UserEntity user = userRepository.findByPhoneNumber(phoneNumber);
        if (user == null || !passwordEncoder.matches(pin, user.getPin())) {
            userStates.put(phoneNumber, "AWAITING_MENU");
            String message = "Invalid PIN. Transfer cancelled. Please select an option: 1. Transfer to REF's Bank 2. Transfer to Other Banks 3. Check Balance 4. Buy Airtime or Data";
            twilioService.sendSms(phoneNumber, message);
            return message;
        }


        String[] details = transferDetails.get(phoneNumber).split(",");
        String sourceAccountNumber = bankIntegrationService.getAccountDetails(phoneNumber).getAccountNumber();
        String destinationAccountNumber = details[0];
        BigDecimal amount = new BigDecimal(details[1]);

        boolean success = bankIntegrationService.transferFunds(sourceAccountNumber, destinationAccountNumber, amount);
        if (success) {
            transferDetails.remove(phoneNumber);
            userStates.put(phoneNumber, "AWAITING_MENU");
            String message = "Transfer successful. Type \"0\" to return to the main menu.";
            twilioService.sendSms(phoneNumber, message);
            return message;
        } else {
            String message ="Transfer failed. Please try again. Type \"0\" to go back to main menu.";
            twilioService.sendSms(phoneNumber, message);
            return message;
        }
    }

    private String handleTransferToOtherBank(String phoneNumber, String action) {
        String[] details = action.split(",");
        if (details.length != 3) {
            String message = "Invalid format. Please enter the destination bank name, account number, and amount separated by commas. Type \"0\" to go back to main menu or type 9 to cancel";
            twilioService.sendSms(phoneNumber, message);
            return message;
        }

        String bankName = details[0];
        String destinationAccountNumber = details[1];
        BigDecimal amount = new BigDecimal(details[2]);

        if (!bankService.isValidBankName(bankName)) {
            return "Invalid bank name. Please try again.";
        }

        String recipientName = paystackService.getRecipientName(destinationAccountNumber, bankName);
        if (recipientName == null) {
            return "Invalid destination account number. Please try again.";
        }

        transferDetails.put(phoneNumber, bankName + "," + destinationAccountNumber + "," + amount);
        userStates.put(phoneNumber, "TRANSFER_TO_OTHER_BANK_CONFIRM");

        return "You are about to transfer " + amount + " to " + recipientName + " (" + bankName + "). Please confirm by typing your PIN or type 9 to cancel.";
    }

    private String confirmTransferToOtherBank(String phoneNumber, String pin) {
        UserEntity user = userRepository.findByPhoneNumber(phoneNumber);
        if (user == null || !passwordEncoder.matches(pin, user.getPin())) {
            userStates.put(phoneNumber, "AWAITING_MENU");
            return "Invalid PIN. Transfer cancelled. Please select an option: 1. Transfer 2. Check Balance 3. Buy Airtime or Data 4. Transfer to Other Bank";
        }

        String[] details = transferDetails.get(phoneNumber).split(",");
        String bankName = details[0];
        String destinationAccountNumber = details[1];
        BigDecimal amount = new BigDecimal(details[2]);

        String recipientCode = paystackService.createTransferRecipient(destinationAccountNumber, bankName);
        String result = paystackService.initiateTransfer(recipientCode, amount.intValue());

        transferDetails.remove(phoneNumber);
        userStates.put(phoneNumber, "AWAITING_MENU");

        return result + " Type \"0\" to return to the main menu.";
    }

    private String handleCancelCommand(String phoneNumber) {
        String state = userStates.getOrDefault(phoneNumber, "NONE");
        switch (state) {
            case "TRANSFER_INITIATED":
                transferDetails.remove(phoneNumber);
                userStates.put(phoneNumber, "MENU_TEXT");
                return "Transfer initiation cancelled. Type \"0\" to return to the main menu";

            case "TRANSFER_CONFIRM":
                transferDetails.remove(phoneNumber);
                userStates.put(phoneNumber, "MENU_TEXT");
                return "Transfer confirmation cancelled. Type \"0\" to return to the main menu";

            case "BUY_AIRTIME_INITIATED":
                transferDetails.remove(phoneNumber);
                userStates.put(phoneNumber, "AWAITING_MENU");
                return "Airtime purchase cancelled. Type \"0\" to return to the main menu";

            case "BUY_DATA_INITIATED":
                transferDetails.remove(phoneNumber);
                userStates.put(phoneNumber, "AWAITING_MENU");
                return "Data purchase cancelled. Type \"0\" to return to the main menu";

            case "TRANSFER_TO_OTHER_BANK_INITIATED":
                transferDetails.remove(phoneNumber);
                userStates.put(phoneNumber, "AWAITING_MENU");
                return "Transfer to other bank initiation cancelled. Type \"0\" to return to the main menu";

            case "TRANSFER_TO_OTHER_BANK_CONFIRM":
                transferDetails.remove(phoneNumber);
                userStates.put(phoneNumber, "AWAITING_MENU");
                return "Transfer to other bank confirmation cancelled. Type \"0\" to return to the main menu";

            default:
                return "There is no ongoing transaction to cancel. Type \"0\" to return to the main menu";
        }
    }


}
