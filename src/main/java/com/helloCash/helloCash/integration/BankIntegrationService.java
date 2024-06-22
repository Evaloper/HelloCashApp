package com.helloCash.helloCash.integration;

//import com.helloCash.helloCash.model.EnquiryRequest;
//import com.helloCash.helloCash.model.NameAccountResponse;
import com.helloCash.helloCash.model.EnquiryRequest;
import com.helloCash.helloCash.model.NameAccountResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class BankIntegrationService {
    private final RestTemplate restTemplate;

    public BankIntegrationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean validatePhoneNumberWithBank(String phoneNumber) {
        String url = "http://localhost:3246/api/v1/user/check-phone?phoneNumber=" + phoneNumber;
        try {
            String response = restTemplate.getForObject(url, String.class);
            System.out.println("Bank API response: " + response); // Print the response for debugging
            return Boolean.parseBoolean(response.trim());
        } catch (Exception e) {
            System.err.println("Error validating phone number with bank: " + e.getMessage());
            return false;
        }
    }

    public NameAccountResponse getAccountDetails(String phoneNumber) {
        String url = "http://localhost:3246/api/v1/user/name-account-enquiry/" + phoneNumber;
        try {
            NameAccountResponse response = restTemplate.getForObject(url, NameAccountResponse.class);
            System.out.println("Account Details API response: " + response); // Print the response for debugging
            return response;
        } catch (Exception e) {
            System.err.println("Error fetching account details: " + e.getMessage());
            e.printStackTrace(); // Add stack trace for more detailed debugging
            return new NameAccountResponse("Error", "Unable to fetch account details.", null, null, null);
        }
    }
}
