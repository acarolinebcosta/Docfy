package io.github.acarolinebcosta.docfy.category.api;

import io.github.acarolinebcosta.docfy.category.domain.CategoryRepository;
import io.github.acarolinebcosta.docfy.shared.openapi.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class CategoryController {

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    @Operation(summary = "List the document category catalogue")
    public ResponseEntity<List<CategoryResponse>> list() {
        List<CategoryResponse> categories = categoryRepository
                .findAllByOrderByNameAsc()
                .stream()
                .map(CategoryResponse::from)
                .toList();

        return ResponseEntity.ok(categories);
    }
}
