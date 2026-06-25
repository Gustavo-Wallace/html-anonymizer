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
+556291064865
556191213756
+5511 972792222
5511972792222
+1 305 9023346
+1609618-4620
+54 9 3757 50-5105
+591 72040940
+351 963 830 852
+595 984 563687
+1 954 393-7920
```

Internal Ticket Number format:

```text
Internal Ticket Number	2450379
```

Only the number after `Internal Ticket Number` should be anonymized.