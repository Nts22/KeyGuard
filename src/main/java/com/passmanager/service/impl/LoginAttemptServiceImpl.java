package com.passmanager.service.impl;

import com.passmanager.service.LoginAttemptService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptServiceImpl implements LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long BLOCK_DURATION_MINUTES = 15;

    private final Map<String, Integer> attemptsCache = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> blockCache = new ConcurrentHashMap<>();

    @Override
    public void loginSucceeded(String username) {
        attemptsCache.remove(username);
        blockCache.remove(username);
    }

    @Override
    public void loginFailed(String username) {
        int attempts = attemptsCache.getOrDefault(username, 0) + 1;
        attemptsCache.put(username, attempts);

        if (attempts >= MAX_ATTEMPTS) {
            blockCache.put(username, LocalDateTime.now().plusMinutes(BLOCK_DURATION_MINUTES));
        }
    }

    @Override
    public boolean isBlocked(String username) {
        LocalDateTime blockTime = blockCache.get(username);
        if (blockTime == null) {
            return false;
        }

        if (LocalDateTime.now().isAfter(blockTime)) {
            blockCache.remove(username);
            attemptsCache.remove(username);
            return false;
        }

        return true;
    }

    @Override
    public int getRemainingAttempts(String username) {
        int attempts = attemptsCache.getOrDefault(username, 0);
        return Math.max(0, MAX_ATTEMPTS - attempts);
    }

    @Override
    public long getBlockTimeRemainingSeconds(String username) {
        LocalDateTime blockTime = blockCache.get(username);
        if (blockTime == null) {
            return 0;
        }

        long remaining = ChronoUnit.SECONDS.between(LocalDateTime.now(), blockTime);
        return Math.max(0, remaining);
    }

}
