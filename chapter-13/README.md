# 8 CPU 환경에서의 24K QPS 목표 부하테스트 실행 보고서
이 보고서는 8 CPU Minikube 환경에서 24,000 QPS (초당 쿼리) 목표를 달성하기 위해 최적화된 Kubernetes 배포 및 Locust 부하 테스트 설정에 대해 설명합니다. 사용자의 리소스 계산을 기반으로 구성이 수정되었습니다.

## 1. 리소스 계산 요약

- **FastAPI Pods**: 4개 (초기 계산)
- **Redis Pods**: 2개 (초기 계산)
- **총 요청 CPU**: (4times1.5)+(2times0.25)=6.5 CPU
- **총 요청 Memory**: (4times1.5)+(2times1)=8 GiB

## 2. Kubernetes 배포 설정

### 2.1. Redis Read-Only 배포

Redis는 데이터 읽기 전용으로 설정되어 있으며, 고가용성과 성능을 위해 최적화되었습니다.

- **replicas**: 3개 파드
- **리소스 요청**: CPU "250m", Memory "512Mi"
- **리소스 제한**: CPU "500m", Memory "1Gi"
- **이미지**: `redis-with-data:latest`
- **활성/준비 프로브**: `redis-cli ping` 명령을 사용하여 Redis 인스턴스의 상태를 주기적으로 확인합니다.
- **PostStart 훅**: 파드 시작 후 미리 로드된 데이터가 정상적으로 있는지 확인하는 스크립트가 실행됩니다.
    

### 2.2. Redis 서비스

Redis 파드에 대한 내부 클러스터 IP 서비스를 제공합니다.

- **타입**: `ClusterIP`
- **포트**: 6379

### 2.3. FastAPI (App) 배포

FastAPI 애플리케이션은 높은 처리량을 위해 Gunicorn 워커와 함께 최적화되었습니다.

- **replicas**: 6개 파드 (계산에 따라 조정됨)
- **topologySpreadConstraints**: 파드가 여러 노드에 고르게 분산되도록 하여 가용성을 높입니다.
- **리소스 요청**: CPU "1000m", Memory "1Gi"
- **리소스 제한**: CPU "1500m", Memory "2Gi"
- **이미지**: `autocomplete-system:latest`
- **명령어**: Gunicorn을 사용하여 3개의 워커와 2000개의 워커 연결을 설정하여 동시성을 높였습니다.
- **환경 변수**: Redis 연결 설정, Python 최적화, Gunicorn 설정 등 다양한 성능 관련 변수가 설정되었습니다. 특히 `REDIS_MAX_CONNECTIONS`, `REDIS_CONNECTION_POOL_SIZE`, `REDIS_SOCKET_KEEPALIVE` 등이 최적화되었습니다.
- **활성/준비 프로브**: `/health` 엔드포인트를 통해 애플리케이션의 상태를 확인합니다.

### 2.4. FastAPI (App) 서비스

FastAPI 파드에 대한 내부 클러스터 IP 서비스를 제공합니다.

- **타입**: `ClusterIP`
- **포트**: 8000

### 2.5. Nginx Ingress

외부 트래픽을 FastAPI 서비스로 라우팅하며, 고성능을 위한 Nginx 설정을 포함합니다.

- **타임아웃 설정**: 연결, 전송, 읽기 타임아웃을 30초로 설정했습니다.
- **Keepalive 설정**: `upstream-keepalive-connections`를 1000, `upstream-keepalive-requests`를 10000으로 설정하여 24K RPS 목표에 대응합니다.
- **버퍼 설정**: 프록시 버퍼링을 활성화하고 버퍼 크기를 최적화했습니다.
- **요청 제한**: 바디 크기를 1MB로 제한합니다.
- **성능 최적화**: HTTP/2 사용 및 `round_robin` 로드 밸런싱을 설정했습니다.
- **연결 제한**: 최대 연결 수를 10000으로 설정했습니다.
- **재시도 설정**: 오류 및 타임아웃 시 업스트림 재시도 로직을 구성했습니다.

### 2.6. Horizontal Pod Autoscaler (HPA)

CPU 및 메모리 사용률에 따라 파드 수를 자동으로 조정하여 트래픽 변화에 유연하게 대응합니다.

- **App HPA**:
    - **최소/최대 replicas**: 6 / 7
    - **메트릭**: CPU 사용률 70%, 메모리 사용률 75%
- **Redis HPA**:
    - **최소/최대 replicas**: 3 / 4
    - **메트릭**: CPU 사용률 70%

### 2.7. Pod Disruption Budget (PDB)

자발적 중단(예: 노드 유지보수) 중에 애플리케이션의 최소 가용 파드 수를 보장하여 서비스 중단을 최소화합니다.

- **App PDB**: 최소 4개 파드 가용 보장 (총 6개 중)
- **Redis PDB**: 최소 2개 파드 가용 보장 (총 3개 중)

## 3. Locust 부하 테스트 설정

제공된 Locust 스크립트는 24,000 RPS 목표를 달성하기 위한 최적화된 부하 테스트 시나리오를 정의합니다.

### 3.1. 주요 최적화

- **공유 클라이언트 풀**: `geventhttpclient.client.HTTPClientPool`을 사용하여 12000개의 동시 연결을 지원하여 리소스 사용을 최적화하고 대규모 사용자 테스트를 가능하게 합니다.
- **타임아웃**: 네트워크 및 연결 타임아웃을 120초로 연장하여 499 에러 발생 가능성을 줄였습니다.
- **기본 헤더**: `Keep-Alive` 설정을 포함하여 연결 유지를 최적화했습니다.
- **타이핑 간격**: `wait_time`을 0.05초에서 0.2초 사이로 설정하여 더 빠른 테스트를 시뮬레이션합니다.
- **점진적 검색**: 랜덤 단어를 선택하여 한 글자씩 점진적으로 검색 요청을 보내는 시나리오를 구현했습니다.
- **에러 핸들링**: 응답 코드 및 JSON 파싱 오류에 대한 명시적인 에러 핸들링을 포함합니다.

