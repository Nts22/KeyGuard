package com.passmanager.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para transferir datos de historial de contraseñas entre capas.
 *
 * Contiene la contraseña DESENCRIPTADA y fecha formateada
 * para mostrar en la UI.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordHistoryDTO {

    /**
     * ID del registro de historial.
     */
    private Long id;

    /**
     * Contraseña antigua en texto plano (desencriptada).
     * Solo existe en memoria, nunca se guarda así en BD.
     */
    private String password;

    /**
     * Fecha y hora en que se cambió la contraseña.
     */
    private LocalDateTime changedAt;

    /**
     * Fecha formateada para mostrar en UI.
     * Formato: "dd/MM/yyyy HH:mm:ss"
     * Ejemplo: "26/01/2024 14:30:15"
     */
    private String formattedDate;
}
