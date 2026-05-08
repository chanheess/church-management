package com.saeanyang.management.security;

import com.saeanyang.management.account.User;
import com.saeanyang.management.account.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public DatabaseUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username.trim().toLowerCase())
            .orElseThrow(() -> new UsernameNotFoundException("아이디를 찾을 수 없습니다."));

        return org.springframework.security.core.userdetails.User
            .withUsername(user.getUsername())
            .password(user.getPasswordHash())
            .authorities(List.of(new SimpleGrantedAuthority(user.getRole())))
            .disabled(!user.isEnabled())
            .accountLocked(user.isLocked(Instant.now()))
            .build();
    }
}
