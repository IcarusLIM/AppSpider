import time, math
import asyncio
import inspect


async def wait_timeout(f, timeout, interval=1):
    start_at = time.time()
    while True:
        r = f()
        if inspect.isawaitable(r):
            r = await r
        if r is not None:
            return r
        if time.time() - start_at > timeout:
            break
        await asyncio.sleep(interval)


async def run(cmd):
    proc = await asyncio.create_subprocess_shell(
        cmd, stdout=asyncio.subprocess.PIPE, stderr=asyncio.subprocess.PIPE
    )

    stdout, stderr = await proc.communicate()
    stdout = stdout.decode() if stdout else ""
    stderr = stderr.decode() if stderr else ""
    return stdout, stderr  # , proc.returncode


# find all sub_str like "{...}"
def json_split(s):
    index_arr = []
    counter = 0
    skip = False
    in_quote = False
    for i in range(len(s)):
        if skip:
            skip = False
            continue
        c = s[i]
        if c == '"':
            in_quote = not in_quote
            continue
        if in_quote:
            if c == "\\":
                skip = True
        else:
            if c == "{":
                counter += 1
                if counter == 1:
                    index_arr.append(i)
            elif c == "}":
                counter -= 1
                if counter == 0:
                    index_arr.append(i)
    json_strs = []
    for i in range(math.floor(len(index_arr) / 2)):
        json_strs.append(s[index_arr[i * 2] : index_arr[i * 2 + 1] + 1])
    return json_strs