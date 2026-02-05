package com.passmanager.repository;

import com.passmanager.model.entity.Category;
import com.passmanager.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUserOrderByNameAsc(User user);

    Optional<Category> findByIdAndUser(Long id, User user);

    boolean existsByNameAndUser(String name, User user);

    void deleteByIdAndUser(Long id, User user);
}
