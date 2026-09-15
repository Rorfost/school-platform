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
  url: string;
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
  url: string;
  status: string;
}

export interface GalleryAlbumResponse {
  id: string;
  title: string;
  description: string | null;
  coverImageId: string | null;
  status: string;
}

export interface GalleryImageResponse {
  id: string;
  altText: string;
  caption: string | null;
  sortOrder: number;
  url: string;
  status: string;
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
