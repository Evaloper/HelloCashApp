package com.helloCash.helloCash.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
@RequiredArgsConstructor
@Configuration
public class PayStackConfig {
    @Value("${paystack.secret-key}")
    private String paystackSecretKey;

    private final RestTemplateConfig restTemplate;
    @Bean
    public HttpHeaders httpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + paystackSecretKey);
        return headers;
    }
}
