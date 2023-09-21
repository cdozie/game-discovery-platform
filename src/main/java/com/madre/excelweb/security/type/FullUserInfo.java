package com.madre.excelweb.security.type;

import com.madre.excelweb.jpa.entity.AppUser;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;

@Data
@RequiredArgsConstructor
public class FullUserInfo {
    private final AppUser appUser;
    private final UserDetails userDetails;

}
