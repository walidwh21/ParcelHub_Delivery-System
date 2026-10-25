package com.parcelhub.logistics.security;

import com.parcelhub.logistics.repository.UserRepository;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * Three independent filter chains, one per portal (admin, customer, rider). Each portal has
 * its own login page, remember-me cookie and logout endpoint; they differ only in which
 * URLs are public.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	private static final String ADMIN_LOGIN = "/admin/login";
	private static final String RIDER_LOGIN = "/rider/login";
	private static final String USER_LOGIN = "/auth/login";
	private static final String REMEMBER_ME_PARAM = "remember-me";
	private static final int REMEMBER_ME_SECONDS = (int) Duration.ofDays(30).toSeconds();

	@Value("${parcelhub.security.remember-me.key}")
	private String rememberMeKey;

	/** Describes how one portal handles sign-in and sign-out. */
	private record PortalFlow(
			String loginPage,
			String loginProcessingUrl,
			String successUrl,
			String failureUrl,
			String logoutUrl,
			String logoutSuccessUrl,
			String rememberMeCookie) {}

	@Bean
	public UserDetailsService userDetailsService(UserRepository userRepository) {
		return new AccountDetailsService(userRepository);
	}

	@Bean
	public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
		http.securityMatcher("/admin/**").authorizeHttpRequests(rules -> rules.requestMatchers(ADMIN_LOGIN)
				.permitAll()
				.requestMatchers("/admin/**")
				.hasRole("ADMIN")
				.anyRequest()
				.authenticated());
		return applyPortalFlow(
						http,
						new PortalFlow(
								ADMIN_LOGIN,
								ADMIN_LOGIN,
								"/admin/dashboard",
								"/admin/login?error=true",
								"/admin/logout",
								ADMIN_LOGIN,
								"admin-remember-me"))
				.build();
	}

	@Bean
	public SecurityFilterChain userSecurityFilterChain(HttpSecurity http) throws Exception {
		http.securityMatcher("/user/**", "/auth/**", "/user/logout", "parcel/shipping-cost", "/")
				.authorizeHttpRequests(
						rules -> rules.requestMatchers(USER_LOGIN, "parcel/shipping-cost", "/", "/auth/register")
								.permitAll()
								.requestMatchers("/user/**")
								.hasRole("USER")
								.anyRequest()
								.authenticated());
		return applyPortalFlow(
						http,
						new PortalFlow(
								USER_LOGIN,
								"/user/login",
								"/user/dashboard",
								"/auth/login?error=true",
								"/user/logout",
								"/auth/logout",
								"user-remember-me"))
				.build();
	}

	@Bean
	public SecurityFilterChain riderSecurityFilterChain(HttpSecurity http) throws Exception {
		http.securityMatcher("/rider/**").authorizeHttpRequests(rules -> rules.requestMatchers(
						RIDER_LOGIN, "/rider/register", "/rider/onboarding/**")
				.permitAll()
				.requestMatchers("/rider/**")
				.hasRole("RIDER")
				.anyRequest()
				.authenticated());
		return applyPortalFlow(
						http,
						new PortalFlow(
								RIDER_LOGIN,
								RIDER_LOGIN,
								"/rider/dashboard",
								"/rider/login?error=true",
								"/rider/logout",
								RIDER_LOGIN,
								"rider-remember-me"))
				.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	private HttpSecurity applyPortalFlow(HttpSecurity http, PortalFlow flow) throws Exception {
		return http.formLogin(form -> form.loginPage(flow.loginPage())
						.loginProcessingUrl(flow.loginProcessingUrl())
						.defaultSuccessUrl(flow.successUrl())
						.failureUrl(flow.failureUrl()))
				.rememberMe(remember -> remember.key(rememberMeKey)
						.rememberMeParameter(REMEMBER_ME_PARAM)
						.tokenValiditySeconds(REMEMBER_ME_SECONDS)
						.rememberMeCookieName(flow.rememberMeCookie()))
				.logout(logout -> logout.logoutUrl(flow.logoutUrl())
						.logoutSuccessUrl(flow.logoutSuccessUrl())
						.invalidateHttpSession(true)
						.clearAuthentication(true))
				.csrf(csrf -> csrf.csrfTokenRepository(new CookieCsrfTokenRepository()));
	}
}
