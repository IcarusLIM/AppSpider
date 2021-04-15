import redis
import logging
import json
import time
from config import redis_client


logger = logging.getLogger()
logger.setLevel(logging.DEBUG)


def save_aweme(key, aweme):
    redis_client.rpush("tiktok:aweme", f"{key}${json.dumps(aweme, ensure_ascii=False)}")


def main():
    while True:
        l = redis_client.lpop("tiktok:resp")
        if l is None:
            time.sleep(5)
            continue
        l = l.decode("utf-8")
        [key, content] = l.split("$", 1)
        key = key.replace("/", "")
        logging.info("append: " + key)
        with open("raw_meta/meta.txt", "a") as f:
            f.write(f"{key}\t{content}\n")

        meta = json.loads(content)
        data = meta.get("data")
        if not data:
            continue
        for item in data:
            if "aweme_info" in item:
                save_aweme(key, item["aweme_info"])
            elif "dynamic_patch" in item and "aweme_list" in item["dynamic_patch"]:
                for aweme in item["dynamic_patch"]["aweme_list"]:
                    save_aweme(key, aweme)


main()