def get_nested(o, attrs):
    obj = o
    for attr in attrs:
        if attr in obj:
            obj = obj[attr]
        else:
            return None
    return obj


# import os
# import datetime

# hdfs_path = "/user/slave/websac/tiktok"
# now = datetime.datetime.now()
# for i in range(30):
#     time_str = (now + datetime.timedelta(days=i)).strftime("%Y-%m-%d")
#     os.system(f"gohdfs mkdir {hdfs_path}/{time_str}")
#     print(time_str)
