package io.github.acarolinebcosta.docfy.document.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;
import java.util.Optional;

public interface DocumentRepository
        extends JpaRepository<Document, UUID> {

    @Override
    @EntityGraph(attributePaths = {"createdBy", "category"})
    Page<Document> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"createdBy", "category"})
    Optional<Document> findById(UUID id);

    @EntityGraph(attributePaths = {"createdBy", "category"})
    Optional<Document> findByCreatedByIdAndTitle(
            UUID createdById,
            String title
    );

    @EntityGraph(attributePaths = {"createdBy", "category"})
    Page<Document> findByCreatedByIdOrStatus(
            UUID createdById,
            DocumentStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"createdBy", "category"})
    @Query("""
            SELECT document
            FROM Document document
            WHERE (
                :canViewAll = true
                OR document.createdBy.id = :viewerId
                OR document.status = :approvedStatus
            )
            AND (
                :searchPattern IS NULL
                OR LOWER(document.title) LIKE :searchPattern
                OR LOWER(document.documentCode) LIKE :searchPattern
            )
            AND (
                :categoryId IS NULL
                OR document.category.id = :categoryId
            )
            AND (
                :status IS NULL
                OR document.status = :status
            )
            """)
    Page<Document> findVisibleByCriteria(
            @Param("canViewAll") boolean canViewAll,
            @Param("viewerId") UUID viewerId,
            @Param("approvedStatus") DocumentStatus approvedStatus,
            @Param("searchPattern") String searchPattern,
            @Param("categoryId") UUID categoryId,
            @Param("status") DocumentStatus status,
            Pageable pageable
    );
}
