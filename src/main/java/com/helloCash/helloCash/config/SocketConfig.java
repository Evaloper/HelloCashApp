package com.helloCash.helloCash.config;

import com.helloCash.helloCash.service.SocketService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
@AllArgsConstructor
@Component
public class SocketConfig implements ApplicationListener<ContextRefreshedEvent> {
    private final SocketService socketService;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        new Thread(socketService).start();
    }
}
