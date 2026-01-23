package com.passmanager.repository;

import com.passmanager.model.entity.Category;
import com.passmanager.model.entity.PasswordEntry;
import com.passmanager.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordEntryRepository extends JpaRepository<PasswordEntry, Long> {

    List<PasswordEntry> findByUserOrderByTitleAsc(User user);

    List<PasswordEntry> findByUserAndCategoryId(User user, Long categoryId);

    @Query("SELECT p FROM PasswordEntry p WHERE p.user = :user AND " +
            "(LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<PasswordEntry> searchByUser(@Param("user") User user, @Param("search") String search);

    @Query("SELECT p FROM PasswordEntry p WHERE p.user = :user AND p.category.id = :categoryId AND " +
            "(LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<PasswordEntry> searchByUserAndCategory(@Param("user") User user,
                                                 @Param("categoryId") Long categoryId,
                                                 @Param("search") String search);

    @Query("SELECT p FROM PasswordEntry p LEFT JOIN FETCH p.customFields WHERE p.id = :id AND p.user = :user")
    Optional<PasswordEntry> findByIdAndUserWithCustomFields(@Param("id") Long id, @Param("user") User user);

    Optional<PasswordEntry> findByIdAndUser(Long id, User user);

    long countByCategoryAndUser(Category category, User user);

    void deleteByIdAndUser(Long id, User user);
}
