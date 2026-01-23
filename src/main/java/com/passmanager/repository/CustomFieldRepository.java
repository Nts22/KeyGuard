package com.passmanager.repository;

import com.passmanager.model.entity.CustomField;
import com.passmanager.model.entity.PasswordEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomFieldRepository extends JpaRepository<CustomField, Long> {

    List<CustomField> findByPasswordEntry(PasswordEntry passwordEntry);

    List<CustomField> findByPasswordEntryId(Long passwordEntryId);

    void deleteByPasswordEntryId(Long passwordEntryId);
}
