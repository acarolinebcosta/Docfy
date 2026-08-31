package io.github.acarolinebcosta.docfy.document.domain;

import io.github.acarolinebcosta.docfy.auth.domain.Role;
import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.auth.domain.UserRepository;
import io.github.acarolinebcosta.docfy.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class DocumentCodeConcurrencyIntegrationTest
        implements PostgresTestContainer {

    private static final int CONCURRENT_CREATIONS = 12;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldGenerateUniqueCodesDuringConcurrentCreation()
            throws Exception {
        User owner = userRepository.save(
                new User(
                        "document-code-" + UUID.randomUUID()
                                + "@docfy.local",
                        "{bcrypt}encoded-password",
                        Role.COLLABORATOR
                )
        );
        CountDownLatch ready =
                new CountDownLatch(CONCURRENT_CREATIONS);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor =
                Executors.newFixedThreadPool(CONCURRENT_CREATIONS);
        List<Future<Document>> futures = new ArrayList<>();
        List<Document> createdDocuments = new ArrayList<>();

        try {
            for (int index = 0; index < CONCURRENT_CREATIONS; index++) {
                int documentIndex = index;
                futures.add(
                        executor.submit(() -> {
                            ready.countDown();
                            start.await();

                            return documentRepository.saveAndFlush(
                                    new Document(
                                            "Concurrent document "
                                                    + documentIndex,
                                            null,
                                            owner
                                    )
                            );
                        })
                );
            }

            ready.await();
            start.countDown();

            for (Future<Document> future : futures) {
                createdDocuments.add(future.get());
            }

            List<String> codes = createdDocuments.stream()
                    .map(Document::getDocumentCode)
                    .toList();

            assertEquals(
                    CONCURRENT_CREATIONS,
                    new HashSet<>(codes).size()
            );
            assertTrue(
                    codes.stream()
                            .allMatch(code -> code.matches("DOC-\\d{6,}"))
            );
        } finally {
            executor.shutdownNow();
            documentRepository.deleteAll(createdDocuments);
            userRepository.deleteById(owner.getId());
        }
    }
}
