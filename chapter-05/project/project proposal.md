# 프로메테우스 경보 시스템 프로젝트 설계서

> **"가상 면접 사례로 배우는 대규모 시스템 설계 기초" 5장 학습 프로젝트**  
> 프로메테우스 기반 실시간 모니터링 및 경보 시스템 구현

---


## 프로젝트 개요

### 목적
- **이론과 실습의 연결**: 시스템 설계 이론을 실제 동작하는 시스템으로 구현
- **프로덕션 레벨 경험**: 실제 운영환경에서 사용되는 모니터링 스택 체험
- **핵심 개념 이해**: Pull 모델, 시계열 DB, 경보 규칙, 알림 관리 등의 실무 적용

### 주요 특징
- **실시간 메트릭 수집**: CPU, 메모리, HTTP 요청 등 다양한 메트릭
- **지능형 경보 시스템**: 임계값 기반 자동 알림 발송
- **확장 가능한 구조**: 마이크로서비스 환경에 적용 가능
- **시각화 대시보드**: 그라파나 연동 실시간 모니터링
- **부하 테스트 내장**: 알림 트리거를 위한 스트레스 테스트 기능

### 학습 목표 연계
| 책의 개념 | 프로젝트 구현 |
|-----------|---------------|
| 시계열 데이터베이스 | 프로메테우스 활용 |
| Pull 모델 | 프로메테우스 스크래핑 |
| 데이터 수집 파이프라인 | Flask → Prometheus → Alertmanager |
| 경보 시스템 | Alertmanager + Slack/Discord |
| 시각화 | 그라파나 대시보드 |

---

## 🏗️ 시스템 아키텍처



### 데이터 플로우
```
1. [Flask App] 메트릭 생성 → /metrics 엔드포인트 노출
                ↓
2. [Prometheus] 15초마다 스크래핑 → 시계열 DB에 저장
                ↓
3. [Prometheus] 알림 규칙 평가 → 임계값 초과 시 알림 생성
                ↓
4. [Alertmanager] 알림 수신 → 그룹핑 및 라우팅
                ↓
5. [알림 채널] 최종 사용자에게 알림 전송
```

---

## 🛠 기술 스택

### Backend 스택
```yaml
애플리케이션:
  - Python 3.9+
  - Flask 2.3.3
  - prometheus_client 0.17.1
  - psutil 5.9.5

모니터링 스택:
  - Prometheus (latest)
  - Alertmanager (latest)
  - Grafana (latest)

인프라:
  - Docker & Docker Compose
  - Linux/macOS/Windows 지원
```

### 통신 프로토콜
- **HTTP/REST**: 메트릭 수집 및 API 통신
- **Webhook**: 알림 전송 (Slack, Discord)
- **PromQL**: 메트릭 쿼리 및 알림 규칙

---

## 📁 프로젝트 구조

```
prometheus-alerting-demo/
├── 📄 docker-compose.yml          # 전체 스택 오케스트레이션
├── 📄 README.md                  # 프로젝트 가이드
├── 📄 test_alerts.sh             # 알림 테스트 스크립트
├── 📄 load_test.sh               # 부하 테스트 스크립트
├── 📄 monitor.sh                 # 실시간 모니터링 스크립트
│
├── 📂 app/                       # Flask 애플리케이션
│   ├── 📄 main.py                # 메인 애플리케이션
│   ├── 📄 requirements.txt       # Python 의존성
│   └── 📄 Dockerfile            # 컨테이너 이미지
│
├── 📂 prometheus/                # 프로메테우스 설정
│   ├── 📄 prometheus.yml         # 메인 설정 파일
│   └── 📄 alert_rules.yml        # 알림 규칙 정의
│
├── 📂 alertmanager/              # 알림매니저 설정
│   └── 📄 alertmanager.yml       # 알림 라우팅 설정
│
├── 📂 grafana/                   # 그라파나 설정
│   └── 📂 provisioning/          # 자동 설정
│       ├── 📂 datasources/
│       └── 📂 dashboards/
│
└── 📂 docs/                      # 문서
    ├── 📄 METRICS.md             # 메트릭 상세 설명
    ├── 📄 ALERTS.md              # 알림 규칙 가이드
    └── 📄 TROUBLESHOOTING.md     # 문제 해결 가이드
```
