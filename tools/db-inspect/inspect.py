# 看一下设备上那份 Room 数据库里到底存了什么。
#
# 为什么需要它：`adb shell run-as` 拿不到 root，`/sdcard` 又不能从 run-as 里写
# （应用 uid 没有外部存储权限），所以二进制得走 `adb exec-out ... cat` + cmd 重定向拉回来。
# PS 自己的 `>` 会破坏二进制，必须让 cmd.exe 做重定向。
#
# 用法（先用 cmd /c 把 xinxu.db、xinxu.db-wal、xinxu.db-shm 拉到同一个目录）：
#   python inspect.py <目录>
import sqlite3
import sys
import os

d = sys.argv[1]
db = os.path.join(d, "xinxu.db")
print(f"db: {db}  ({os.path.getsize(db)} B)")
for extra in ("xinxu.db-wal", "xinxu.db-shm"):
    p = os.path.join(d, extra)
    if os.path.exists(p):
        print(f"  + {extra}  ({os.path.getsize(p)} B)")

c = sqlite3.connect(db)
print("tables:", [r[0] for r in c.execute("SELECT name FROM sqlite_master WHERE type='table'")])
print("user_version:", c.execute("PRAGMA user_version").fetchone()[0])

try:
    rows = c.execute("SELECT date, colorIds, note, updatedAt FROM day_entry ORDER BY date").fetchall()
except sqlite3.OperationalError as e:
    print("query failed:", e)
    sys.exit(1)

print(f"day_entry: {len(rows)} 行")
for r in rows:
    print("   ", r)
