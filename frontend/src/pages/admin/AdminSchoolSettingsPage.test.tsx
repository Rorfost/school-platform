import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { AdminSchoolSettingsPage } from "@/pages/admin/AdminSchoolSettingsPage";
import { queryKeys } from "@/api/queryKeys";

const mockSchool = {
  id: "school-1",
  name: "PM Shri Dhadhana Primary School",
  shortName: "Dhadhana Primary",
  schoolCode: "24030401801",
  address: "Dhadhana, Sami, Patan",
  city: "Dhadhana",
  state: "Gujarat",
  postalCode: "384245",
  email: "principal@school.edu",
  phone: null,
  website: null,
  mapsUrl: null,
  about: "A school serving students since 1950.",
  establishedYear: 1950,
  medium: "Gujarati",
  schoolType: "Primary",
  logoUrl: "https://assets.example/school-logo.png",
};

function renderWithData(ui: React.ReactElement, preloaded = true) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  if (preloaded) {
    queryClient.setQueryData(queryKeys.school, mockSchool);
  }
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>{ui}</MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("AdminSchoolSettingsPage", () => {
  it("renders page title, all form sections and Save button when data is loaded", () => {
    renderWithData(<AdminSchoolSettingsPage />);

    expect(screen.getByText("School Identity & Settings")).toBeInTheDocument();
    expect(screen.getByText("Official Identity & Code")).toBeInTheDocument();
    expect(screen.getByText("School Branding")).toBeInTheDocument();
    expect(screen.getByText("Address & Location")).toBeInTheDocument();
    expect(screen.getByText("Contact Details")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Save School Settings/i })).toBeInTheDocument();
  });

  it("renders form inputs pre-filled with school data from cache", () => {
    renderWithData(<AdminSchoolSettingsPage />);

    expect(screen.getByDisplayValue("PM Shri Dhadhana Primary School")).toBeInTheDocument();
    expect(screen.getByDisplayValue("24030401801")).toBeInTheDocument();
    expect(screen.getByDisplayValue("Gujarat")).toBeInTheDocument();
  });

  it("shows a preview and upload action for a supported logo image", () => {
    renderWithData(<AdminSchoolSettingsPage />);
    const file = new File(["image"], "school.png", { type: "image/png" });

    fireEvent.change(screen.getByLabelText("Change Logo"), { target: { files: [file] } });

    expect(screen.getByRole("button", { name: "Upload Logo" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Remove Logo" })).toBeInTheDocument();
  });

  it("rejects an unsupported logo file before upload", () => {
    renderWithData(<AdminSchoolSettingsPage />);
    const file = new File(["not an image"], "school.gif", { type: "image/gif" });

    fireEvent.change(screen.getByLabelText("Change Logo"), { target: { files: [file] } });

    expect(screen.getByRole("alert")).toHaveTextContent("Please choose a PNG, JPG or WebP image.");
  });

  it("rejects an oversized logo before upload", () => {
    renderWithData(<AdminSchoolSettingsPage />);
    const file = new File([new Uint8Array(10 * 1024 * 1024 + 1)], "school.png", {
      type: "image/png",
    });

    fireEvent.change(screen.getByLabelText("Change Logo"), { target: { files: [file] } });

    expect(screen.getByRole("alert")).toHaveTextContent("The logo file is too large.");
  });
});
