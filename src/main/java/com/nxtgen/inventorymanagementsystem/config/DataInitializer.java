package com.nxtgen.inventorymanagementsystem.config;

import com.nxtgen.inventorymanagementsystem.entity.User;
import com.nxtgen.inventorymanagementsystem.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByUsername("admin").isEmpty()) {
                userRepository.save(new User("admin", passwordEncoder.encode("admin123"), "ADMIN"));
            }

            if (userRepository.findByUsername("staff").isEmpty()) {
                userRepository.save(new User("staff", passwordEncoder.encode("staff123"), "STAFF"));
            }

            if (userRepository.findByUsername("viewer").isEmpty()) {
                userRepository.save(new User("viewer", passwordEncoder.encode("viewer123"), "VIEWER"));
            }
        };
    }
}