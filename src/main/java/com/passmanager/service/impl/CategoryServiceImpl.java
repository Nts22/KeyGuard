package com.passmanager.service.impl;

import com.passmanager.exception.ResourceNotFoundException;
import com.passmanager.mapper.CategoryMapper;
import com.passmanager.model.dto.CategoryDTO;
import com.passmanager.model.entity.Category;
import com.passmanager.model.entity.User;
import com.passmanager.repository.CategoryRepository;
import com.passmanager.service.CategoryService;
import com.passmanager.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final UserService userService;

    public CategoryServiceImpl(CategoryRepository categoryRepository,
                               CategoryMapper categoryMapper,
                               UserService userService) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
        this.userService = userService;
    }

    private User getCurrentUser() {
        return userService.getCurrentUser();
    }

    @Override
    public List<CategoryDTO> findAll() {
        return categoryRepository.findByUserOrderByNameAsc(getCurrentUser()).stream()
                .map(categoryMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public CategoryDTO create(String name, String icon) {
        User user = getCurrentUser();

        if (categoryRepository.existsByNameAndUser(name, user)) {
            throw new IllegalArgumentException("La categoría ya existe: " + name);
        }

        Category category = Category.builder()
                .name(name)
                .icon(icon)
                .user(user)
                .build();

        return categoryMapper.toDTO(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryDTO update(Long id, String name, String icon) {
        Category category = categoryRepository.findByIdAndUser(id, getCurrentUser())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría", id));

        category.setName(name);
        category.setIcon(icon);

        return categoryMapper.toDTO(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        categoryRepository.deleteByIdAndUser(id, getCurrentUser());
    }

    @Override
    @Transactional
    public void initDefaultCategories() {
        User user = getCurrentUser();
        if (categoryRepository.findByUserOrderByNameAsc(user).isEmpty()) {
            create("Redes Sociales", "social");
            create("Bancos", "bank");
            create("Email", "email");
            create("Streaming", "video");
            create("Trabajo", "work");
            create("Otros", "other");
        }
    }
}
