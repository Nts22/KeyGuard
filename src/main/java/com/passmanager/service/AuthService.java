package com.passmanager.service;

import com.passmanager.model.dto.UserDTO;
import com.passmanager.model.entity.User;

import java.util.List;

public interface AuthService {

    boolean hasUsers();

    List<UserDTO> getAllUsers();

    User createUser(String username, String password);

    boolean authenticate(String username, String password);

    void logout();

    boolean isAuthenticated();

    User getCurrentUser();

    boolean isLoginBlocked(String username);

    int getRemainingLoginAttempts(String username);

    long getBlockTimeRemainingSeconds(String username);
}
