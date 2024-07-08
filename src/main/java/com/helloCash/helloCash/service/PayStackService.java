package com.helloCash.helloCash.service;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class PayStackService {

    @Autowired
    private BankService bankService;
    @Value("${paystack.api.url}")
    private String paystackBaseUrl;
    @Value("${paystack.secret-key}")
    private String secretKey;

    public String getRecipientName(String accountNumber, String bankName) {
        String bankCode = bankService.getBankCode(bankName);

        if (bankCode == null) {
            throw new IllegalArgumentException("Invalid bank name: " + bankName);
        }

        RestTemplate restTemplate = new RestTemplate();
        String url = paystackBaseUrl + "/bank/resolve?account_number=" + accountNumber + "&bank_code=" + bankCode;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", secretKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            JSONObject jsonResponse = new JSONObject(response.getBody());
            if (jsonResponse.getBoolean("status")) {
                return jsonResponse.getJSONObject("data").getString("account_name");
            } else {
                throw new RuntimeException("Failed to resolve account name: " + jsonResponse.getString("message"));
            }
        } else {
            throw new RuntimeException("Failed to connect to Paystack API");
        }
    }

    public String createTransferRecipient(String accountNumber, String bankName) {
        String bankCode = bankService.getBankCode(bankName);

        if (bankCode == null) {
            throw new IllegalArgumentException("Invalid bank name: " + bankName);
        }

        RestTemplate restTemplate = new RestTemplate();
        String url = paystackBaseUrl + "/transferrecipient";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", secretKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        JSONObject requestBody = new JSONObject();
        requestBody.put("type", "nuban");
        requestBody.put("name", getRecipientName(accountNumber, bankName));
        requestBody.put("account_number", accountNumber);
        requestBody.put("bank_code", bankCode);
        requestBody.put("currency", "NGN");

        HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            JSONObject jsonResponse = new JSONObject(response.getBody());
            if (jsonResponse.getBoolean("status")) {
                return jsonResponse.getJSONObject("data").getString("recipient_code");
            } else {
                throw new RuntimeException("Failed to create transfer recipient: " + jsonResponse.getString("message"));
            }
        } else {
            throw new RuntimeException("Failed to connect to Paystack API");
        }
    }

    public String initiateTransfer(String recipientCode, int amount) {
        RestTemplate restTemplate = new RestTemplate();
        String url = paystackBaseUrl + "/transfer";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", secretKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        JSONObject requestBody = new JSONObject();
        requestBody.put("source", "balance");
        requestBody.put("amount", amount * 100); // Paystack amount is in kobo
        requestBody.put("recipient", recipientCode);

        HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            JSONObject jsonResponse = new JSONObject(response.getBody());
            if (jsonResponse.getBoolean("status")) {
                return "Transfer successful.";
            } else {
                throw new RuntimeException("Transfer failed: " + jsonResponse.getString("message"));
            }
        } else {
            throw new RuntimeException("Failed to connect to Paystack API");
        }
    }
}
