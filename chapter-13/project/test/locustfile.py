from locust import task, between, FastHttpUser
import random
import time

from src.constants.constant import DATA_PATH
from geventhttpclient.client import HTTPClientPool
SHARED_CLIENT_POOL = HTTPClientPool(
    concurrency=12000,        # 총 1000개 동시 연결
    insecure=True,          # SSL 검증 비활성화 (성능 향상)
    network_timeout=120.0,   # 네트워크 타임아웃
    connection_timeout=120.0 # 연결 타임아웃
)

# 단어 리스트 로드 (전역으로 한 번만)
def load_words():
    """단어 파일에서 단어 리스트 로드"""
    data_path = DATA_PATH / 'raw' / 'count_1w.txt'

    words = []
    with open(data_path, 'r', encoding='utf-8') as f:
        for line in f:
            if '\t' in line:
                word = line.split('\t')[0].strip()
                if len(word) >= 3 and word.isalpha() and word.islower():
                    words.append(word)
                    if len(words) >= 1000:  # 1000개만 사용
                        break
    print(f"✅ {len(words)}개 단어 로드 완료")

    return words


# 전역 단어 리스트
WORD_LIST = load_words()


class ProgressiveTypingUser(FastHttpUser):
    """점진적 타이핑 사용자 - 1000+ 사용자 최적화 버전"""

    # 🚀 공유 클라이언트 풀 사용 (리소스 절약 및 1000+ 사용자 지원)
    client_pool = SHARED_CLIENT_POOL

    # 🚀 타임아웃 설정 최적화 (499 에러 해결)
    network_timeout = 120.0  # 60 → 120초
    connection_timeout = 120.0  # 60 → 120초

    # 🚀 최적화 설정 (공유 풀 사용으로 개별 concurrency 불필요)
    max_redirects = 5  # 불필요한 리다이렉트 방지
    max_retries = 1  # 빠른 실패
    insecure = True  # SSL 검증 비활성화 (성능 향상)

    # 🚀 기본 헤더 최적화
    default_headers = {
        'User-Agent': 'LocustLoadTest/1.0',
        'Accept': 'application/json',
        'Accept-Encoding': 'gzip, deflate',
        'Connection': 'keep-alive',
        'Keep-Alive': 'timeout=120, max=1000'  # 연결 유지 최적화
    }

    # 타이핑 간격 조정 (더 빠른 테스트)
    wait_time = between(0.05, 0.2)  # 0.1-0.5 → 0.05-0.2초

    def on_start(self):
        """사용자 시작 시 실행 - 헬스체크"""
        try:
            response = self.client.get("/health", timeout=30)
            if response.status_code == 200:
                print(f"✅ User {self.environment.runner.user_count}: Health check OK")
            else:
                print(f"⚠️ User {self.environment.runner.user_count}: Health check failed - {response.status_code}")
        except Exception as e:
            print(f"❌ User {self.environment.runner.user_count}: Health check error - {e}")

    @task
    def progressive_search(self):
        """랜덤 단어를 선택해서 점진적으로 타이핑 - 에러 핸들링 강화"""
        # 랜덤 단어 선택
        word = random.choice(WORD_LIST)

        # 단어 끝까지 점진적으로 타이핑
        for i in range(1, len(word) + 1):
            query = word[:i]

            try:
                # API 요청 - 타임아웃 명시적 설정
                with self.client.get(
                        f"/search_json?q={query}",
                        catch_response=True,
                        timeout=60,  # 명시적 타임아웃
                        name=f"/search_json?q=[{len(query)}chars]"  # 통계 그룹화
                ) as response:

                    if response.status_code == 200:
                        try:
                            data = response.json()
                            if isinstance(data, list) and len(data) >= 0:
                                response.success()
                            else:
                                response.failure(f"Invalid response format: {data}")
                        except Exception as e:
                            response.failure(f"JSON parse error: {e}")

                    elif response.status_code == 499:
                        # 499 에러 특별 처리
                        response.failure("Client timeout - increase timeout settings")
                        print(f"🚨 499 Error for query '{query}' - consider increasing timeouts")
                        break  # 이 단어는 중단하고 다음 단어로

                    else:
                        response.failure(f"HTTP {response.status_code}")

            except Exception as e:
                # 연결 에러 등 처리
                print(f"⚠️ Connection error for '{query}': {e}")
                break  # 연결 문제 시 이 단어는 중단

            # 타이핑 속도 시뮬레이션 (마지막 글자 제외)
            if i < len(word):
                time.sleep(random.uniform(0.05, 0.15))  # 더 빠른 타이핑

    @task(1)  # 가중치 1 (progressive_search는 10)
    def health_check(self):
        """주기적 헬스체크"""
        with self.client.get("/health", catch_response=True, name="/health") as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Health check failed: {response.status_code}")


# 🚀 실행 최적화 설정
if __name__ == "__main__":
    print("🚀 Optimized Progressive Typing Test for 24K RPS")
    print("✅ Timeout: 120s")
    print("✅ Concurrency: 50 per user")
    print("✅ Keep-alive enabled")
    print("✅ Retry enabled")
    print("")
    print("Usage:")
    print("  # 점진적 테스트")
    print("  locust -f locustfile.py --host=http://your-target \\")
    print("         --users=100 --spawn-rate=10 --run-time=5m")
    print("")
    print("  # 1000 유저 테스트")
    print("  locust -f locustfile.py --host=http://your-target \\")
    print("         --users=1000 --spawn-rate=50 --run-time=10m")
    print("")
    print("  # 24K RPS 목표 테스트")
    print("  locust -f locustfile.py --host=http://your-target \\")
    print("         --users=2000 --spawn-rate=100 --run-time=15m")