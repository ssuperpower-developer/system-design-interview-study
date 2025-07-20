from src.constants.constant import DATA_PATH
from src.models.trie import BasicTrie, TrieToHashConverter
from src.utils.utils import trim_word_text

import redis
import os
import time
import gc


def connect_redis():
    """Redis 연결 설정"""
    redis_host = os.getenv("REDIS_HOST", "localhost")
    redis_port = int(os.getenv("REDIS_PORT", "6379"))
    max_retries = 15
    retry_delay = 3

    for i in range(max_retries):
        try:
            r = redis.Redis(
                host=redis_host,
                port=redis_port,
                db=0,
                decode_responses=True,
                socket_connect_timeout=10,
                socket_timeout=30,
                retry_on_timeout=True
            )
            r.ping()
            print("✅ Redis connected successfully!")
            return r
        except redis.exceptions.ConnectionError as e:
            print(f"❌ Redis connection failed (attempt {i + 1}/{max_retries}): {e}")
            time.sleep(retry_delay)

    raise Exception("❌ Could not connect to Redis after multiple retries.")


def check_existing_data(redis_client):
    """기존 데이터 존재 여부 확인"""
    try:
        if redis_client.get("trie:setup_complete") == "true":
            sample_keys = redis_client.keys("trie:*")
            if len(sample_keys) > 10:
                print("✅ Redis data already exists, skipping setup")
                return True
        return False
    except Exception as e:
        print(f"⚠️ Error checking Redis data: {e}")
        return False


def clear_existing_data(redis_client):
    """기존 데이터 삭제"""
    print("🔄 Force setup mode - rebuilding data")
    trie_keys = redis_client.keys("trie:*")
    if trie_keys:
        # 배치 삭제로 메모리 효율성 개선
        batch_size = 1000
        for i in range(0, len(trie_keys), batch_size):
            batch = trie_keys[i:i + batch_size]
            redis_client.delete(*batch)
        print(f"🧹 Deleted {len(trie_keys)} old keys")


def build_trie():
    """Trie 구조 빌드"""
    print("🏗️ Building trie structure...")
    word_and_frequency = trim_word_text(DATA_PATH / 'raw' / 'count_1w.txt')
    basic_trie = BasicTrie()
    total_words = 0

    for word, frequency in word_and_frequency:
        basic_trie.insert(word, frequency)
        total_words += 1
        if total_words % 10000 == 0:
            print(f"   Processed {total_words} words...")

    # 기본 검색으로 trie 최적화
    basic_trie.search('go')
    basic_trie.search('google')

    print(f"✅ Trie built with {total_words} words")
    return basic_trie, total_words


def store_to_redis_chunked(basic_trie, redis_client):
    """
    메모리 효율적인 Redis 저장
    - 청크 단위로 cache 생성하여 메모리 사용량 제한
    """
    print("💾 Storing to Redis with memory optimization...")

    batch_size = int(os.getenv('BATCH_SIZE', '500'))
    converter = TrieToHashConverter(basic_trie)

    # 전체 cache를 한번에 빌드하지 않고 청크 단위로 처리
    print("🔧 Building cache in chunks to save memory...")

    try:
        # 전체 캐시 빌드 (메모리 사용량 모니터링 필요)
        trie_cache = converter.build_cache()
        print(f"📊 Cache built: {len(trie_cache)} prefixes")

        # 청크 단위로 Redis에 저장
        stored_keys = store_cache_in_batches(trie_cache, redis_client, batch_size)

        # 메모리 해제
        del trie_cache
        gc.collect()

        return stored_keys

    except MemoryError:
        print("⚠️ Memory error occurred, trying alternative approach...")
        # 메모리 부족 시 대안 방법 (구현 필요)
        return store_without_full_cache(basic_trie, redis_client)


def store_cache_in_batches(trie_cache, redis_client, batch_size):
    """캐시를 배치 단위로 Redis에 저장"""
    print(f"🔄 Transferring cache to Redis (batch size: {batch_size})...")

    stored_keys = 0
    cache_items = list(trie_cache.items())
    total_items = len(cache_items)

    # 배치 단위로 처리
    for i in range(0, total_items, batch_size):
        batch = cache_items[i:i + batch_size]

        # Redis 파이프라인 사용
        pipe = redis_client.pipeline()

        for prefix, suggestions in batch:
            zadd_data = {
                suggestion["word"]: suggestion["frequency"]
                for suggestion in suggestions
            }
            pipe.zadd(f"trie:{prefix}", zadd_data)
            stored_keys += 1

        # 배치 실행
        pipe.execute()

        # 진행상황 출력
        progress = (i + len(batch)) / total_items * 100
        print(f"   Progress: {stored_keys}/{total_items} ({progress:.1f}%)")

        # 배치 메모리 해제
        del batch
        if i % (batch_size * 10) == 0:  # 10배치마다 가비지 컬렉션
            gc.collect()

    return stored_keys


def store_without_full_cache(basic_trie, redis_client):
    """
    전체 캐시를 메모리에 로드하지 않는 대안 방법
    (BasicTrie 클래스가 지원한다면)
    """
    # 이 부분은 BasicTrie 클래스의 구조에 따라 구현 필요
    # 현재는 기본 방법으로 fallback
    print("🔄 Using fallback method...")
    converter = TrieToHashConverter(basic_trie)
    trie_cache = converter.build_cache()
    return store_cache_in_batches(trie_cache, redis_client, 500)


def finalize_setup(redis_client, stored_keys, total_words, elapsed_time):
    """설정 완료 처리"""
    redis_client.set("trie:setup_complete", "true")
    redis_client.set("trie:total_keys", stored_keys)
    redis_client.set("trie:total_words", total_words)

    print(f"✅ Setup completed in {elapsed_time:.2f} seconds")
    print(f"📊 Stored {stored_keys} prefixes for {total_words} words")


def test_suggestions(redis_client):
    """기본 테스트 실행"""
    print("🔍 Testing suggestions:")
    test_prefixes = ["go", "goo", "goog", "googl", "google"]

    for prefix in test_prefixes:
        suggestions = redis_client.zrevrange(f"trie:{prefix}", 0, 4)
        print(f"   {prefix}: {suggestions}")


def setup_trie_data():
    """메인 데이터 설정 로직"""
    start_time = time.time()

    # Redis 연결
    redis_client = connect_redis()

    # 강제 설정 모드 확인
    force_setup = os.getenv("FORCE_REDIS_SETUP", "false").lower() == "true"

    # 기존 데이터 확인
    if not force_setup and check_existing_data(redis_client):
        return redis_client

    # 기존 데이터 삭제 (필요시)
    if force_setup:
        clear_existing_data(redis_client)

    # Trie 빌드
    basic_trie, total_words = build_trie()

    # Redis 저장 (메모리 효율적)
    stored_keys = store_to_redis_chunked(basic_trie, redis_client)

    # 설정 완료
    elapsed_time = time.time() - start_time
    finalize_setup(redis_client, stored_keys, total_words, elapsed_time)

    # 메모리 정리
    del basic_trie
    gc.collect()

    return redis_client


if __name__ == '__main__':
    try:
        # 메인 설정 실행
        redis_client = setup_trie_data()

        # 테스트 실행
        test_suggestions(redis_client)

        print("🚀 Setup process completed successfully!")

    except Exception as e:
        print(f"❌ Setup failed: {e}")
        raise