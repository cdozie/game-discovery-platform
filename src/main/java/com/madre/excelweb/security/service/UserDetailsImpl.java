package com.madre.excelweb.security.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.madre.excelweb.jpa.entity.AppRole;
import com.madre.excelweb.jpa.entity.AppUser;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public class UserDetailsImpl implements UserDetails {
    private static final long serialVersionUID = 1L;

    private final Long id;

    private final String username;

    private final String email;

    @JsonIgnore
    private final String password;

    private final Collection<? extends GrantedAuthority> authorities;

    private final boolean accountNonLocked;

    private final boolean enabled;

    public static UserDetailsImpl build(AppUser user) {
        List<GrantedAuthority> authorities = (user.getRoles() == null ? Set.<AppRole>of() : user.getRoles()).stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().getName()))
                .collect(Collectors.toList());
        return new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                authorities,
                !user.isCurrentlyBanned(),
                true);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
