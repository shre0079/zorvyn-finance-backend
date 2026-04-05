//package com.shreyash.zorvyn.finance_dashboard_backend;
//
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//
//@Configuration
//public class PasswordGenerator {
//
//    @Bean
//    public CommandLineRunner generateHash() {
//        return args -> {
//            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
//            String rawPassword = "Viewer@123";
//            String hash = encoder.encode(rawPassword);
//
//            System.out.println("BCrypt hash: " + hash);
//        };
//    }
//}
