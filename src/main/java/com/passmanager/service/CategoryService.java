package com.passmanager.service;

import com.passmanager.model.dto.CategoryDTO;
import com.passmanager.model.entity.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryService {

    List<CategoryDTO> findAll();

    Optional<Category> findById(Long id);

    Optional<Category> findByName(String name);

    CategoryDTO create(String name, String icon);

    CategoryDTO update(Long id, String name, String icon);

    void delete(Long id);

    long countEntries(Category category);

    void initDefaultCategories();
}
