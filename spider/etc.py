import os

# with open("aweme_meta/2021-04-18.txt", "w") as wf:
#     for i in range(10):
#         with open(f"aweme_meta/{i}.txt", "r") as rf:
#             for l in rf.readlines():
#                 wf.write(l)

with open("raw_meta/2021-04-17.txt", "w") as wf:
    for fn in os.listdir("raw_meta"):
        if fn.startswith("2021-04"):
            continue
        os.remove(f"raw_meta/{fn}")
        # key = fn[:-4]
        # with open(f"raw_meta/{fn}", "r") as rf:
        #     for l in rf.readlines():
        #         wf.write(f"{key} \t{l}")

# with open("raw_meta/2021-04-18.txt", "w") as wf, open("raw_meta/meta.txt", "r") as rf:
#     for l in rf.readlines():
#         wf.write(l)
