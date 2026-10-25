package com.parcelhub.logistics.dto.paymentapiresponse;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class GatewayValidationResponse {

	private final String status;
	private final String tranDate;
	private final String tranId;
	private final String valId;
	private final String amount;
	private final String storeAmount;
	private final String currency;
	private final String bankTranId;
	private final String cardType;
	private final String cardNo;
	private final String cardIssuer;
	private final String cardBrand;
	private final String cardIssuerCountry;
	private final String cardIssuerCountryCode;
	private final String currencyType;
	private final String currencyAmount;
	private final String currencyRate;
	private final String baseFair;
	private final String valueA;
	private final String valueB;
	private final String valueC;
	private final String valueD;
	private final String emiInstalment;
	private final String emiAmount;
	private final String emiDescription;

	@JsonCreator
	public GatewayValidationResponse(
			@JsonProperty("status") String status,
			@JsonProperty("tran_date") String tranDate,
			@JsonProperty("tran_id") String tranId,
			@JsonProperty("val_id") String valId,
			@JsonProperty("amount") String amount,
			@JsonProperty("store_amount") String storeAmount,
			@JsonProperty("currency") String currency,
			@JsonProperty("bank_tran_id") String bankTranId,
			@JsonProperty("card_type") String cardType,
			@JsonProperty("card_no") String cardNo,
			@JsonProperty("card_issuer") String cardIssuer,
			@JsonProperty("card_brand") String cardBrand,
			@JsonProperty("card_issuer_country") String cardIssuerCountry,
			@JsonProperty("card_issuer_country_code") String cardIssuerCountryCode,
			@JsonProperty("currency_type") String currencyType,
			@JsonProperty("currency_amount") String currencyAmount,
			@JsonProperty("currency_rate") String currencyRate,
			@JsonProperty("base_fair") String baseFair,
			@JsonProperty("value_a") String valueA,
			@JsonProperty("value_b") String valueB,
			@JsonProperty("value_c") String valueC,
			@JsonProperty("value_d") String valueD,
			@JsonProperty("emi_instalment") String emiInstalment,
			@JsonProperty("emi_amount") String emiAmount,
			@JsonProperty("emi_description") String emiDescription) {
		this.status = status;
		this.tranDate = tranDate;
		this.tranId = tranId;
		this.valId = valId;
		this.amount = amount;
		this.storeAmount = storeAmount;
		this.currency = currency;
		this.bankTranId = bankTranId;
		this.cardType = cardType;
		this.cardNo = cardNo;
		this.cardIssuer = cardIssuer;
		this.cardBrand = cardBrand;
		this.cardIssuerCountry = cardIssuerCountry;
		this.cardIssuerCountryCode = cardIssuerCountryCode;
		this.currencyType = currencyType;
		this.currencyAmount = currencyAmount;
		this.currencyRate = currencyRate;
		this.baseFair = baseFair;
		this.valueA = valueA;
		this.valueB = valueB;
		this.valueC = valueC;
		this.valueD = valueD;
		this.emiInstalment = emiInstalment;
		this.emiAmount = emiAmount;
		this.emiDescription = emiDescription;
	}
}
