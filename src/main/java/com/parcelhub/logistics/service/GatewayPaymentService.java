package com.parcelhub.logistics.service;

import com.parcelhub.logistics.dto.paymentapiresponse.GatewayInitResponse;
import com.parcelhub.logistics.dto.paymentapiresponse.GatewayValidationResponse;
import com.parcelhub.logistics.dto.request.ParcelBookingRequest;
import com.parcelhub.logistics.entity.Account;
import com.parcelhub.logistics.exception.GatewayInitializationException;
import com.parcelhub.logistics.util.GatewayHttpUtil;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class GatewayPaymentService implements PaymentService {
	private static final String SUCCESS_URL = "/sslcommerz/success";
	private static final String FAILURE_URL = "/sslcommerz/failure";
	private static final String ERROR_URL = "/sslcommerz/cancel";
	private static final String VALIDATION_URL = "/validator/api/validationserverAPI.php";

	private final RestTemplate restTemplate;
	private final HashVerificationService hashVerificationService;

	@Value("${sslcommerz.store-id}")
	private String storeId;

	@Value("${sslcommerz.store-passwd}")
	private String storePasswd;

	@Value("${sslcommerz.init-url}")
	private String paymentInitializationUrl;

	@Value("${sslcommerz.base-url}")
	private String baseUrl;

	@Value("${sslcommerz.validation-url}")
	private String sslczURL;

	@Override
	public String getPaymentUrl(ParcelBookingRequest dto, Account sender) {
		MultiValueMap<String, String> formData = buildFormData(dto, sender);
		HttpEntity<MultiValueMap<String, String>> requestEntity = buildHttpEntity(formData);
		ResponseEntity<GatewayInitResponse> responseEntity = sendPaymentRequest(requestEntity);
		return handlePaymentResponse(responseEntity, dto.getPaymentMethod());
	}

	private MultiValueMap<String, String> buildFormData(ParcelBookingRequest dto, Account sender) {
		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("store_id", storeId);
		formData.add("store_passwd", storePasswd);
		formData.add("total_amount", dto.getPrice().toString());
		formData.add("currency", "BDT");
		formData.add("tran_id", dto.getTransactionId());
		formData.add("success_url", baseUrl + SUCCESS_URL);
		formData.add("fail_url", baseUrl + FAILURE_URL);
		formData.add("cancel_url", baseUrl + ERROR_URL);
		formData.add("cus_name", sender.getFullName());
		formData.add("cus_email", sender.getEmail());
		formData.add("product_name", dto.getCategoryId().toString());
		formData.add("product_category", dto.getCategoryId().toString());
		formData.add("product_profile", "general");
		return formData;
	}

	private HttpEntity<MultiValueMap<String, String>> buildHttpEntity(MultiValueMap<String, String> formData) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		return new HttpEntity<>(formData, headers);
	}

	private ResponseEntity<GatewayInitResponse> sendPaymentRequest(
			HttpEntity<MultiValueMap<String, String>> requestEntity) {
		return restTemplate.postForEntity(paymentInitializationUrl, requestEntity, GatewayInitResponse.class);
	}

	private String handlePaymentResponse(ResponseEntity<GatewayInitResponse> responseEntity, String paymentMethod) {
		if (!HttpStatus.OK.equals(responseEntity.getStatusCode())) {
			throw new GatewayInitializationException("Failed to send payment request");
		}

		GatewayInitResponse body = responseEntity.getBody();
		if (body == null || "FAILED".equalsIgnoreCase(body.getStatus())) {
			throw new GatewayInitializationException(
					"Failed: " + (body != null ? body.getFailedreason() : "No response body"));
		}

		return body.getDesc().stream()
				.filter(desc -> desc.getGw().equalsIgnoreCase(paymentMethod))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("Payment method not found"))
				.getRedirectGatewayURL();
	}

	public boolean orderValidate(
			String merchantTrnxnId,
			String merchantTrnxnAmount,
			String merchantTrnxnCurrency,
			Map<String, String> requestParameters)
			throws IOException, NoSuchAlgorithmException {

		if (!hashVerificationService.verifyIPNHash(requestParameters, storePasswd)) return false;

		GatewayValidationResponse response = getValidatedResponse(requestParameters);
		if (response == null) return false;

		return merchantTrnxnId.equals(response.getTranId())
				&& (Math.abs(Double.parseDouble(merchantTrnxnAmount) - Double.parseDouble(response.getCurrencyAmount()))
						< 1)
				&& merchantTrnxnCurrency.equals(response.getCurrencyType());
	}

	private GatewayValidationResponse getValidatedResponse(Map<String, String> requestParameters) throws IOException {
		String validUrl = buildValidationUrl(requestParameters);
		String json = GatewayHttpUtil.getByOpeningJavaUrlConnection(validUrl);

		if (json.isEmpty()) return null;

		GatewayValidationResponse response = GatewayHttpUtil.extractValidatorResponse(json);
		String status = response.getStatus();
		if (!("VALID".equals(status) || "VALIDATED".equals(status))) return null;

		return response;
	}

	private String buildValidationUrl(Map<String, String> params) {
		String encodedValID = URLEncoder.encode(params.get("val_id"), StandardCharsets.UTF_8);
		String encodedStoreID = URLEncoder.encode(storeId, StandardCharsets.UTF_8);
		String encodedStorePassword = URLEncoder.encode(storePasswd, StandardCharsets.UTF_8);

		return sslczURL + VALIDATION_URL + "?val_id="
				+ encodedValID + "&store_id="
				+ encodedStoreID + "&store_passwd="
				+ encodedStorePassword + "&v=1&format=json";
	}
}
