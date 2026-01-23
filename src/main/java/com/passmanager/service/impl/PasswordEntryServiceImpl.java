package com.passmanager.service.impl;

import com.passmanager.exception.ResourceNotFoundException;
import com.passmanager.mapper.PasswordEntryMapper;
import com.passmanager.model.dto.PasswordEntryDTO;
import com.passmanager.model.entity.PasswordEntry;
import com.passmanager.model.entity.User;
import com.passmanager.repository.PasswordEntryRepository;
import com.passmanager.service.PasswordEntryService;
import com.passmanager.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PasswordEntryServiceImpl implements PasswordEntryService {

    private final PasswordEntryRepository passwordEntryRepository;
    private final PasswordEntryMapper passwordEntryMapper;
    private final UserService userService;

    public PasswordEntryServiceImpl(PasswordEntryRepository passwordEntryRepository,
                                    PasswordEntryMapper passwordEntryMapper,
                                    UserService userService) {
        this.passwordEntryRepository = passwordEntryRepository;
        this.passwordEntryMapper = passwordEntryMapper;
        this.userService = userService;
    }

    private User getCurrentUser() {
        return userService.getCurrentUser();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PasswordEntryDTO> findAll() {
        return passwordEntryRepository.findByUserOrderByTitleAsc(getCurrentUser()).stream()
                .map(passwordEntryMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PasswordEntryDTO> findByCategory(Long categoryId) {
        return passwordEntryRepository.findByUserAndCategoryId(getCurrentUser(), categoryId).stream()
                .map(passwordEntryMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PasswordEntryDTO> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return findAll();
        }
        return passwordEntryRepository.searchByUser(getCurrentUser(), query.trim()).stream()
                .map(passwordEntryMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PasswordEntryDTO> searchByCategory(Long categoryId, String query) {
        if (query == null || query.trim().isEmpty()) {
            return findByCategory(categoryId);
        }
        return passwordEntryRepository.searchByUserAndCategory(getCurrentUser(), categoryId, query.trim()).stream()
                .map(passwordEntryMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PasswordEntryDTO> findById(Long id) {
        return passwordEntryRepository.findByIdAndUserWithCustomFields(id, getCurrentUser())
                .map(entry -> passwordEntryMapper.toDTO(entry, true));
    }

    @Override
    @Transactional
    public PasswordEntryDTO create(PasswordEntryDTO dto) {
        PasswordEntry entry = new PasswordEntry();
        entry.setUser(getCurrentUser());
        passwordEntryMapper.updateEntityFromDTO(entry, dto);
        return passwordEntryMapper.toDTO(passwordEntryRepository.save(entry));
    }

    @Override
    @Transactional
    public PasswordEntryDTO update(Long id, PasswordEntryDTO dto) {
        PasswordEntry entry = passwordEntryRepository.findByIdAndUser(id, getCurrentUser())
                .orElseThrow(() -> new ResourceNotFoundException("PasswordEntry", id));

        entry.getCustomFields().clear();
        passwordEntryMapper.updateEntityFromDTO(entry, dto);

        return passwordEntryMapper.toDTO(passwordEntryRepository.save(entry));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        passwordEntryRepository.deleteByIdAndUser(id, getCurrentUser());
    }
}
