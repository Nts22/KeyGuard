package com.passmanager.service;

import com.passmanager.model.dto.UserDTO;
import com.passmanager.model.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

    List<UserDTO> findAll();

    Optional<User> findById(Long id);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    User getCurrentUser();

    void setCurrentUser(User user);

    void clearCurrentUser();

    boolean hasCurrentUser();
}
