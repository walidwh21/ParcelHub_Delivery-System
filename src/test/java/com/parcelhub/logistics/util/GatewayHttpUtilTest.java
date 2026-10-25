package com.parcelhub.logistics.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.parcelhub.logistics.dto.paymentapiresponse.GatewayValidationResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class GatewayHttpUtilTest {

	@InjectMocks
	private GatewayHttpUtil sslCommerzUtil;

	@Mock
	private URL mockUrl;

	@Mock
	private URLConnection mockConnection;

	@Mock
	private InputStream mockInputStream;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void testExtractValidatorResponse_Success() throws IOException {
		String jsonResponse = "{\"status\":\"VALID\",\"tran_date\":\"2025-04-09\",\"tran_id\":\"12345\","
				+ "\"val_id\":\"67890\",\"amount\":\"1000\"}";

		GatewayValidationResponse response = GatewayHttpUtil.extractValidatorResponse(jsonResponse);

		assertNotNull(response);
		assertEquals("VALID", response.getStatus());
		assertEquals("2025-04-09", response.getTranDate());
		assertEquals("12345", response.getTranId());
		assertEquals("67890", response.getValId());
		assertEquals("1000", response.getAmount());
	}

	@Test
	void testExtractValidatorResponse_WithUnknownProperties() throws IOException {
		String jsonResponse = "{\"status\":\"VALID\",\"tran_date\":\"2025-04-09\",\"unknown_field\":\"test\"}";

		GatewayValidationResponse response = GatewayHttpUtil.extractValidatorResponse(jsonResponse);

		assertNotNull(response);
		assertEquals("VALID", response.getStatus());
		assertEquals("2025-04-09", response.getTranDate());
		assertNull(response.getValueA());
	}

	@Test
	void testExtractValidatorResponse_InvalidJson() {
		String invalidJson = "not a json string";

		assertThrows(IOException.class, () -> {
			GatewayHttpUtil.extractValidatorResponse(invalidJson);
		});
	}

	@Test
	void testGetByOpeningJavaUrlConnection_Success() throws IOException {
		String testUrl = "http://test.com";
		String expectedResponse = "test response";

		when(mockUrl.openConnection()).thenReturn(mockConnection);
		when(mockConnection.getInputStream()).thenReturn(mockInputStream);

		try (MockedConstruction<BufferedReader> mocked =
				Mockito.mockConstruction(BufferedReader.class, (mock, context) -> {
					when(mock.readLine()).thenReturn(expectedResponse).thenReturn(null);
				})) {

			try (MockedConstruction<URL> mockedUrl = Mockito.mockConstruction(
					URL.class, (mock, context) -> when(mock.openConnection()).thenReturn(mockConnection))) {

				String result = GatewayHttpUtil.getByOpeningJavaUrlConnection(testUrl);

				assertEquals(expectedResponse, result);
				verify(mockConnection).setConnectTimeout(5000);
				verify(mockConnection).setReadTimeout(5000);
				verify(mockConnection).getInputStream();
			}
		}
	}

	@Test
	void testGetByOpeningJavaUrlConnection_MultipleLines() throws IOException {
		String testUrl = "http://test.com";
		String line1 = "line1";
		String line2 = "line2";
		String expectedResponse = line1 + line2;

		when(mockUrl.openConnection()).thenReturn(mockConnection);
		when(mockConnection.getInputStream()).thenReturn(mockInputStream);

		try (MockedConstruction<BufferedReader> mocked =
				Mockito.mockConstruction(BufferedReader.class, (mock, context) -> {
					when(mock.readLine()).thenReturn(line1).thenReturn(line2).thenReturn(null);
				})) {
			try (MockedConstruction<URL> mockedUrl = Mockito.mockConstruction(
					URL.class, (mock, context) -> when(mock.openConnection()).thenReturn(mockConnection))) {

				String result = GatewayHttpUtil.getByOpeningJavaUrlConnection(testUrl);

				assertEquals(expectedResponse, result);
			}
		}
	}

	@Test
	void testGetByOpeningJavaUrlConnection_IOException() {
		String testUrl = "http://test.com";

		try (MockedConstruction<URL> mockedUrl = Mockito.mockConstruction(URL.class, (mock, context) -> {
			when(mock.openConnection()).thenThrow(new IOException("Connection failed"));
		})) {

			assertThrows(IOException.class, () -> {
				GatewayHttpUtil.getByOpeningJavaUrlConnection(testUrl);
			});
		}
	}
}
