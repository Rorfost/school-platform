import { lazy, Suspense } from "react";
import { createBrowserRouter, RouterProvider } from "react-router-dom";
import { AdminLayout } from "@/components/layout/AdminLayout";
import { PublicLayout } from "@/components/layout/PublicLayout";
import { AdminLoginPage } from "@/features/auth/AdminLoginPage";
import { RequireAdmin } from "@/features/auth/RequireAdmin";

import { NotFoundPage } from "@/pages/NotFoundPage";
import { RouteErrorPage } from "@/pages/RouteErrorPage";
import { LoadingState } from "@/components/common/StatusPanel";

const AboutPage = lazy(() => import("@/pages/AboutPage").then(({ AboutPage }) => ({ default: AboutPage })));
const ContactPage = lazy(() => import("@/pages/ContactPage").then(({ ContactPage }) => ({ default: ContactPage })));
const DownloadsPage = lazy(() => import("@/pages/DownloadsPage").then(({ DownloadsPage }) => ({ default: DownloadsPage })));
const GalleryAlbumPage = lazy(() => import("@/pages/GalleryAlbumPage").then(({ GalleryAlbumPage }) => ({ default: GalleryAlbumPage })));
const GalleryPage = lazy(() => import("@/pages/GalleryPage").then(({ GalleryPage }) => ({ default: GalleryPage })));
const HomePage = lazy(() => import("@/pages/HomePage").then(({ HomePage }) => ({ default: HomePage })));
const MaterialsPage = lazy(() => import("@/pages/MaterialsPage").then(({ MaterialsPage }) => ({ default: MaterialsPage })));
const NoticeDetailPage = lazy(() => import("@/pages/NoticeDetailPage").then(({ NoticeDetailPage }) => ({ default: NoticeDetailPage })));
const NoticesPage = lazy(() => import("@/pages/NoticesPage").then(({ NoticesPage }) => ({ default: NoticesPage })));
const PrincipalDeskPage = lazy(() => import("@/pages/PrincipalDeskPage").then(({ PrincipalDeskPage }) => ({ default: PrincipalDeskPage })));
const ResultsInfoPage = lazy(() => import("@/pages/ResultsInfoPage").then(({ ResultsInfoPage }) => ({ default: ResultsInfoPage })));
const StudentCornerPage = lazy(() => import("@/pages/StudentCornerPage").then(({ StudentCornerPage }) => ({ default: StudentCornerPage })));

const AdminAcademicYearsPage = lazy(() => import("@/pages/admin/AdminAcademicYearsPage").then(({ AdminAcademicYearsPage }) => ({ default: AdminAcademicYearsPage })));
const AdminAccountPage = lazy(() => import("@/pages/admin/AdminAccountPage").then(({ AdminAccountPage }) => ({ default: AdminAccountPage })));
const AdminAssessmentsPage = lazy(() => import("@/pages/admin/AdminAssessmentsPage").then(({ AdminAssessmentsPage }) => ({ default: AdminAssessmentsPage })));
const AdminDashboardPage = lazy(() => import("@/pages/admin/AdminDashboardPage").then(({ AdminDashboardPage }) => ({ default: AdminDashboardPage })));
const AdminDownloadsPage = lazy(() => import("@/pages/admin/AdminDownloadsPage").then(({ AdminDownloadsPage }) => ({ default: AdminDownloadsPage })));
const AdminGalleryPage = lazy(() => import("@/pages/admin/AdminGalleryPage").then(({ AdminGalleryPage }) => ({ default: AdminGalleryPage })));
const AdminGalleryAlbumPage = lazy(() => import("@/pages/admin/AdminGalleryAlbumPage").then(({ AdminGalleryAlbumPage }) => ({ default: AdminGalleryAlbumPage })));
const AdminMaterialsPage = lazy(() => import("@/pages/admin/AdminMaterialsPage").then(({ AdminMaterialsPage }) => ({ default: AdminMaterialsPage })));
const AdminNoticesPage = lazy(() => import("@/pages/admin/AdminNoticesPage").then(({ AdminNoticesPage }) => ({ default: AdminNoticesPage })));
const AdminPrincipalProfilePage = lazy(() => import("@/pages/admin/AdminPrincipalProfilePage").then(({ AdminPrincipalProfilePage }) => ({ default: AdminPrincipalProfilePage })));
const AdminSchoolSettingsPage = lazy(() => import("@/pages/admin/AdminSchoolSettingsPage").then(({ AdminSchoolSettingsPage }) => ({ default: AdminSchoolSettingsPage })));
const AdminStandardsPage = lazy(() => import("@/pages/admin/AdminStandardsPage").then(({ AdminStandardsPage }) => ({ default: AdminStandardsPage })));
const AdminSubjectMappingsPage = lazy(() => import("@/pages/admin/AdminSubjectMappingsPage").then(({ AdminSubjectMappingsPage }) => ({ default: AdminSubjectMappingsPage })));
const AdminSubjectsPage = lazy(() => import("@/pages/admin/AdminSubjectsPage").then(({ AdminSubjectsPage }) => ({ default: AdminSubjectsPage })));

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
          { path: "gallery/:albumId", element: <AdminGalleryAlbumPage /> },
          { path: "downloads", element: <AdminDownloadsPage /> },
          { path: "account", element: <AdminAccountPage /> },
        ],
      },
    ],
  },
]);

export function AppRouter() {
  const isAdminRoute = window.location.pathname.startsWith("/admin");
  return (
    <Suspense
      fallback={
        <div className="p-4 sm:p-6">
          <LoadingState message={isAdminRoute ? "Loading page..." : "પાનું ખૂલી રહ્યું છે..."} />
        </div>
      }
    >
      <RouterProvider router={router} />
    </Suspense>
  );
}
