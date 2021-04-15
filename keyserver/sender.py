import csv
import redis

# with open("keys.csv", "r") as rf:
#     with open("out.txt", "w") as wf:
#         reader = csv.reader(rf)
#         for row in reader:
#             wf.write(row[1].replace("\n", " ")+"\n")


client = redis.Redis(host="10.143.15.226", port="6379", password="waibiwaibiwaibibabo")

with open("out.txt", "r") as f:
    for l in f.readlines():
        client.rpush("tiktok:search", l.strip())