package com.helloCash.helloCash.payload.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SmsRequest {
    private String phoneNumber;
    private String message;
}

