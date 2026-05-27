package com.ctn.offerwall.user.seed;

import com.ctn.offerwall.user.config.SeedProperties;
import com.ctn.offerwall.user.domain.AppUser;
import com.ctn.offerwall.user.domain.NotificationPreferences;
import com.ctn.offerwall.user.domain.UserRole;
import com.ctn.offerwall.user.repository.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class LocalSeedRunner implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "test.admin@example.com";
    private static final String ADMIN_PASSWORD = "testpassword";

    private final SeedProperties properties;
    private final Environment environment;
    private final AppUserRepository userRepository;

    public LocalSeedRunner(SeedProperties properties,
                           Environment environment,
                           AppUserRepository userRepository) {
        this.properties = properties;
        this.environment = environment;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!properties.isEnabled() || !hasLocalSeedProfile()) {
            return;
        }

        AppUser admin = userRepository.findByEmailIgnoreCase(ADMIN_EMAIL)
                .orElseGet(() -> userRepository.save(new AppUser(
                        ADMIN_EMAIL,
                        ADMIN_PASSWORD,
                        new NotificationPreferences(false, true)
                )));
        admin.addRole(UserRole.USER);
        admin.addRole(UserRole.ADMIN);
    }

    private boolean hasLocalSeedProfile() {
        return List.of(environment.getActiveProfiles()).contains("local-seed");
    }
}
