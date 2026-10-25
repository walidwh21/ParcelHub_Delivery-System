package com.parcelhub.logistics.service;

import static org.junit.jupiter.api.Assertions.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HashVerificationServiceTest {
	@Mock
	private HashVerificationService hashVerificationService;

	@BeforeEach
	void setUp() {
		hashVerificationService = new HashVerificationService();
	}

	@Test
	void testVerifyIPNHash_Success() throws NoSuchAlgorithmException {
		Map<String, String> requestParams = new HashMap<>();
		String storePassword = "secret123";

		// Simulate verify_key: key1,key2
		requestParams.put("verify_key", "key1,key2");
		requestParams.put("key1", "value1");
		requestParams.put("key2", "value2");

		// Build expected hash
		String storePassMd5 = md5(storePassword);
		String hashString = "key1=value1&key2=value2&store_passwd=" + storePassMd5;
		String expectedHash = md5(hashString);

		requestParams.put("verify_sign", expectedHash);

		boolean result = hashVerificationService.verifyIPNHash(requestParams, storePassword);
		assertTrue(result);
	}

	@Test
	void testVerifyIPNHash_Failure_MissingVerifySign() throws NoSuchAlgorithmException {
		Map<String, String> requestParams = new HashMap<>();
		requestParams.put("verify_key", "key1,key2");
		requestParams.put("key1", "value1");
		requestParams.put("key2", "value2");

		boolean result = hashVerificationService.verifyIPNHash(requestParams, "secret123");
		assertFalse(result);
	}

	@Test
	void testVerifyIPNHash_Failure_EmptyVerifySign() throws NoSuchAlgorithmException {
		Map<String, String> requestParams = new HashMap<>();
		requestParams.put("verify_sign", "");
		requestParams.put("verify_key", "key1,key2");

		boolean result = hashVerificationService.verifyIPNHash(requestParams, "secret123");
		assertFalse(result);
	}

	@Test
	void testVerifyIPNHash_Failure_MissingVerifyKey() throws NoSuchAlgorithmException {
		Map<String, String> requestParams = new HashMap<>();
		requestParams.put("verify_sign", "somesign");

		boolean result = hashVerificationService.verifyIPNHash(requestParams, "secret123");
		assertFalse(result);
	}

	@Test
	void testVerifyIPNHash_Failure_EmptyVerifyKey() throws NoSuchAlgorithmException {
		Map<String, String> requestParams = new HashMap<>();
		requestParams.put("verify_sign", "somesign");
		requestParams.put("verify_key", "");

		boolean result = hashVerificationService.verifyIPNHash(requestParams, "secret123");
		assertFalse(result);
	}

	@Test
	void testVerifyIPNHash_Failure_HashMismatch() throws NoSuchAlgorithmException {
		Map<String, String> requestParams = new HashMap<>();
		requestParams.put("verify_sign", "invalidhash");
		requestParams.put("verify_key", "key1,key2");
		requestParams.put("key1", "value1");
		requestParams.put("key2", "value2");

		boolean result = hashVerificationService.verifyIPNHash(requestParams, "secret123");
		assertFalse(result);
	}

	@Test
	void testMd5_ProducesExpectedLength() throws NoSuchAlgorithmException {
		String hash = callMd5("test");
		assertEquals(32, hash.length());
	}

	// Helper to reuse private md5 logic
	private String md5(String s) throws NoSuchAlgorithmException {
		return callMd5(s);
	}

	// Internal MD5 implementation matching the service for test purposes
	private String callMd5(String s) throws NoSuchAlgorithmException {
		byte[] bytesOfMessage = s.getBytes();
		MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] theDigest = md.digest(bytesOfMessage);
		StringBuilder sb = new StringBuilder();
		for (byte b : theDigest) {
			sb.append(Integer.toHexString((b & 0xFF) | 0x100).substring(1, 3));
		}
		return sb.toString();
	}
}
