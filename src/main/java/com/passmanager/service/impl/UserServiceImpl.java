package com.passmanager.service.impl;

import com.passmanager.model.dto.UserDTO;
import com.passmanager.model.entity.User;
import com.passmanager.repository.UserRepository;
import com.passmanager.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private User currentUser;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<UserDTO> findAll() {
        return userRepository.findAllByOrderByUsernameAsc().stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public User getCurrentUser() {
        if (currentUser == null) {
            throw new IllegalStateException("No hay usuario autenticado");
        }
        return currentUser;
    }

    @Override
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    @Override
    public void clearCurrentUser() {
        this.currentUser = null;
    }

    @Override
    public boolean hasCurrentUser() {
        return currentUser != null;
    }

    private UserDTO toDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
