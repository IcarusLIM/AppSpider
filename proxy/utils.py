import math
from urllib.parse import unquote

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


def resolve_formdata(content):
    d = {}
    for i in content.split("&"):
        [k, v] = [s.strip() for s in i.split("=")]
        d[k] = unquote(v)
    return d