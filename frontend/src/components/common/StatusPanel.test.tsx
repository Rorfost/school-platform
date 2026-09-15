import { fireEvent, render, screen } from "@testing-library/react";
import { EmptyState, ErrorState, LoadingState } from "@/components/common/StatusPanel";

describe("StatusPanel components", () => {
  it("renders LoadingState with accessible role and message", () => {
    render(<LoadingState message="વિગતો આવી રહી છે..." />);

    const statuses = screen.getAllByRole("status");
    expect(statuses[0]).toBeVisible();
    expect(screen.getByText("વિગતો આવી રહી છે...")).toBeVisible();
  });

  it("renders ErrorState and triggers retry callback", () => {
    const handleRetry = vi.fn();
    render(<ErrorState message="કંઈક ખોટું થયું." onRetry={handleRetry} />);

    expect(screen.getByRole("alert")).toBeVisible();
    expect(screen.getByText("કંઈક ખોટું થયું.")).toBeVisible();

    const retryButton = screen.getByRole("button", { name: "ફરી પ્રયત્ન કરો" });
    expect(retryButton).toBeVisible();
    fireEvent.click(retryButton);

    expect(handleRetry).toHaveBeenCalledTimes(1);
  });

  it("renders EmptyState with title and description", () => {
    render(
      <EmptyState
        title="કોઈ ફાઈલ મળી નથી."
        description="થોડા સમય પછી ફરી પ્રયત્ન કરો."
      />,
    );

    expect(screen.getByText("કોઈ ફાઈલ મળી નથી.")).toBeVisible();
    expect(screen.getByText("થોડા સમય પછી ફરી પ્રયત્ન કરો.")).toBeVisible();
  });
});
