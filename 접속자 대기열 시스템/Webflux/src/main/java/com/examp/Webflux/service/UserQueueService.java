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
    private final String USER_QUEUE_WAIT_KEY = "users:queue:%s:wait";
    private final String USER_QUEUE_PROCEED_KEY = "users:queue:%s:proceed";


    public Mono<Long> registerWaitQueue(final String queue, final Long userId) {
        // unix timestamp
        long unixTimeStamp = Instant.now().getEpochSecond();
        return reactiveRedisTemplate.opsForZSet().add(USER_QUEUE_WAIT_KEY.formatted(queue), userId.toString(), unixTimeStamp)
                .flatMap(added -> {
                    if (!added) {
                        return Mono.error(new ApplicationException(ErrorCode.QUEUE_ALREADY_REGISTERED));
                    }
                    return reactiveRedisTemplate.opsForZSet().rank(USER_QUEUE_WAIT_KEY.formatted(queue), userId.toString())
                            .map(rank -> rank >= 0 ? rank + 1 : 0);
                });
    }

    //진입이 가능한 상태인지 조회


    //집입 허용
    public Mono<Long> allowUser(final String queue, Long count) {
        return reactiveRedisTemplate.opsForZSet().popMin(USER_QUEUE_WAIT_KEY.formatted(queue), count)
                .flatMap(member -> reactiveRedisTemplate.opsForZSet().add(USER_QUEUE_PROCEED_KEY.formatted(queue), member.getValue(), Instant.now().getEpochSecond()))
                .count();
    }


}
