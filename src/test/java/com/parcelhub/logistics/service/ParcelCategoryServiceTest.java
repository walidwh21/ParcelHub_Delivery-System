package com.parcelhub.logistics.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.parcelhub.logistics.entity.ParcelCategory;
import com.parcelhub.logistics.repository.ParcelCategoryRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParcelCategoryServiceTest {

	@Mock
	private ParcelCategoryRepository categoryRepository;

	@InjectMocks
	private ParcelCategoryService categoryService;

	private UUID categoryId;
	private ParcelCategory sampleCategory;

	@BeforeEach
	void setUp() {
		categoryId = UUID.randomUUID();
		sampleCategory = new ParcelCategory();
		sampleCategory.setId(categoryId);
		sampleCategory.setCategory("Electronics");
	}

	@Test
	void testGetAllCategories() {
		List<ParcelCategory> categories = Arrays.asList(sampleCategory, new ParcelCategory());
		when(categoryRepository.findAll()).thenReturn(categories);

		List<ParcelCategory> result = categoryService.getAllCategories();

		assertEquals(2, result.size());
		verify(categoryRepository, times(1)).findAll();
	}

	@Test
	void testGetCategoryById_Found() {
		when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(sampleCategory));

		ParcelCategory result = categoryService.getCategoryById(categoryId);

		assertNotNull(result);
		assertEquals(categoryId, result.getId());
		verify(categoryRepository, times(1)).findById(categoryId);
	}

	@Test
	void testGetCategoryById_NotFound() {
		when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

		ParcelCategory result = categoryService.getCategoryById(categoryId);

		assertNull(result);
		verify(categoryRepository, times(1)).findById(categoryId);
	}

	@Test
	void testAddCategory() {
		when(categoryRepository.save(sampleCategory)).thenReturn(sampleCategory);

		ParcelCategory result = categoryService.addCategory(sampleCategory);

		assertNotNull(result);
		assertEquals(categoryId, result.getId());
		verify(categoryRepository, times(1)).save(sampleCategory);
	}

	@Test
	void testUpdateCategory_Found() {
		when(categoryRepository.existsById(categoryId)).thenReturn(true);
		when(categoryRepository.save(sampleCategory)).thenReturn(sampleCategory);

		ParcelCategory result = categoryService.updateCategory(categoryId, sampleCategory);

		assertNotNull(result);
		assertEquals(categoryId, result.getId());
		verify(categoryRepository, times(1)).existsById(categoryId);
		verify(categoryRepository, times(1)).save(sampleCategory);
	}

	@Test
	void testUpdateCategory_NotFound() {
		when(categoryRepository.existsById(categoryId)).thenReturn(false);

		ParcelCategory result = categoryService.updateCategory(categoryId, sampleCategory);

		assertNull(result);
		verify(categoryRepository, times(1)).existsById(categoryId);
		verify(categoryRepository, times(0)).save(sampleCategory);
	}

	@Test
	void testDeleteCategory() {
		doNothing().when(categoryRepository).deleteById(categoryId);

		categoryService.deleteCategory(categoryId);

		verify(categoryRepository, times(1)).deleteById(categoryId);
	}
}
