import os

from fastapi import FastAPI, Request, Form
from fastapi.responses import HTMLResponse
from fastapi.templating import Jinja2Templates

import redis

app = FastAPI()
templates = Jinja2Templates(directory="templates")

REDIS_HOST = os.getenv("REDIS_HOST", "localhost")
redis_pool = redis.connection.BlockingConnectionPool(
    host=REDIS_HOST,
    port=6379,
    db=0,
    decode_responses=True,
    max_connections=500,  # RPS 5000 대응
    timeout=20,
    retry_on_timeout=True,
    socket_keepalive=True
)
r = redis.Redis(connection_pool=redis_pool)

@app.get("/health")
async def health_check():
    return {"status": "OK"}

@app.get("/", response_class=HTMLResponse)
async def read_root(request: Request):
    return templates.TemplateResponse("index.html", {"request": request})


@app.get("/search", response_class=HTMLResponse)
async def search(request: Request, q: str = ""):
    """극한 성능 최적화"""
    suggestions = r.zrevrange(f"trie:{q.lower()}", 0, 4) if q else []

    return templates.TemplateResponse(
        "suggestions.html",
        {"request": request, "suggestions": suggestions}
    )

@app.get("/search_json")
async def fast_autocomplete(q: str = ""):
    """극한 성능 테스트용 - 예외 처리 없음"""
    return r.zrevrange(f"trie:{q.lower()}", 0, 4) if q else []
