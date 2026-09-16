import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { AdminPrincipalProfilePage } from "@/pages/admin/AdminPrincipalProfilePage";
import { queryKeys } from "@/api/queryKeys";

const mockProfile = {
  fullName: "Hirabhai Patel",
  biography: "School principal with 20 years of service.",
  qualification: "M.A., B.Ed.",
  designation: "Head Teacher",
  message: "Welcome to our school.",
  portraitObjectKey: null,
  email: "principal@school.edu",
  phone: null,
  isPublic: true,
  isContactPublic: true,
};

function renderWithData(ui: React.ReactElement) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  queryClient.setQueryData(queryKeys.principalProfile, mockProfile);
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>{ui}</MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("AdminPrincipalProfilePage", () => {
  it("renders page title and all form sections when data is loaded", () => {
    renderWithData(<AdminPrincipalProfilePage />);

    expect(screen.getByText("Principal Profile & Message")).toBeInTheDocument();
    expect(screen.getByText("Identity & Designation")).toBeInTheDocument();
    expect(screen.getByText("Principal's Desk Message (Gujarati / English)")).toBeInTheDocument();
    expect(screen.getByText("Public Visibility Options")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Save Profile/i })).toBeInTheDocument();
  });

  it("renders inputs pre-filled with profile data from cache", () => {
    renderWithData(<AdminPrincipalProfilePage />);

    expect(screen.getByDisplayValue("Hirabhai Patel")).toBeInTheDocument();
    expect(screen.getByDisplayValue("M.A., B.Ed.")).toBeInTheDocument();
    expect(screen.getByDisplayValue("Welcome to our school.")).toBeInTheDocument();
  });

  it("renders public visibility checkboxes checked by default when isPublic is true", () => {
    renderWithData(<AdminPrincipalProfilePage />);

    const checkboxes = screen.getAllByRole("checkbox");
    // isPublic and isContactPublic both true
    expect(checkboxes[0]).toBeChecked();
    expect(checkboxes[1]).toBeChecked();
  });
});
