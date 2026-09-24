#!/usr/bin/env python3
"""Collapse step-per-row Swag Labs test cases into one row per test case."""

from __future__ import annotations

import argparse
import csv
import os
import sys
import tempfile
from collections import OrderedDict
from pathlib import Path
from typing import Iterable


SOURCE_COLUMNS = [
    "TC_ID",
    "Title",
    "Area",
    "Priority",
    "Precondition",
    "Step",
    "Action",
    "Expected Result",
    "Test Data",
    "Platform",
    "Automated Method",
]

TARGET_COLUMNS = [
    "TC_ID",
    "Title",
    "Area",
    "Priority",
    "Precondition",
    "Step Action",
    "Expected Result",
    "Test Data",
    "Platform",
    "Automated Method",
]

CONSISTENT_COLUMNS = [
    "Title",
    "Area",
    "Priority",
    "Precondition",
    "Platform",
    "Automated Method",
]


class FormatError(ValueError):
    """Raised when input cannot be transformed without guessing."""


def clean(value: str | None) -> str:
    return "" if value is None else value.strip()


def default_output_path(source: Path) -> Path:
    return source.with_name(f"{source.stem}-formatted{source.suffix}")


def read_rows(source: Path) -> tuple[list[str], list[dict[str, str]]]:
    with source.open("r", encoding="utf-8-sig", newline="") as handle:
        reader = csv.DictReader(handle)
        header = list(reader.fieldnames or [])
        if header not in (SOURCE_COLUMNS, TARGET_COLUMNS):
            raise FormatError(
                "unsupported header; expected either:\n"
                f"  {','.join(SOURCE_COLUMNS)}\n"
                "or:\n"
                f"  {','.join(TARGET_COLUMNS)}"
            )

        rows: list[dict[str, str]] = []
        for line_number, raw in enumerate(reader, start=2):
            if None in raw:
                raise FormatError(f"line {line_number}: too many CSV fields")

            row = {column: clean(raw.get(column)) for column in header}
            if not any(row.values()):
                continue

            # A pasted formatted example may follow the expanded source. Stop at
            # its repeated target header so generated data is never re-ingested.
            if (
                header == SOURCE_COLUMNS
                and row["TC_ID"] == "TC_ID"
                and row["Step"] == "Step Action"
            ):
                break

            rows.append(row)

    if not rows:
        raise FormatError("input contains no test-case rows")
    return header, rows


def one_value(case_id: str, rows: list[dict[str, str]], column: str) -> str:
    values = list(OrderedDict.fromkeys(row[column] for row in rows if row[column]))
    if len(values) > 1:
        joined = " | ".join(values)
        raise FormatError(f"{case_id}: conflicting {column} values: {joined}")
    return values[0] if values else ""


def unique_test_data(rows: Iterable[dict[str, str]]) -> str:
    values = OrderedDict.fromkeys(row["Test Data"] for row in rows if row["Test Data"])
    return "; ".join(values)


def collapse(rows: list[dict[str, str]]) -> list[dict[str, str]]:
    cases: OrderedDict[str, list[dict[str, str]]] = OrderedDict()
    for row_number, row in enumerate(rows, start=2):
        case_id = row["TC_ID"]
        if not case_id:
            raise FormatError(f"line {row_number}: TC_ID is empty")
        cases.setdefault(case_id, []).append(row)

    output: list[dict[str, str]] = []
    for case_id, case_rows in cases.items():
        numbered: list[tuple[int, dict[str, str]]] = []
        seen_steps: set[int] = set()
        for row in case_rows:
            try:
                step = int(row["Step"])
            except ValueError as error:
                raise FormatError(f"{case_id}: Step must be a positive integer") from error
            if step < 1:
                raise FormatError(f"{case_id}: Step must be a positive integer")
            if step in seen_steps:
                raise FormatError(f"{case_id}: duplicate Step {step}")
            if not row["Action"]:
                raise FormatError(f"{case_id}: Action is empty for Step {step}")
            if not row["Expected Result"]:
                raise FormatError(f"{case_id}: Expected Result is empty for Step {step}")
            seen_steps.add(step)
            numbered.append((step, row))

        numbered.sort(key=lambda item: item[0])
        expected_steps = list(range(1, len(numbered) + 1))
        actual_steps = [step for step, _ in numbered]
        if actual_steps != expected_steps:
            raise FormatError(
                f"{case_id}: steps must be consecutive from 1; found {actual_steps}"
            )

        result = {column: "" for column in TARGET_COLUMNS}
        result["TC_ID"] = case_id
        for column in CONSISTENT_COLUMNS:
            result[column] = one_value(case_id, case_rows, column)
        result["Step Action"] = "\n".join(
            f"{step}. {row['Action']}" for step, row in numbered
        )
        result["Expected Result"] = "\n".join(
            f"{step}. {row['Expected Result']}" for step, row in numbered
        )
        result["Test Data"] = unique_test_data(row for _, row in numbered)
        output.append(result)

    return output


def normalize_target(rows: list[dict[str, str]]) -> list[dict[str, str]]:
    seen: set[str] = set()
    for row_number, row in enumerate(rows, start=2):
        case_id = row["TC_ID"]
        if not case_id:
            raise FormatError(f"line {row_number}: TC_ID is empty")
        if case_id in seen:
            raise FormatError(f"line {row_number}: duplicate target row for {case_id}")
        if not row["Step Action"] or not row["Expected Result"]:
            raise FormatError(f"{case_id}: numbered actions and expected results are required")
        seen.add(case_id)
    return rows


def write_rows(destination: Path, rows: list[dict[str, str]], force: bool) -> None:
    destination = destination.resolve()
    if destination.exists() and not force:
        raise FormatError(f"output already exists: {destination} (use --force to replace it)")
    destination.parent.mkdir(parents=True, exist_ok=True)

    temporary_name: str | None = None
    try:
        with tempfile.NamedTemporaryFile(
            "w",
            encoding="utf-8",
            newline="",
            dir=destination.parent,
            prefix=f".{destination.name}.",
            suffix=".tmp",
            delete=False,
        ) as handle:
            temporary_name = handle.name
            writer = csv.DictWriter(handle, fieldnames=TARGET_COLUMNS, lineterminator="\n")
            writer.writeheader()
            writer.writerows(rows)
        os.replace(temporary_name, destination)
        temporary_name = None
    finally:
        if temporary_name:
            Path(temporary_name).unlink(missing_ok=True)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Collapse Swag Labs step-per-row test cases into one row per case."
    )
    parser.add_argument("input", type=Path, help="source CSV path")
    parser.add_argument(
        "--output",
        "-o",
        type=Path,
        help="output CSV path (default: <input-stem>-formatted.csv)",
    )
    parser.add_argument(
        "--force",
        action="store_true",
        help="replace an existing output file, including in-place conversion",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    source = args.input.resolve()
    destination = (args.output or default_output_path(source)).resolve()
    try:
        header, rows = read_rows(source)
        output_rows = collapse(rows) if header == SOURCE_COLUMNS else normalize_target(rows)
        write_rows(destination, output_rows, args.force)
    except (FormatError, OSError, csv.Error) as error:
        print(f"error: {error}", file=sys.stderr)
        return 2

    if header == SOURCE_COLUMNS:
        print(
            f"Formatted {len(output_rows)} test cases from {len(rows)} step rows -> "
            f"{destination}"
        )
    else:
        print(f"Normalized {len(output_rows)} test cases -> {destination}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
