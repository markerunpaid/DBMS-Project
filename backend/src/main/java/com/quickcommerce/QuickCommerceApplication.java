package com.quickcommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// logins are handled by AuthService + JWT, so skip Boot's generated in-memory user
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class QuickCommerceApplication {

	public static void main(String[] args) {
		SpringApplication.run(QuickCommerceApplication.class, args);
	}

}
