from locust import task, between, FastHttpUser
import random
import time

from src.constants.constant import DATA_PATH


# 단어 리스트 로드 (전역으로 한 번만)
def load_words():
    """단어 파일에서 단어 리스트 로드"""
    data_path = DATA_PATH / 'raw'/ 'count_1w.txt'

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
    """점진적 타이핑 사용자"""
    wait_time = between(0.1, 0.5)  # 타이핑 간격

    @task
    def progressive_search(self):
        """랜덤 단어를 선택해서 점진적으로 타이핑"""
        # 랜덤 단어 선택
        word = random.choice(WORD_LIST)

        # 단어 끝까지 점진적으로 타이핑
        for i in range(1, len(word) + 1):
            query = word[:i]

            # API 요청
            with self.client.get(f"/search_json?q={query}", catch_response=True) as response:
                if response.status_code == 200:
                    response.success()
                else:
                    response.failure(f"HTTP {response.status_code}")

            # 타이핑 속도 시뮬레이션 (마지막 글자 제외)
            if i < len(word):
                time.sleep(random.uniform(0.1, 0.3))


if __name__ == "__main__":
    print("🚀 Simple Progressive Typing Test")
    print("Usage:")
    print("  locust -f simple_progressive.py --host=http://your-target --users=50 --spawn-rate=5")