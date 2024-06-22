package com.helloCash.helloCash.model;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NameAccountResponse {
    private String responseCode;
    private String responseMessage;
    private String firstName;
    private String lastName;
    private String accountNumber;
}