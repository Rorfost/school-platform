export interface ResponsiveMediaOptions {
    /**
     * Target widths in pixels to generate srcset candidates for.
     */
    widths?: number[];
    /**
     * Fallback width in pixels for the default src attribute.
     */
    defaultWidth?: number;
    /**
     * Image quality (1-100). Defaults to 80.
     */
    quality?: number;
    /**
     * Auto format conversion (e.g. webp/avif via f-auto). Defaults to true.
     */
    formatAuto?: boolean;
    /**
     * Crop/fit mode, e.g. "c-at_max" or "c-maintain_ratio".
     */
    cropMode?: string;
    /**
     * Specific aspect ratio if required, e.g. "16-9" or "4-3".
     */
    aspectRatio?: string;
}

/**
 * Checks if a given URL is an ImageKit URL or endpoint that supports query transformation parameters (?tr=...).
 * Non-ImageKit URLs or data URLs/blobs should be served directly without transformation params.
 */
export function isImageKitUrl(url: string | null | undefined): boolean {
    if (!url) return false;
    const trimmed = url.trim();
    if (trimmed.startsWith("data:") || trimmed.startsWith("blob:")) return false;
    try {
        const parsed = new URL(trimmed, "https://placeholder.internal");
        return parsed.hostname.includes("imagekit.io");
    } catch {
        return false;
    }
}

/**
 * Checks if a URL points to a non-image document (like a PDF).
 * Transform parameters must never be appended to PDFs.
 */
export function isPdfDocument(url: string | null | undefined): boolean {
    if (!url) return false;
    try {
        const parsed = new URL(url.trim(), "https://placeholder.internal");
        return parsed.pathname.toLowerCase().endsWith(".pdf");
    } catch {
        return false;
    }
}

/**
 * Builds an ImageKit transformation query string for a target width.
 */
export function buildImageKitTransformation(
    width: number,
    options?: Omit<ResponsiveMediaOptions, "widths" | "defaultWidth">,
): string {
    const parts: string[] = [`w-${width}`];

    if (options?.cropMode) {
        parts.push(options.cropMode);
    } else {
        parts.push("c-at_max");
    }

    if (options?.aspectRatio) {
        parts.push(`ar-${options.aspectRatio}`);
    }

    const quality = options?.quality ?? 80;
    parts.push(`q-${quality}`);

    if (options?.formatAuto !== false) {
        parts.push("f-auto");
    }

    return `tr=${parts.join(",")}`;
}

/**
 * Appends or updates the ImageKit transform query string on a URL.
 */
export function transformImageKitUrl(
    url: string,
    width: number,
    options?: Omit<ResponsiveMediaOptions, "widths" | "defaultWidth">,
): string {
    if (!isImageKitUrl(url) || isPdfDocument(url)) {
        return url;
    }

    try {
        const parsed = new URL(url);
        // Strip any existing tr query parameter to prevent duplicate/conflicting transformations
        parsed.searchParams.delete("tr");
        const transformParam = buildImageKitTransformation(width, options);
        const separator = parsed.search ? "&" : "?";
        return `${parsed.origin}${parsed.pathname}${parsed.search}${separator}${transformParam}`;
    } catch {
        return url;
    }
}

/**
 * Generates an img src and srcset pair for responsive delivery.
 */
export function buildResponsiveSrcSet(
    url: string,
    options?: ResponsiveMediaOptions,
): { src: string; srcSet?: string } {
    if (!url || !isImageKitUrl(url) || isPdfDocument(url)) {
        return { src: url };
    }

    const widths = options?.widths ?? [320, 640, 960, 1280];
    const defaultWidth = options?.defaultWidth ?? widths[widths.length - 1];

    const src = transformImageKitUrl(url, defaultWidth, options);
    const srcSetEntries = widths.map((w) => `${transformImageKitUrl(url, w, options)} ${w}w`);

    return {
        src,
        srcSet: srcSetEntries.join(", "),
    };
}
