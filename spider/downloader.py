from utils import get_nested
from config import redis_client
import asyncio
import json
import logging
import aiohttp
import os, sys, signal
from datetime import datetime
from functools import partial

logger = logging.getLogger()
logger.setLevel(logging.DEBUG)
headers = {
    "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.80 Safari/537.36",
}

file_save_dir = "videos"
hdfs_path = "/user/slave/websac/tiktok"
loop = asyncio.get_event_loop()
STOP_FLAG = False

timeout3600 = aiohttp.ClientTimeout(total=3600)


async def download_video(url, fn):
    status_code = 200
    for retry in range(2):
        try:
            async with aiohttp.ClientSession() as session:
                async with session.get(url, timeout=timeout3600) as r:
                    if r.status == 200:
                        await save_video(r.content, fn)
                        return
                    else:
                        status_code = r.status
                        await asyncio.sleep(3)
                        logger.debug(f"retry {url} {retry}")
        except Exception as e:
            raise Exception(f"download {url} fail: {type(e).__name__}")
    raise Exception(f"download {url} fail, status {status_code}")


async def save_video(stream_reader, fn):
    with open(f"{file_save_dir}/{fn}.download", "wb") as file:
        while True:
            chunk = await stream_reader.read(1024 * 4)
            if len(chunk) == 0:
                break
            file.write(chunk)
    os.rename(f"{file_save_dir}/{fn}.download", f"{file_save_dir}/{fn}")


async def upload_video(fn):
    logger.debug(f"start upload video {fn}")
    day = datetime.now().strftime("%Y-%m-%d")
    await asyncio.get_event_loop().run_in_executor(
        None,
        partial(os.system, f"gohdfs put {file_save_dir}/{fn} {hdfs_path}/{day}/{fn}"),
    )
    os.remove(f"{file_save_dir}/{fn}")


async def task(i):
    aweme_file = open(f"aweme_meta/{i}.txt", "a")
    while not STOP_FLAG:
        item = redis_client.lpop("tiktok:aweme")
        if item is None:
            await asyncio.sleep(5)
            continue
        [key, aweme] = item.decode("utf-8").split("$", 1)
        aweme_file.write(f"{key}\t{aweme}\n")
        aweme = json.loads(aweme)

        file_name = aweme.get("aweme_id") + ".mp4"
        download_urls = get_nested(aweme, ["video", "download_addr", "url_list"])
        if not download_urls:
            continue
        try:
            for url in download_urls:
                try:
                    await download_video(url, file_name)
                    break
                except Exception as e:
                    logger.warning(e)
                    continue
            await upload_video(file_name)
        except Exception as e:
            logger.warning(e)
    aweme_file.close()


def signal_recv(signal, frame):
    global STOP_FLAG
    if not STOP_FLAG:
        STOP_FLAG = True
        logger.info("Cancel received, press ctrl+c again for force exit!")
    else:
        sys.exit(1)


signal.signal(signal.SIGINT, signal_recv)


async def clean_uncomplete():
    for fname in os.listdir(file_save_dir):
        if fname.endswith(".mp4.download"):
            os.remove(f"{file_save_dir}/{fname}")
        if fname.endswith(".mp4"):
            await upload_video(fname)


async def main():
    await clean_uncomplete()
    await asyncio.gather(*([task(i) for i in range(10)]))
    # await session.close()


asyncio.run(main())