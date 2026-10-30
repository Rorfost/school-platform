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
                            // Never retry client errors, auth failures, 404s, or rate limits
                            if ([400, 401, 403, 404, 422, 429].includes(error.status)) {
                                return false;
                            }
                        }
                        // Bound read retries to maximum 1 attempt for cold starts/timeouts
                        return failureCount < 1;
                    },
                    refetchOnWindowFocus: false,
                    staleTime: 1000 * 60 * 5,
                    gcTime: 1000 * 60 * 30,
                },
                mutations: {
                    // Do NOT replay mutating writes (POST/PUT/PATCH/DELETE) automatically.
                    // Accidental retry during cold starts or transient timeouts could result
                    // in duplicated resources or inconsistent database state.
                    retry: false,
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
