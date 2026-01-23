package com.passmanager.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordEntryDTO {

    private Long id;
    private String title;
    private String username;
    private String email;
    private String password;
    private String url;
    private String notes;
    private Long categoryId;
    private String categoryName;
    @Builder.Default
    private List<CustomFieldDTO> customFields = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CustomFieldDTO {
        private Long id;
        private String fieldName;
        private String fieldValue;
        private boolean sensitive;
    }
}
