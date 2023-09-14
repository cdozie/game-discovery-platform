package com.madre.excelweb.api.model;

import java.util.List;

public record UserInfoResponse(Long id, String username, String email, List<String> role) {
}
