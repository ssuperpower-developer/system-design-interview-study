# Dockerfile
FROM python:3.11-slim

# 헬스체크용 curl 설치
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/* \
    && apt-get clean

# uv 설치
RUN pip install uv

# 작업 디렉토리 설정
WORKDIR /app

# uv 환경 변수 설정
ENV UV_CACHE_DIR=/tmp/uv-cache \
    UV_COMPILE_BYTECODE=1 \
    UV_LINK_MODE=copy

# 의존성 파일 복사
COPY pyproject.toml uv.lock ./

# 의존성 설치 (캐시 활용)
RUN --mount=type=cache,target=/tmp/uv-cache \
    uv sync --frozen --no-dev

# 애플리케이션 코드 복사
COPY . .

# 헬스체크용 스크립트 추가
RUN echo '#!/bin/bash\ncurl -f http://localhost:8000/health || exit 1' > /healthcheck.sh \
    && chmod +x /healthcheck.sh

# worker 설정이 kubernetis단에서 변경되었을 때 이곳도 함께 변경해야한다.
CMD ["uv", "run", "gunicorn", "main:app", \
     "-w", "4", \
     "-k", "uvicorn.workers.UvicornWorker", \
     "--bind", "0.0.0.0:8000", \
     "--worker-connections", "2000", \
     "--max-requests", "10000", \
     "--max-requests-jitter", "1000", \
     "--timeout", "120", \
     "--graceful-timeout", "30", \
     "--keep-alive", "5", \
     "--backlog", "2048", \
     "--preload", \
     "--worker-tmp-dir", "/dev/shm", \
     "--access-logfile", "-", \
     "--error-logfile", "-", \
     "--log-level", "warning"]
