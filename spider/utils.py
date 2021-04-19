from datetime import datetime

def get_nested(o, attrs):
    obj = o
    for attr in attrs:
        if attr in obj:
            obj = obj[attr]
        else:
            return None
    return obj

meta_file = None
meta_file_time = None
def get_meta_file(dir):
    global meta_file
    global meta_file_time
    day = datetime.now().strftime("%Y-%m-%d")
    if day != meta_file_time:
        if meta_file:
            meta_file.close()
        meta_file = open(f"{dir}/{day}.txt", "a")
        meta_file_time = day
    return meta_file


# import os
# import datetime

# hdfs_path = "/user/slave/websac/tiktok"
# now = datetime.datetime.now()
# for i in range(30):
#     time_str = (now + datetime.timedelta(days=i)).strftime("%Y-%m-%d")
#     os.system(f"gohdfs mkdir {hdfs_path}/{time_str}")
#     print(time_str)
