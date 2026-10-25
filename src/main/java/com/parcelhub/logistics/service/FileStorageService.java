package com.parcelhub.logistics.service;

import com.parcelhub.logistics.exception.FileStorageException;
import com.parcelhub.logistics.exception.FileValidationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@Service
public class FileStorageService {

	@Value("${storage.local.path}")
	private String localUploadPath;

	private static final long MAX_FILE_SIZE = (long) 5 * 1024 * 1024;

	// Allowed file extensions
	private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "pdf");

	// Allowed MIME types
	private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList("image/jpeg", "image/png", "application/pdf");

	private void validateFile(MultipartFile file) {
		if (file.isEmpty()) {
			throw new FileValidationException("File is empty");
		}

		if (file.getSize() > MAX_FILE_SIZE) {
			throw new FileValidationException("File size exceeds the maximum limit of 5MB");
		}

		String originalFileName = file.getOriginalFilename();
		if (originalFileName == null || originalFileName.contains("..")) {
			throw new FileValidationException("Invalid file name");
		}

		String extension = FilenameUtils.getExtension(originalFileName);
		if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
			throw new FileValidationException("Unsupported file extension: " + extension);
		}

		// Get MIME type directly from the MultipartFile
		String mimeType = file.getContentType();

		if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType)) {
			throw new FileValidationException("Invalid file type: " + mimeType);
		}
	}

	private String sanitizeFileName(String fileName) {
		if (fileName == null) {
			throw new IllegalArgumentException("Filename cannot be null");
		}
		return fileName.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
	}

	public String storeFile(MultipartFile file) {
		try {
			validateFile(file);

			String uniqueFileName = UUID.randomUUID() + "_" + sanitizeFileName(file.getOriginalFilename());

			// Combine and normalize the path
			Path targetDir = Paths.get(localUploadPath).toAbsolutePath().normalize();
			Path filePath = targetDir.resolve(uniqueFileName).normalize();

			// Ensure that the resulting path is still within the target directory
			if (!filePath.startsWith(targetDir)) {
				throw new FileStorageException("Invalid file path: potential path traversal attack");
			}

			Files.createDirectories(filePath.getParent());
			Files.write(filePath, file.getBytes());

			return uniqueFileName;

		} catch (IOException e) {
			throw new FileStorageException("Failed to store file locally", e);
		}
	}
}
