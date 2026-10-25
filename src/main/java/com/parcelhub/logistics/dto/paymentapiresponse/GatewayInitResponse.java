package com.parcelhub.logistics.dto.paymentapiresponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GatewayInitResponse {
	private String status;
	private String failedreason;
	private String sessionkey;
	private Gw gw;
	private String redirectGatewayURL;
	private String directPaymentURLBank;
	private String directPaymentURLCard;
	private String directPaymentURL;
	private String redirectGatewayURLFailed;

	@JsonProperty("GatewayPageURL")
	private String gatewayPageURL;

	private String storeBanner;
	private String storeLogo;
	private List<Desc> desc;

	@JsonProperty("is_direct_pay_enable")
	private String isDirectPayEnable;

	@Setter
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Gw {
		private String visa;
		private String master;
		private String amex;
		private String othercards;
		private String internetbanking;
		private String mobilebanking;
	}

	@Setter
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Desc {
		private String name;
		private String type;
		private String logo;
		private String gw;

		@JsonProperty("r_flag")
		private String rFlag;

		private String redirectGatewayURL;
	}
}
