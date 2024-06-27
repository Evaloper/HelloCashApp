package com.helloCash.helloCash.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TwilioService {
    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.phone.number}")
    private String fromPhoneNumber;

    public void sendSms(String toPhoneNumber, String message) {
        try {
            Twilio.init(accountSid, authToken);
            Message sentMessage = Message.creator(new PhoneNumber(toPhoneNumber), new PhoneNumber(fromPhoneNumber), message).create();
            System.out.println("SMS sent successfully: " + sentMessage.getSid());
        } catch (Exception e) {
            System.err.println("Error sending SMS: " + e.getMessage());
        }
    }
}
