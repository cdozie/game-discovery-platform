package com.madre.excelweb.config;

import com.madre.excelweb.jpa.entity.AppRole;
import com.madre.excelweb.jpa.repositories.RoleRepository;
import com.madre.excelweb.jpa.repositories.UserRepository;
import com.madre.excelweb.models.Role;
import com.madre.excelweb.service.CatalogConsolidationService;
import com.madre.excelweb.service.CatalogSyncCoordinator;
import com.madre.excelweb.service.GameStatsMaintenanceService;
import com.madre.excelweb.service.SystemJobService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

@Configuration
public class DataInitializationConfig {

    @Bean
    public TransactionTemplate catalogConsolidationTransactionTemplate(final PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    @Bean
    public ApplicationRunner roleInitializer(final RoleRepository roleRepository) {
        return args -> Arrays.stream(Role.values())
                .filter(role -> roleRepository.findByName(role).isEmpty())
                .map(role -> AppRole.builder().name(role).build())
                .forEach(roleRepository::save);
    }

    @Bean
    public ApplicationRunner adminBootstrapInitializer(final RoleRepository roleRepository,
                                                       final UserRepository userRepository,
                                                       final Environment environment,
                                                       final TransactionTemplate transactionTemplate) {
        return args -> {
            final Set<String> bootstrapUsernames = parseCsv(environment.getProperty("web.admin.bootstrap-usernames"));
            final Set<String> bootstrapEmails = parseCsv(environment.getProperty("web.admin.bootstrap-emails"));
            if (bootstrapUsernames.isEmpty() && bootstrapEmails.isEmpty()) {
                return;
            }

            transactionTemplate.executeWithoutResult(status -> {
                final AppRole adminRole = roleRepository.findByName(Role.ADMIN)
                        .orElseGet(() -> roleRepository.save(AppRole.builder().name(Role.ADMIN).build()));

                userRepository.findAll().stream()
                        .filter(user -> bootstrapUsernames.contains(user.getUsername()) || bootstrapEmails.contains(user.getEmail()))
                        .filter(user -> user.getRoles() == null || user.getRoles().stream().noneMatch(role -> role.getName() == Role.ADMIN))
                        .forEach(user -> {
                            final Set<AppRole> roles = new LinkedHashSet<>(user.getRoles() == null ? Set.of() : user.getRoles());
                            roles.add(adminRole);
                            user.setRoles(roles);
                            userRepository.save(user);
                        });
            });
        };
    }

    @Bean
    public ApplicationRunner catalogMaintenanceInitializer(final CatalogConsolidationService catalogConsolidationService,
                                                           final CatalogSyncCoordinator catalogSyncCoordinator,
                                                           final GameStatsMaintenanceService gameStatsMaintenanceService,
                                                           final SystemJobService systemJobService,
                                                           final Environment environment,
                                                           final ConfigurableApplicationContext applicationContext) {
        return args -> {
            systemJobService.failStaleRunningJobs();
            catalogSyncCoordinator.failStaleRunningSyncs();
            final String maintenanceMode = environment.getProperty("web.app.catalog.maintenance-mode", "").trim().toLowerCase();
            if ("consolidate".equals(maintenanceMode)) {
                catalogConsolidationService.backfillCatalogGroupKeys();
                catalogConsolidationService.consolidateCatalogVariants();
                gameStatsMaintenanceService.rebuildAllGameStats();
                System.exit(SpringApplication.exit(applicationContext, () -> 0));
            }

            catalogConsolidationService.backfillCatalogGroupKeys();
            gameStatsMaintenanceService.rebuildAllGameStats();
        };
    }

    private Set<String> parseCsv(final String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return Set.of();
        }

        final Set<String> values = new LinkedHashSet<>();
        Arrays.stream(rawValue.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .forEach(values::add);
        return values;
    }
}
