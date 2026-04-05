package com.madre.excelweb.service.util;

import com.madre.excelweb.models.AdminGameField;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class AdminOverrideUtils {

    private AdminOverrideUtils() {
    }

    public static Set<AdminGameField> parseLockedFields(final String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return new LinkedHashSet<>();
        }

        return CatalogGroupingUtils.parseDelimitedValues(rawValue).stream()
                .map(AdminGameField::fromNullable)
                .flatMap(java.util.Optional::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static String serializeLockedFields(final Collection<AdminGameField> fields) {
        if (fields == null || fields.isEmpty()) {
            return "";
        }

        return fields.stream()
                .map(AdminGameField::name)
                .distinct()
                .collect(Collectors.joining(", "));
    }

    public static boolean isLocked(final String rawValue, final AdminGameField field) {
        return parseLockedFields(rawValue).contains(field);
    }

    public static String mergeLockedFields(final String existingFields, final Collection<AdminGameField> additionalFields) {
        final Set<AdminGameField> mergedFields = new LinkedHashSet<>(parseLockedFields(existingFields));
        if (additionalFields != null) {
            mergedFields.addAll(additionalFields);
        }
        return serializeLockedFields(mergedFields);
    }
}
