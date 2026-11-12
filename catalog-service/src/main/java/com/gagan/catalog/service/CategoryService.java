package com.gagan.catalog.service;

import com.gagan.catalog.dto.CategoryRequest;
import com.gagan.catalog.dto.CategoryResponse;

import java.util.List;

public interface CategoryService {
    CategoryResponse createCategory(CategoryRequest request);
    List<CategoryResponse> getAllCategories();
    CategoryResponse getCategoryById(Long categoryId);
}