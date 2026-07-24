package com.devinolabs.uap.identity.infrastructure.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.devinolabs.uap.TestcontainersConfiguration;

@SpringBootTest(properties = {
		"uap.identity.http.rate-limit.capacity=2",
		"uap.identity.http.rate-limit.window=10m"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class IdentityRateLimitHttpTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void rateLimitReturnsTooManyRequests() throws Exception {
		String body = """
				{"email":"%s","password":"SecurePass123!"}
				""".formatted("rate." + UUID.randomUUID() + "@example.com");

		mockMvc.perform(post("/api/v1/identity/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/identity/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"SecurePass123!"}
								""".formatted("rate." + UUID.randomUUID() + "@example.com")))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/identity/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"SecurePass123!"}
								""".formatted("rate." + UUID.randomUUID() + "@example.com")))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.code").value("RATE_LIMITED"));
	}

}
