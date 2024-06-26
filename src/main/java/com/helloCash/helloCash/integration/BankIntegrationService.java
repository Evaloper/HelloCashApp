package com.helloCash.helloCash.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helloCash.helloCash.payload.response.NameAccountResponse;
import com.helloCash.helloCash.payload.response.AccountInfo;
//import com.helloCash.helloCash.service.TwilioService;
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

//    private final TwilioService smsSenderService;


    public boolean validatePhoneNumberWithBank(String phoneNumber) {
        String url = "http://localhost:3246/api/v1/user/check-phone?phoneNumber=" + phoneNumber;

        HttpHeaders headers = new HttpHeaders();
        headers.set("API-Key", apiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            System.out.println("Bank API response: " + response.getBody()); // Print the response for debugging
            boolean isValid = Boolean.parseBoolean(response.getBody().trim());
//            smsSenderService.sendSms(phoneNumber, "Phone validation: " + (isValid ? "Valid" : "Invalid"));
            return isValid;
        } catch (Exception e) {
            System.err.println("Error validating phone number with bank: " + e.getMessage());
            return false;
        }
    }

    public NameAccountResponse getAccountDetails(String phoneNumber) {
        String url = "http://localhost:3246/api/v1/user/name-account-enquiry/" + phoneNumber;

        HttpHeaders headers = new HttpHeaders();
        headers.set("API-Key", apiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<NameAccountResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity, NameAccountResponse.class);
            System.out.println("Account Details API response: " + response.getBody()); // Print the response for debugging
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Error fetching account details: " + e.getMessage());
            e.printStackTrace(); // Add stack trace for more detailed debugging
            return new NameAccountResponse("Error", "Unable to fetch account details.", null, null, null, null);
        }
    }


    public AccountInfo getBalance(String accountNumber) {
        String url = "http://localhost:3246/api/v1/user/balance-enquiry?accountNumber=" + accountNumber;

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

            return new AccountInfo(accountName, accountBalance, accountNum);
        } catch (Exception e) {
            System.err.println("Error fetching balance: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }


}
