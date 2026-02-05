package com.passmanager.service;

import com.passmanager.model.dto.PasswordHistoryDTO;
import com.passmanager.model.entity.PasswordEntry;

import java.util.List;

/**
 * Servicio para gestionar el historial de contraseñas.
 *
 * Funcionalidades principales:
 * - Guardar versiones antiguas cuando se actualiza una contraseña
 * - Recuperar historial completo de una entrada
 * - Limpieza automática de versiones antiguas (mantener solo últimas 10)
 * - Eliminar historial cuando se elimina una entrada
 */
public interface PasswordHistoryService {

    /**
     * Guarda una versión antigua de la contraseña en el historial.
     * Se llama automáticamente antes de actualizar una contraseña.
     *
     * @param entry Entrada cuya contraseña se está cambiando
     * @param oldPassword Contraseña antigua (en texto plano, se cifrará automáticamente)
     */
    void savePasswordHistory(PasswordEntry entry, String oldPassword);

    /**
     * Obtiene el historial completo de una entrada de contraseña.
     * Las contraseñas se retornan desencriptadas en el DTO.
     *
     * @param passwordEntryId ID de la entrada
     * @return Lista de PasswordHistoryDTO ordenada por fecha descendente
     * @throws com.passmanager.exception.ResourceNotFoundException si la entrada no existe
     */
    List<PasswordHistoryDTO> getHistory(Long passwordEntryId);
}
