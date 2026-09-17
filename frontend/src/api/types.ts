export interface SchoolResponse {
  id: string;
  name: string;
  shortName: string | null;
  schoolCode: string | null;
  address: string | null;
  city: string | null;
  state: string | null;
  postalCode: string | null;
  email: string | null;
  phone: string | null;
  website: string | null;
  mapsUrl: string | null;
  about: string | null;
  establishedYear: number | null;
  medium: string | null;
  schoolType: string | null;
  logoUrl: string | null;
}

export interface PrincipalProfileResponse {
  fullName: string;
  biography: string | null;
  qualification: string | null;
  designation: string | null;
  message: string | null;
  portraitObjectKey: string | null;
  email: string | null;
  phone: string | null;
  isPublic: boolean;
  isContactPublic: boolean;
}

export interface StandardResponse {
  id: string;
  code: string;
  name: string;
  displayOrder: number;
}

export interface SubjectResponse {
  id: string;
  code: string;
  name: string;
  displayOrder: number;
}

export interface AcademicYearResponse {
  id: string;
  name: string;
  startDate: string;
  endDate: string;
  status: "CURRENT" | "ARCHIVED";
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface NoticeResponse {
  id: string;
  title: string;
  body: string;
  pinned: boolean;
  expiresAt: string | null;
  attachmentFilename: string | null;
  attachmentUrl: string | null;
  status: string;
}

export interface MaterialResponse {
  id: string;
  title: string;
  description: string | null;
  materialType: string;
  academicYearId: string | null;
  standardSubjectId: string | null;
  filename: string;
  contentType: string;
  byteSize: number;
  url: string | null;
  status: string;
}

export interface DownloadResponse {
  id: string;
  title: string;
  description: string | null;
  category: string | null;
  academicYearId: string | null;
  filename: string;
  contentType: string;
  byteSize: number;
  url: string | null;
  status: string;
}

export interface GalleryAlbumResponse {
  id: string;
  title: string;
  description: string | null;
  coverImageId: string | null;
  coverImageThumbnailUrl: string | null;
  imageCount: number;
  status: string;
}

export interface GalleryAlbumUpdateRequest {
  title: string;
  description?: string | null;
}

export interface GalleryImageResponse {
  id: string;
  altText: string;
  caption: string | null;
  sortOrder: number;
  url: string | null;
  thumbnailUrl: string | null;
  status: string;
}

export interface StandardSubjectResponse {
  id: string;
  standardId: string;
  subjectId: string;
  sortOrder: number;
}

export interface AssessmentSubjectResponse {
  standardSubjectId: string;
  maximumMarks: number;
  passingMarks: number;
}

export interface AssessmentResponse {
  id: string;
  academicYearId: string;
  standardId: string;
  assessmentTypeId: string;
  title: string;
  description: string | null;
  assessmentDate: string | null;
  status: "DRAFT" | "PUBLISHED" | "ARCHIVED";
  subjects: AssessmentSubjectResponse[];
}

export interface SchoolUpdateRequest {
  name: string;
  shortName?: string | null;
  schoolCode?: string | null;
  address?: string | null;
  city?: string | null;
  state?: string | null;
  postalCode?: string | null;
  email?: string | null;
  phone?: string | null;
  website?: string | null;
  mapsUrl?: string | null;
  about?: string | null;
  establishedYear?: number | null;
  medium?: string | null;
  schoolType?: string | null;
}

export interface PrincipalProfileUpdateRequest {
  fullName: string;
  biography?: string | null;
  qualification?: string | null;
  designation?: string | null;
  message?: string | null;
  portraitObjectKey?: string | null;
  email?: string | null;
  phone?: string | null;
  isPublic: boolean;
  isContactPublic: boolean;
}

export interface AcademicYearRequest {
  name: string;
  startsOn: string;
  endsOn: string;
  current: boolean;
}

export interface StandardRequest {
  code: string;
  displayName: string;
  sortOrder: number;
  archived: boolean;
}

export interface SubjectRequest {
  code: string;
  name: string;
  sortOrder: number;
  archived: boolean;
}

export interface StandardSubjectRequest {
  standardId: string;
  subjectId: string;
  sortOrder: number;
}

export interface AssessmentSubjectRequest {
  standardSubjectId: string;
  maximumMarks: number;
  passingMarks: number;
}

export interface AssessmentRequest {
  academicYearId: string;
  standardId: string;
  assessmentTypeId: string;
  title: string;
  description?: string | null;
  assessmentDate?: string | null;
  subjects: AssessmentSubjectRequest[];
}

export interface NoticeRequest {
  title: string;
  body: string;
  pinned: boolean;
  expiresAt?: string | null;
}

export interface GalleryAlbumRequest {
  title: string;
  description?: string | null;
  coverImageId?: string | null;
}

export interface PasswordChangeRequest {
  currentPassword: string;
  newPassword: string;
}

export interface PrincipalAccountResponse {
  id: string;
  email: string;
  role: "PRINCIPAL";
  mustChangePassword: boolean;
  schoolSlug: string;
}

export interface CsrfTokenResponse {
  token: string;
  headerName: string;
}

export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  code?: string;
  requestId?: string;
}

export interface DashboardSummaryResponse {
  totalStudents: number;
  totalStandards: number;
  totalSubjects: number;
  publishedMaterials: number;
  publishedNotices: number;
  publishedGalleryAlbums: number;
  totalDownloads: number;
}

export interface AuditLogResponse {
  id: string;
  schoolId: string;
  actorAdminUserId: string | null;
  action: string;
  targetType: string;
  targetId: string | null;
  requestId: string | null;
  metadata: string;
  createdAt: string;
}
