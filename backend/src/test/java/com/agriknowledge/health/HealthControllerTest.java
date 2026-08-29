package com.agriknowledge.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HealthControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void healthIsPubliclyReachable() throws Exception {
		mockMvc.perform(get("/api/v1/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.service").value("agriculture-knowledge-api"));
	}

	@Test
	void protectedEndpointsRequireAuthentication() throws Exception {
		// Deliberately not /api/v1/materials: that became public in phase 4, and
		// this assertion silently rotted until the test suite caught it.
		mockMvc.perform(get("/api/v1/users/me"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/api/v1/learning-paths"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void publicReadingDoesNotRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/materials")).andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/exams")).andExpect(status().isOk());
	}

}
