const removeTrailingSlash = (value: string) => value.replace(/\/$/, "");

export const apiBaseUrl = removeTrailingSlash(import.meta.env.VITE_API_BASE_URL ?? "");
export const assetBaseUrl = removeTrailingSlash(import.meta.env.VITE_ASSET_BASE_URL ?? "");
