package io.github.acarolinebcosta.docfy.development;

import io.github.acarolinebcosta.docfy.auth.domain.Role;
import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.auth.domain.UserRepository;
import io.github.acarolinebcosta.docfy.category.domain.Category;
import io.github.acarolinebcosta.docfy.category.domain.CategoryRepository;
import io.github.acarolinebcosta.docfy.document.application.CreateDocumentCommand;
import io.github.acarolinebcosta.docfy.document.application.DocumentApplicationService;
import io.github.acarolinebcosta.docfy.document.application.DocumentWorkflowService;
import io.github.acarolinebcosta.docfy.document.domain.Document;
import io.github.acarolinebcosta.docfy.document.domain.DocumentRepository;
import io.github.acarolinebcosta.docfy.document.domain.DocumentStatus;
import io.github.acarolinebcosta.docfy.file.application.DocumentFileService;
import io.github.acarolinebcosta.docfy.file.application.UploadDocumentFileCommand;
import io.github.acarolinebcosta.docfy.file.domain.DocumentFileRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
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
                    "Regulation",
                    SeedWorkflow.DRAFT,
                    null
            ),
            new SeedDocument(
                    "ana@docfy.local",
                    "Information Security Policy",
                    "Security controls for information handled by Docfy.",
                    "Regulation",
                    SeedWorkflow.REJECT_AND_RESUBMIT,
                    null
            ),
            new SeedDocument(
                    "ana@docfy.local",
                    "Software Release Checklist",
                    "Release readiness checks for software delivery.",
                    "Other",
                    SeedWorkflow.APPROVED,
                    null
            ),
            new SeedDocument(
                    "ana@docfy.local",
                    "Operational Procedure",
                    "Standard procedure for recurring operational work.",
                    "Other",
                    SeedWorkflow.ARCHIVED,
                    null
            ),
            new SeedDocument(
                    "joao@docfy.local",
                    "Architecture Guidelines",
                    "Architecture principles for maintainable services.",
                    "Other",
                    SeedWorkflow.DRAFT,
                    null
            ),
            new SeedDocument(
                    "joao@docfy.local",
                    "Incident Response Procedure",
                    "Response steps for operational and security incidents.",
                    "Notice",
                    SeedWorkflow.IN_REVIEW,
                    null
            ),
            new SeedDocument(
                    "joao@docfy.local",
                    "Supplier Agreement",
                    "Approved baseline agreement for external suppliers.",
                    "Contract",
                    SeedWorkflow.APPROVED,
                    null
            ),
            new SeedDocument(
                    "joao@docfy.local",
                    "Meeting Minutes",
                    "Archived record of a completed governance meeting.",
                    "Meeting Minutes",
                    SeedWorkflow.ARCHIVED,
                    null
            ),
            new SeedDocument(
                    "ana@docfy.local",
                    "Ata de Revisão do MVP",
                    "Registro da revisão funcional e das evidências do MVP.",
                    "Meeting Minutes",
                    SeedWorkflow.APPROVED,
                    new SeedAttachment(
                            "ata-revisao-mvp.txt",
                            "text/plain"
                    )
            ),
            new SeedDocument(
                    "ana@docfy.local",
                    "Certificado de Treinamento",
                    "Certificado sintético para demonstração do catálogo.",
                    "Certificate",
                    SeedWorkflow.DRAFT,
                    new SeedAttachment(
                            "certificado-treinamento.txt",
                            "text/plain"
                    )
            ),
            new SeedDocument(
                    "joao@docfy.local",
                    "Comunicado de Manutenção",
                    "Comunicado sobre uma janela fictícia de manutenção.",
                    "Notice",
                    SeedWorkflow.IN_REVIEW,
                    null
            ),
            new SeedDocument(
                    "joao@docfy.local",
                    "Contrato de Prestação de Serviço",
                    "Minuta sintética para validação de anexos e busca.",
                    "Contract",
                    SeedWorkflow.DRAFT,
                    new SeedAttachment(
                            "contrato-prestacao-servico.txt",
                            "text/plain"
                    )
            ),
            new SeedDocument(
                    "joao@docfy.local",
                    "Ofício de Governança",
                    "Comunicação formal fictícia sobre governança documental.",
                    "Official Letter",
                    SeedWorkflow.APPROVED,
                    null
            )
    );

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final DocumentRepository documentRepository;
    private final DocumentApplicationService documentService;
    private final DocumentWorkflowService workflowService;
    private final DocumentFileService fileService;
    private final DocumentFileRepository fileRepository;
    private final PasswordEncoder passwordEncoder;

    public DevelopmentSeedService(
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            DocumentRepository documentRepository,
            DocumentApplicationService documentService,
            DocumentWorkflowService workflowService,
            DocumentFileService fileService,
            DocumentFileRepository fileRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.documentRepository = documentRepository;
        this.documentService = documentService;
        this.workflowService = workflowService;
        this.fileService = fileService;
        this.fileRepository = fileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public boolean initialize(String rawPassword) {
        validatePassword(rawPassword);

        Map<String, User> existingUsers = loadExistingSeedUsers();

        boolean initialized = false;

        if (existingUsers.isEmpty()) {
            existingUsers = createSeedUsers(rawPassword);
            initialized = true;
        } else if (existingUsers.size() != SEED_USERS.size()) {
            throw new IllegalStateException(
                    "Partial development seed detected. "
                            + "Reset the local development data before starting again."
            );
        } else {
            validateExistingSeedUsers(existingUsers, rawPassword);
        }

        return createSeedDocuments(existingUsers) || initialized;
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

    private boolean createSeedDocuments(Map<String, User> users) {
        User manager = users.get("manager@docfy.local");
        boolean initialized = false;

        for (SeedDocument seedDocument : SEED_DOCUMENTS) {
            User owner = users.get(seedDocument.ownerEmail());
            Document existingDocument = documentRepository
                    .findByCreatedByIdAndTitle(
                            owner.getId(),
                            seedDocument.title()
                    )
                    .orElse(null);

            if (existingDocument != null) {
                initialized = ensureAttachment(
                        existingDocument,
                        owner,
                        seedDocument.attachment()
                ) || initialized;
                continue;
            }

            Category category = categoryRepository
                    .findByName(seedDocument.categoryName())
                    .orElseThrow(() -> new IllegalStateException(
                            "Development seed category not found: "
                                    + seedDocument.categoryName()
                    ));

            Document document = documentService.create(
                    new CreateDocumentCommand(
                            seedDocument.title(),
                            seedDocument.description(),
                            category.getId()
                    ),
                    owner
            );

            ensureAttachment(
                    document,
                    owner,
                    seedDocument.attachment()
            );

            applyWorkflow(
                    document,
                    owner,
                    manager,
                    seedDocument.workflow()
            );
            initialized = true;
        }

        return initialized;
    }

    private boolean ensureAttachment(
            Document document,
            User owner,
            SeedAttachment attachment
    ) {
        if (attachment == null
                || fileRepository.existsByDocumentIdAndOriginalFilename(
                        document.getId(),
                        attachment.filename()
                )) {
            return false;
        }

        if (document.getStatus() != DocumentStatus.DRAFT) {
            throw new IllegalStateException(
                    "Incomplete development seed attachment for non-draft document: "
                            + document.getTitle()
            );
        }

        fileService.upload(
                document.getId(),
                new UploadDocumentFileCommand(
                        attachment.filename(),
                        attachment.contentType(),
                        readSeedFile(attachment.filename())
                ),
                owner
        );

        return true;
    }

    private byte[] readSeedFile(String filename) {
        ClassPathResource resource = new ClassPathResource(
                "development-seed/" + filename
        );

        try (java.io.InputStream input = resource.getInputStream()) {
            return input.readAllBytes();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not read development seed file: " + filename,
                    exception
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
            String categoryName,
            SeedWorkflow workflow,
            SeedAttachment attachment
    ) {
    }

    private record SeedAttachment(
            String filename,
            String contentType
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
