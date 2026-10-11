const removeTrailingSlash = (value: string) => value.replace(/\/$/, "");

export const apiBaseUrl = removeTrailingSlash(import.meta.env.VITE_API_BASE_URL ?? "");
export const assetBaseUrl = removeTrailingSlash(import.meta.env.VITE_ASSET_BASE_URL ?? "");
const DEFAULT_BACKEND_URL = "https://school-platform-gxrt.onrender.com";

// For large uploads (Excel workbooks) we bypass the Cloudflare/Vercel proxy and POST directly
// to the backend origin to avoid the proxy subrequest timeouts.
export const uploadBaseUrl = removeTrailingSlash(
  import.meta.env.VITE_UPLOAD_BASE_URL || import.meta.env.VITE_API_BASE_URL || DEFAULT_BACKEND_URL,
);
