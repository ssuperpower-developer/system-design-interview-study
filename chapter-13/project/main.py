from fastapi import FastAPI, Request, Form
from fastapi.responses import HTMLResponse
from fastapi.templating import Jinja2Templates

import redis

app = FastAPI()
templates = Jinja2Templates(directory="templates")
r = redis.Redis(host='localhost', port=6379, db=0, decode_responses=True)

@app.get("/", response_class=HTMLResponse)
async def read_root(request: Request):
    return templates.TemplateResponse("index.html", {"request": request})

@app.post("/search", response_class=HTMLResponse)
async def search(request: Request, search: str = Form("")):
    suggestions = []
    if search:
        suggestions = r.zrevrange(f"trie:{search}", 0, 9)
    return templates.TemplateResponse("suggestions.html", {"request": request, "suggestions": suggestions})
