package com.helloCash.helloCash.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SmsRequest {
    private String phoneNumber;
    private String message;
}

