#!/usr/bin/env python3
"""Generate or verify the one-row-per-case Zebrunner import CSV."""

import argparse
import csv
import io
import pathlib
import sys

from test_case_data import SOURCE_PATH, read_cases


ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT_PATH = ROOT / "docs" / "test-cases-formatted-import.csv"
OUTPUT_COLUMNS = [
    "Title",
    "Suite",
    "Suite Description",
    "Description",
    "Pre-conditions",
    "Priority",
    "Automation State",
    "Step Action",
    "Expected Result",
]


def parse_args():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--ids",
        metavar="SL-18,SL-19",
        help="comma-separated test case IDs to include; omit to include every case",
    )
    parser.add_argument(
        "--output",
        type=pathlib.Path,
        default=OUT_PATH,
        help=f"output CSV path (default: {OUT_PATH.relative_to(ROOT)})",
    )
    parser.add_argument(
        "--check",
        action="store_true",
        help="fail instead of writing when the default checked-in export is out of date",
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


def build_rows(cases):
    rows = []
    for case in cases:
        actions = "\n".join(
            f"{number}. {action}" for number, (action, _) in enumerate(case.steps, start=1)
        )
        results = "\n".join(
            f"{number}. {expected}" for number, (_, expected) in enumerate(case.steps, start=1)
        )
        rows.append(
            {
                "Title": f"{case.case_id} {case.title}",
                "Suite": case.suite,
                "Suite Description": case.suite_description,
                "Description": case.description,
                "Pre-conditions": case.preconditions,
                "Priority": case.priority,
                "Automation State": case.automation_state,
                "Step Action": actions,
                "Expected Result": results,
            }
        )
    return rows


def render(rows):
    output = io.StringIO(newline="")
    writer = csv.DictWriter(output, fieldnames=OUTPUT_COLUMNS, lineterminator="\n")
    writer.writeheader()
    writer.writerows(rows)
    return output.getvalue()


def main():
    args = parse_args()
    try:
        cases = select_cases(read_cases(), args.ids)
        content = render(build_rows(cases))
    except (ValueError, OSError, csv.Error) as error:
        print(f"error: {error}", file=sys.stderr)
        raise SystemExit(2)

    output = args.output.resolve()
    if args.check:
        if args.ids is not None or output != OUT_PATH.resolve():
            raise SystemExit("--check only supports the default full export")
        if not output.exists() or output.read_text(encoding="utf-8") != content:
            print(
                f"{output.relative_to(ROOT)} is out of date; run "
                f"{pathlib.Path(__file__).relative_to(ROOT)}",
                file=sys.stderr,
            )
            raise SystemExit(1)
        print(f"{output.name} matches {SOURCE_PATH.name}")
        return

    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", newline="", encoding="utf-8") as handle:
        handle.write(content)
    print(f"{output.name}: {len(cases)} test cases")


if __name__ == "__main__":
    main()
