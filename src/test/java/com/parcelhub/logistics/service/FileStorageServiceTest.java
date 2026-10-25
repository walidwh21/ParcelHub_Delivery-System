package com.parcelhub.logistics.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.parcelhub.logistics.exception.FileStorageException;
import com.parcelhub.logistics.exception.FileValidationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {
	@Mock
	private FileStorageService fileStorageService;

	private final String tempStoragePath = "uploads";

	@BeforeEach
	void setUp() {
		fileStorageService = new FileStorageService();
		ReflectionTestUtils.setField(fileStorageService, "localUploadPath", tempStoragePath);
	}

	@Test
	void testStoreValidFileSuccessfully() throws IOException {
		byte[] content = IOUtils.toByteArray(getClass().getResourceAsStream("/test-files/sample.pdf"));
		MockMultipartFile file = new MockMultipartFile("file", "sample.pdf", "application/pdf", content);

		String storedFileName = fileStorageService.storeFile(file);

		Path storedPath = Paths.get(tempStoragePath, storedFileName);
		assertTrue(Files.exists(storedPath));
		assertTrue(storedFileName.endsWith(".pdf"));

		// Cleanup
		Files.deleteIfExists(storedPath);
	}

	@Test
	void testEmptyFileThrowsException() {
		MockMultipartFile file = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

		FileValidationException ex = assertThrows(FileValidationException.class, () -> {
			fileStorageService.storeFile(file);
		});
		assertEquals("File is empty", ex.getMessage());
	}

	@Test
	void testOversizedFileThrowsException() {
		byte[] largeContent = new byte[6 * 1024 * 1024]; // 6MB
		MockMultipartFile file = new MockMultipartFile("file", "large.pdf", "application/pdf", largeContent);

		FileValidationException ex = assertThrows(FileValidationException.class, () -> {
			fileStorageService.storeFile(file);
		});
		assertTrue(ex.getMessage().contains("File size exceeds"));
	}

	@Test
	void testInvalidFileNameThrowsException() {
		MockMultipartFile file =
				new MockMultipartFile("file", "../malicious.pdf", "application/pdf", "data".getBytes());

		FileValidationException ex = assertThrows(FileValidationException.class, () -> {
			fileStorageService.storeFile(file);
		});
		assertEquals("Invalid file name", ex.getMessage());
	}

	@Test
	void testInvalidExtensionThrowsException() {
		MockMultipartFile file =
				new MockMultipartFile("file", "script.exe", "application/octet-stream", "data".getBytes());

		FileValidationException ex = assertThrows(FileValidationException.class, () -> {
			fileStorageService.storeFile(file);
		});
		assertTrue(ex.getMessage().contains("Unsupported file extension"));
	}

	@Test
	void testInvalidMimeTypeThrowsException() {
		MockMultipartFile file = new MockMultipartFile("file", "image.jpg", "text/plain", "fake image data".getBytes());

		FileValidationException ex = assertThrows(FileValidationException.class, () -> {
			fileStorageService.storeFile(file);
		});
		assertTrue(ex.getMessage().contains("Invalid file type"));
	}

	@Test
	void testIOExceptionThrowsStorageException() throws IOException {
		// Create a real file to avoid issues with probeContentType
		byte[] dummyBytes = "dummy".getBytes();
		MockMultipartFile realFile = new MockMultipartFile("file", "test.png", "image/png", dummyBytes);

		// Spy the real file to mock getBytes() only
		MultipartFile spyFile = spy(realFile);
		doThrow(new IOException("Disk error")).when(spyFile).getBytes();

		FileStorageException ex = assertThrows(FileStorageException.class, () -> {
			fileStorageService.storeFile(spyFile);
		});

		assertTrue(ex.getMessage().contains("Failed to store file"));
	}

	@Test
	void testNullOriginalFileNameThrowsException() {
		MultipartFile file = mock(MultipartFile.class);
		when(file.isEmpty()).thenReturn(false);
		when(file.getSize()).thenReturn(1024L);
		when(file.getOriginalFilename()).thenReturn(null);

		FileValidationException ex = assertThrows(FileValidationException.class, () -> {
			fileStorageService.storeFile(file);
		});

		assertEquals("Invalid file name", ex.getMessage());
	}

	@Test
	void testNullMimeTypeThrowsException() {
		MultipartFile file = mock(MultipartFile.class);
		when(file.isEmpty()).thenReturn(false);
		when(file.getSize()).thenReturn(1024L);
		when(file.getOriginalFilename()).thenReturn("file.pdf");
		when(file.getContentType()).thenReturn(null);

		FileValidationException ex = assertThrows(FileValidationException.class, () -> {
			fileStorageService.storeFile(file);
		});

		assertTrue(ex.getMessage().contains("Invalid file type"));
	}

	@Test
	void testSanitizeFileNameWithNullThrowsIllegalArgumentException() {
		assertThrows(IllegalArgumentException.class, () -> {
			ReflectionTestUtils.invokeMethod(fileStorageService, "sanitizeFileName", (Object) null);
		});
	}
}
