package com.examp.Webflux.controller;

import com.examp.Webflux.dto.AllowUserResponse;
import com.examp.Webflux.dto.RegisterUserResponse;
import com.examp.Webflux.service.UserQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * packageName   : com.examp.Webflux.controller
 * Author        : imhyeong-gyu
 * Data          : 2025. 8. 8.
 * Description   :
 */

@RestController
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
public class UserQueueController {

    private final UserQueueService userQueueService;

    @PostMapping("")
    public Mono<RegisterUserResponse> registerUser(
            @RequestParam(name = "queue", defaultValue = "default") String queue,
            @RequestParam(name = "user_id") Long userId) {
        return userQueueService.registerWaitQueue(queue,userId)
                .map(RegisterUserResponse::new);
    }

    @PostMapping("/allow")
    public Mono<AllowUserResponse> allowUser(
            @RequestParam(name = "queue", defaultValue = "default") String queue,
            @RequestParam(name = "count") Long count
    ) {
        return userQueueService.allowUser(queue, count)
                .map(allowed -> new AllowUserResponse(count, allowed));
    }
}
