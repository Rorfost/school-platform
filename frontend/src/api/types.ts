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
  logoObjectKey: string | null;
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
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface NoticeResponse {
  id: string;
  title: string;
  content: string;
  isPinned: boolean;
  publishedAt: string | null;
  attachmentFilename: string | null;
  attachmentContentType: string | null;
  attachmentObjectKey: string | null;
}

export interface MaterialResponse {
  id: string;
  title: string;
  description: string | null;
  standardSubjectId: string | null;
  academicYearId: string | null;
  objectKey: string;
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  publishedAt: string | null;
}

export interface DownloadResponse {
  id: string;
  title: string;
  category: string | null;
  academicYearId: string | null;
  objectKey: string;
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  publishedAt: string | null;
}

export interface GalleryAlbumResponse {
  id: string;
  title: string;
  description: string | null;
  coverImageObjectKey: string | null;
  publishedAt: string | null;
}

export interface GalleryImageResponse {
  id: string;
  albumId: string;
  objectKey: string;
  altText: string;
  caption: string | null;
  displayOrder: number;
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
