#!/usr/bin/env python3
"""Reshape docs/test-cases.csv into the CSV the Zebrunner TCM importer accepts.

Keeps the CSV as the single source of truth. Run after editing it:
    python3 docs/generate_zebrunner_import.py
To generate an import containing only specific cases:
    python3 docs/generate_zebrunner_import.py --ids SL-18,SL-19,SL-20,SL-21,SL-22
The result is written to docs/zebrunner-test-cases.csv and uploaded by hand on the
project's Test Cases page (Import > CSV).

Only a fixed set of column names is recognised by the importer; everything else is
silently ignored. That is why TC_ID moves into the title, and why Platform, Test Data
and Automated Method are folded into the description and the step text instead of
being columns of their own.

Steps stay one-per-row, which the importer supports directly ("one step per CSV file
row"), so the row shape of the source CSV survives the migration unchanged. Case-level
values are repeated on every row of a case rather than left blank after the first:
the importer groups rows by Title, and identical values are unambiguous under either
reading of that grouping.
"""
import argparse
import csv
import pathlib
import re
from collections import OrderedDict

ROOT = pathlib.Path(__file__).resolve().parent.parent
CSV_PATH = ROOT / "docs" / "test-cases.csv"
OUT_PATH = ROOT / "docs" / "zebrunner-test-cases.csv"
TEST_DIR = ROOT / "src" / "test" / "java" / "com" / "mobile" / "swaglabs" / "qa" / "test"

# The importer's recognised columns, in the order they are written out.
COLUMNS = [
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

# Nested suites are created on import from the ">" delimiter, parents included.
ROOT_SUITE = "Swag Labs Mobile"
AREA_ORDER = ("Login", "Product Grid", "Filtering", "Cart", "Account")
AREA_DESCRIPTIONS = {
    "Login": "Authentication: the accounts this build accepts, and the rejection of one it does not.",
    "Product Grid": "The catalog grid, its layout toggle, and navigation into and back out of product details.",
    "Filtering": "The four sort options and the order they impose on the grid.",
    "Cart": "Adding and removing items, the cart badge, and cart contents across navigation.",
    "Account": "The navigation menu and logout.",
}
AUTOMATION_STATE = "Automated"


def parse_args():
    parser = argparse.ArgumentParser(
        description="Generate a Zebrunner TCM import CSV from docs/test-cases.csv."
    )
    parser.add_argument(
        "--ids",
        metavar="SL-01,SL-02",
        help="comma-separated test case IDs to include; omit to include every case",
    )
    return parser.parse_args()


def find_test_classes():
    """Map each automated method name to the test class that declares it."""
    owners = {}
    for source in sorted(TEST_DIR.glob("*Test.java")):
        for method in re.findall(r"public void (test\w+)\s*\(", source.read_text(encoding="utf-8")):
            owners[method] = source.stem
    return owners


def read_cases():
    cases = OrderedDict()
    with CSV_PATH.open(newline="", encoding="utf-8") as handle:
        for row in csv.DictReader(handle):
            case = cases.setdefault(row["TC_ID"], {"meta": row, "steps": []})
            case["steps"].append(row)
    return cases


def select_cases(cases, ids):
    """Return only requested cases, preserving their order in the source CSV."""
    if ids is None:
        return cases

    requested = {case_id.strip() for case_id in ids.split(",") if case_id.strip()}
    if not requested:
        raise SystemExit("--ids must contain at least one test case ID.")

    unknown = requested - set(cases)
    if unknown:
        raise SystemExit(f"Unknown test case IDs: {', '.join(sorted(unknown))}")

    return OrderedDict((case_id, case) for case_id, case in cases.items() if case_id in requested)


def describe(meta, steps, owners):
    """Everything the importer has no column for, as the case description."""
    method = meta["Automated Method"]
    owner = owners.get(method)
    if owner is None:
        raise SystemExit(
            f"{meta['TC_ID']}: no test class declares '{method}'. "
            f"Update docs/test-cases.csv or the test method name."
        )

    lines = [
        meta["Title"] + ".",
        "",
        f"Legacy ID: {meta['TC_ID']}",
        f"Platforms: {meta['Platform'].replace(';', ', ')}",
        f"Automated by: {owner}.{method}",
    ]
    data = sorted({step["Test Data"].strip() for step in steps if step["Test Data"].strip()})
    if data:
        lines.append(f"Test data: {'; '.join(data)}")
    return "\n".join(lines)


def step_text(step):
    """The action, with this step's own test data inlined so nothing is lost."""
    action = step["Action"].strip()
    data = step["Test Data"].strip()
    return f"{action} (test data: {data})" if data else action


def build_rows(cases, owners, require_all_areas=True):
    rows = []
    for area in AREA_ORDER:
        in_area = [case for case in cases.values() if case["meta"]["Area"] == area]
        if require_all_areas and not in_area:
            raise SystemExit(f"No cases found for area '{area}'; check AREA_ORDER.")
        for case in in_area:
            meta = case["meta"]
            shared = {
                # TC_ID is not an importable field, and Zebrunner assigns its own keys,
                # so the legacy id is prefixed onto the title to stay searchable.
                "Title": f"{meta['TC_ID']} {meta['Title']}",
                "Suite": f"{ROOT_SUITE} > {area}",
                "Suite Description": AREA_DESCRIPTIONS[area],
                "Description": describe(meta, case["steps"], owners),
                "Pre-conditions": meta["Precondition"],
                "Priority": meta["Priority"],
                "Automation State": AUTOMATION_STATE,
            }
            for step in case["steps"]:
                rows.append(
                    dict(shared, **{
                        "Step": step_text(step),
                        "Expected Result": step["Expected Result"].strip(),
                    })
                )
    unknown = {case["meta"]["Area"] for case in cases.values()} - set(AREA_ORDER)
    if unknown:
        raise SystemExit(f"Areas missing from AREA_ORDER: {', '.join(sorted(unknown))}")
    return rows


def main():
    args = parse_args()
    cases = select_cases(read_cases(), args.ids)
    rows = build_rows(cases, find_test_classes(), require_all_areas=args.ids is None)
    with OUT_PATH.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=COLUMNS)
        writer.writeheader()
        writer.writerows(rows)
    print(f"{OUT_PATH.name}: {len(cases)} test cases, {len(rows)} steps")


if __name__ == "__main__":
    main()
