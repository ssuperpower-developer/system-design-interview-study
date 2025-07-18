# simple_locustfile.py - 기존 파일 수정 버전

from locust import HttpUser, task, events, constant
import time
import random
import json
import os
from datetime import datetime


class QPSStressUser(HttpUser):
    """QPS 한계 테스트용 사용자 - 목표 10,000 QPS"""
    wait_time = constant(0.001)  # 1ms 대기 (거의 없음)

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
                    # 응답 시간이 100ms 초과하면 실패로 간주
                    if response_time > 0.1:
                        response.failure(f"Too slow: {response_time:.3f}s")
                    else:
                        response.success()
                except:
                    response.failure("Invalid JSON response")
            else:
                response.failure(f"HTTP {response.status_code}")


class HighVolumeUser(HttpUser):
    """고볼륨 테스트 - 가장 무거운 쿼리만"""
    wait_time = constant(0.001)

    heavy_queries = ["a", "e", "i", "o", "u"]  # 결과가 많은 쿼리들

    @task
    def heavy_autocomplete(self):
        query = random.choice(self.heavy_queries)
        start_time = time.time()

        with self.client.get(f"/search_json?q={query}", catch_response=True) as response:
            response_time = time.time() - start_time

            if response.status_code == 200:
                if response_time > 0.1:  # 100ms 기준
                    response.failure(f"Heavy query too slow: {response_time:.3f}s")
                else:
                    response.success()
            else:
                response.failure(f"HTTP {response.status_code}")


# === 기록 기능 추가 ===
class BenchmarkRecorder:
    def __init__(self):
        self.config_name = os.getenv('CONFIG_NAME', 'unknown')
        self.start_time = None
        self.results = {
            'config': self.config_name,
            'start_time': None,
            'end_time': None,
            'duration': 0,
            'peak_qps': 0,
            'peak_users': 0,
            'total_requests': 0,
            'total_failures': 0,
            'failure_rate': 0,
            'avg_response_time': 0,
            'max_response_time': 0,
            'target_achieved': False
        }

    def save_results(self):
        """결과를 JSON 파일로 저장"""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"benchmark_{self.config_name}_{timestamp}.json"

        with open(filename, 'w') as f:
            json.dump(self.results, f, indent=2)

        print(f"💾 결과 저장됨: {filename}")
        return filename


# 글로벌 레코더
recorder = BenchmarkRecorder()


@events.test_start.add_listener
def on_test_start(environment, **kwargs):
    recorder.start_time = time.time()
    recorder.results['start_time'] = recorder.start_time

    print("🚀 QPS 스트레스 테스트 시작!")
    print(f"🔧 설정: {recorder.config_name}")
    print("🎯 목표: 단일 컨테이너 10,000 QPS")
    print("=" * 50)


@events.test_stop.add_listener
def on_test_stop(environment, **kwargs):
    end_time = time.time()
    recorder.results['end_time'] = end_time
    recorder.results['duration'] = end_time - recorder.start_time

    # 통계 수집
    if hasattr(environment, 'stats'):
        stats = environment.stats.total
        recorder.results['total_requests'] = stats.num_requests
        recorder.results['total_failures'] = stats.num_failures
        recorder.results['failure_rate'] = (stats.num_failures / max(stats.num_requests, 1)) * 100
        recorder.results['avg_response_time'] = stats.avg_response_time
        recorder.results['max_response_time'] = stats.max_response_time

        # 목표 달성 여부 (10,000 QPS + 실패율 1% 이하)
        recorder.results['target_achieved'] = (
                recorder.results['peak_qps'] >= 10000 and
                recorder.results['failure_rate'] <= 1.0
        )

        # 테스트 상태 분류
        if recorder.results['failure_rate'] > 10:
            test_status = "❌ FAILED (높은 실패율)"
        elif recorder.results['peak_qps'] >= 10000:
            test_status = "✅ SUCCESS (목표 달성)"
        elif recorder.results['peak_qps'] >= 7000:
            test_status = "⚠️ PARTIAL (부분 성공)"
        else:
            test_status = "❌ FAILED (낮은 성능)"

        recorder.results['test_status'] = test_status

    # 결과 출력
    print("=" * 50)
    print(f"📊 최종 결과 - {recorder.config_name}")
    print(f"   테스트 시간: {recorder.results['duration']:.1f}초")
    print(f"   총 요청: {recorder.results['total_requests']:,}")
    print(f"   실패: {recorder.results['total_failures']:,}")
    print(f"   실패율: {recorder.results['failure_rate']:.2f}%")
    print(f"   최대 QPS: {recorder.results['peak_qps']:.1f}")
    print(f"   평균 응답시간: {recorder.results['avg_response_time']:.1f}ms")
    print(f"   최대 응답시간: {recorder.results['max_response_time']:.1f}ms")
    print(f"   테스트 상태: {recorder.results.get('test_status', 'UNKNOWN')}")
    print(f"   목표 달성: {'✅ YES' if recorder.results['target_achieved'] else '❌ NO'}")

    # 성능 개선 제안
    if recorder.results['failure_rate'] > 10:
        print("\n💡 개선 제안:")
        print("   - 사용자 수 감소 시도")
        print("   - 서버 설정 최적화 필요")
        print("   - 응답시간 초과 확인")
    elif recorder.results['peak_qps'] < 7000:
        print("\n💡 개선 제안:")
        print("   - uvloop + httptools 조합 시도")
        print("   - 워커 수 조정 고려")
        print("   - 애플리케이션 코드 최적화")

    # 파일 저장
    recorder.save_results()


