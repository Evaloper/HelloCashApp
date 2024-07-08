package com.helloCash.helloCash.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class BankService {

    private static final Map<String, String> BANK_CODES = new HashMap<>();

    static {
        BANK_CODES.put("ACCESS BANK", "044");
        BANK_CODES.put("CITIBANK", "023");
        BANK_CODES.put("DIAMOND BANK", "063");
        BANK_CODES.put("ECOBANK", "050");
        BANK_CODES.put("FIDELITY BANK", "070");
        BANK_CODES.put("FIRST BANK", "011");
        BANK_CODES.put("FCMB", "214");
        BANK_CODES.put("GTBANK", "058");
        BANK_CODES.put("HERITAGE BANK", "030");
        BANK_CODES.put("KEYSTONE BANK", "082");
    }

    public String getBankCode(String bankName) {
        return BANK_CODES.get(bankName.toUpperCase());
    }

    public boolean isValidBankName(String bankName) {
        return BANK_CODES.containsKey(bankName.toUpperCase());
    }

    public Map<String, String> getBankNames() {
        return BANK_CODES;
    }
}
