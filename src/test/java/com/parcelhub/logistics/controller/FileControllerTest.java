package com.parcelhub.logistics.controller;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class FileControllerTest {

	private FileController controller;

	@TempDir
	Path tempUploadDir;

	@BeforeEach
	void setUp() throws Exception {
		controller = new FileController();
		// inject uploadDir via reflection
		Field uploadDirField = FileController.class.getDeclaredField("uploadDir");
		uploadDirField.setAccessible(true);
		uploadDirField.set(controller, tempUploadDir.toString());
		// create a sample file
		Files.writeString(tempUploadDir.resolve("sample.txt"), "hello world");
	}

	@Test
	void getFile_whenFileExists_returnsOkWithResource() throws Exception {
		ResponseEntity<Resource> resp = controller.getFile("sample.txt");

		assertEquals(HttpStatus.OK, resp.getStatusCode());
		assertEquals("inline; filename=\"sample.txt\"", resp.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
		assertEquals("text/plain", resp.getHeaders().getContentType().toString());

		try (InputStream is = resp.getBody().getInputStream()) {
			byte[] data = is.readAllBytes();
			assertEquals("hello world", new String(data));
		}
	}

	@Test
	void getFile_whenFileDoesNotExist_returnsNotFound() {
		ResponseEntity<Resource> resp = controller.getFile("no-such.txt");
		assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
	}

	@Test
	void getFile_whenFilenameContainsTraversalChars_returnsBadRequest() {
		assertEquals(HttpStatus.BAD_REQUEST, controller.getFile("../secret.txt").getStatusCode());
		assertEquals(HttpStatus.BAD_REQUEST, controller.getFile("foo/bar.txt").getStatusCode());
		assertEquals(HttpStatus.BAD_REQUEST, controller.getFile("foo\\bar.txt").getStatusCode());
		assertEquals(HttpStatus.BAD_REQUEST, controller.getFile("bad%name.txt").getStatusCode());
	}

	@Test
	void getFile_whenPathNormalizationEscapesUploadDir_returnsBadRequest() throws Exception {
		Path outside = Files.createTempFile("outside", ".txt");
		Files.writeString(outside, "oops");

		String filename = "../" + outside.getFileName().toString();
		assertEquals(HttpStatus.BAD_REQUEST, controller.getFile(filename).getStatusCode());
	}
}
