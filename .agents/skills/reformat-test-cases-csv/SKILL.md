---
name: reformat-test-cases-csv
description: Reformat this project's step-per-row test-case CSV into the canonical one-row-per-case CSV with numbered multiline actions and expected results. Use for requests to format, collapse, convert, or normalize Swag Labs test cases; do not use for Zebrunner imports or unrelated CSV schemas.
---

# Reformat Test Cases CSV

Convert the expanded test-case schema into exactly these columns, in this order:

```text
TC_ID,Title,Area,Priority,Precondition,Step Action,Expected Result,Test Data,Platform,Automated Method
```

Use `scripts/reformat_test_cases_csv.py` for the transformation. It accepts the project's expanded source schema (`Step` and `Action` are separate columns), groups every row sharing a `TC_ID`, and emits one row per test case.

## Workflow

1. Use the input path named by the user. If none is named, use `docs/test-cases.csv` when it exists.
2. Choose the user's requested output path. If none is given, write beside the input as `<stem>-formatted.csv`; preserve the source file.
3. Run:

   ```bash
   python3 <skill-dir>/scripts/reformat_test_cases_csv.py <input.csv> --output <output.csv>
   ```

   Add `--force` only when the user asked to replace an existing output or edit the source in place.
4. Parse the resulting CSV with Python's `csv` module and confirm the reported test-case count, exact header, and readable first record before reporting completion.

## Transformation rules

- Preserve test-case order by first appearance and sort each case's steps by its positive integer `Step` value.
- Build `Step Action` as `1. <Action>`, `2. <Action>`, and so on, separated by literal newlines inside the CSV cell.
- Build `Expected Result` with the same step numbers and line breaks.
- Preserve `TC_ID`, title, area, priority, precondition, platform, and automated method. Fail on conflicting non-empty values within one test case rather than guessing.
- Preserve all non-empty test data. When different steps contain different values, join the unique values in first-appearance order with `; `.
- Treat Markdown-looking escapes in pasted examples as presentation artifacts: output literal `TC_ID`, `_`, and `1.`, never backslash-escaped forms or surrounding `[[...]]`.
- Use UTF-8 and standard CSV quoting. Multiline cells must be quoted by the CSV writer.
- If a source file has a trailing, already-formatted example beginning with a repeated `TC_ID` header, ignore that trailing section. Never mix it into the source cases.

The script intentionally rejects `docs/zebrunner-test-cases.csv` and other schemas because their field mapping requires separate product decisions.
