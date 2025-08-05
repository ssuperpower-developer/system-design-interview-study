package org;

import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.args.GeoUnit;
import redis.clients.jedis.params.GeoSearchParam;
import redis.clients.jedis.resps.GeoRadiusResponse;

import java.util.List;
import java.util.stream.IntStream;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, World!");

        try (var jedisPool = new JedisPool("localhost", 6379)) {
            try (Jedis jedis = jedisPool.getResource()) {
                // string
//                jedis.set("users:300:email", "hi@naver.com");
//                jedis.set("users:300:name", "kim");
//                jedis.set("users:300:age", "25");
//
//                String userEmail = jedis.get("users:300:email");
//                System.out.println(userEmail);
//
//                List<String> userInfo = jedis.mget("users:300:email", "users:300:name", "users:300:age");
//                userInfo.forEach(System.out::println);
//
//                long counter = jedis.incr("counter");
//                System.out.println(counter);
//
//                counter = jedis.incrBy("counter", 10L);
//                System.out.println(counter);
//
//                counter = jedis.decr("counter");
//                System.out.println(counter);
//
//                counter = jedis.decrBy("counter", 10L);
//                System.out.println(counter);
//
//                Pipeline pipeline = jedis.pipelined();
//                pipeline.set("users:400:email", "hi@naver.com");
//                pipeline.set("users:400:name", "kim");
//                pipeline.set("users:400:age", "25");
//                List<Object> objects = pipeline.syncAndReturnAll();
//                objects.forEach(i -> System.out.println(i.toString()));

                // list
                // 1. stack
//                jedis.rpush("stack1", "aaaa");
//                jedis.rpush("stack1", "bbbb");
//                jedis.rpush("stack1", "cccc");
//
//                List<String> stack1 = jedis.lrange("stack1", 0, -1);
//                stack1.forEach(System.out::println);
//
//                while(true) {
//                    List<String> blpop = jedis.blpop(10, "queue:blocking");
//                    if(blpop != null) {
//                        blpop.forEach(System.out::println);
//                    }
//                }

                // set
//                jedis.sadd("users:500:follow", "100", "200", "300");
//                jedis.srem("users:500:follow", "100");
//
//                Set<String> members = jedis.smembers("users:500:follow");
//                members.forEach(System.out::println);
//
//                System.out.println(jedis.sismember("users:500:follow", "200"));
//                System.out.println(jedis.sismember("users:500:follow", "120"));
//
//                System.out.println(jedis.scard("users:500:follow"));
//
//                Set<String> sinter = jedis.sinter("users:500:follow", "users:100:follow");
//                sinter.forEach(System.out::println);

                // hash
//                jedis.hset("users:2:info", "name", "kim");
//                var userInfo = new HashMap<String, String>();
//                userInfo.put("email", "hi@naver.com");
//                userInfo.put("phone", "010-XXXX-XXXX");
//
//                jedis.hset("users:2:info", userInfo);
//
//                jedis.hdel("users:2:info", "phone");
//
//                System.out.println(jedis.hget("users:2:info", "email"));
//                Map<String,String> user2Info = jedis.hgetAll("users:2:info");
//                user2Info.forEach((k, v) -> System.out.println("key:" + k + ",value:" + v));
//
//                jedis.hincrBy("users:2:info", "visits", 1);

                // sorted set
//                var scores = new HashMap<String, Double>();
//                scores.put("user1", 100.0);
//                scores.put("user2", 30.0);
//                scores.put("user3", 50.0);
//                scores.put("user4", 80.0);
//                scores.put("user5", 15.0);
//                jedis.zadd("game2:scores", scores);
//
//                List<String> zrange = jedis.zrange("game2:scores", 0, Long.MAX_VALUE);
//                zrange.forEach(System.out::println);
//
//                List<Tuple> tuples = jedis.zrangeWithScores("game2:scores", 0, Long.MAX_VALUE);
//                tuples.forEach(i -> System.out.printf("%s %f%n", i.getElement(), i.getScore()));
//
//                System.out.println(jedis.zcard("game2:scores"));
//
//                jedis.zincrby("game2:scores", 100.0, "user5");
//
//                tuples = jedis.zrangeWithScores("game2:scores", 0, Long.MAX_VALUE);
//                tuples.forEach(i -> System.out.printf("%s %f%n", i.getElement(), i.getScore()));

                // geo
//                jedis.geoadd("stores2:geo", 127.02985530619755, 37.49911212874, "some1");
//                jedis.geoadd("stores2:geo", 127.0333352287619, 37.491921163986234, "some2");
//
//                Double geodist = jedis.geodist("stores2:geo", "some1", "some2");
//                System.out.println(geodist);
//
//                List<GeoRadiusResponse> radiusResponses1 = jedis.geosearch(
//                        "stores2:geo",
//                        new GeoCoordinate(127.033, 37.495),
//                        500,
//                        GeoUnit.M
//                );
//
//                List<GeoRadiusResponse> radiusResponses2 = jedis.geosearch("stores2:geo",
//                        new GeoSearchParam()
//                                .fromLonLat(new GeoCoordinate(127.033, 37.495))
//                                .byRadius(500, GeoUnit.M)
//                                .withCoord()
//                );
//
//                radiusResponses2.forEach(response -> {
//                    System.out.println("%s %f %f".formatted(
//                            response.getMemberByString(),
//                            response.getCoordinate().getLatitude(),
//                            response.getCoordinate().getLongitude()
//                    ));
//                });
//
//                jedis.unlink("stores2:geo");

                // bitmap
                jedis.setbit("request-somepage-20250805", 100, true);
                jedis.setbit("request-somepage-20250805", 200, true);
                jedis.setbit("request-somepage-20250805", 300, true);

                System.out.println(jedis.getbit("request-somepage-20250805", 100));
                System.out.println(jedis.getbit("request-somepage-20250805", 50));

                System.out.println(jedis.bitcount("request-somepage-20250805"));

                // bitmap vs set
                Pipeline pipeline = jedis.pipelined();
                IntStream.rangeClosed(0, 100000).forEach(i -> {
                    pipeline.sadd("request-somepage-set-20250805", String.valueOf(i), "1");
                    pipeline.setbit("request-somepage-bit-20250805", i, true);

                    if(i == 1000) {
                        pipeline.sync();
                    }

                    pipeline.sync();
                });
            }
        }
    }
}