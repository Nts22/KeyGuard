package com.passmanager.service;

import com.passmanager.model.entity.Tag;
import com.passmanager.model.entity.User;

import java.util.List;
import java.util.Optional;

public interface TagService {

    List<Tag> findAllByUser(User user);

    Optional<Tag> findByNameAndUser(String name, User user);

    Tag createTag(String name, String color, User user);

    Tag updateTag(Long id, String name, String color);

    void deleteTag(Long id);

    boolean existsByNameAndUser(String name, User user);
}
