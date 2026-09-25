#!/usr/bin/env python3
"""Create a one-row-per-case import CSV from test-cases-formatted.csv.

The source already contains Zebrunner suite metadata and numbered multiline steps.
This script selects requested legacy IDs, validates the numbering, and writes the
action column as ``Step Action`` for import targets that use that field name.
"""

import argparse
import csv
import pathlib
import re
from collections import OrderedDict


ROOT = pathlib.Path(__file__).resolve().parent.parent
SOURCE_PATH = ROOT / "docs" / "test-cases-formatted.csv"
OUT_PATH = ROOT / "docs" / "test-cases-formatted-import.csv"

SOURCE_COLUMNS = [
    "Title",
    "Suite",
    "Suite Description",
    "Description",
    "Pre-conditions",
    "Priority",
    "Automation State",
    "Step",
    "Expected Result",
]

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

CASE_ID_PATTERN = re.compile(r"^(SL-\d+)\s+")
NUMBERED_LINE_PATTERN = re.compile(r"^(\d+)\.\s+(.+)$")


def parse_args():
    parser = argparse.ArgumentParser(
        description="Generate a one-row-per-case formatted import CSV."
    )
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
    return parser.parse_args()


def case_id_from_title(title):
    match = CASE_ID_PATTERN.match(title.strip())
    if match is None:
        raise SystemExit(f"Title does not start with an SL-nn ID: {title!r}")
    return match.group(1)


def read_cases():
    cases = OrderedDict()
    with SOURCE_PATH.open(newline="", encoding="utf-8") as handle:
        reader = csv.DictReader(handle)
        if reader.fieldnames != SOURCE_COLUMNS:
            raise SystemExit(
                "Unsupported formatted CSV header. Expected: "
                + ",".join(SOURCE_COLUMNS)
            )
        for row in reader:
            case_id = case_id_from_title(row["Title"])
            if case_id in cases:
                raise SystemExit(f"Duplicate formatted row for {case_id}.")
            cases[case_id] = row
    if not cases:
        raise SystemExit(f"No test cases found in {SOURCE_PATH}.")
    return cases


def select_cases(cases, ids):
    if ids is None:
        return cases

    requested = {case_id.strip() for case_id in ids.split(",") if case_id.strip()}
    if not requested:
        raise SystemExit("--ids must contain at least one test case ID.")

    unknown = requested - set(cases)
    if unknown:
        raise SystemExit(f"Unknown test case IDs: {', '.join(sorted(unknown))}")

    return OrderedDict((case_id, row) for case_id, row in cases.items() if case_id in requested)


def numbered_steps(case_id, column, value):
    lines = value.splitlines()
    if not lines:
        raise SystemExit(f"{case_id}: {column} is empty.")

    numbers = []
    for line in lines:
        match = NUMBERED_LINE_PATTERN.fullmatch(line.strip())
        if match is None:
            raise SystemExit(f"{case_id}: invalid numbered line in {column}: {line!r}")
        numbers.append(int(match.group(1)))

    expected = list(range(1, len(lines) + 1))
    if numbers != expected:
        raise SystemExit(
            f"{case_id}: {column} numbering must be consecutive from 1; found {numbers}."
        )
    return numbers


def build_rows(cases):
    output = []
    for case_id, row in cases.items():
        action_numbers = numbered_steps(case_id, "Step Action", row["Step"])
        result_numbers = numbered_steps(case_id, "Expected Result", row["Expected Result"])
        if action_numbers != result_numbers:
            raise SystemExit(
                f"{case_id}: Step Action and Expected Result numbering do not match."
            )

        formatted = {column: row[column] for column in SOURCE_COLUMNS if column != "Step"}
        formatted["Step Action"] = row["Step"]
        output.append({column: formatted[column] for column in OUTPUT_COLUMNS})
    return output


def write_rows(path, rows):
    path = path.resolve()
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=OUTPUT_COLUMNS, lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)
    return path


def main():
    args = parse_args()
    cases = select_cases(read_cases(), args.ids)
    rows = build_rows(cases)
    output = write_rows(args.output, rows)
    print(f"{output.name}: {len(rows)} test cases")


if __name__ == "__main__":
    main()
