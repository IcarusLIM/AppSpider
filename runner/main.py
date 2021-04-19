import subprocess
import re
from utils import wait_timeout, run
from functools import partial
import logging
import asyncio
import redis
import random


logging.basicConfig(
    level=logging.DEBUG,
    format="[%(asctime)s] - (%(module)s.%(funcName)s)[%(levelname)s]: %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S %z",
)
logger = logging.getLogger(__name__)


redis_client = redis.Redis(
    host="10.143.15.226", port="6379", password="waibiwaibiwaibibabo"
)

"""
List of devices attached
192.168.56.106:5555     device
"""


async def list_adb_devices():
    stdout, _ = await run("adb devices")
    adb_devices = []
    for line in stdout.split("\n"):
        match = re.match("^(\d+(\.\d+){3}:\d+).*device$", line)
        if match:
            device_id = match.group(1)
            stdout, _ = await run(f"adb -s {device_id} shell getprop")
            name = ""
            for l in stdout.split("\n"):
                if l.startswith("[ro.product.model]: ["):
                    name = l[len("[ro.product.model]: [") : -1]
                    break
            adb_devices.append({"device_id": device_id, "name": name})
    return adb_devices


"""
"Google Nexus 6P" {930d14d5-10ab-46ce-afcf-127ba366011f}
"""


async def list_devices():
    adb_devices = await list_adb_devices()
    stdout, _ = await run("vboxmanage list vms")
    devices = []
    for line in stdout.split("\n"):
        match = re.match('"(.+)".+{.+}', line)
        if match:
            name = match.group(1)
            if not name.startswith("Clone"):
                continue
            adb_device = next(filter(lambda x: x["name"] == name, adb_devices), None)
            devices.append(
                {
                    "name": name,
                    "status": adb_device is not None,
                    "device_id": adb_device["device_id"] if adb_device else None,
                }
            )
    return devices


async def stop_device(device):
    await run(f"player -n \"{device['name']}\" -x")


async def start_device(device):
    async def get_device_id(name):
        adb_devices = await list_adb_devices()
        new_device = next(filter(lambda x: x["name"] == name, adb_devices), None)
        return new_device and new_device["device_id"]

    async def is_fully_boot(adb_device_id):
        stdout, _ = await run(f"adb -s {adb_device_id} shell getprop init.svc.bootanim")
        logging.debug("boot status: " + stdout)
        return adb_device_id if stdout.find("stopped") >= 0 else None

    logger.info("try start device: " + str(device))
    subprocess.Popen(["player", "-n", device["name"]])
    device_id = await wait_timeout(
        partial(get_device_id, device["name"]), 60, interval=3
    )
    if device_id is None:
        logger.info("device start fail: " + str(device))
        await stop_device(device)
        return False
    boot_status = await wait_timeout(partial(is_fully_boot, device_id), 30)
    if boot_status is None:
        logger.info("device boot fail: " + str(device))
        await stop_device(device)
        return False
    logger.info("device boot finish: " + str(device))
    device["status"] = True
    device["device_id"] = device_id
    return True


async def run_app(adb_device_id):
    return await run(
        f"adb -s {adb_device_id} shell am instrument -w"
        " -e debug false"
        " -e class com.hamster.androidappspider.TiktokSearchTest"
        " com.hamster.androidappspider.test/androidx.test.runner.AndroidJUnitRunner"
    )


async def clear_data(adb_device_id):
    return await run(f"adb -s {adb_device_id} shell pm clear com.ss.android.ugc.aweme")


async def change_android_id(adb_device_id):
    random_id = "".join([random.choice("abcdef1234567890") for i in range(16)])
    await run(
        f"adb -s {adb_device_id} shell settings put secure android_id {random_id}"
    )
    return random_id


def is_exclude_device(device):
    return redis_client.sismember("tiktok:device:exclude", device["name"])


def is_device_in_use(device):
    return (
        device["status"]
        or device["name"] in running_devices_set
        or is_exclude_device(device)
    )


def mark_device_in_use(device):
    running_devices_set.add(device["name"])


def unmark_device_in_use(device):
    running_devices_set.remove(device["name"])


running_devices_set = set()


async def task():
    while True:
        devices = await list_devices()
        idle_devices = [x for x in devices if not is_device_in_use(x)]
        device = None if len(idle_devices) == 0 else random.choice(idle_devices)
        if device:
            mark_device_in_use(device)
            is_start = await start_device(device)
            if is_start:
                # mac address is hooked by xposed module, and awalys change after boot
                await change_android_id(device["device_id"])
                while True:
                    await clear_data(device["device_id"])
                    await asyncio.sleep(3)
                    logger.info("Run Test: " + str(device))
                    stdout, stderr = await run_app(device["device_id"])
                    logger.warning("Test exit with stdout: " + stdout)
                    logger.warning("Test exit with stderr: " + stderr)
                    if stdout.find("java.lang.RuntimeException: DeviceScrapped") >= 0:
                        await stop_device(device)
                        break
                    elif (
                        stdout.find("BootMayNotFinish") >= 0
                        or re.search("device.*not found", stderr)
                        or stderr.find("android.util.AndroidException") >= 0
                    ):
                        await stop_device(device)
                        break
                    await asyncio.sleep(10)
            unmark_device_in_use(device)
            await asyncio.sleep(10)
        else:
            logger.info("no idle device")
            await asyncio.sleep(120)


async def main():
    await asyncio.gather(*[task() for i in range(6)])


if __name__ == "__main__":
    asyncio.run(main())