"""Validated access to the canonical one-row-per-case test design CSV."""

from __future__ import annotations

import csv
import pathlib
import re
from dataclasses import dataclass


ROOT = pathlib.Path(__file__).resolve().parent.parent
SOURCE_PATH = ROOT / "docs" / "test-cases-formatted.csv"

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

CASE_TITLE_PATTERN = re.compile(r"^(SL-\d+)\s+(.+)$")
NUMBERED_LINE_PATTERN = re.compile(r"^(\d+)\.\s+(.+)$")


@dataclass(frozen=True)
class TestCase:
    case_id: str
    title: str
    suite: str
    suite_description: str
    description: str
    preconditions: str
    priority: str
    automation_state: str
    steps: tuple[tuple[str, str], ...]

    @property
    def area(self) -> str:
        return self.suite.rsplit(" > ", maxsplit=1)[-1]

    def description_value(self, label: str) -> str | None:
        prefix = f"{label}: "
        return next(
            (line[len(prefix):] for line in self.description.splitlines() if line.startswith(prefix)),
            None,
        )


def _numbered_values(case_id: str, column: str, value: str) -> list[str]:
    lines = value.splitlines()
    if not lines:
        raise ValueError(f"{case_id}: {column} is empty")

    values = []
    for expected_number, line in enumerate(lines, start=1):
        match = NUMBERED_LINE_PATTERN.fullmatch(line.strip())
        if match is None:
            raise ValueError(f"{case_id}: invalid numbered line in {column}: {line!r}")
        number, text = int(match.group(1)), match.group(2)
        if number != expected_number:
            raise ValueError(
                f"{case_id}: {column} numbering must be consecutive from 1; "
                f"expected {expected_number}, found {number}"
            )
        values.append(text)
    return values


def read_cases(path: pathlib.Path = SOURCE_PATH) -> list[TestCase]:
    cases = []
    seen_ids = set()
    with path.open(newline="", encoding="utf-8") as handle:
        reader = csv.DictReader(handle)
        if reader.fieldnames != SOURCE_COLUMNS:
            raise ValueError(
                "Unsupported formatted CSV header. Expected: " + ",".join(SOURCE_COLUMNS)
            )

        for row_number, row in enumerate(reader, start=2):
            match = CASE_TITLE_PATTERN.fullmatch(row["Title"].strip())
            if match is None:
                raise ValueError(
                    f"row {row_number}: title does not start with an SL-nn ID: {row['Title']!r}"
                )
            case_id, title = match.groups()
            if case_id in seen_ids:
                raise ValueError(f"row {row_number}: duplicate formatted row for {case_id}")

            actions = _numbered_values(case_id, "Step", row["Step"])
            results = _numbered_values(case_id, "Expected Result", row["Expected Result"])
            if len(actions) != len(results):
                raise ValueError(
                    f"{case_id}: Step and Expected Result contain different numbers of lines"
                )
            if f"Legacy ID: {case_id}" not in row["Description"].splitlines():
                raise ValueError(f"{case_id}: Description has no matching Legacy ID")

            cases.append(
                TestCase(
                    case_id=case_id,
                    title=title,
                    suite=row["Suite"],
                    suite_description=row["Suite Description"],
                    description=row["Description"],
                    preconditions=row["Pre-conditions"],
                    priority=row["Priority"],
                    automation_state=row["Automation State"],
                    steps=tuple(zip(actions, results)),
                )
            )
            seen_ids.add(case_id)

    if not cases:
        raise ValueError(f"No test cases found in {path}")
    return cases
