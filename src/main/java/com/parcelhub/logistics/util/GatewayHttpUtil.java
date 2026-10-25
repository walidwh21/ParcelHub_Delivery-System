package com.parcelhub.logistics.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parcelhub.logistics.dto.paymentapiresponse.GatewayValidationResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public final class GatewayHttpUtil {
	private GatewayHttpUtil() {}

	public static GatewayValidationResponse extractValidatorResponse(String response) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		return mapper.readValue(response, GatewayValidationResponse.class);
	}

	public static String getByOpeningJavaUrlConnection(String stringUrl) throws IOException {
		URL url = new URL(stringUrl);
		URLConnection conn = url.openConnection();
		conn.setConnectTimeout(5000);
		conn.setReadTimeout(5000);
		BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		StringBuilder output = new StringBuilder();
		String outputLine;
		while ((outputLine = br.readLine()) != null) {
			output.append(outputLine);
		}
		br.close();
		return output.toString();
	}
}
