import { createBrowserRouter, RouterProvider } from "react-router-dom";
import { PublicLayout } from "@/components/layout/PublicLayout";
import { AdminPlaceholderPage } from "@/pages/AdminPlaceholderPage";
import { HomePage } from "@/pages/HomePage";
import { NotFoundPage } from "@/pages/NotFoundPage";
import { RouteErrorPage } from "@/pages/RouteErrorPage";

const router = createBrowserRouter([
  {
    path: "/",
    element: <PublicLayout />,
    errorElement: <RouteErrorPage />,
    children: [
      { index: true, element: <HomePage /> },
      { path: "admin", element: <AdminPlaceholderPage /> },
      { path: "*", element: <NotFoundPage /> },
    ],
  },
]);

export function AppRouter() {
  return <RouterProvider router={router} />;
}
