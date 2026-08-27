#!/usr/bin/env python3
"""Regenerate the test-case section of README.md from docs/test-cases.csv.

Keeps the CSV as the single source of truth. Run after editing the CSV:
    python3 docs/generate_readme_cases.py
Everything between the BEGIN/END markers in README.md is replaced.
"""
import csv
import pathlib
import re
from collections import OrderedDict

ROOT = pathlib.Path(__file__).resolve().parent.parent
CSV_PATH = ROOT / "docs" / "test-cases.csv"
README = ROOT / "README.md"
BEGIN = "<!-- BEGIN GENERATED TEST CASES -->"
END = "<!-- END GENERATED TEST CASES -->"


def escape(text):
    return text.replace("|", "\\|")


def build():
    cases = OrderedDict()
    with CSV_PATH.open(newline="", encoding="utf-8") as handle:
        for row in csv.DictReader(handle):
            case = cases.setdefault(row["TC_ID"], {"meta": row, "steps": []})
            case["steps"].append(row)

    out = [BEGIN, ""]
    for area in ("Product Grid", "Filtering", "Cart", "Account", "Login"):
        in_area = [(k, v) for k, v in cases.items() if v["meta"]["Area"] == area]
        if not in_area:
            continue
        out.append(f"### {area}")
        out.append("")
        for tc_id, case in in_area:
            meta = case["meta"]
            out.append(f"#### {tc_id} — {meta['Title']}")
            out.append("")
            out.append(f"- **Priority:** {meta['Priority']}")
            out.append(f"- **Precondition:** {meta['Precondition']}")
            out.append(f"- **Platforms:** {meta['Platform'].replace(';', ', ')}")
            data = sorted({s["Test Data"] for s in case["steps"] if s["Test Data"].strip()})
            if data:
                out.append(f"- **Test data:** {', '.join(data)}")
            out.append(f"- **Automated by:** `{meta['Automated Method']}`")
            out.append("")
            out.append("| # | Action | Expected Result |")
            out.append("|---|--------|-----------------|")
            for step in case["steps"]:
                out.append(
                    f"| {step['Step']} | {escape(step['Action'])} | {escape(step['Expected Result'])} |"
                )
            out.append("")
    out.append(END)
    return "\n".join(out)


def main():
    section = build()
    text = README.read_text(encoding="utf-8")
    pattern = re.compile(re.escape(BEGIN) + r".*?" + re.escape(END), re.DOTALL)
    if not pattern.search(text):
        raise SystemExit(f"Markers {BEGIN} / {END} not found in README.md")
    README.write_text(pattern.sub(lambda _: section, text), encoding="utf-8")
    print(f"README.md test-case section regenerated from {CSV_PATH.name}")


if __name__ == "__main__":
    main()
