from hdfs import InsecureClient

hdfs_client = InsecureClient(
    "http://master004.diablo.hadoop.nm.ted:50070/", user="slave"
)

# for fn in hdfs_client.list("/user/slave/websac/tiktok/2021-04-12"):
#     print(fn)
#     hdfs_client.delete("/user/slave/websac/tiktok/2021-04-12/" + fn)
hdfs_client.makedirs("/user/slave/websac/tiktok/2021-04-12")

