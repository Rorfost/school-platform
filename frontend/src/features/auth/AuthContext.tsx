import {
  createContext,
  useCallback,
  useContext,
  type PropsWithChildren,
} from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { apiRequest } from "@/api/client";
import { queryKeys } from "@/api/queryKeys";
import type { PrincipalAccountResponse } from "@/api/types";

interface AuthContextValue {
  principal: PrincipalAccountResponse | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<PrincipalAccountResponse>;
  logout: () => Promise<void>;
  refetchSession: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: PropsWithChildren) {
  const queryClient = useQueryClient();

  const {
    data: principal,
    isLoading,
    refetch,
  } = useQuery({
    queryKey: queryKeys.authMe,
    queryFn: async () => {
      try {
        return await apiRequest<PrincipalAccountResponse>("/api/v1/admin/auth/me");
      } catch {
        return null;
      }
    },
    staleTime: 1000 * 60 * 10,
    retry: false,
  });

  const login = useCallback(
    async (email: string, password: string) => {
      const response = await apiRequest<PrincipalAccountResponse>(
        "/api/v1/admin/auth/login",
        {
          method: "POST",
          body: { email, password },
        },
      );
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
    } finally {
      queryClient.setQueryData(queryKeys.authMe, null);
      queryClient.invalidateQueries();
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
        login,
        logout,
        refetchSession,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
