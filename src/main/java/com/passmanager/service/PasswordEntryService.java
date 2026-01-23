package com.passmanager.service;

import com.passmanager.model.dto.PasswordEntryDTO;

import java.util.List;
import java.util.Optional;

public interface PasswordEntryService {

    List<PasswordEntryDTO> findAll();

    List<PasswordEntryDTO> findByCategory(Long categoryId);

    List<PasswordEntryDTO> search(String query);

    List<PasswordEntryDTO> searchByCategory(Long categoryId, String query);

    Optional<PasswordEntryDTO> findById(Long id);

    PasswordEntryDTO create(PasswordEntryDTO dto);

    PasswordEntryDTO update(Long id, PasswordEntryDTO dto);

    void delete(Long id);
}
