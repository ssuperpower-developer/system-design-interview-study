package com.examp.Webflux.service;

import com.examp.Webflux.exception.ApplicationException;
import com.examp.Webflux.exception.ErrorCode;
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

    // %s 는 queue를 여러개 운영할 수 있기 때문에 가변적으로 설정한다.
    private final String USER_WAIT_QUEUE_KEY = "users:queue:%s:wait";


    public Mono<Long> registerWaitQueue(final String queue, final Long userId) {
        // unix timestamp
        long unixTimeStamp = Instant.now().getEpochSecond();
        return reactiveRedisTemplate.opsForZSet().add(USER_WAIT_QUEUE_KEY.formatted(queue), userId.toString(), unixTimeStamp)
                .flatMap(added -> {
                    if (!added) {
                        return Mono.error(new ApplicationException(ErrorCode.QUEUE_ALREADY_REGISTERED));
                    }
                    return reactiveRedisTemplate.opsForZSet().rank(USER_WAIT_QUEUE_KEY.formatted(queue), userId.toString())
                            .map(rank -> rank >= 0 ? rank + 1 : 0);
                });
    }

}
