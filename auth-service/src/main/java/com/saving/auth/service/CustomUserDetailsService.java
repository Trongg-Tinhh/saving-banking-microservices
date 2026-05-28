package com.saving.auth.service;

import com.saving.auth.entity.User;
import com.saving.auth.entity.UserRole;
import com.saving.auth.exception.BusinessException;
import com.saving.auth.exception.ErrorCode;
import com.saving.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameWithRoles(username)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });

        if (!user.isActive()) {
            log.warn("User {} is not active: status={}", username, user.getStatus());
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE,
                    "Account status: " + user.getStatus());
        }

        List<SimpleGrantedAuthority> authorities = user.getUserRoles().stream()
                .map(UserRole::getRole)
                .map(role -> new SimpleGrantedAuthority(role.getRoleCode()))
                .collect(Collectors.toList());

        log.debug("Loaded user: {} with roles: {}", username,
                authorities.stream().map(SimpleGrantedAuthority::getAuthority).toList());

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked("LOCKED".equals(user.getStatus()))
                .credentialsExpired(false)
                .disabled("INACTIVE".equals(user.getStatus()))
                .build();
    }
}
