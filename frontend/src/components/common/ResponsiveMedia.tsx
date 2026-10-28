import { useState, type ImgHTMLAttributes, type ReactNode } from "react";
import { Image as ImageIcon } from "lucide-react";
import {
    buildResponsiveSrcSet,
    isImageKitUrl,
    isPdfDocument,
    type ResponsiveMediaOptions,
} from "./media";

export interface ResponsiveMediaProps extends Omit<
    ImgHTMLAttributes<HTMLImageElement>,
    "src" | "srcSet"
> {
    /**
     * Source URL of the image (ImageKit or other).
     */
    src?: string | null;
    /**
     * Accessible text description for the image.
     */
    alt: string;
    /**
     * Responsive srcset & transformation options.
     */
    transformOptions?: ResponsiveMediaOptions;
    /**
     * Custom fallback rendered when image fails to load or URL is absent.
     */
    fallback?: ReactNode;
    /**
     * Render container element with specified aspect ratio class or style.
     */
    containerClassName?: string;
    /**
     * Aspect ratio ratio hint to prevent Cumulative Layout Shift (CLS).
     * e.g., "16/9", "4/3", "1/1".
     */
    aspectRatio?: string;
}

/**
 * ResponsiveMedia renders an accessible, layout-stable image with ImageKit transformations,
 * automatic srcset candidates, async decoding, lazy loading (or eager priority), and error fallback.
 */
export function ResponsiveMedia({
    src,
    alt,
    transformOptions,
    fallback,
    containerClassName,
    aspectRatio,
    className = "size-full object-cover",
    loading = "lazy",
    decoding = "async",
    onError,
    ...rest
}: ResponsiveMediaProps) {
    const [hasError, setHasError] = useState(false);

    if (!src || hasError) {
        if (fallback) {
            return <>{fallback}</>;
        }
        return (
            <div
                className={`flex items-center justify-center bg-slate-100 text-slate-400 ${containerClassName ?? "size-full"}`}
                style={aspectRatio ? { aspectRatio } : undefined}
                role="img"
                aria-label={alt}
            >
                <ImageIcon size={32} aria-hidden="true" />
            </div>
        );
    }

    // PDFs are documents, not renderable images; don't attempt ImageKit transformations
    if (isPdfDocument(src)) {
        return (
            <div
                className={`flex items-center justify-center bg-slate-100 text-slate-400 ${containerClassName ?? "size-full"}`}
                style={aspectRatio ? { aspectRatio } : undefined}
            >
                <ImageIcon size={32} aria-hidden="true" />
            </div>
        );
    }

    const { src: responsiveSrc, srcSet } = isImageKitUrl(src)
        ? buildResponsiveSrcSet(src, transformOptions)
        : { src, srcSet: undefined };

    const imgElement = (
        <img
            src={responsiveSrc}
            srcSet={srcSet}
            alt={alt}
            loading={loading}
            decoding={decoding}
            className={className}
            onError={(e) => {
                setHasError(true);
                onError?.(e);
            }}
            {...rest}
        />
    );

    if (containerClassName || aspectRatio) {
        return (
            <div className={containerClassName} style={aspectRatio ? { aspectRatio } : undefined}>
                {imgElement}
            </div>
        );
    }

    return imgElement;
}
