import type { FieldErrors, FieldValues, Path } from "react-hook-form";

export function getFieldError<T extends FieldValues>(errors: FieldErrors<T>, field: Path<T>) {
  return errors[field]?.message?.toString();
}
