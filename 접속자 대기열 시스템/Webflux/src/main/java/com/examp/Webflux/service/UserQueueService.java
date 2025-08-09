package com.examp.Webflux.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * packageName   : com.examp.Webflux.service
 * Author        : imhyeong-gyu
 * Data          : 2025. 8. 8.
 * Description   :
 */
@Service
@RequiredArgsConstructor
public class UserQueueService {

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    public Mono<Long> registerWaitQueue(final Long userId){
        //redis sortedset
        // -key : userID
        // - value: unix timestamp

        var unixTimeStamp = Instant.now().getEpochSecond();
        return reactiveRedisTemplate.opsForZSet().add("user-queue", userId.toString(), unixTimeStamp)
                .filter(i -> i)
                .switchIfEmpty(Mono.error(new Exception("already registered user")))
                .flatMap(i -> reactiveRedisTemplate.opsForZSet().rank("user-queue", userId.toString()))
                .map(i -> i >= 0 ? i + 1 : 0);

    }

}
