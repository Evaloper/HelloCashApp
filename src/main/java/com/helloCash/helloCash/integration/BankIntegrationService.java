package com.helloCash.helloCash.integration;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class BankIntegrationService {
    private final RestTemplate restTemplate;

    public BankIntegrationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean validatePhoneNumberWithBank(String phoneNumber) {
        // Mock implementation - replace with actual API call
        String url = "http://localhost:8080/api/v1/user/check-phone?phoneNumber=" + phoneNumber;
        try {
            String response = restTemplate.getForObject(url, String.class);
            System.out.println("Bank API response: " + response); // Print the response for debugging
            // Parse the response if needed. Assuming the API returns a simple boolean in plain text for now.
            return Boolean.parseBoolean(response.trim());
        } catch (Exception e) {
            System.err.println("Error validating phone number with bank: " + e.getMessage());
            return false;
        }
    }
}
