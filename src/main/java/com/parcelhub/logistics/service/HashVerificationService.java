package com.parcelhub.logistics.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;

@Service
public class HashVerificationService {
	public static final String VERIFY_KEY = "verify_key";
	public static final String VERIFY_SIGN = "verify_sign";

	public boolean verifyIPNHash(Map<String, String> requestParameters, String storePassword)
			throws NoSuchAlgorithmException {

		if (!hasRequiredParams(requestParameters)) {
			return false;
		}

		String verifyKey = requestParameters.get(VERIFY_KEY);
		if (verifyKey.isEmpty()) return false;

		TreeMap<String, String> sortedParams = buildSortedParams(requestParameters, verifyKey, storePassword);
		String hashString = buildHashString(sortedParams);
		String generatedHash = md5(hashString);

		return generatedHash.equals(requestParameters.get(VERIFY_SIGN));
	}

	private boolean hasRequiredParams(Map<String, String> params) {
		return params.containsKey(VERIFY_SIGN)
				&& !params.get(VERIFY_SIGN).isEmpty()
				&& params.containsKey(VERIFY_KEY)
				&& !params.get(VERIFY_KEY).isEmpty();
	}

	private TreeMap<String, String> buildSortedParams(Map<String, String> params, String verifyKey, String storePass)
			throws NoSuchAlgorithmException {

		String[] keyList = verifyKey.split(",");
		TreeMap<String, String> sortedMap = new TreeMap<>();
		for (String key : keyList) {
			sortedMap.put(key, params.get(key));
		}

		sortedMap.put("store_passwd", md5(storePass));
		return sortedMap;
	}

	private String buildHashString(TreeMap<String, String> sortedParams) {
		StringBuilder result = new StringBuilder();
		for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
			result.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
		}

		String resultString = result.toString();
		return resultString.substring(0, resultString.length() - 1);
	}

	@SuppressWarnings("squid:S4790")
	private String md5(String s) throws NoSuchAlgorithmException {
		byte[] bytesOfMessage = s.getBytes(StandardCharsets.UTF_8);
		MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] theDigest = md.digest(bytesOfMessage);
		StringBuilder sb = new StringBuilder();
		for (byte b : theDigest) {
			sb.append(Integer.toHexString((b & 0xFF) | 0x100).substring(1, 3));
		}
		return sb.toString();
	}
}
