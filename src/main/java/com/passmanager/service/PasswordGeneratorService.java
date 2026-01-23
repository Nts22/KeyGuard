package com.passmanager.service;

public interface PasswordGeneratorService {

    String generate(int length, boolean includeUppercase, boolean includeDigits, boolean includeSymbols);

    String generateDefault();

    int calculateStrength(String password);
}
