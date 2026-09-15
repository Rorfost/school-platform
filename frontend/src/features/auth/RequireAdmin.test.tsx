import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { RequireAdmin } from "@/features/auth/RequireAdmin";
import * as UseAuthModule from "@/features/auth/useAuth";

describe("RequireAdmin route guard", () => {
  it("redirects unauthenticated users to /admin/login", () => {
    vi.spyOn(UseAuthModule, "useAuth").mockReturnValue({
      principal: null,
      isAuthenticated: false,
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
      refetchSession: vi.fn(),
    });

    render(
      <MemoryRouter initialEntries={["/admin"]}>
        <Routes>
          <Route element={<RequireAdmin />}>
            <Route path="/admin" element={<div>Admin Secret Dashboard</div>} />
          </Route>
          <Route path="/admin/login" element={<div>Login Screen</div>} />
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.queryByText("Admin Secret Dashboard")).not.toBeInTheDocument();
    expect(screen.getByText("Login Screen")).toBeInTheDocument();
  });

  it("renders protected child route when authenticated", () => {
    vi.spyOn(UseAuthModule, "useAuth").mockReturnValue({
      principal: {
        id: "1",
        email: "principal@school.edu",
        role: "PRINCIPAL",
        mustChangePassword: false,
        schoolSlug: "dhadhana-primary",
      },
      isAuthenticated: true,
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
      refetchSession: vi.fn(),
    });

    render(
      <MemoryRouter initialEntries={["/admin"]}>
        <Routes>
          <Route element={<RequireAdmin />}>
            <Route path="/admin" element={<div>Admin Secret Dashboard</div>} />
          </Route>
          <Route path="/admin/login" element={<div>Login Screen</div>} />
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByText("Admin Secret Dashboard")).toBeInTheDocument();
    expect(screen.queryByText("Login Screen")).not.toBeInTheDocument();
  });
});
