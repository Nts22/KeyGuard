package com.passmanager.service;

import com.passmanager.model.dto.CategoryDTO;

import java.util.List;

public interface CategoryService {

    List<CategoryDTO> findAll();

    CategoryDTO create(String name, String icon);

    CategoryDTO update(Long id, String name, String icon);

    void delete(Long id);

    void initDefaultCategories();
}
