package io.github.acarolinebcosta.docfy.file.domain;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentFileRepository
        extends JpaRepository<DocumentFile, UUID> {

    @EntityGraph(attributePaths = "uploadedBy")
    List<DocumentFile> findByDocumentIdOrderByUploadedAtAscIdAsc(
            UUID documentId
    );

    @EntityGraph(attributePaths = "uploadedBy")
    Optional<DocumentFile> findByIdAndDocumentId(
            UUID id,
            UUID documentId
    );

    boolean existsByDocumentIdAndOriginalFilename(
            UUID documentId,
            String originalFilename
    );
}
