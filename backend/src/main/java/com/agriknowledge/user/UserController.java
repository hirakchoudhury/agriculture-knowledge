package com.agriknowledge.user;

import com.agriknowledge.auth.dto.UpdateProfileRequest;
import com.agriknowledge.auth.dto.UserResponse;
import com.agriknowledge.auth.jwt.AuthPrincipal;
import com.agriknowledge.common.NotFoundException;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserRepository users;

	public UserController(UserRepository users) {
		this.users = users;
	}

	@GetMapping("/me")
	UserResponse me(@AuthenticationPrincipal AuthPrincipal principal) {
		return UserResponse.from(load(principal));
	}

	@PatchMapping("/me")
	@Transactional
	UserResponse updateMe(@AuthenticationPrincipal AuthPrincipal principal,
			@Valid @RequestBody UpdateProfileRequest request) {
		User user = load(principal);
		if (request.name() != null) {
			user.setName(request.name().trim());
		}
		if (request.avatarUrl() != null) {
			user.setAvatarUrl(request.avatarUrl().isBlank() ? null : request.avatarUrl());
		}
		return UserResponse.from(user);
	}

	/**
	 * The token carries id, email and role, but the profile endpoint returns the
	 * live row — a name changed on another device should show up immediately rather
	 * than waiting for the access token to expire.
	 */
	private User load(AuthPrincipal principal) {
		return users.findById(principal.userId())
				.orElseThrow(() -> NotFoundException.of("Account", principal.userId()));
	}

}
