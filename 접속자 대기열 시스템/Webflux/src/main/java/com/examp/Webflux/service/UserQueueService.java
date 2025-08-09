package com.examp.Webflux.service;

import com.examp.Webflux.exception.ApplicationException;
import com.examp.Webflux.exception.ErrorCode;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.time.Instant;

/**
 * packageName   : com.examp.Webflux.service
 * Author        : imhyeong-gyu
 * Data          : 2025. 8. 8.
 * Description   :
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class UserQueueService {

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    // %s 는 queue를 여러개 운영할 수 있기 때문에 가변적으로 설정한다.
    private final String USER_QUEUE_WAIT_KEY = "users:queue:%s:wait";
    private final String USER_QUEUE_WAIT_KEY_FOR_SCAN = "users:queue:*:wait";
    private final String USER_QUEUE_PROCEED_KEY = "users:queue:%s:proceed";

    @Value("${scheduler.enabled}")
    private Boolean scheduling = false;


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

    //진입이 가능한 상태인지 조회 - queue에 찾고자 하는 UserId가 있다면 현재 반환가능하다고 판단
    public Mono<Boolean> isAllowed(final String queue, final Long userId) {
        return reactiveRedisTemplate.opsForZSet().rank(USER_QUEUE_PROCEED_KEY.formatted(queue), userId.toString())
                .defaultIfEmpty(-1L)
                .map(rank -> rank >= 0);
    }


    //집입 허용
    public Mono<Long> allowUser(final String queue, Long count) {
        return reactiveRedisTemplate.opsForZSet().popMin(USER_QUEUE_WAIT_KEY.formatted(queue), count)
                .flatMap(member -> reactiveRedisTemplate.opsForZSet().add(USER_QUEUE_PROCEED_KEY.formatted(queue), member.getValue(), Instant.now().getEpochSecond()))
                .count();
    }

    public Mono<Long> getRank(final String queue, final Long userId) {
        return reactiveRedisTemplate.opsForZSet().rank(USER_QUEUE_WAIT_KEY.formatted(queue), userId.toString())
                .defaultIfEmpty(-1L)
                .map(rank -> rank >= 0 ? rank + 1 : rank);
    }

    //서버가 시작한 뒤 5초 뒤에 3초 간격으로
    @Scheduled(initialDelay = 5000, fixedDelay = 10000)
    public void scheduleAllowUser(){

        if (!scheduling) {
            log.info("pass scheduling");
            return;
        }
        var maxAllowUserCount = 1L;

        reactiveRedisTemplate.scan(ScanOptions.scanOptions().
                        match(USER_QUEUE_WAIT_KEY_FOR_SCAN)
                        .count(100)
                        .build())
                .map(key -> key.split(":")[2])
                .flatMap(queue -> allowUser(queue, maxAllowUserCount).map(allowed -> Tuples.of(queue, allowed)))
                .doOnNext(tuple -> log.info("Tried %d and allowed %d members of %s queue".formatted(maxAllowUserCount, tuple.getT2(), tuple.getT1())))
                .subscribe();

    }

}
