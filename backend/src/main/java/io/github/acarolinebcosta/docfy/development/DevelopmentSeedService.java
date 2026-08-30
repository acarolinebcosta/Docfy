package io.github.acarolinebcosta.docfy.development;

import io.github.acarolinebcosta.docfy.auth.domain.Role;
import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.auth.domain.UserRepository;
import io.github.acarolinebcosta.docfy.document.application.CreateDocumentCommand;
import io.github.acarolinebcosta.docfy.document.application.DocumentApplicationService;
import io.github.acarolinebcosta.docfy.document.application.DocumentWorkflowService;
import io.github.acarolinebcosta.docfy.document.domain.Document;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Profile("dev")
public class DevelopmentSeedService {

    private static final List<SeedUser> SEED_USERS = List.of(
            new SeedUser("admin@docfy.local", Role.ADMIN),
            new SeedUser("manager@docfy.local", Role.MANAGER),
            new SeedUser("ana@docfy.local", Role.COLLABORATOR),
            new SeedUser("joao@docfy.local", Role.COLLABORATOR)
    );

    private static final List<SeedDocument> SEED_DOCUMENTS = List.of(
            new SeedDocument(
                    "ana@docfy.local",
                    "Quality Policy",
                    "Quality principles and responsibilities for Docfy.",
                    SeedWorkflow.DRAFT
            ),
            new SeedDocument(
                    "ana@docfy.local",
                    "Information Security Policy",
                    "Security controls for information handled by Docfy.",
                    SeedWorkflow.REJECT_AND_RESUBMIT
            ),
            new SeedDocument(
                    "ana@docfy.local",
                    "Software Release Checklist",
                    "Release readiness checks for software delivery.",
                    SeedWorkflow.APPROVED
            ),
            new SeedDocument(
                    "ana@docfy.local",
                    "Operational Procedure",
                    "Standard procedure for recurring operational work.",
                    SeedWorkflow.ARCHIVED
            ),
            new SeedDocument(
                    "joao@docfy.local",
                    "Architecture Guidelines",
                    "Architecture principles for maintainable services.",
                    SeedWorkflow.DRAFT
            ),
            new SeedDocument(
                    "joao@docfy.local",
                    "Incident Response Procedure",
                    "Response steps for operational and security incidents.",
                    SeedWorkflow.IN_REVIEW
            ),
            new SeedDocument(
                    "joao@docfy.local",
                    "Supplier Agreement",
                    "Approved baseline agreement for external suppliers.",
                    SeedWorkflow.APPROVED
            ),
            new SeedDocument(
                    "joao@docfy.local",
                    "Meeting Minutes",
                    "Archived record of a completed governance meeting.",
                    SeedWorkflow.ARCHIVED
            )
    );

    private final UserRepository userRepository;
    private final DocumentApplicationService documentService;
    private final DocumentWorkflowService workflowService;
    private final PasswordEncoder passwordEncoder;

    public DevelopmentSeedService(
            UserRepository userRepository,
            DocumentApplicationService documentService,
            DocumentWorkflowService workflowService,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.documentService = documentService;
        this.workflowService = workflowService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public boolean initialize(String rawPassword) {
        validatePassword(rawPassword);

        Map<String, User> existingUsers = loadExistingSeedUsers();

        if (existingUsers.isEmpty()) {
            Map<String, User> users = createSeedUsers(rawPassword);
            createSeedDocuments(users);
            return true;
        }

        if (existingUsers.size() != SEED_USERS.size()) {
            throw new IllegalStateException(
                    "Partial development seed detected. "
                            + "Reset the local development data before starting again."
            );
        }

        validateExistingSeedUsers(existingUsers, rawPassword);

        return false;
    }

    private void validatePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalStateException(
                    "DOCFY_DEV_SEED_PASSWORD must be configured "
                            + "when the dev profile is active"
            );
        }
    }

    private Map<String, User> loadExistingSeedUsers() {
        Map<String, User> users = new LinkedHashMap<>();

        for (SeedUser seedUser : SEED_USERS) {
            userRepository.findByEmail(seedUser.email())
                    .ifPresent(user ->
                            users.put(seedUser.email(), user)
                    );
        }

        return users;
    }

    private Map<String, User> createSeedUsers(String rawPassword) {
        Map<String, User> users = new LinkedHashMap<>();

        for (SeedUser seedUser : SEED_USERS) {
            User user = userRepository.save(
                    new User(
                            seedUser.email(),
                            passwordEncoder.encode(rawPassword),
                            seedUser.role()
                    )
            );

            users.put(seedUser.email(), user);
        }

        return users;
    }

    private void validateExistingSeedUsers(
            Map<String, User> users,
            String rawPassword
    ) {
        for (SeedUser seedUser : SEED_USERS) {
            User user = users.get(seedUser.email());

            if (!user.isActive()
                    || user.getRole() != seedUser.role()
                    || !passwordEncoder.matches(
                            rawPassword,
                            user.getPasswordHash()
                    )) {
                throw new IllegalStateException(
                        "Existing development seed user is incompatible: "
                                + seedUser.email()
                );
            }
        }
    }

    private void createSeedDocuments(Map<String, User> users) {
        User manager = users.get("manager@docfy.local");

        for (SeedDocument seedDocument : SEED_DOCUMENTS) {
            User owner = users.get(seedDocument.ownerEmail());

            Document document = documentService.create(
                    new CreateDocumentCommand(
                            seedDocument.title(),
                            seedDocument.description()
                    ),
                    owner
            );

            applyWorkflow(
                    document,
                    owner,
                    manager,
                    seedDocument.workflow()
            );
        }
    }

    private void applyWorkflow(
            Document document,
            User owner,
            User manager,
            SeedWorkflow workflow
    ) {
        switch (workflow) {
            case DRAFT -> {
            }
            case IN_REVIEW -> workflowService.submit(
                    document.getId(),
                    owner
            );
            case REJECT_AND_RESUBMIT -> {
                workflowService.submit(document.getId(), owner);
                workflowService.reject(document.getId(), manager);
                workflowService.submit(document.getId(), owner);
            }
            case APPROVED -> {
                workflowService.submit(document.getId(), owner);
                workflowService.approve(document.getId(), manager);
            }
            case ARCHIVED -> {
                workflowService.submit(document.getId(), owner);
                workflowService.approve(document.getId(), manager);
                workflowService.archive(document.getId(), manager);
            }
        }
    }

    private record SeedUser(String email, Role role) {
    }

    private record SeedDocument(
            String ownerEmail,
            String title,
            String description,
            SeedWorkflow workflow
    ) {
    }

    private enum SeedWorkflow {
        DRAFT,
        IN_REVIEW,
        REJECT_AND_RESUBMIT,
        APPROVED,
        ARCHIVED
    }
}