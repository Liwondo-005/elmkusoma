package tz.elmkusoma.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;

@Configuration
public class UserDetailsConfig {

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return email -> {
            User user = userRepository.findByEmailAndIsDeletedFalse(email)
                    .orElseThrow(() -> new UsernameNotFoundException(
                            "User not found with email: " + email));
            return org.springframework.security.core.userdetails.User.builder()
                    .username(user.getEmail())
                    .password(user.getPasswordHash())
                    .authorities("ROLE_" + user.getRole().name())
                    .build();
        };
    }
}
