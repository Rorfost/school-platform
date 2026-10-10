const removeTrailingSlash = (value: string) => value.replace(/\/$/, "");

export const apiBaseUrl = removeTrailingSlash(import.meta.env.VITE_API_BASE_URL ?? "");
export const assetBaseUrl = removeTrailingSlash(import.meta.env.VITE_ASSET_BASE_URL ?? "");
// For large uploads (Excel workbooks) we bypass the Cloudflare Worker proxy and POST directly
// to the backend origin to avoid the 30-second subrequest timeout.
export const uploadBaseUrl = removeTrailingSlash(
  import.meta.env.VITE_UPLOAD_BASE_URL ?? import.meta.env.VITE_API_BASE_URL ?? "",
);
