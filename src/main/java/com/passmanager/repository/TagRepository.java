package com.passmanager.repository;

import com.passmanager.model.entity.Tag;
import com.passmanager.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    List<Tag> findByUserOrderByNameAsc(User user);

    Optional<Tag> findByNameAndUser(String name, User user);

    boolean existsByNameAndUser(String name, User user);
}