# 테스트 중단 조건 설정
import gevent
from locust.runners import STATE_STOPPING, STATE_STOPPED, STATE_CLEANUP, MasterRunner, LocalRunner


class TestStopConditions:
    FAILURE_RATE_THRESHOLD = 0.1  # 10% 실패율 초과시 중단
    CHECK_INTERVAL = 1  # 1초마다 체크
    MIN_REQUESTS = 100  # 최소 100개 요청 후 체크 시작


# 백그라운드 체커 함수 (공식 방법)
def failure_rate_checker(environment):
    """실패율을 모니터링하고 임계값 초과시 테스트 중단"""
    while not environment.runner.state in [STATE_STOPPING, STATE_STOPPED, STATE_CLEANUP]:
        time.sleep(TestStopConditions.CHECK_INTERVAL)

        stats = environment.runner.stats.total
        if stats.num_requests < TestStopConditions.MIN_REQUESTS:
            continue

        failure_rate = stats.fail_ratio

        if failure_rate > TestStopConditions.FAILURE_RATE_THRESHOLD:
            print(f"🚨 테스트 자동 중단: 실패율 {failure_rate:.1%} > {TestStopConditions.FAILURE_RATE_THRESHOLD:.1%}")
            print(f"📊 현재 통계: 요청 {stats.num_requests:,}, 실패 {stats.num_failures:,}")
            environment.runner.quit()
            return
        else:
            # 주기적 상태 출력
            current_qps = stats.current_rps
            avg_response_time = stats.avg_response_time
            print(f"📊 상태 체크: QPS {current_qps:.1f}, 실패율 {failure_rate:.1%}, 평균응답 {avg_response_time:.1f}ms")


@events.init.add_listener
def on_locust_init(environment, **_kwargs):
    """Locust 초기화 시 백그라운드 체커 시작"""
    # 워커에서는 실행하지 않음, 마스터나 스탠드얼론에서만
    if isinstance(environment.runner, MasterRunner) or isinstance(environment.runner, LocalRunner):
        gevent.spawn(failure_rate_checker, environment)


@events.request.add_listener
def on_request(request_type, name, response_time, response_length, response, context, exception, start_time, url,
               **kwargs):
    # QPS 추적 (간소화)
    if hasattr(context, 'locust') and hasattr(context.locust, 'environment'):
        env = context.locust.environment
        if hasattr(env, 'stats'):
            current_qps = env.stats.total.current_rps
            current_users = getattr(env.runner, 'user_count', 0)

            # 최대값 업데이트
            if current_qps > recorder.results['peak_qps']:
                recorder.results['peak_qps'] = current_qps
            if current_users > recorder.results['peak_users']:
                recorder.results['peak_users'] = current_users


# 실행 가이드
if __name__ == "__main__":
    print("📋 실행 가이드:")
    print("1. 서버 실행: python main.py")
    print("2. 벤치마크 실행:")
    print("   CONFIG_NAME=uvloop_httptools locust -f simple_locustfile.py --host=http://localhost:8000")
    print("3. 웹 UI: http://localhost:8089")
    print("4. 권장 설정: 1000 users, spawn rate 50")
    print("")
    print("🚨 자동 중단 조건:")
    print(f"   - 실패율 {TestStopConditions.FAILURE_RATE_THRESHOLD}% 초과시 자동 중단")
    print(f"   - {TestStopConditions.CHECK_INTERVAL}초마다 상태 체크")
    print(f"   - 최소 {TestStopConditions.MIN_REQUESTS}개 요청 후 체크 시작")
    print("")
    print("🔧 테스트할 설정들:")
    print("   - asyncio_h11 (기준선)")
    print("   - uvloop_h11 (이벤트 루프 개선)")
    print("   - uvloop_httptools (최고 성능)")