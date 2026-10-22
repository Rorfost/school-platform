import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { PropsWithChildren } from "react";
import { useState } from "react";
import { ApiError } from "@/api/client";
import { queryKeys } from "@/api/queryKeys";
import { AuthProvider } from "@/features/auth/AuthContext";

export function AppProviders({ children }: PropsWithChildren) {
    const [queryClient] = useState(() => {
        const client = new QueryClient({
            defaultOptions: {
                queries: {
                    retry: (failureCount, error) => {
                        if (error instanceof ApiError) {
                            if ([400, 401, 403, 404, 422, 429].includes(error.status)) {
                                return false;
                            }
                        }
                        return failureCount < 1;
                    },
                    refetchOnWindowFocus: false,
                    staleTime: 1000 * 60 * 5,
                    gcTime: 1000 * 60 * 30,
                },
            },
        });

        client.setQueryDefaults(queryKeys.publicRoot, {
            staleTime: 1000 * 60 * 5,
            gcTime: 1000 * 60 * 30,
            refetchOnWindowFocus: false,
        });

        const dynamicPublicContentPolicy = {
            staleTime: 1000 * 30,
            gcTime: 1000 * 60 * 5,
            refetchOnWindowFocus: false,
        };
        client.setQueryDefaults(queryKeys.noticesRoot, dynamicPublicContentPolicy);
        client.setQueryDefaults(queryKeys.materialsRoot, dynamicPublicContentPolicy);
        client.setQueryDefaults(queryKeys.downloadsRoot, dynamicPublicContentPolicy);
        client.setQueryDefaults(queryKeys.galleryAlbumsRoot, dynamicPublicContentPolicy);

        client.setQueryDefaults(queryKeys.adminRoot, {
            staleTime: 0,
            gcTime: 1000 * 60 * 5,
            retry: false,
            refetchOnWindowFocus: false,
        });

        return client;
    });

    return (
        <QueryClientProvider client={queryClient}>
            <AuthProvider>{children}</AuthProvider>
        </QueryClientProvider>
    );
}
