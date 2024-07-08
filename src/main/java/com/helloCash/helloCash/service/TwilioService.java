package com.helloCash.helloCash.service;

import com.helloCash.helloCash.config.TwilioConfig;
import com.helloCash.helloCash.payload.request.SmsRequest;
import com.helloCash.helloCash.repository.SmsRequestRepository;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.AllArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TwilioService {
    private final TwilioConfig twilioConfig;
    private final SmsRequestRepository smsRequestRepository;

    public void sendSms(String toPhoneNumber, String message) {
        try {
            Twilio.init(twilioConfig.getAccountSid(), twilioConfig.getAuthToken());
            Message sentMessage = Message.creator(new PhoneNumber(toPhoneNumber), new PhoneNumber(twilioConfig.getFromPhoneNumber()), message).create();
            System.out.println("SMS sent successfully: " + sentMessage.getSid());
        } catch (Exception e) {
            System.err.println("Error sending SMS: " + e.getMessage());
        }
    }

    public void receiveSms(String fromPhoneNumber, String messageBody, SocketService socketService) {
        try {
            // Log the received message
            System.out.println("Received SMS from: " + fromPhoneNumber);
            System.out.println("Message: " + messageBody);

            // Save the message to the database
            SmsRequest smsMessage = new SmsRequest(fromPhoneNumber, messageBody);
            smsRequestRepository.save(smsMessage);

            // Broadcast the message to connected clients
            socketService.broadcastMessage("Received SMS from: " + fromPhoneNumber + ", Message: " + messageBody);
        } catch (Exception e) {
            System.err.println("Error processing received SMS: " + e.getMessage());
        }
    }
}
