package io.github.acarolinebcosta.docfy;

import io.github.acarolinebcosta.docfy.auth.security.JwtProperties;
import io.github.acarolinebcosta.docfy.file.config.DocumentFileProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        JwtProperties.class,
        DocumentFileProperties.class
})
public class DocfyBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocfyBackendApplication.class, args);
    }
}
