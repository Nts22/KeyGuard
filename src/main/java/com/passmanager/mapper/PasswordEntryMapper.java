package com.passmanager.mapper;

import com.passmanager.model.dto.PasswordEntryDTO;
import com.passmanager.model.entity.Category;
import com.passmanager.model.entity.CustomField;
import com.passmanager.model.entity.PasswordEntry;
import com.passmanager.repository.CategoryRepository;
import com.passmanager.service.EncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PasswordEntryMapper {

    private static final Logger log = LoggerFactory.getLogger(PasswordEntryMapper.class);
    private static final String DECRYPTION_ERROR = "***ERROR***";

    private final EncryptionService encryptionService;
    private final CategoryRepository categoryRepository;

    public PasswordEntryMapper(EncryptionService encryptionService,
                               CategoryRepository categoryRepository) {
        this.encryptionService = encryptionService;
        this.categoryRepository = categoryRepository;
    }

    public PasswordEntryDTO toDTO(PasswordEntry entry) {
        return toDTO(entry, false);
    }

    public PasswordEntryDTO toDTO(PasswordEntry entry, boolean includeCustomFields) {
        String decryptedPassword = decryptAndVerify(entry);

        List<PasswordEntryDTO.CustomFieldDTO> customFieldDTOs = new ArrayList<>();
        if (includeCustomFields && entry.getCustomFields() != null) {
            customFieldDTOs = entry.getCustomFields().stream()
                    .map(this::toCustomFieldDTO)
                    .toList();
        }

        List<PasswordEntryDTO.TagDTO> tagDTOs = new ArrayList<>();
        if (entry.getTags() != null) {
            tagDTOs = entry.getTags().stream()
                    .map(this::toTagDTO)
                    .toList();
        }

        return PasswordEntryDTO.builder()
                .id(entry.getId())
                .title(entry.getTitle())
                .username(entry.getUsername())
                .email(entry.getEmail())
                .password(decryptedPassword)
                .url(entry.getUrl())
                .notes(entry.getNotes())
                .categoryId(entry.getCategory() != null ? entry.getCategory().getId() : null)
                .categoryName(entry.getCategory() != null ? entry.getCategory().getName() : null)
                .favorite(entry.getFavorite())
                .passwordLastChanged(entry.getPasswordLastChanged())
                .customFields(customFieldDTOs)
                .tags(tagDTOs)
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .build();
    }

    public void updateEntityFromDTO(PasswordEntry entry, PasswordEntryDTO dto) {
        entry.setTitle(dto.getTitle());
        entry.setUsername(dto.getUsername());
        entry.setEmail(dto.getEmail());

        // Actualizar contraseña y fecha de último cambio si cambió la contraseña
        String oldEncryptedPassword = entry.getPassword();
        String newEncryptedPassword = encryptionService.encrypt(dto.getPassword());

        if (oldEncryptedPassword == null || !oldEncryptedPassword.equals(newEncryptedPassword)) {
            entry.setPassword(newEncryptedPassword);
            entry.setHmacTag(encryptionService.sign(newEncryptedPassword));
            entry.setPasswordLastChanged(java.time.LocalDateTime.now());
        }

        entry.setUrl(dto.getUrl());
        entry.setNotes(dto.getNotes());
        entry.setFavorite(dto.getFavorite());

        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElse(null);
            entry.setCategory(category);
        } else {
            entry.setCategory(null);
        }

        if (dto.getCustomFields() != null) {
            for (PasswordEntryDTO.CustomFieldDTO fieldDTO : dto.getCustomFields()) {
                CustomField field = CustomField.builder()
                        .fieldName(fieldDTO.getFieldName())
                        .fieldValue(fieldDTO.isSensitive()
                                ? encryptionService.encrypt(fieldDTO.getFieldValue())
                                : fieldDTO.getFieldValue())
                        .sensitive(fieldDTO.isSensitive())
                        .build();
                entry.addCustomField(field);
            }
        }
    }

    private PasswordEntryDTO.CustomFieldDTO toCustomFieldDTO(CustomField field) {
        String value = field.isSensitive()
                ? decryptField(field.getFieldValue())
                : field.getFieldValue();

        return PasswordEntryDTO.CustomFieldDTO.builder()
                .id(field.getId())
                .fieldName(field.getFieldName())
                .fieldValue(value)
                .sensitive(field.isSensitive())
                .build();
    }

    /**
     * Descifra la contraseña de una entrada verificando primero la firma HMAC (Key C).
     * Si hmacTag es nulo (datos pre-migración) se descifra sin verificar firma.
     */
    private String decryptAndVerify(PasswordEntry entry) {
        try {
            if (entry.getHmacTag() != null) {
                if (!encryptionService.verifySignature(entry.getPassword(), entry.getHmacTag())) {
                    log.warn("Firma HMAC inválida para entrada id={}", entry.getId());
                    return DECRYPTION_ERROR;
                }
            }
            return encryptionService.decrypt(entry.getPassword());
        } catch (Exception e) {
            log.warn("Error al desencriptar entrada id={}: {}", entry.getId(), e.getMessage());
            return DECRYPTION_ERROR;
        }
    }

    private String decryptField(String encryptedValue) {
        try {
            return encryptionService.decrypt(encryptedValue);
        } catch (Exception e) {
            log.warn("Error al desencriptar campo: {}", e.getMessage());
            return DECRYPTION_ERROR;
        }
    }

    private PasswordEntryDTO.TagDTO toTagDTO(com.passmanager.model.entity.Tag tag) {
        return PasswordEntryDTO.TagDTO.builder()
                .id(tag.getId())
                .name(tag.getName())
                .color(tag.getColor())
                .build();
    }
}
