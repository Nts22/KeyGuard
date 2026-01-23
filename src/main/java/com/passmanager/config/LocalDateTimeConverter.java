package com.passmanager.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Converter(autoApply = false)
public class LocalDateTimeConverter implements AttributeConverter<LocalDateTime, String> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String convertToDatabaseColumn(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(FORMATTER);
    }

    @Override
    public LocalDateTime convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(dbData, FORMATTER);
    }
}
