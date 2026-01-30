package com.passmanager.mapper;

import com.passmanager.model.dto.PasswordHistoryDTO;
import com.passmanager.model.entity.PasswordEntry;
import com.passmanager.model.entity.PasswordHistory;
import com.passmanager.service.EncryptionService;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * Mapper para convertir entre PasswordHistory (Entity) y PasswordHistoryDTO.
 *
 * Responsabilidades:
 * - Desencriptar contraseñas al convertir a DTO (para mostrar en UI)
 * - Encriptar contraseñas al convertir a Entity (para guardar en BD)
 * - Formatear fechas para presentación
 */
@Component
public class PasswordHistoryMapper {

    private final EncryptionService encryptionService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public PasswordHistoryMapper(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    /**
     * Convierte una entidad PasswordHistory a DTO.
     * Desencripta la contraseña y formatea la fecha.
     *
     * @param entity Entidad a convertir
     * @return DTO con datos desencriptados
     */
    public PasswordHistoryDTO toDTO(PasswordHistory entity) {
        String decryptedPassword;
        try {
            decryptedPassword = encryptionService.decrypt(entity.getPassword());
        } catch (Exception e) {
            decryptedPassword = "***ERROR***";  // Manejo de errores de descifrado
        }

        return PasswordHistoryDTO.builder()
                .id(entity.getId())
                .password(decryptedPassword)
                .changedAt(entity.getChangedAt())
                .formattedDate(entity.getChangedAt().format(DATE_FORMATTER))
                .build();
    }

    /**
     * Convierte un DTO a entidad PasswordHistory.
     * Encripta la contraseña antes de guardar.
     *
     * @param dto DTO con datos en texto plano
     * @param entry Entrada a la que pertenece este historial
     * @return Entidad con contraseña encriptada
     */
    public PasswordHistory toEntity(PasswordHistoryDTO dto, PasswordEntry entry) {
        return PasswordHistory.builder()
                .passwordEntry(entry)
                .password(encryptionService.encrypt(dto.getPassword()))
                .build();
    }
}
