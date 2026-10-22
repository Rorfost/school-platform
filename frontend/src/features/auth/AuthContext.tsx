import { createContext, useCallback, useEffect, type PropsWithChildren } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError, apiRequest, refreshCsrfToken } from "@/api/client";
import { queryKeys } from "@/api/queryKeys";
import type { PrincipalAccountResponse } from "@/api/types";

export interface AuthContextValue {
    principal: PrincipalAccountResponse | null;
    isAuthenticated: boolean;
    isLoading: boolean;
    sessionError?: boolean;
    login: (email: string, password: string) => Promise<PrincipalAccountResponse>;
    logout: () => Promise<void>;
    refetchSession: () => Promise<void>;
}

// eslint-disable-next-line react-refresh/only-export-components
export const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function removeAdminDataQueries(queryClient: ReturnType<typeof useQueryClient>) {
    queryClient.removeQueries({
        predicate: (query) =>
            query.queryKey[0] === queryKeys.adminRoot[0] &&
            query.queryKey[1] === queryKeys.adminRoot[1] &&
            query.queryKey[2] === "admin" &&
            query.queryKey[3] !== "auth",
    });
}

export function AuthProvider({ children }: PropsWithChildren) {
    const queryClient = useQueryClient();

    const {
        data: principal,
        isLoading,
        isError,
        refetch,
    } = useQuery({
        queryKey: queryKeys.authMe,
        queryFn: async () => {
            try {
                return await apiRequest<PrincipalAccountResponse>("/api/v1/admin/auth/me");
            } catch (error) {
                if (error instanceof ApiError && [401, 403].includes(error.status)) {
                    return null;
                }
                throw error;
            }
        },
        staleTime: 0,
        retry: false,
    });

    useEffect(() => {
        if (!isLoading && !isError && !principal) {
            removeAdminDataQueries(queryClient);
        }
    }, [isError, isLoading, principal, queryClient]);

    const login = useCallback(
        async (email: string, password: string) => {
            // A different principal may sign in on this browser without a full page reload.
            removeAdminDataQueries(queryClient);
            queryClient.setQueryData(queryKeys.authMe, null);
            const response = await apiRequest<PrincipalAccountResponse>(
                "/api/v1/admin/auth/login",
                {
                    method: "POST",
                    body: { email, password },
                },
            );
            await refreshCsrfToken();
            queryClient.setQueryData(queryKeys.authMe, response);
            return response;
        },
        [queryClient],
    );

    const logout = useCallback(async () => {
        try {
            await apiRequest<void>("/api/v1/admin/auth/logout", {
                method: "POST",
            });
            await refreshCsrfToken();
        } finally {
            queryClient.setQueryData(queryKeys.authMe, null);
            removeAdminDataQueries(queryClient);
        }
    }, [queryClient]);

    const refetchSession = useCallback(async () => {
        await refetch();
    }, [refetch]);

    return (
        <AuthContext.Provider
            value={{
                principal: principal ?? null,
                isAuthenticated: !!principal,
                isLoading,
                sessionError: isError,
                login,
                logout,
                refetchSession,
            }}
        >
            {children}
        </AuthContext.Provider>
    );
}
