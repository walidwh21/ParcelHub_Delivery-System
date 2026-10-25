package com.parcelhub.logistics.service;

import com.parcelhub.logistics.entity.ParcelCategory;
import com.parcelhub.logistics.repository.ParcelCategoryRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ParcelCategoryService {

	private final ParcelCategoryRepository categoryRepository;

	public List<ParcelCategory> getAllCategories() {
		return categoryRepository.findAll();
	}

	public ParcelCategory getCategoryById(UUID categoryId) {
		Optional<ParcelCategory> category = categoryRepository.findById(categoryId);
		return category.orElse(null);
	}

	public ParcelCategory addCategory(ParcelCategory category) {
		return categoryRepository.save(category);
	}

	public ParcelCategory updateCategory(UUID categoryId, ParcelCategory category) {
		if (categoryRepository.existsById(categoryId)) {
			category.setId(categoryId); // Ensure the ID is set correctly for update
			return categoryRepository.save(category);
		}
		return null;
	}

	public void deleteCategory(UUID categoryId) {
		categoryRepository.deleteById(categoryId);
	}
}
