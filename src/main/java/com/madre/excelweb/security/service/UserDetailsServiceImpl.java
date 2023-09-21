package com.madre.excelweb.security.service;

import com.madre.excelweb.jpa.entity.AppUser;
import com.madre.excelweb.jpa.repositories.UserRepository;
import com.madre.excelweb.security.type.FullUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String usernameOrEmail) {

        //Make it so that the user can login with either their username or email by taking whichever one is not null
        return userRepository.getUserByEmail(usernameOrEmail)
                .or(() -> userRepository.getUserByUsername(usernameOrEmail))
                .map(UserDetailsImpl::build)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    }

    @Transactional
    public FullUserInfo loadUserWithDetailsById(Long id) {
        AppUser appUser = userRepository.getUserById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        UserDetails userDetails = UserDetailsImpl.build(appUser);

        return new FullUserInfo(appUser, userDetails);
    }
}
