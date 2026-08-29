package io.github.acarolinebcosta.docfy.document.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentRepository
        extends JpaRepository<Document, UUID> {

    @Override
    @EntityGraph(attributePaths = "createdBy")
    Page<Document> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "createdBy")
    Page<Document> findByCreatedByIdOrStatus(
            UUID createdById,
            DocumentStatus status,
            Pageable pageable
    );
}
