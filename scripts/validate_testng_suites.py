#!/usr/bin/env python3
"""Parse and validate every TestNG suite without contacting a device farm."""

import pathlib
import re
import sys
import xml.etree.ElementTree as ET


ROOT = pathlib.Path(__file__).resolve().parent.parent
SUITE_DIR = ROOT / "src" / "test" / "resources" / "testng_suites"
TEST_SOURCE_DIR = ROOT / "src" / "test" / "java"
EXPECTED_SUITE_COUNT = 17


def source_for(class_name):
    return TEST_SOURCE_DIR / pathlib.Path(*class_name.split(".")).with_suffix(".java")


def main():
    suites = sorted(SUITE_DIR.glob("*.xml"))
    errors = []
    if len(suites) != EXPECTED_SUITE_COUNT:
        errors.append(
            f"expected {EXPECTED_SUITE_COUNT} TestNG XML files, found {len(suites)}"
        )

    suite_names = set()
    for suite in suites:
        try:
            root = ET.parse(suite).getroot()
        except ET.ParseError as error:
            errors.append(f"{suite.name}: invalid XML: {error}")
            continue

        if root.tag != "suite":
            errors.append(f"{suite.name}: root element must be <suite>, found <{root.tag}>")
            continue
        name = root.get("name", "").strip()
        if not name:
            errors.append(f"{suite.name}: suite name is required")
        elif name in suite_names:
            errors.append(f"{suite.name}: duplicate suite name {name!r}")
        suite_names.add(name)

        tests = root.findall("test")
        classes = root.findall("./test/classes/class")
        if not tests:
            errors.append(f"{suite.name}: at least one <test> is required")
        if not classes:
            errors.append(f"{suite.name}: at least one test <class> is required")

        for class_element in classes:
            class_name = class_element.get("name", "").strip()
            source = source_for(class_name)
            if not class_name:
                errors.append(f"{suite.name}: class name is required")
                continue
            if not source.is_file():
                errors.append(f"{suite.name}: test class source not found: {class_name}")
                continue

            source_text = source.read_text(encoding="utf-8")
            for include in class_element.findall("./methods/include"):
                method = include.get("name", "").strip()
                if not method:
                    errors.append(f"{suite.name}: included method name is required")
                elif re.search(rf"\b{re.escape(method)}\s*\(", source_text) is None:
                    errors.append(
                        f"{suite.name}: included method {class_name}.{method} was not found"
                    )

    if errors:
        print("TestNG suite validation failed:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1

    print(f"Parsed and validated all {len(suites)} TestNG suite XML files")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
