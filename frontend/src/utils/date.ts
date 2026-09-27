/**
 * Formats any date input (ISO string, Date object, YYYY-MM-DD, M/D/YY, timestamp, etc.)
 * into strict DD/MM/YYYY format across the entire portal.
 */
export function formatDate(input: string | Date | number | null | undefined): string {
  if (input === null || input === undefined) {
    return "-";
  }

  if (input instanceof Date) {
    if (isNaN(input.getTime())) return "-";
    const day = String(input.getDate()).padStart(2, "0");
    const month = String(input.getMonth() + 1).padStart(2, "0");
    const year = input.getFullYear();
    return `${day}/${month}/${year}`;
  }

  if (typeof input === "number") {
    const d = new Date(input);
    if (isNaN(d.getTime())) return "-";
    const day = String(d.getDate()).padStart(2, "0");
    const month = String(d.getMonth() + 1).padStart(2, "0");
    const year = d.getFullYear();
    return `${day}/${month}/${year}`;
  }

  const str = String(input).trim();
  if (!str) return "-";

  // Check if string is a numeric Excel serial number (e.g. "39301" or "36982")
  if (/^\d{4,5}(\.\d+)?$/.test(str)) {
    const serial = Number(str);
    if (serial > 1000 && serial < 100000) {
      const utcDays = Math.floor(serial - 25569);
      const utcValue = utcDays * 86400;
      const dateInfo = new Date(utcValue * 1000);
      if (!isNaN(dateInfo.getTime())) {
        const day = String(dateInfo.getUTCDate()).padStart(2, "0");
        const month = String(dateInfo.getUTCMonth() + 1).padStart(2, "0");
        const year = dateInfo.getUTCFullYear();
        return `${day}/${month}/${year}`;
      }
    }
  }

  // Match YYYY-MM-DD or YYYY/MM/DD or YYYY-MM-DDTHH:mm:ss
  const isoMatch = str.match(/^(\d{4})[-/](\d{1,2})[-/](\d{1,2})/);
  if (isoMatch) {
    const [, y, m, d] = isoMatch;
    return `${d.padStart(2, "0")}/${m.padStart(2, "0")}/${y}`;
  }

  // Match DD-MM-YYYY or DD/MM/YYYY or D/M/YYYY or M/D/YY
  const slashMatch = str.match(/^(\d{1,2})[-/](\d{1,2})[-/](\d{2,4})/);
  if (slashMatch) {
    const [, p1, p2, p3] = slashMatch;
    if (p3.length === 4) {
      let day = p1;
      let month = p2;
      if (Number(p2) > 12) {
        day = p2;
        month = p1;
      }
      return `${day.padStart(2, "0")}/${month.padStart(2, "0")}/${p3}`;
    } else if (p3.length === 2) {
      const year = Number(p3) > 50 ? `19${p3}` : `20${p3}`;
      return `${p1.padStart(2, "0")}/${p2.padStart(2, "0")}/${year}`;
    }
  }

  const parsed = new Date(str);
  if (!isNaN(parsed.getTime())) {
    const day = String(parsed.getDate()).padStart(2, "0");
    const month = String(parsed.getMonth() + 1).padStart(2, "0");
    const year = parsed.getFullYear();
    return `${day}/${month}/${year}`;
  }

  return str;
}
