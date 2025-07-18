from locust import HttpUser, task, events, constant
import time
import random


class QPSStressUser(HttpUser):
    """QPS 한계 테스트용 사용자"""
    wait_time = constant(0.001)  # 1ms 대기 (거의 없음 - 최대한 빠르게)

    # 단일 단어 쿼리들 (다양한 부하 패턴)
    queries = [
        "a",  # 매우 높은 부하 (결과 많음)
        "app",  # 높은 부하
        "google",  # 중간 부하
        "the",  # 높은 부하
        "cat",  # 중간 부하
        "dog",  # 중간 부하
        "car",  # 중간 부하
        "book",  # 중간 부하
        "house",  # 중간 부하
        "water"  # 중간 부하
    ]

    @task
    def autocomplete_stress(self):
        """최대한 빠른 자동완성 요청"""
        query = random.choice(self.queries)

        start_time = time.time()
        with self.client.get(f"/search_json?q={query}", catch_response=True) as response:
            response_time = time.time() - start_time

            if response.status_code == 200:
                try:
                    data = response.json()
                    # 응답 시간이 500ms 초과하면 실패로 간주
                    if response_time > 0.5:
                        response.failure(f"Too slow: {response_time:.3f}s")
                    else:
                        response.success()
                except:
                    response.failure("Invalid JSON response")
            else:
                response.failure(f"HTTP {response.status_code}")


class HighVolumeUser(HttpUser):
    """고볼륨 테스트 - 가장 무거운 쿼리만"""
    wait_time = constant(0.001)  # 1ms 대기

    heavy_queries = ["a", "e", "i", "o", "u"]  # 결과가 많은 쿼리들

    @task
    def heavy_autocomplete(self):
        query = random.choice(self.heavy_queries)
        start_time = time.time()

        with self.client.get(f"/search_json?q={query}", catch_response=True) as response:
            response_time = time.time() - start_time

            if response.status_code == 200:
                if response_time > 0.3:  # 더 엄격한 기준
                    response.failure(f"Heavy query too slow: {response_time:.3f}s")
                else:
                    response.success()
            else:
                response.failure(f"HTTP {response.status_code}")


# 통계 수집을 위한 이벤트 핸들러
stats_data = {
    "start_time": None,
    "peak_qps": 0,
    "current_users": 0,
    "failure_rate": 0,
    "avg_response_time": 0
}


@events.test_start.add_listener
def on_test_start(environment, **kwargs):
    stats_data["start_time"] = time.time()
    print("🚀 QPS 스트레스 테스트 시작!")
    print("목표: 시스템 한계 QPS 찾기")
    print("=" * 50)


@events.test_stop.add_listener
def on_test_stop(environment, **kwargs):
    elapsed = time.time() - stats_data["start_time"]
    print("=" * 50)
    print(f"📊 최종 결과:")
    print(f"   테스트 시간: {elapsed:.1f}초")
    print(f"   최대 달성 QPS: {stats_data['peak_qps']:.1f}")
    print(f"   최대 동시 사용자: {stats_data['current_users']}")
    print(f"   최종 실패율: {stats_data['failure_rate']:.1f}%")
    print(f"   평균 응답시간: {stats_data['avg_response_time']:.3f}초")


@events.request.add_listener
def on_request(request_type, name, response_time, response_length, response, context, exception, start_time, url,
               **kwargs):
    # 실시간 통계 업데이트는 성능에 영향을 주므로 최소화
    pass


# 실시간 모니터링용 (옵션)
def print_current_stats(environment):
    """현재 통계 출력"""
    if hasattr(environment, 'stats'):
        stats = environment.stats.total
        current_qps = stats.current_rps
        failure_rate = (stats.num_failures / max(stats.num_requests, 1)) * 100
        avg_response_time = stats.avg_response_time / 1000  # ms to seconds

        # 최대값 업데이트
        if current_qps > stats_data["peak_qps"]:
            stats_data["peak_qps"] = current_qps

        stats_data["failure_rate"] = failure_rate
        stats_data["avg_response_time"] = avg_response_time
        stats_data["current_users"] = environment.runner.user_count

        print(f"⚡ 현재 QPS: {current_qps:.1f} | "
              f"사용자: {environment.runner.user_count} | "
              f"실패율: {failure_rate:.1f}% | "
              f"응답시간: {avg_response_time:.3f}s")


# 커스텀 실행을 위한 클래스
class QPSTestRunner:
    """QPS 테스트 실행기"""

    @staticmethod
    def run_gradual_increase():
        """점진적 사용자 증가 테스트"""
        print("📈 점진적 QPS 증가 테스트 가이드:")
        print("1. Locust 웹 UI에서 다음 단계로 진행:")
        print("   - 1단계: 10 users, spawn rate 5")
        print("   - 2단계: 25 users, spawn rate 5")
        print("   - 3단계: 50 users, spawn rate 10")
        print("   - 4단계: 100 users, spawn rate 20")
        print("   - 5단계: 200 users, spawn rate 50")
        print("   - 6단계: 500 users, spawn rate 100")
        print("   - 7단계: 1000+ users (한계 도전)")
        print()
        print("⚠️ 각 단계마다 1-2분 안정화 후 다음 단계로!")
        print("💥 실패율 10% 넘거나 응답시간 500ms 넘으면 한계점!")


if __name__ == "__main__":
    QPSTestRunner.run_gradual_increase()