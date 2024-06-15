package com.helloCash.helloCash;

import com.helloCash.helloCash.service.SocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HelloCashApplication implements CommandLineRunner {

	private final SocketService socketService;

	@Autowired
	public HelloCashApplication(SocketService socketService) {
		this.socketService = socketService;
	}

	public static void main(String[] args) {
		SpringApplication.run(HelloCashApplication.class, args);
	}

	@Override
	public void run(String... args) {
		new Thread(socketService).start();
	}
}