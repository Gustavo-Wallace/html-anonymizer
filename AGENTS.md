# Project Instructions for Codex

This is a Java desktop tool for anonymizing sensitive data in HTML files.

## Main goal

Build a Windows desktop application that:
- receives an input folder;
- recursively scans all `.html` and `.htm` files;
- generates an output folder with the same folder structure;
- writes only modified HTML files to the output folder;
- does not copy non-HTML files;
- anonymizes phone numbers and Internal Ticket Number values;
- preserves the original formatting of matched values whenever possible;
- keeps replacements consistent during one execution.

## Technical constraints

- Use Java.
- Use Maven.
- Use Swing for the desktop interface.
- Keep core logic independent from UI logic.
- Add unit tests for core behavior.
- Do not modify the original files.
- Do not generate CSV files.
- Prefer small, focused changes.

## Package organization

- `core`: anonymization logic.
- `file`: folder traversal and file processing.
- `ui`: Swing interface.
- `Main`: application entry point.

## Sensitive data rules

Phone examples to support:

```text
+550000000000
550000000000
+5500 000000000
5500000000000
+0 000 0000000
+0000000-0000
+00 0 0000 00-0000
+000 00000000
+000 000 000 000
+000 000 000000
+0 000 000-0000
```

Internal Ticket Number format:

```text
Internal Ticket Number	0000001
```

Only the number after `Internal Ticket Number` should be anonymized.
