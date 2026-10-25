package com.parcelhub.logistics.controller;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/files")
@Slf4j
public class FileController {

	@Value("${storage.local.path}")
	private String uploadDir;

	@GetMapping("/{filename:.+}")
	public ResponseEntity<Resource> getFile(@PathVariable String filename) {
		try {
			if (filename.contains("..")
					|| filename.contains("/")
					|| filename.contains("\\")
					|| filename.contains("%")) {
				return ResponseEntity.badRequest().build();
			}

			Path filePath = Paths.get(uploadDir).resolve(filename).normalize();

			// Prevent directory traversal attack
			if (!filePath.startsWith(Paths.get(uploadDir))) {
				return ResponseEntity.badRequest().body(null);
			}

			Resource resource = new UrlResource(filePath.toUri());

			if (!resource.exists() || !resource.isReadable()) {
				return ResponseEntity.notFound().build();
			}

			// Determine content type
			Optional<MediaType> mediaType = MediaTypeFactory.getMediaType(resource);
			return ResponseEntity.ok()
					.contentType(mediaType.orElse(MediaType.APPLICATION_OCTET_STREAM))
					.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
					.body(resource);

		} catch (MalformedURLException e) {
			log.warn("Malformed file URL: " + e.getMessage());
			return ResponseEntity.badRequest().build();
		}
	}
}
