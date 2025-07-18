from src.constants.constant import DATA_PATH
from src.models.trie import BasicTrie, TrieToHashConverter
from src.utils.utils import trim_word_text

import redis
import os
import time


def check_redis_data_exists(r):
    """Redis에 데이터가 이미 있는지 확인"""
    try:
        # 설정 완료 플래그 확인
        if r.get("trie:setup_complete") == "true":
            # 실제 데이터도 확인 (샘플 체크)
            sample_keys = r.keys("trie:*")
            if len(sample_keys) > 10:  # 최소 10개 키는 있어야 함
                print("✅ Redis data already exists, skipping setup")
                return True
        return False
    except Exception as e:
        print(f"⚠️ Error checking Redis data: {e}")
        return False

def get_suggestions(prefix, limit=10):
    """Redis에서 직접 조회 - 진짜 O(1)"""
    return r.zrevrange(f"trie:{prefix}", 0, limit - 1)

if __name__ == '__main__':
    # Redis 연결
    REDIS_HOST = os.getenv("REDIS_HOST", "localhost")
    FORCE_SETUP = os.getenv("FORCE_REDIS_SETUP", "false").lower() == "true"

    r = None
    max_retries = 15  # 재시도 횟수 증가
    retry_delay = 3  # 대기 시간 증가

    for i in range(max_retries):
        try:
            r = redis.Redis(host=REDIS_HOST, port=6379, db=0, decode_responses=True, socket_connect_timeout=5)
            r.ping()
            print("✅ Redis connected successfully!")
            break
        except redis.exceptions.ConnectionError as e:
            print(f"❌ Redis connection failed (attempt {i + 1}/{max_retries}): {e}")
            time.sleep(retry_delay)

    if not r:
        raise Exception("❌ Could not connect to Redis after multiple retries.")

    # 중복 실행 방지 체크
    if not FORCE_SETUP and check_redis_data_exists(r):
        print("🚀 Redis setup skipped - using existing data")
    else:
        if FORCE_SETUP:
            print("🔄 Force setup mode - rebuilding data")
            # 기존 데이터 삭제
            trie_keys = r.keys("trie:*")
            if trie_keys:
                r.delete(*trie_keys)
                print(f"🧹 Deleted {len(trie_keys)} old keys")

        print("🏗️ Building trie data...")
        start_time = time.time()

        # 기존 트라이 빌드 코드 (변경 없음)
        word_and_frequency = trim_word_text(DATA_PATH / 'raw' / 'count_1w.txt')
        basic_trie = BasicTrie()
        total_words = 0

        for word, frequency in word_and_frequency:
            basic_trie.insert(word, frequency)
            total_words += 1
            # 진행상황 표시
            if total_words % 10000 == 0:
                print(f"   Processed {total_words} words...")

        basic_trie.search('go')
        basic_trie.search('google')

        converter = TrieToHashConverter(basic_trie)
        trie_cache = converter.build_cache()

        print("💾 Storing to Redis...")

        # 기존 Redis 저장 코드 (파이프라인으로 최적화)
        pipe = r.pipeline()
        stored_keys = 0

        for prefix, suggestions in trie_cache.items():
            zadd_data = {}
            for suggestion in suggestions:
                zadd_data[suggestion["word"]] = suggestion["frequency"]
            pipe.zadd(f"trie:{prefix}", zadd_data)
            stored_keys += 1

            # 배치 처리 (1000개씩)
            if stored_keys % 1000 == 0:
                pipe.execute()
                pipe = r.pipeline()
                print(f"   Stored {stored_keys} prefixes...")

        # 남은 데이터 실행
        pipe.execute()

        # 설정 완료 플래그 저장
        r.set("trie:setup_complete", "true")
        r.set("trie:total_keys", stored_keys)
        r.set("trie:total_words", total_words)

        elapsed = time.time() - start_time
        print(f"✅ Setup completed in {elapsed:.2f} seconds")
        print(f"📊 Stored {stored_keys} prefixes for {total_words} words")




    # 기존 테스트 코드 (변경 없음)
    print("🔍 Testing suggestions:")
    test_prefixes = ["go", "goo", "goog", "googl", "google"]
    for prefix in test_prefixes:
        suggestions = get_suggestions(prefix, 5)
        print(f"   {prefix}: {suggestions}")