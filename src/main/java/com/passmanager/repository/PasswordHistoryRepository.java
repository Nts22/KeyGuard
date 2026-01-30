package com.passmanager.repository;

import com.passmanager.model.entity.PasswordEntry;
import com.passmanager.model.entity.PasswordHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para acceder a los datos de historial de contraseñas.
 *
 * Proporciona queries personalizadas para:
 * - Obtener historial completo de una entrada
 * - Contar versiones almacenadas
 * - Obtener la versión más antigüa (para limpieza)
 * - Eliminar todo el historial de una entrada
 */
@Repository
public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, Long> {

    /**
     * Obtiene todo el historial de una entrada específica,
     * ordenado por fecha descendente (más reciente primero).
     *
     * @param passwordEntry Entrada cuyo historial se quiere obtener
     * @return Lista de registros de historial ordenados por fecha desc
     */
    List<PasswordHistory> findByPasswordEntryOrderByChangedAtDesc(PasswordEntry passwordEntry);

    /**
     * Cuenta cuántos registros de historial tiene una entrada.
     * Útil para verificar si se excedió el límite de versiones.
     *
     * @param passwordEntry Entrada a contar
     * @return Número de registros de historial
     */
    long countByPasswordEntry(PasswordEntry passwordEntry);

    /**
     * Obtiene el registro de historial MÁS ANTIGUO de una entrada.
     * Usado para eliminar la versión más vieja cuando se excede el límite.
     *
     * @param passwordEntry Entrada cuyo historial más antiguo se quiere obtener
     * @return Optional con el registro más antiguo, o empty si no hay historial
     */
    Optional<PasswordHistory> findTopByPasswordEntryOrderByChangedAtAsc(PasswordEntry passwordEntry);

    /**
     * Elimina todo el historial de una entrada específica.
     * Usado cuando se elimina una entrada completa.
     *
     * @param passwordEntry Entrada cuyo historial se quiere eliminar
     */
    void deleteByPasswordEntry(PasswordEntry passwordEntry);
}
