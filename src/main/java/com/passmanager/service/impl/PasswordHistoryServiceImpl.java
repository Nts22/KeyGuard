package com.passmanager.service.impl;

import com.passmanager.exception.ResourceNotFoundException;
import com.passmanager.mapper.PasswordHistoryMapper;
import com.passmanager.model.dto.PasswordHistoryDTO;
import com.passmanager.model.entity.PasswordEntry;
import com.passmanager.model.entity.PasswordHistory;
import com.passmanager.repository.PasswordEntryRepository;
import com.passmanager.repository.PasswordHistoryRepository;
import com.passmanager.service.EncryptionService;
import com.passmanager.service.PasswordHistoryService;
import com.passmanager.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implementación del servicio de historial de contraseñas.
 *
 * Características:
 * - Guarda automáticamente hasta 10 versiones anteriores
 * - Cifrado AES-256-GCM de todas las contraseñas históricas
 * - Limpieza automática de versiones antiguas
 * - Aislamiento por usuario (seguridad)
 */
@Service
public class PasswordHistoryServiceImpl implements PasswordHistoryService {

    /**
     * Número máximo de versiones de historial a mantener.
     * Después de este límite, se eliminan las versiones más antiguas.
     */
    private static final int MAX_HISTORY_VERSIONS = 10;

    private final PasswordHistoryRepository historyRepository;
    private final PasswordEntryRepository entryRepository;
    private final PasswordHistoryMapper historyMapper;
    private final UserService userService;
    private final EncryptionService encryptionService;

    public PasswordHistoryServiceImpl(PasswordHistoryRepository historyRepository,
                                      PasswordEntryRepository entryRepository,
                                      PasswordHistoryMapper historyMapper,
                                      UserService userService,
                                      EncryptionService encryptionService) {
        this.historyRepository = historyRepository;
        this.entryRepository = entryRepository;
        this.historyMapper = historyMapper;
        this.userService = userService;
        this.encryptionService = encryptionService;
    }

    @Override
    @Transactional
    public void savePasswordHistory(PasswordEntry entry, String oldPassword) {
        // Crear registro de historial con contraseña encriptada
        PasswordHistory history = PasswordHistory.builder()
                .passwordEntry(entry)
                .password(encryptionService.encrypt(oldPassword))
                .build();

        historyRepository.save(history);

        // Limpiar versiones antiguas si se excede el límite
        cleanupOldHistory(entry, MAX_HISTORY_VERSIONS);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PasswordHistoryDTO> getHistory(Long passwordEntryId) {
        // Verificar que la entrada existe y pertenece al usuario actual (seguridad)
        PasswordEntry entry = entryRepository.findByIdAndUser(passwordEntryId, userService.getCurrentUser())
                .orElseThrow(() -> new ResourceNotFoundException("PasswordEntry", passwordEntryId));

        // Obtener historial ordenado por fecha descendente (más reciente primero)
        return historyRepository.findByPasswordEntryOrderByChangedAtDesc(entry)
                .stream()
                .map(historyMapper::toDTO)
                .toList();
    }

    @Transactional
    private void cleanupOldHistory(PasswordEntry entry, int maxVersions) {
        long count = historyRepository.countByPasswordEntry(entry);

        // Eliminar versiones más antiguas mientras se exceda el límite
        while (count > maxVersions) {
            Optional<PasswordHistory> oldest = historyRepository
                    .findTopByPasswordEntryOrderByChangedAtAsc(entry);

            if (oldest.isPresent()) {
                historyRepository.delete(oldest.get());
                count--;
            } else {
                break;  // No hay más registros para eliminar
            }
        }
    }

}
