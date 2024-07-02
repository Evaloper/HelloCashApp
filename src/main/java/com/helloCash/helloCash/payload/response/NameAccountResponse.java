package com.helloCash.helloCash.payload.response;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NameAccountResponse {
    private String responseCode;
    private String responseMessage;
    private String firstName;
    private String lastName;
    private String otherName;
    private String accountNumber;
    private BigDecimal accountBalance;
}