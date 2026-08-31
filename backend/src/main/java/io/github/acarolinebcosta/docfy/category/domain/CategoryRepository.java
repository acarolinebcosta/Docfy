package io.github.acarolinebcosta.docfy.category.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository
        extends JpaRepository<Category, UUID> {

    List<Category> findAllByOrderByNameAsc();

    Optional<Category> findByName(String name);
}
