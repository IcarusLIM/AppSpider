from main import list_adb_devices, list_devices
import asyncio


async def test_list_adb_devices():
    print(await list_adb_devices())


async def test_list_devices():
    print(await list_devices())


asyncio.run(test_list_devices())