package com.madre.excelweb.service.util;

import java.util.UUID;

public final class PublicIdUtils {
    private PublicIdUtils() {
    }

    public static String newPublicId() {
        return UUID.randomUUID().toString();
    }
}
