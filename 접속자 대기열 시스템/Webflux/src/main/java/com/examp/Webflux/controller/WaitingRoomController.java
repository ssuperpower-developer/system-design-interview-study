package com.examp.Webflux.controller;

import com.examp.Webflux.service.UserQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

/**
 * packageName   : com.examp.Webflux.controller
 * Author        : imhyeong-gyu
 * Data          : 2025. 8. 9.
 * Description   :
 */

@Controller
@RequestMapping("/api/v1/waiting-room")
@RequiredArgsConstructor
public class WaitingRoomController {

    private final UserQueueService userQueueService;

    @GetMapping
    Mono<Rendering> waitingRoomPage(@RequestParam(name = "queue", defaultValue = "default") String queue,
                                    @RequestParam(name = "user_id") Long userId,
                                    @RequestParam(name = "redirect_url") String url)
    {
        return userQueueService.isAllowed(queue, userId)
                .filter(allowed-> allowed)
                .flatMap(allowed -> Mono.just(Rendering.redirectTo(url).build()))
                .switchIfEmpty(
                        userQueueService.registerWaitQueue(queue, userId)
                                .onErrorResume(ex -> userQueueService.getRank(queue, userId))
                                                                .map(rank -> Rendering.view("waiting-room.html")
                                        .modelAttribute("number", rank)
                                        .modelAttribute("userId", userId)
                                        .modelAttribute("queue", queue)
                                        .modelAttribute("redirectUrl", url)
                                        .build())
                );
    }

}
