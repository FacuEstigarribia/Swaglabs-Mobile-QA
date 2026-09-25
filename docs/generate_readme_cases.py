#!/usr/bin/env python3
"""Regenerate or verify README.md's test-case section."""

import argparse
import pathlib
import re
import sys
from collections import OrderedDict

from test_case_data import SOURCE_PATH, read_cases


ROOT = pathlib.Path(__file__).resolve().parent.parent
README = ROOT / "README.md"
BEGIN = "<!-- BEGIN GENERATED TEST CASES -->"
END = "<!-- END GENERATED TEST CASES -->"


def escape(text):
    return text.replace("|", "\\|")


def build():
    by_area = OrderedDict()
    for case in read_cases():
        by_area.setdefault(case.area, []).append(case)

    out = [BEGIN, ""]
    for area, cases in by_area.items():
        out.extend([f"### {area}", ""])
        for case in cases:
            platforms = case.description_value("Platforms")
            test_data = case.description_value("Test data")
            automated_by = case.description_value("Automated by")
            if platforms is None or automated_by is None:
                raise ValueError(
                    f"{case.case_id}: Description must contain Platforms and Automated by metadata"
                )

            out.extend(
                [
                    f"#### {case.case_id} — {case.title}",
                    "",
                    f"- **Priority:** {case.priority}",
                    f"- **Precondition:** {case.preconditions}",
                    f"- **Platforms:** {platforms}",
                ]
            )
            if test_data:
                out.append(f"- **Test data:** {test_data}")
            out.extend(
                [
                    f"- **Automated by:** `{automated_by}`",
                    "",
                    "| # | Action | Expected Result |",
                    "|---|--------|-----------------|",
                ]
            )
            for number, (action, expected) in enumerate(case.steps, start=1):
                out.append(f"| {number} | {escape(action)} | {escape(expected)} |")
            out.append("")
    out.append(END)
    return "\n".join(out)


def parse_args():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--check",
        action="store_true",
        help="fail instead of writing when README.md is out of date",
    )
    return parser.parse_args()


def main():
    args = parse_args()
    section = build()
    text = README.read_text(encoding="utf-8")
    pattern = re.compile(re.escape(BEGIN) + r".*?" + re.escape(END), re.DOTALL)
    if not pattern.search(text):
        raise SystemExit(f"Markers {BEGIN} / {END} not found in README.md")
    updated = pattern.sub(lambda _: section, text)

    if args.check:
        if updated != text:
            print(
                f"README.md is out of date; run {pathlib.Path(__file__).relative_to(ROOT)}",
                file=sys.stderr,
            )
            raise SystemExit(1)
        print(f"README.md matches {SOURCE_PATH.name}")
        return

    README.write_text(updated, encoding="utf-8")
    print(f"README.md test-case section regenerated from {SOURCE_PATH.name}")


if __name__ == "__main__":
    main()