### 3.2. 사용 예시

Locust 스크립트 실행을 위한 권장 명령은 다음과 같습니다.

```
# 점진적 테스트
locust -f locustfile.py --host=http://your-target \
       --users=100 --spawn-rate=10 --run-time=5m

# 1000 유저 테스트
locust -f locustfile.py --host=http://your-target \
       --users=1000 --spawn-rate=50 --run-time=10m

# 24K RPS 목표 테스트
locust -f locustfile.py --host=http://your-target \
       --users=2000 --spawn-rate=100 --run-time=15m

```

## 4. 결론

이 보고서에 제시된 Kubernetes 배포 및 Locust 부하 테스트 설정은 8 CPU Minikube 환경에서 24,000 QPS 목표를 달성하기 위한 최적화된 접근 방식을 제공합니다. **Locust를 통한 부하 테스트 결과, 동시 사용자 2000명 환경에서 최대 9000 RPS (초당 요청 수)에 도달하는 성능을 확인했습니다.** 리소스 할당, 네트워크 설정, 자동 확장 및 안정성 확보를 통해 고성능 및 고가용성 시스템을 구축하는 데 기여할 것입니다.

## 5. 트러블슈팅 및 학습 경험

프로젝트를 진행하며 다음과 같은 주요 트러블슈팅과 학습 경험을 통해 시스템의 안정성과 성능을 지속적으로 개선했습니다.

- **Minikube 환경 및 네트워크 문제 해결**:
    
    - `minikube tunnel` 사용 시 SSH 파일 디스크립터(FD) 고갈 문제가 발생하여 안정적인 연결 유지가 어려웠습니다.
    - 이를 해결하기 위해 `nohup kubectl port-forward -n ingress-nginx svc/ingress-nginx-controller 8080:80 > ingress-forward.log 2>&1 &` 명령을 사용하여 `ingress-nginx-controller` 서비스의 포트 포워딩을 백그라운드에서 안정적으로 유지하는 방식을 도입했습니다.
    - 이 과정에서 Kubernetes의 네트워크 구조 및 서비스 노출 방식에 대한 깊이 있는 이해를 얻었습니다.
        
- **단일 파드 성능 최적화**:
    
    - FastAPI 애플리케이션의 단일 파드 성능을 개선하기 위해 `uvloop` (비동기 이벤트 루프) 및 `httptools` (HTTP 파서)와 같은 고성능 라이브러리를 도입하고 관련 환경 변수를 설정했습니다.
    - Gunicorn 워커 수(`WEB_CONCURRENCY`, `GUNICORN_WORKERS`) 및 워커 연결 수(`GUNICORN_WORKER_CONNECTIONS`)를 최적화하여 각 파드의 동시 처리량을 증대시켰습니다.
        
- **API 게이트웨이 및 부하 분산 개선**:
    
    - 초기에는 간단한 로드 밸런서를 사용하여 API를 노출했지만, 더 복잡한 트래픽 관리와 성능 최적화를 위해 Nginx Ingress로 전환했습니다.
    - Nginx Ingress를 통해 연결 타임아웃, Keepalive 연결 수, 버퍼링, 요청 제한 등 상세한 성능 최적화 어노테이션을 적용하여 24K QPS 목표 달성을 위한 트래픽 처리 능력을 강화했습니다.
        
- **Redis 및 애플리케이션 안정성 확보**:
    
    - Redis `PostStart` 훅을 통해 미리 로드된 데이터의 상태를 확인하는 로직을 추가하여 초기 배포의 안정성을 확보했습니다.
    - FastAPI 및 Redis에 대한 활성(Liveness) 및 준비(Readiness) 프로브를 세밀하게 설정하여 파드의 건강 상태를 정확히 모니터링하고 비정상 파드를 자동으로 재시작하거나 제거하도록 구성했습니다.
    - Redis 연결 풀 크기(`REDIS_CONNECTION_POOL_SIZE`) 및 최대 연결 수(`REDIS_CONNECTION_POOL_SIZE`)를 조정하여 Redis 서버의 부하를 관리하고 연결 관련 문제를 최소화했습니다.
        
- **자동 확장 및 고가용성 전략 수립**:
    
    - CPU 및 메모리 사용률 기반의 Horizontal Pod Autoscaler(HPA)를 도입하여 트래픽 변동에 따른 파드 수를 자동으로 조절함으로써 시스템의 유연성을 확보했습니다.
    - Pod Disruption Budget(PDB)을 설정하여 자발적 중단(예: 노드 유지보수) 시에도 서비스의 최소 가용 파드 수를 보장하여 서비스 중단을 최소화했습니다.
    - `topologySpreadConstraints`를 통해 파드를 여러 노드에 고르게 분산 배치하여 단일 노드 장애에 대한 복원력을 강화했습니다.
        
- **부하 테스트를 통한 성능 병목 식별 및 해결**:
    
    - Locust 부하 테스트 스크립트의 타임아웃(`network_timeout`, `connection_timeout`)을 연장하고 공유 클라이언트 풀(`SHARED_CLIENT_POOL`)을 도입하여 대규모 동시 사용자 테스트 환경을 구축했습니다.
    - 499(Client timeout) 에러와 같은 실제 부하 테스트 중 발생한 문제들을 해결하며 시스템의 병목 지점을 식별하고 개선하는 데 집중했습니다.