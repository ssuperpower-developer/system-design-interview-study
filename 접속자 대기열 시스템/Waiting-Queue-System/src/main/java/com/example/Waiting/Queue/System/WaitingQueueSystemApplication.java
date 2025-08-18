package com.example.Waiting.Queue.System;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@SpringBootApplication
@Controller
public class WaitingQueueSystemApplication {

	RestTemplate restTemplate = new RestTemplate();

	public static void main(String[] args) {
		SpringApplication.run(WaitingQueueSystemApplication.class, args);
	}

	@GetMapping("/")
	public String index(@RequestParam(name = "queue", defaultValue = "default") String queue,
						@RequestParam(name = "user_id") Long userId,
						HttpServletRequest request  // 쿠키 읽기 위해 추가
	) {
		// 1. 현재 요청에서 쿠키 확인
		String cookieName = "user-queue-%s-%d-token".formatted(queue,userId);
		String token = extractTokenFromCookies(request, cookieName);

		// 2. 토큰이 없으면 바로 대기열로 리다이렉트
		if (token == null) {
			return "redirect:http://127.0.0.1:9010/api/v1/waiting-room?user_id=%d&redirect_url=%s"
					.formatted(userId, "http://127.0.0.1:9000?user_id=%d".formatted(userId));
		}

		// 3. 토큰이 있으면 allowed 체크 (쿠키 포함해서 전달)
		var uri = UriComponentsBuilder
				.fromUriString("http://127.0.0.1:9010")
				.path("/api/v1/queue/allowed")
				.queryParam("queue", queue)
				.queryParam("user_id", userId)
				.encode()
				.build()
				.toUri();

		// HttpHeaders에 쿠키 설정
		HttpHeaders headers = new HttpHeaders();
		headers.set("Cookie", cookieName + "=" + token);
		HttpEntity<?> entity = new HttpEntity<>(headers);

		ResponseEntity<Boolean> response = restTemplate.exchange(uri, HttpMethod.GET, entity, Boolean.class);

		if (response.getBody() == null || !response.getBody()) {
			return "redirect:http://127.0.0.1:9010/api/v1/waiting-room?user_id=%d&redirect_url=%s"
					.formatted(userId, "http://127.0.0.1:9000?user_id=%d".formatted(userId));
		}

		return "index";
	}

	private String extractTokenFromCookies(HttpServletRequest request, String cookieName) {
		if (request.getCookies() != null) {
			for (Cookie cookie : request.getCookies()) {
				if (cookieName.equals(cookie.getName())) {
					return cookie.getValue();
				}
			}
		}
		return null;
	}
}