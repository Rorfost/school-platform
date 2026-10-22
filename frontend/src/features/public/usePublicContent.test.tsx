import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { apiRequest } from "@/api/client";
import { queryKeys } from "@/api/queryKeys";
import type { NoticeResponse, PageResponse } from "@/api/types";
import { usePublicNotice } from "./usePublicContent";

vi.mock("@/api/client", () => ({
    apiRequest: vi.fn(),
}));

const notice: NoticeResponse = {
    id: "notice-75",
    title: "શાળાનો કાર્યક્રમ",
    body: "Synthetic published notice",
    pinned: false,
    expiresAt: null,
    attachmentFilename: null,
    attachmentUrl: null,
    status: "PUBLISHED",
};

function createWrapper(queryClient: QueryClient) {
    return function Wrapper({ children }: { children: React.ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    };
}

describe("usePublicNotice", () => {
    it("reuses a fresh notice list entry when navigating from the list", () => {
        const queryClient = new QueryClient({
            defaultOptions: { queries: { retry: false, staleTime: 60_000 } },
        });
        const page: PageResponse<NoticeResponse> = {
            items: [notice],
            page: 0,
            size: 50,
            totalItems: 1,
            totalPages: 1,
        };
        queryClient.setQueryData(queryKeys.notices({ page: 0, size: 50 }), page);

        const { result } = renderHook(() => usePublicNotice(notice.id), {
            wrapper: createWrapper(queryClient),
        });

        expect(result.current.data).toEqual(notice);
        expect(apiRequest).not.toHaveBeenCalled();
    });

    it("loads a directly linked notice by ID instead of downloading the first page", async () => {
        vi.mocked(apiRequest).mockResolvedValueOnce(notice);
        const queryClient = new QueryClient({
            defaultOptions: { queries: { retry: false } },
        });

        const { result } = renderHook(() => usePublicNotice(notice.id), {
            wrapper: createWrapper(queryClient),
        });

        await waitFor(() => expect(result.current.data).toEqual(notice));
        expect(apiRequest).toHaveBeenCalledWith(`/api/v1/public/notices/${notice.id}`);
    });
});
