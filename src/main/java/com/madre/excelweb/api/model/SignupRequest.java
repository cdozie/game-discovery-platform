package com.madre.excelweb.api.model;

import java.util.Set;

public record SignupRequest(String username, String email, String password, Set<String> roles) {
}
