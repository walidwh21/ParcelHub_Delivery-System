package com.parcelhub.logistics.security;

import com.parcelhub.logistics.entity.Account;
import com.parcelhub.logistics.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/** Loads an {@link Account} by e-mail address and exposes its roles as granted authorities. */
public class AccountDetailsService implements UserDetailsService {

	private final UserRepository accounts;

	public AccountDetailsService(UserRepository accounts) {
		this.accounts = accounts;
	}

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		Account account = accounts.findByEmail(email)
				.orElseThrow(() -> new UsernameNotFoundException("No account registered for e-mail: " + email));

		return User.withUsername(account.getEmail())
				.password(account.getPassword())
				.authorities(account.getRoles().stream()
						.map(role -> new SimpleGrantedAuthority(role.name()))
						.toList())
				.build();
	}
}
