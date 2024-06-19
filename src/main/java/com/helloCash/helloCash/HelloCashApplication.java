package com.helloCash.helloCash;

import com.helloCash.helloCash.service.SocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HelloCashApplication {

	public static void main(String[] args) {
		SpringApplication.run(HelloCashApplication.class, args);
	}

}