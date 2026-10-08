"""反算《测试用例.md》各章用例条数，校验统计表与正文行数是否自洽。

规矩 R12：文档里的统计数字一律用脚本从正文行反算，不得手填。
用法：python verify-testcase-stats.py docs/02-设计文档/测试用例.md
"""
import re
import sys
from collections import OrderedDict

TYPE_MAP = {"正常": "normal", "异常": "error", "边界": "edge"}


def main(path: str) -> int:
    text = open(path, encoding="utf-8").read()
    lines = text.splitlines()

    # 1) 逐行扫描，按 "## ... （TC-X）" 标题切章，收集用例行
    section = None
    actual: "OrderedDict[str, dict]" = OrderedDict()
    case_re = re.compile(r"^\|\s*([A-Z]\d{2})\s*\|\s*(正常|异常|边界)\s*\|")
    head_re = re.compile(r"^##\s+.*?（(TC-[A-Z])）")

    for ln in lines:
        m = head_re.match(ln)
        if m:
            section = m.group(1)
            actual.setdefault(section, {"normal": 0, "error": 0, "edge": 0})
            continue
        m = case_re.match(ln)
        if m and section:
            actual[section][TYPE_MAP[m.group(2)]] += 1

    # 2) 解析统计表
    declared: "OrderedDict[str, dict]" = OrderedDict()
    for ln in lines:
        m = re.match(
            r"^\|\s*[^|]*?(TC-[A-Z])\s*\|\s*(\d+)\s*\|\s*(\d+)\s*\|\s*(\d+)\s*\|\s*(\d+)\s*\|",
            ln.strip(),
        )
        if m:
            declared[m.group(1)] = {
                "normal": int(m.group(2)),
                "error": int(m.group(3)),
                "edge": int(m.group(4)),
                "total": int(m.group(5)),
            }

    # 3) 比对
    ok = True
    print(f"{'章节':<8}{'实际(正/异/边)':<20}{'统计表(正/异/边)':<20}{'结果'}")
    for sec in actual:
        a = actual[sec]
        d = declared.get(sec)
        a_str = f"{a['normal']}/{a['error']}/{a['edge']}={sum(a.values())}"
        if d is None:
            print(f"{sec:<8}{a_str:<20}{'—':<20}统计表缺该章")
            ok = False
            continue
        d_str = f"{d['normal']}/{d['error']}/{d['edge']}={d['total']}"
        same = (
            a["normal"] == d["normal"]
            and a["error"] == d["error"]
            and a["edge"] == d["edge"]
            and sum(a.values()) == d["total"]
        )
        print(f"{sec:<8}{a_str:<20}{d_str:<20}{'一致' if same else '不一致 ✗'}")
        ok = ok and same

    tn = sum(v["normal"] for v in actual.values())
    te = sum(v["error"] for v in actual.values())
    tx = sum(v["edge"] for v in actual.values())
    tt = tn + te + tx
    print(f"\n实际合计：正常 {tn} / 异常 {te} / 边界 {tx} = {tt} 条")

    # 4) 头部声明的总条数
    m = re.search(r"共\s*\*\*(\d+)\s*条\*\*", text)
    if m:
        head = int(m.group(1))
        print(f"文档头部声明：{head} 条 → {'一致' if head == tt else '不一致 ✗'}")
        ok = ok and head == tt

    print("\n结论：" + ("全部自洽" if ok else "存在不一致，需修正"))
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1] if len(sys.argv) > 1 else "docs/02-设计文档/测试用例.md"))
