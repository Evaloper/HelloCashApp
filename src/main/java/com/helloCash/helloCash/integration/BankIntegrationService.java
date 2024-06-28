package com.helloCash.helloCash.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helloCash.helloCash.payload.response.NameAccountResponse;
import com.helloCash.helloCash.payload.response.AccountInfo;
//import com.helloCash.helloCash.service.TwilioService;
import com.helloCash.helloCash.service.TwilioService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class BankIntegrationService {

    @Value("${api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    private final TwilioService twilioService;

    private final String minibankUrl = "http://localhost:3246/api/v1/user";

    public boolean validatePhoneNumberWithBank(String phoneNumber) {
        String url = minibankUrl + "/check-phone?phoneNumber=" + phoneNumber;

        HttpHeaders headers = new HttpHeaders();
        headers.set("API-Key", apiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            System.out.println("Bank API response: " + response.getBody()); // Print the response for debugging
            boolean isValid = Boolean.parseBoolean(response.getBody().trim());
            return isValid;
        } catch (Exception e) {
            System.err.println("Error validating phone number with bank: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public NameAccountResponse getAccountDetails(String phoneNumber) {
        String url = minibankUrl + "/name-account-enquiry/" + phoneNumber;

        HttpHeaders headers = new HttpHeaders();
        headers.set("API-Key", apiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<NameAccountResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity, NameAccountResponse.class);
            System.out.println("Account Details API response: " + response.getBody()); // Print the response for debugging

            NameAccountResponse accountDetails = response.getBody();
            String message = "Welcome " + accountDetails.getFirstName() + " " + accountDetails.getLastName() +
                    ". Your HelloCash app has been activated successfully. Here is your REFS account number " +
                    accountDetails.getAccountNumber() + " \n Please select an option: \n1. Transfer \n2. Check balance \n3. Buy airtime or data";
//            twilioService.sendSms(phoneNumber, message);

            return accountDetails;
        } catch (Exception e) {
            System.err.println("Error fetching account details: " + e.getMessage());
            e.printStackTrace(); // Add stack trace for more detailed debugging

            String errorMessage = "Error fetching account details. Please try again later.";
            return new NameAccountResponse("Error", "Unable to fetch account details.", null, null, null, null);
        }
    }

    public AccountInfo getBalance(String accountNumber, String phoneNumber) {
        String url = minibankUrl + "/balance-enquiry?accountNumber=" + accountNumber;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("API-Key", "helloCash-api-key");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            System.out.println("Balance API response: " + response.getBody());

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response.getBody());

            JsonNode accountInfoNode = rootNode.path("accountInfo");
            String accountName = accountInfoNode.path("accountName").asText();
            BigDecimal accountBalance = accountInfoNode.path("accountBalance").decimalValue();
            String accountNum = accountInfoNode.path("accountNumber").asText();

            String balanceMessage = "Hello " + accountName + ", your account balance is: " + accountBalance.toString();
//            twilioService.sendSms(phoneNumber, balanceMessage);

            return new AccountInfo(accountName, accountBalance, accountNum);
        } catch (Exception e) {
            System.err.println("Error fetching balance: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public boolean transferFunds(String sourceAccountNumber, String destinationAccountNumber, BigDecimal amount) {
        String url = minibankUrl + "/transfer";

        HttpHeaders headers = new HttpHeaders();
        headers.set("API-Key", apiKey);
        headers.set("Content-Type", "application/json");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("sourceAccountNumber", sourceAccountNumber);
        requestBody.put("destinationAccountNumber", destinationAccountNumber);
        requestBody.put("amount", amount);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            // Log the request body
            System.out.println("Transfer Request Body: " + requestBody.toString());

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            // Log the response
            System.out.println("Transfer API response: " + response.getBody());

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            System.err.println("Error transferring funds: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public NameAccountResponse getNameDetails(String accountNumber) {
        String url = minibankUrl + "/name-enquiry?accountNumber=" + accountNumber;

        HttpHeaders headers = new HttpHeaders();
        headers.set("API-Key", apiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<NameAccountResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity, NameAccountResponse.class);
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Error fetching name details: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }


}
