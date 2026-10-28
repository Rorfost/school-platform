import { render, screen, fireEvent } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { ResponsiveMedia } from "./ResponsiveMedia";
import {
    buildImageKitTransformation,
    buildResponsiveSrcSet,
    isImageKitUrl,
    isPdfDocument,
    transformImageKitUrl,
} from "./media";

describe("ResponsiveMedia & media utils", () => {
    describe("isImageKitUrl", () => {
        it("identifies ImageKit URLs correctly", () => {
            expect(isImageKitUrl("https://ik.imagekit.io/myschool/photo.jpg")).toBe(true);
            expect(isImageKitUrl("https://ik.imagekit.io/myschool/photo.jpg?tr=w-800")).toBe(true);
            expect(isImageKitUrl("https://example.com/photo.jpg")).toBe(false);
            expect(isImageKitUrl("data:image/png;base64,1234")).toBe(false);
            expect(isImageKitUrl(null)).toBe(false);
            expect(isImageKitUrl("")).toBe(false);
        });
    });

    describe("isPdfDocument", () => {
        it("identifies PDF URLs correctly", () => {
            expect(isPdfDocument("https://ik.imagekit.io/myschool/syllabus.pdf")).toBe(true);
            expect(isPdfDocument("https://ik.imagekit.io/myschool/syllabus.PDF")).toBe(true);
            expect(isPdfDocument("https://ik.imagekit.io/myschool/photo.jpg")).toBe(false);
            expect(isPdfDocument(null)).toBe(false);
        });
    });

    describe("buildImageKitTransformation", () => {
        it("creates correct default transformation parameters", () => {
            const transform = buildImageKitTransformation(640);
            expect(transform).toBe("tr=w-640,c-at_max,q-80,f-auto");
        });

        it("applies custom options when specified", () => {
            const transform = buildImageKitTransformation(320, {
                quality: 75,
                cropMode: "c-maintain_ratio",
                aspectRatio: "16-9",
                formatAuto: false,
            });
            expect(transform).toBe("tr=w-320,c-maintain_ratio,ar-16-9,q-75");
        });
    });

    describe("transformImageKitUrl", () => {
        it("replaces existing tr parameters cleanly", () => {
            const original = "https://ik.imagekit.io/myschool/photo.jpg?tr=w-800,h-600";
            const transformed = transformImageKitUrl(original, 400);
            expect(transformed).toContain("tr=w-400");
            expect(transformed).not.toContain("w-800");
        });

        it("returns original URL if not ImageKit", () => {
            const nonIk = "https://example.com/image.png";
            expect(transformImageKitUrl(nonIk, 400)).toBe(nonIk);
        });

        it("does not transform PDF documents", () => {
            const pdf = "https://ik.imagekit.io/myschool/circular.pdf";
            expect(transformImageKitUrl(pdf, 400)).toBe(pdf);
        });
    });

    describe("buildResponsiveSrcSet", () => {
        it("generates srcset with default widths for ImageKit URLs", () => {
            const ik = "https://ik.imagekit.io/myschool/photo.jpg";
            const { src, srcSet } = buildResponsiveSrcSet(ik, { widths: [320, 640] });
            expect(src).toContain("w-640");
            expect(srcSet).toContain("w-320");
            expect(srcSet).toContain("320w");
            expect(srcSet).toContain("w-640");
            expect(srcSet).toContain("640w");
        });
    });

    describe("ResponsiveMedia Component", () => {
        it("renders responsive image with srcSet, lazy loading, and async decoding", () => {
            render(
                <ResponsiveMedia
                    src="https://ik.imagekit.io/myschool/photo.jpg"
                    alt="કૅમ્પસ તસવીર"
                    transformOptions={{ widths: [320, 640], defaultWidth: 640 }}
                    sizes="(max-width: 640px) 100vw, 50vw"
                    aspectRatio="16/9"
                />,
            );

            const img = screen.getByAltText("કૅમ્પસ તસવીર");
            expect(img).toBeInTheDocument();
            expect(img).toHaveAttribute("loading", "lazy");
            expect(img).toHaveAttribute("decoding", "async");
            expect(img).toHaveAttribute("srcSet");
            expect(img.getAttribute("srcSet")).toContain("320w");
        });

        it("renders fallback UI when src is missing", () => {
            render(
                <ResponsiveMedia
                    src={null}
                    alt="ગેરહાજર તસવીર"
                    fallback={<div data-testid="custom-fallback">કોઈ તસવીર નથી</div>}
                />,
            );

            expect(screen.getByTestId("custom-fallback")).toBeInTheDocument();
        });

        it("renders default fallback placeholder on image error", () => {
            render(
                <ResponsiveMedia
                    src="https://ik.imagekit.io/myschool/broken.jpg"
                    alt="તૂટેલી તસવીર"
                />,
            );

            const img = screen.getByAltText("તૂટેલી તસવીર");
            fireEvent.error(img);

            // Once error triggers, fallback placeholder with role img and aria-label is rendered
            expect(screen.getByRole("img", { name: "તૂટેલી તસવીર" })).toBeInTheDocument();
        });

        it("renders non-ImageKit images without transformation or srcSet", () => {
            render(<ResponsiveMedia src="https://example.com/external.png" alt="બાહ્ય તસવીર" />);

            const img = screen.getByAltText("બાહ્ય તસવીર");
            expect(img).toHaveAttribute("src", "https://example.com/external.png");
            expect(img).not.toHaveAttribute("srcSet");
        });
    });
});
