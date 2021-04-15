from utils import json_split, resolve_formdata
from mitmproxy import http
import redis
import json

client = redis.Redis(host="10.143.15.226", port="6379", password="waibiwaibiwaibibabo")
search_url_perfix = "https://aweme.snssdk.com/aweme/v1/general/search/stream/"


def response(flow: http.HTTPFlow):
    if flow.request.url and flow.request.url.startswith(search_url_perfix):
        content = flow.request.content.decode("utf-8")
        keyword = resolve_formdata(content).get("keyword")

        resp_text = flow.response.text
        for s in json_split(resp_text):
            j = json.loads(s)
            if "data" in j and j.get("status_code") == 0:
                client.lpush(
                    "tiktok:resp", f"{keyword}${json.dumps(j, ensure_ascii=False)}"
                )
            else:
                client.lpush("tiktok:fail", f"{keyword}${s}")
