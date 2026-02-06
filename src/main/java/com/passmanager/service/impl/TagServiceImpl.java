package com.passmanager.service.impl;

import com.passmanager.exception.ResourceNotFoundException;
import com.passmanager.model.entity.Tag;
import com.passmanager.model.entity.User;
import com.passmanager.repository.TagRepository;
import com.passmanager.service.TagService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;

    public TagServiceImpl(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Override
    public List<Tag> findAllByUser(User user) {
        return tagRepository.findByUserOrderByNameAsc(user);
    }

    @Override
    public Optional<Tag> findByNameAndUser(String name, User user) {
        return tagRepository.findByNameAndUser(name, user);
    }

    @Override
    @Transactional
    public Tag createTag(String name, String color, User user) {
        if (existsByNameAndUser(name, user)) {
            throw new IllegalArgumentException("Ya existe un tag con el nombre: " + name);
        }

        Tag tag = Tag.builder()
                .name(name)
                .color(color)
                .user(user)
                .build();

        return tagRepository.save(tag);
    }

    @Override
    @Transactional
    public Tag updateTag(Long id, String name, String color) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", id));

        // Verificar que el nuevo nombre no esté en uso (excepto por el mismo tag)
        Optional<Tag> existing = tagRepository.findByNameAndUser(name, tag.getUser());
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new IllegalArgumentException("Ya existe un tag con el nombre: " + name);
        }

        tag.setName(name);
        tag.setColor(color);

        return tagRepository.save(tag);
    }

    @Override
    @Transactional
    public void deleteTag(Long id) {
        if (!tagRepository.existsById(id)) {
            throw new ResourceNotFoundException("Tag", id);
        }
        tagRepository.deleteById(id);
    }

    @Override
    public boolean existsByNameAndUser(String name, User user) {
        return tagRepository.existsByNameAndUser(name, user);
    }
}
