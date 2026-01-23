package com.passmanager.mapper;

import com.passmanager.model.dto.CategoryDTO;
import com.passmanager.model.entity.Category;
import com.passmanager.model.entity.User;
import com.passmanager.repository.PasswordEntryRepository;
import com.passmanager.service.UserService;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    private final PasswordEntryRepository passwordEntryRepository;
    private final UserService userService;

    public CategoryMapper(PasswordEntryRepository passwordEntryRepository,
                          UserService userService) {
        this.passwordEntryRepository = passwordEntryRepository;
        this.userService = userService;
    }

    public CategoryDTO toDTO(Category category) {
        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .icon(category.getIcon())
                .entryCount((int) countEntries(category))
                .build();
    }

    public long countEntries(Category category) {
        User user = userService.getCurrentUser();
        return passwordEntryRepository.countByCategoryAndUser(category, user);
    }
}
