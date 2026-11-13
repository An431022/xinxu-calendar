# 心绪日历 proguard 规则
#
# **release 变体是开着混淆的**（`isMinifyEnabled = true` + `isShrinkResources = true`，
# 9.3MB → 1.3MB）。所以这个文件不是占位，是"真出问题时往哪儿加规则"。
#
# 目前是空的，因为 Room 和 Compose 都自带 consumer rules，实测不用手写 keep：
#   2026-10-19 在模拟器上装了 release 包，逐项验过启动、涂色落库、杀进程重进、
#   统计页、设置页、导出 JSON、导入 JSON —— 全通过，没有 NoSuchMethod/ClassNotFound。
#
# 如果以后 release 出「debug 正常、release 崩」的问题，按这个顺序查：
#   1. 先在 app/build/outputs/mapping/release/mapping.txt 里反解崩掉的类名
#      （这个文件每次构建都会重新生成，**每次都要跟包一起留档**，否则堆栈读不出来）
#   2. 再针对那个库加 -keep，**别一上来就 -dontobfuscate** —— 那等于关掉整个 release 的意义
#
# 特别注意：**Room 的实体类、DAO 接口不要动**。它们靠注解处理器生成实现，
# 混淆掉字段名会让列名对不上（`note` 列存的是 JSON，字段名是数据的一部分，不是可随便改的标识）。
