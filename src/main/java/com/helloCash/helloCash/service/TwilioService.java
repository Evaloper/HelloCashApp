package com.helloCash.helloCash.service;

import com.helloCash.helloCash.config.TwilioConfig;
import com.helloCash.helloCash.payload.request.SmsRequest;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TwilioService {
    private final TwilioConfig twilioConfig;

    public void sendSms(String toPhoneNumber, String message) {
        try {
            Twilio.init(twilioConfig.getAccountSid(), twilioConfig.getAuthToken());
            Message sentMessage = Message.creator(new PhoneNumber(toPhoneNumber), new PhoneNumber(twilioConfig.getFromPhoneNumber()), message).create();
            System.out.println("SMS sent successfully: " + sentMessage.getSid());
        } catch (Exception e) {
            System.err.println("Error sending SMS: " + e.getMessage());
        }
    }
}
