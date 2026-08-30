package io.github.acarolinebcosta.docfy.audit.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentAuditEventRepository
        extends JpaRepository<DocumentAuditEvent, UUID> {

    List<DocumentAuditEvent>
            findByDocumentIdOrderByOccurredAtAscIdAsc(
                    UUID documentId
            );
}