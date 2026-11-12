package com.gagan.catalog.service.impl;

import com.gagan.catalog.constant.ErrorCodeEnum;
import com.gagan.catalog.dto.CategoryRequest;
import com.gagan.catalog.dto.CategoryResponse;
import com.gagan.catalog.entity.Category;
import com.gagan.catalog.exception.CatalogServiceException;
import com.gagan.catalog.repository.CategoryRepository;
import com.gagan.catalog.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String normalizedName = request.getName().trim();

        if (categoryRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new CatalogServiceException(ErrorCodeEnum.CATEGORY_ALREADY_EXISTS);
        }

        Category category = Category.builder()
                .name(normalizedName)
                .description(request.getDescription())
                .build();

        Category saved = categoryRepository.save(category);
        log.info("Created new catalog category: id=[{}], name=[{}]", saved.getCategoryId(), saved.getName());

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CatalogServiceException(ErrorCodeEnum.CATEGORY_NOT_FOUND));
        return mapToResponse(category);
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .categoryId(category.getCategoryId())
                .name(category.getName())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .build();
    }
}