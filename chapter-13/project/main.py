import os

from fastapi import FastAPI, Request, Form
from fastapi.responses import HTMLResponse
from fastapi.templating import Jinja2Templates
import redis.asyncio as redis
from contextlib import asynccontextmanager

redis_client = None

REDIS_HOST = os.getenv("REDIS_HOST", "localhost")

@asynccontextmanager
async def lifespan(app: FastAPI):
    # 시작 시 Redis 연결
    global redis_client
    redis_client = redis.from_url(
        f"redis://{REDIS_HOST}:6379",
        encoding="utf-8",
        decode_responses=True,
        max_connections=50,  # Worker당 50개 연결
        socket_keepalive=True,
        socket_keepalive_options=(1, 3, 5),
        health_check_interval=30
    )

    yield

    # 종료 시 정리
    await redis_client.close()
    await redis_client.connection_pool.disconnect()


app = FastAPI(lifespan=lifespan)
templates = Jinja2Templates(directory="templates")


@app.get("/health")
async def health_check():
    return {"status": "OK"}


@app.get("/", response_class=HTMLResponse)
async def read_root(request: Request):
    return templates.TemplateResponse("index.html", {"request": request})


@app.get("/search", response_class=HTMLResponse)
async def search(request: Request, q: str = ""):
    """극한 성능 최적화"""
    suggestions = redis_client.zrevrange(f"trie:{q.lower()}", 0, 4) if q else []

    return templates.TemplateResponse(
        "suggestions.html",
        {"request": request, "suggestions": suggestions}
    )

@app.get("/search_json")
async def fast_autocomplete(q: str = ""):
    if not q:
        return []
    # 비동기 Redis 호출
    return await redis_client.zrevrange(f"trie:{q.lower()}", 0, 4)
