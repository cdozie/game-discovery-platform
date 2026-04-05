package com.madre.excelweb.security.jwt;

import com.madre.excelweb.jpa.entity.AppUser;
import com.madre.excelweb.security.service.UserDetailsServiceImpl;
import com.madre.excelweb.security.type.FullUserInfo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class AuthTokenFilter extends OncePerRequestFilter {
    public static final String USER_INFO_ATTRIBUTE = "userInfo";
    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            final String jwt = parseJwt(request);
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {

                final Long id = jwtUtils.getUserIdFromToken(jwt);
                final FullUserInfo fullUserInfo = userDetailsService.loadUserWithDetailsById(id);
                final UserDetails userDetails = fullUserInfo.getUserDetails();

                final AppUser appUser = fullUserInfo.getAppUser();
                if (!userDetails.isAccountNonLocked() || !userDetails.isEnabled()) {
                    response.addHeader("Set-Cookie", jwtUtils.getCleanJwtCookie().toString());
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                final UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );
                request.setAttribute(USER_INFO_ATTRIBUTE, appUser);
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication:", e);
        }
        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        return jwtUtils.getJwtFromCookies(request);
    }

}
