# Result Import Contract and Safety

> **The actual Excel workbook format has NOT yet been defined. The real workbook/template will be provided by the project owner. Do not implement or assume the Excel schema before that file is reviewed.**

## Intended responsibilities

The principal creates an assessment, obtains or provides the approved format, uploads a workbook, reviews validation and a preview, explicitly confirms a transactional import, and then separately publishes draft results. Apache POI is the planned parser once the contract exists.

## Import principles

- Accept uploads only from an authorized principal and under a documented server-side size/type policy.
- Keep temporary source files private, short-lived, and cleaned after success/failure according to the implemented policy.
- Validate before mutation; show a human-readable preview and errors before confirmation.
- Confirmation performs a transactional import where possible. On failure, do not leave partial result records; record safe audit details and provide rollback/recovery guidance.
- Detect duplicate imports or overlapping result data using the eventual approved business key; require an explicit, auditable resolution rather than silently overwriting data.
- Imported results are draft. Only explicit principal publication makes an assessment available to public individual lookup.
- Tests will use owner-provided representative workbooks after the real contract is documented.

## Pending Excel Contract

The following are deliberately unknown: workbook and sheet names; columns and headers; subject ordering; student identifiers; whether a result PIN is included; marks formats; absent representation; formulas; total/percentage columns; merged cells; multiple-sheet behavior; validation meanings; duplicate identity; maximum/passing marks; and any calculated-result rules. The implementation must review the real workbook and record these decisions before parser or import logic begins.
