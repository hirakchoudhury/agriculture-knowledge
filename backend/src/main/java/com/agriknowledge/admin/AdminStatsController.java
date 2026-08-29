package com.agriknowledge.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/stats")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatsController {

	private final AdminStatsService stats;

	public AdminStatsController(AdminStatsService stats) {
		this.stats = stats;
	}

	@GetMapping
	AdminStats get() {
		return stats.collect();
	}

}
