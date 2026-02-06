package com.passmanager.service.impl;

import com.passmanager.exception.ResourceNotFoundException;
import com.passmanager.mapper.PasswordEntryMapper;
import com.passmanager.model.dto.PasswordEntryDTO;
import com.passmanager.model.entity.PasswordEntry;
import com.passmanager.model.entity.Tag;
import com.passmanager.model.entity.User;
import com.passmanager.repository.PasswordEntryRepository;
import com.passmanager.repository.TagRepository;
import com.passmanager.service.EncryptionService;
import com.passmanager.service.PasswordEntryService;
import com.passmanager.service.PasswordHistoryService;
import com.passmanager.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PasswordEntryServiceImpl implements PasswordEntryService {

    private final PasswordEntryRepository passwordEntryRepository;
    private final PasswordEntryMapper passwordEntryMapper;
    private final UserService userService;
    private final PasswordHistoryService passwordHistoryService;
    private final EncryptionService encryptionService;
    private final TagRepository tagRepository;
    private final com.passmanager.service.AuditLogService auditLogService;

    public PasswordEntryServiceImpl(PasswordEntryRepository passwordEntryRepository,
                                    PasswordEntryMapper passwordEntryMapper,
                                    UserService userService,
                                    PasswordHistoryService passwordHistoryService,
                                    EncryptionService encryptionService,
                                    TagRepository tagRepository,
                                    com.passmanager.service.AuditLogService auditLogService) {
        this.passwordEntryRepository = passwordEntryRepository;
        this.passwordEntryMapper = passwordEntryMapper;
        this.userService = userService;
        this.passwordHistoryService = passwordHistoryService;
        this.encryptionService = encryptionService;
        this.tagRepository = tagRepository;
        this.auditLogService = auditLogService;
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
        PasswordEntry saved = passwordEntryRepository.save(entry);

        // Registrar creación
        auditLogService.log(getCurrentUser(),
                com.passmanager.model.entity.AuditLog.ActionType.CREATE_ENTRY,
                "Creada contraseña: " + dto.getTitle(),
                saved.getId(),
                com.passmanager.model.entity.AuditLog.ResultType.SUCCESS);

        return passwordEntryMapper.toDTO(saved);
    }

    @Override
    @Transactional
    public PasswordEntryDTO update(Long id, PasswordEntryDTO dto) {
        PasswordEntry entry = passwordEntryRepository.findByIdAndUser(id, getCurrentUser())
                .orElseThrow(() -> new ResourceNotFoundException("PasswordEntry", id));

        // Guardar contraseña antigua en historial si cambió
        String oldPassword = encryptionService.decrypt(entry.getPassword());
        String newPassword = dto.getPassword();

        if (!oldPassword.equals(newPassword)) {
            passwordHistoryService.savePasswordHistory(entry, oldPassword);
        }

        entry.getCustomFields().clear();
        passwordEntryMapper.updateEntityFromDTO(entry, dto);

        PasswordEntry updated = passwordEntryRepository.save(entry);

        // Registrar actualización
        auditLogService.log(getCurrentUser(),
                com.passmanager.model.entity.AuditLog.ActionType.UPDATE_ENTRY,
                "Actualizada contraseña: " + dto.getTitle(),
                id,
                com.passmanager.model.entity.AuditLog.ResultType.SUCCESS);

        return passwordEntryMapper.toDTO(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        // Obtener la entrada antes de eliminarla para el log
        PasswordEntry entry = passwordEntryRepository.findByIdAndUser(id, getCurrentUser())
                .orElseThrow(() -> new ResourceNotFoundException("PasswordEntry", id));

        String title = entry.getTitle();
        passwordEntryRepository.deleteByIdAndUser(id, getCurrentUser());

        // Registrar eliminación
        auditLogService.log(getCurrentUser(),
                com.passmanager.model.entity.AuditLog.ActionType.DELETE_ENTRY,
                "Eliminada contraseña: " + title,
                id,
                com.passmanager.model.entity.AuditLog.ResultType.SUCCESS);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PasswordEntryDTO> findFavorites() {
        return passwordEntryRepository.findByUserAndFavoriteTrueOrderByTitleAsc(getCurrentUser()).stream()
                .map(passwordEntryMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public void toggleFavorite(Long id) {
        PasswordEntry entry = passwordEntryRepository.findByIdAndUser(id, getCurrentUser())
                .orElseThrow(() -> new ResourceNotFoundException("PasswordEntry", id));

        entry.setFavorite(!Boolean.TRUE.equals(entry.getFavorite()));
        passwordEntryRepository.save(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PasswordEntryDTO> findByTag(Long tagId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", tagId));

        return passwordEntryRepository.findByUserAndTag(getCurrentUser(), tag).stream()
                .map(passwordEntryMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public void addTagToEntry(Long entryId, Long tagId) {
        PasswordEntry entry = passwordEntryRepository.findByIdAndUser(entryId, getCurrentUser())
                .orElseThrow(() -> new ResourceNotFoundException("PasswordEntry", entryId));

        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", tagId));

        if (!entry.getTags().contains(tag)) {
            entry.getTags().add(tag);
            passwordEntryRepository.save(entry);
        }
    }

    @Override
    @Transactional
    public void removeTagFromEntry(Long entryId, Long tagId) {
        PasswordEntry entry = passwordEntryRepository.findByIdAndUser(entryId, getCurrentUser())
                .orElseThrow(() -> new ResourceNotFoundException("PasswordEntry", entryId));

        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", tagId));

        entry.getTags().remove(tag);
        passwordEntryRepository.save(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PasswordEntryDTO> findOldPasswords(int daysThreshold) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(daysThreshold);
        return passwordEntryRepository.findByUserAndPasswordLastChangedBefore(getCurrentUser(), threshold).stream()
                .map(passwordEntryMapper::toDTO)
                .toList();
    }
}
