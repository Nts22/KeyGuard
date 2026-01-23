package com.passmanager.service;

public interface LoginAttemptService {

    void loginSucceeded(String username);

    void loginFailed(String username);

    boolean isBlocked(String username);

    int getRemainingAttempts(String username);

    long getBlockTimeRemainingSeconds(String username);

    int getMaxAttempts();

    long getBlockDurationMinutes();
}
