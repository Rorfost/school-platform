import { createBrowserRouter, RouterProvider } from "react-router-dom";
import { AdminLayout } from "@/components/layout/AdminLayout";
import { PublicLayout } from "@/components/layout/PublicLayout";
import { AdminLoginPage } from "@/features/auth/AdminLoginPage";
import { RequireAdmin } from "@/features/auth/RequireAdmin";
import { AboutPage } from "@/pages/AboutPage";
import { AdminPlaceholderPage } from "@/pages/AdminPlaceholderPage";
import { ContactPage } from "@/pages/ContactPage";
import { DownloadsPage } from "@/pages/DownloadsPage";
import { GalleryPage } from "@/pages/GalleryPage";
import { HomePage } from "@/pages/HomePage";
import { MaterialsPage } from "@/pages/MaterialsPage";
import { NotFoundPage } from "@/pages/NotFoundPage";
import { NoticesPage } from "@/pages/NoticesPage";
import { PrincipalDeskPage } from "@/pages/PrincipalDeskPage";
import { ResultsInfoPage } from "@/pages/ResultsInfoPage";
import { RouteErrorPage } from "@/pages/RouteErrorPage";
import { StudentCornerPage } from "@/pages/StudentCornerPage";

export const router = createBrowserRouter([
  {
    path: "/",
    element: <PublicLayout />,
    errorElement: <RouteErrorPage />,
    children: [
      { index: true, element: <HomePage /> },
      { path: "about", element: <AboutPage /> },
      { path: "principal", element: <PrincipalDeskPage /> },
      { path: "student", element: <StudentCornerPage /> },
      { path: "student/materials", element: <MaterialsPage /> },
      { path: "student/results", element: <ResultsInfoPage /> },
      { path: "student/downloads", element: <DownloadsPage /> },
      { path: "notices", element: <NoticesPage /> },
      { path: "gallery", element: <GalleryPage /> },
      { path: "contact", element: <ContactPage /> },
      { path: "*", element: <NotFoundPage /> },
    ],
  },
  {
    path: "/admin/login",
    element: <AdminLoginPage />,
    errorElement: <RouteErrorPage />,
  },
  {
    path: "/admin",
    element: <RequireAdmin />,
    errorElement: <RouteErrorPage />,
    children: [
      {
        element: <AdminLayout />,
        children: [
          { index: true, element: <AdminPlaceholderPage /> },
        ],
      },
    ],
  },
]);

export function AppRouter() {
  return <RouterProvider router={router} />;
}
