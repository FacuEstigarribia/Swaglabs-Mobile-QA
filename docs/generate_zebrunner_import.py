#!/usr/bin/env python3
"""Generate or verify the step-per-row Zebrunner TCM import CSV."""

import argparse
import csv
import io
import pathlib
import re
import sys

from test_case_data import SOURCE_COLUMNS, SOURCE_PATH, read_cases


ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT_PATH = ROOT / "docs" / "zebrunner-test-cases.csv"
TEST_DIR = ROOT / "src" / "test" / "java" / "com" / "mobile" / "swaglabs" / "qa" / "test"


def parse_args():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--ids",
        metavar="SL-18,SL-19",
        help="comma-separated test case IDs to include; omit to include every case",
    )
    parser.add_argument(
        "--check",
        action="store_true",
        help="fail instead of writing when the checked-in export is out of date",
    )
    return parser.parse_args()


def select_cases(cases, ids):
    if ids is None:
        return cases
    requested = {case_id.strip() for case_id in ids.split(",") if case_id.strip()}
    if not requested:
        raise ValueError("--ids must contain at least one test case ID")
    unknown = requested - {case.case_id for case in cases}
    if unknown:
        raise ValueError(f"Unknown test case IDs: {', '.join(sorted(unknown))}")
    return [case for case in cases if case.case_id in requested]


def find_test_methods():
    methods = set()
    for source in sorted(TEST_DIR.glob("*Test.java")):
        for method in re.findall(r"public void (test\w+)\s*\(", source.read_text(encoding="utf-8")):
            methods.add(f"{source.stem}.{method}")
    return methods


def build_rows(cases):
    test_methods = find_test_methods()
    rows = []
    for case in cases:
        automated_by = case.description_value("Automated by")
        if automated_by not in test_methods:
            raise ValueError(
                f"{case.case_id}: no test class declares '{automated_by}'; "
                f"update {SOURCE_PATH.name} or the test method name"
            )
        shared = {
            "Title": f"{case.case_id} {case.title}",
            "Suite": case.suite,
            "Suite Description": case.suite_description,
            "Description": case.description,
            "Pre-conditions": case.preconditions,
            "Priority": case.priority,
            "Automation State": case.automation_state,
        }
        for action, expected in case.steps:
            rows.append(dict(shared, Step=action, **{"Expected Result": expected}))
    return rows


def render(rows):
    output = io.StringIO(newline="")
    writer = csv.DictWriter(output, fieldnames=SOURCE_COLUMNS, lineterminator="\n")
    writer.writeheader()
    writer.writerows(rows)
    return output.getvalue()


def main():
    args = parse_args()
    try:
        cases = select_cases(read_cases(), args.ids)
        rows = build_rows(cases)
        content = render(rows)
    except (ValueError, OSError, csv.Error) as error:
        print(f"error: {error}", file=sys.stderr)
        raise SystemExit(2)

    if args.check:
        if args.ids is not None:
            raise SystemExit("--check cannot be combined with --ids")
        if not OUT_PATH.exists() or OUT_PATH.read_text(encoding="utf-8") != content:
            print(
                f"{OUT_PATH.relative_to(ROOT)} is out of date; run "
                f"{pathlib.Path(__file__).relative_to(ROOT)}",
                file=sys.stderr,
            )
            raise SystemExit(1)
        print(f"{OUT_PATH.name} matches {SOURCE_PATH.name}")
        return

    with OUT_PATH.open("w", encoding="utf-8", newline="") as handle:
        handle.write(content)
    print(f"{OUT_PATH.name}: {len(cases)} test cases, {len(rows)} steps")


if __name__ == "__main__":
    main()
