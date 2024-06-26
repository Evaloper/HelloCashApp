package com.helloCash.helloCash.payload.response;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SmsResponse {
    private String phoneNumber;
    private String message;
}
