from fastapi import FastAPI, Request, Form
from fastapi.responses import HTMLResponse
from fastapi.templating import Jinja2Templates

import redis

app = FastAPI()
templates = Jinja2Templates(directory="templates")
import os

REDIS_HOST = os.getenv("REDIS_HOST", "localhost")
r = redis.Redis(host=REDIS_HOST, port=6379, db=0, decode_responses=True)

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
