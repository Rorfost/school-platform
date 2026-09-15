import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { PropsWithChildren } from "react";
import { useState } from "react";
import { ApiError } from "@/api/client";

export function AppProviders({ children }: PropsWithChildren) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
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
            staleTime: 1000 * 60 * 5, // 5 minutes cache for static public content
          },
        },
      }),
  );

  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}
