package com.helloCash.helloCash.model;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SmsResponse {
    private String phoneNumber;
    private String message;
}
