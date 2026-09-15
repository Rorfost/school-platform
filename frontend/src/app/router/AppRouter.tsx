import { createBrowserRouter, RouterProvider } from "react-router-dom";
import { AdminLayout } from "@/components/layout/AdminLayout";
import { PublicLayout } from "@/components/layout/PublicLayout";
import { AdminLoginPage } from "@/features/auth/AdminLoginPage";
import { RequireAdmin } from "@/features/auth/RequireAdmin";

import { AboutPage } from "@/pages/AboutPage";
import { ContactPage } from "@/pages/ContactPage";
import { DownloadsPage } from "@/pages/DownloadsPage";
import { GalleryAlbumPage } from "@/pages/GalleryAlbumPage";
import { GalleryPage } from "@/pages/GalleryPage";
import { HomePage } from "@/pages/HomePage";
import { MaterialsPage } from "@/pages/MaterialsPage";
import { NotFoundPage } from "@/pages/NotFoundPage";
import { NoticeDetailPage } from "@/pages/NoticeDetailPage";
import { NoticesPage } from "@/pages/NoticesPage";
import { PrincipalDeskPage } from "@/pages/PrincipalDeskPage";
import { ResultsInfoPage } from "@/pages/ResultsInfoPage";
import { RouteErrorPage } from "@/pages/RouteErrorPage";
import { StudentCornerPage } from "@/pages/StudentCornerPage";

// Admin Pages
import { AdminAcademicYearsPage } from "@/pages/admin/AdminAcademicYearsPage";
import { AdminAccountPage } from "@/pages/admin/AdminAccountPage";
import { AdminAssessmentsPage } from "@/pages/admin/AdminAssessmentsPage";
import { AdminDashboardPage } from "@/pages/admin/AdminDashboardPage";
import { AdminDownloadsPage } from "@/pages/admin/AdminDownloadsPage";
import { AdminGalleryPage } from "@/pages/admin/AdminGalleryPage";
import { AdminMaterialsPage } from "@/pages/admin/AdminMaterialsPage";
import { AdminNoticesPage } from "@/pages/admin/AdminNoticesPage";
import { AdminPrincipalProfilePage } from "@/pages/admin/AdminPrincipalProfilePage";
import { AdminSchoolSettingsPage } from "@/pages/admin/AdminSchoolSettingsPage";
import { AdminStandardsPage } from "@/pages/admin/AdminStandardsPage";
import { AdminSubjectMappingsPage } from "@/pages/admin/AdminSubjectMappingsPage";
import { AdminSubjectsPage } from "@/pages/admin/AdminSubjectsPage";

const router = createBrowserRouter([
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
      { path: "notices/:id", element: <NoticeDetailPage /> },
      { path: "gallery", element: <GalleryPage /> },
      { path: "gallery/:albumId", element: <GalleryAlbumPage /> },
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
          { index: true, element: <AdminDashboardPage /> },
          { path: "school", element: <AdminSchoolSettingsPage /> },
          { path: "principal", element: <AdminPrincipalProfilePage /> },
          { path: "academic-years", element: <AdminAcademicYearsPage /> },
          { path: "standards", element: <AdminStandardsPage /> },
          { path: "subjects", element: <AdminSubjectsPage /> },
          { path: "subject-mappings", element: <AdminSubjectMappingsPage /> },
          { path: "assessments", element: <AdminAssessmentsPage /> },
          { path: "materials", element: <AdminMaterialsPage /> },
          { path: "notices", element: <AdminNoticesPage /> },
          { path: "gallery", element: <AdminGalleryPage /> },
          { path: "downloads", element: <AdminDownloadsPage /> },
          { path: "account", element: <AdminAccountPage /> },
        ],
      },
    ],
  },
]);

export function AppRouter() {
  return <RouterProvider router={router} />;
}
