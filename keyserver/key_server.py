from aiohttp import web
import redis
import logging

client = redis.Redis(host="10.143.15.226", port="6379", password="waibiwaibiwaibibabo")
keylist_key="tiktok:search"

async def handle(request):
    key = ""
    if "restore" in request.query:
        key = request.query["restore"]
        client.lpush(keylist_key, key)
    else:
        key = client.lpop(keylist_key).decode("utf-8")
        logging.info(key)
    return web.Response(text=key)


app = web.Application()
app.add_routes([web.get("/", handle), web.get("/{name}", handle)])

if __name__ == "__main__":
    web.run_app(app, port=4396)
