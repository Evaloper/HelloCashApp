package com.helloCash.helloCash.controller;



import com.helloCash.helloCash.service.TwilioService;
import com.helloCash.helloCash.service.SocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TwilioController {

    @Autowired
    private TwilioService twilioService;

    @Autowired
    private SocketService socketService;

    @PostMapping("/sms")
    public String receiveSms(@RequestParam("From") String from, @RequestParam("Body") String body) {
        twilioService.receiveSms(from, body, socketService);
        socketService.broadcastMessage("Received SMS from: " + from + ", Message: " + body);
        return "Message received";
    }
}
