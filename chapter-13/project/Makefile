.PHONY: help start stop restart deploy delete status logs clean port-forward stop-port-forward build-images setup loadtest

# 기본 설정
DEPLOYMENT_FILE := deployment.yaml
NAMESPACE := default
LOG_TAIL := 50
PORT_FORWARD_LOG := ingress-forward.log

help: ## 사용 가능한 명령어 목록 출력
	@echo "Kubernetes Deployment Makefile"
	@echo ""
	@echo "사용 가능한 명령어:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}'

# Minikube 관리
start: ## Minikube 시작 (8 CPU, 20GB RAM, 50GB Disk)
	minikube start --cpus=8 --memory=20g --disk-size=50g
	minikube status
	minikube addons enable ingress
	minikube addons enable metrics-server

stop: ## Minikube 중지
	minikube stop

delete-cluster: ## Minikube 클러스터 삭제
	minikube delete --all

# Docker 이미지 빌드 및 로드
build-images: ## Docker 이미지 빌드 및 Minikube에 로드
	@echo "Docker 환경을 Minikube로 설정..."
	@eval $$(minikube docker-env); \
	docker build -t autocomplete-system:latest -f Dockerfile .; \
	docker build -t redis-with-data:latest -f Dockerfile.redis .

load-images: ## 기존 Docker 이미지를 Minikube에 로드
	minikube image load autocomplete-system:latest
	minikube image load redis-with-data:latest

# 배포 관리
deploy: ## Kubernetes 리소스 배포
	kubectl apply -f $(DEPLOYMENT_FILE)
	@echo "배포 완료. Pod 상태 확인 중..."
	@sleep 3
	kubectl get pods

delete: ## 배포된 리소스 삭제
	kubectl delete -f $(DEPLOYMENT_FILE) || true

restart: delete deploy ## 전체 재배포 (삭제 후 다시 배포)

rollout-restart: ## App Deployment만 재시작
	kubectl rollout restart deployment/app-deployment
	kubectl rollout status deployment/app-deployment

# 포트 포워딩
port-forward: ## Ingress Controller 포트 포워딩 (백그라운드)
	@echo "포트 포워딩 시작: localhost:8080 -> ingress-nginx-controller:80"
	@pkill -f "kubectl port-forward.*ingress-nginx" || true
	@sleep 1
	nohup kubectl port-forward -n ingress-nginx svc/ingress-nginx-controller 8080:80 > $(PORT_FORWARD_LOG) 2>&1 &
	@sleep 2
	@echo "포트 포워딩 프로세스:"
	@ps aux | grep "kubectl port-forward" | grep -v grep || echo "포트 포워딩 프로세스를 찾을 수 없습니다"

stop-port-forward: ## 포트 포워딩 중지
	@echo "포트 포워딩 프로세스 종료 중..."
	@pkill -f "kubectl port-forward.*ingress-nginx" || echo "실행 중인 포트 포워딩이 없습니다"

# 상태 확인
status: ## 전체 리소스 상태 확인
	@echo "=== Pods ==="
	kubectl get pods -o wide
	@echo ""
	@echo "=== Services ==="
	kubectl get services
	@echo ""
	@echo "=== Deployments ==="
	kubectl get deployments
	@echo ""
	@echo "=== HPA ==="
	kubectl get hpa
	@echo ""
	@echo "=== Ingress ==="
	kubectl get ingress

watch-pods: ## Pod 상태 실시간 모니터링
	kubectl get pods -w

watch-hpa: ## HPA 상태 실시간 모니터링
	kubectl get hpa -w

top-pods: ## Pod 리소스 사용량 확인
	kubectl top pods

top-nodes: ## Node 리소스 사용량 확인
	kubectl top nodes

# 로그 확인
logs: ## App 로그 확인 (최근 50줄)
	kubectl logs -l app=app --tail=$(LOG_TAIL)

logs-follow: ## App 로그 실시간 추적
	kubectl logs -l app=app -f --max-log-requests=10

logs-redis: ## Redis 로그 확인
	kubectl logs -l app=redis --tail=$(LOG_TAIL)

port-forward-logs: ## 포트 포워딩 로그 확인
	tail -f $(PORT_FORWARD_LOG)

# 디버깅
describe-app: ## App Deployment 상세 정보
	kubectl describe deployment app-deployment

describe-redis: ## Redis Deployment 상세 정보
	kubectl describe deployment redis-readonly

events: ## 최근 이벤트 확인
	kubectl get events --sort-by=.metadata.creationTimestamp

shell-app: ## App Pod에 쉘로 접속
	kubectl exec -it deployment/app-deployment -- sh

shell-redis: ## Redis Pod에 쉘로 접속
	kubectl exec -it deployment/redis-readonly -- sh

redis-cli: ## Redis CLI 접속
	kubectl exec -it deployment/redis-readonly -- redis-cli

# 정리
clean: delete stop-port-forward ## 모든 리소스 삭제 및 포트 포워딩 중지
	@echo "정리 완료"

clean-logs: ## 로그 파일 삭제
	rm -f $(PORT_FORWARD_LOG) *.log

# 전체 설정
setup: start build-images deploy port-forward ## 전체 환경 설정 (Minikube 시작, 이미지 빌드, 배포, 포트 포워딩)
	@echo ""
	@echo "=== 설정 완료 ==="
	@echo "접속 URL: http://localhost:8080"
	@echo ""
	@echo "유용한 명령어:"
	@echo "  make status           - 전체 상태 확인"
	@echo "  make logs             - 애플리케이션 로그 확인"
	@echo "  make watch-pods       - Pod 상태 실시간 모니터링"
	@echo "  make top-pods         - 리소스 사용량 확인"

# 테스트 및 벤치마크
test-health: ## Health check 테스트
	curl -s http://localhost:8080/health | jq .

test-autocomplete: ## Autocomplete API 테스트
	curl -s "http://localhost:8080/autocomplete?prefix=test" | jq .

loadtest: ## Locust 부하 테스트 시작 (4 프로세스, 웹 UI: http://localhost:8089)
	@echo "Locust 부하 테스트 시작..."
	@echo "웹 UI: http://localhost:8089"
	locust -f test/locustfile.py --host=http://localhost:8080 --processes 4

# Minikube IP 확인
get-ip: ## Minikube IP 주소 확인
	@echo "Minikube IP: $$(minikube ip)"
	@echo "Service URL: http://$$(minikube ip):8080"
