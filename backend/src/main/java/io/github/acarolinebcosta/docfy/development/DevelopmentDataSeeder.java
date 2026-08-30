package io.github.acarolinebcosta.docfy.development;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevelopmentDataSeeder implements ApplicationRunner {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DevelopmentDataSeeder.class);

    private final DevelopmentSeedService seedService;
    private final String seedPassword;

    public DevelopmentDataSeeder(
            DevelopmentSeedService seedService,
            @Value("${docfy.development.seed.password}")
            String seedPassword
    ) {
        this.seedService = seedService;
        this.seedPassword = seedPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean initialized = seedService.initialize(seedPassword);

        if (initialized) {
            LOGGER.info("Development seed initialized");
        } else {
            LOGGER.info("Development seed already present");
        }
    }
}
