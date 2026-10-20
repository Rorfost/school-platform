/*
 * Copyright (c) Rorfost. All rights reserved.
 * SPDX-License-Identifier: PROPRIETARY
 */

package com.rorfost.schoolportal.common.fixture;

import com.rorfost.schoolportal.academic.domain.Standard;
import com.rorfost.schoolportal.content.domain.Download;
import com.rorfost.schoolportal.content.domain.GalleryAlbum;
import com.rorfost.schoolportal.content.domain.GalleryImage;
import com.rorfost.schoolportal.content.domain.Notice;
import com.rorfost.schoolportal.content.domain.StudyMaterial;
import com.rorfost.schoolportal.featureflag.domain.FeatureKey;
import com.rorfost.schoolportal.featureflag.domain.SchoolFeature;
import com.rorfost.schoolportal.school.domain.AnnualExamResult;
import com.rorfost.schoolportal.school.domain.AnnualExamResultSubject;
import com.rorfost.schoolportal.school.domain.PrincipalProfile;
import com.rorfost.schoolportal.school.domain.School;
import com.rorfost.schoolportal.tenant.domain.TenantContext;

import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Deterministic builder of synthetic multi-tenant datasets for performance baselines and
 * reproducible integration benchmarks (Sprint 31).
 *
 * <p>Constructs 3 distinct tenants with different module topologies:
 *
 * <ul>
 *   <li><strong>Tenant A (Full Feature Set / Gallery Rich):</strong> All core features enabled,
 *       photo gallery heavy (12 albums, 72 photos), published notices, study materials, downloads,
 *       full academic catalog (Standards 1-8), synthetic students and annual exam results.
 *   <li><strong>Tenant B (Results Rich / Moderate Media):</strong> Exam results &amp; academic
 *       catalog heavy (Standards 1-10, 50 synthetic student marks entries), moderate notices,
 *       gallery disabled or minimal (1 album, 4 photos).
 *   <li><strong>Tenant C (Minimal Public Presence):</strong> Only basic school info, principal
 *       desk, and notices enabled. Gallery, materials, downloads, and exam results disabled.
 * </ul>
 *
 * <p><strong>Data Privacy Guarantee:</strong> All student names, roll numbers, GR numbers, and
 * marks are strictly synthetic. No real pupil data or Aadhaar/UID numbers are ever used or created.
 */
public class SyntheticTenantDatasetBuilder {

    private final TenantFixtureFactory baseFixtures;

    public SyntheticTenantDatasetBuilder() {
        this.baseFixtures = new TenantFixtureFactory();
    }

    public SyntheticTenantDatasetBuilder(TenantFixtureFactory baseFixtures) {
        this.baseFixtures = baseFixtures;
    }

    public TenantDataset buildTenantA() {
        UUID schoolId = baseFixtures.schoolIdA;
        School school = baseFixtures.schoolA;
        PrincipalProfile profile = baseFixtures.profileA;
        TenantContext context = baseFixtures.contextA;

        // Features: All enabled
        List<SchoolFeature> features = new ArrayList<>();
        for (FeatureKey fk : FeatureKey.values()) {
            features.add(new SchoolFeature(schoolId, fk, true));
        }

        // Gallery: 12 albums, 6 photos each = 72 photos
        List<GalleryAlbum> albums = new ArrayList<>();
        List<GalleryImage> images = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            UUID albumId = UUID.fromString(String.format("a1b00000-0000-0000-0000-%012d", i));
            GalleryAlbum album =
                    new GalleryAlbum(schoolId, "Album Event " + i, "Annual Event description " + i);
            album.publish(Instant.parse("2026-01-01T00:00:00Z"));
            ReflectionTestUtils.setField(album, "id", albumId);
            albums.add(album);

            for (int j = 1; j <= 6; j++) {
                UUID imgId = UUID.fromString(String.format("a1c00000-0000-0000-%04d-%08d", i, j));
                GalleryImage img =
                        new GalleryImage(
                                schoolId,
                                albumId,
                                "school-platform-bucket",
                                String.format(
                                        "schools/%s/gallery/album-%d/img-%d.webp", schoolId, i, j),
                                String.format("img-%d-%d.webp", i, j),
                                "image/webp",
                                102400L,
                                "sha256-img-dummy",
                                "img-file-id-" + i + "-" + j,
                                "Alt text for photo " + j,
                                "Caption for photo " + j + " in album " + i,
                                j);
                ReflectionTestUtils.setField(img, "id", imgId);
                images.add(img);
            }
        }

        // Notices: 15 published notices
        List<Notice> notices = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            Notice notice =
                    new Notice(
                            schoolId,
                            "Notice Announcement #" + i,
                            "Public notification content for academic session #" + i);
            notice.update(
                    "Notice Announcement #" + i,
                    "Public notification content for academic session #" + i,
                    i <= 2,
                    null);
            notice.publish(Instant.parse("2026-02-01T10:00:00Z").plusSeconds(i * 3600));
            ReflectionTestUtils.setField(
                    notice,
                    "id",
                    UUID.fromString(String.format("a1d00000-0000-0000-0000-%012d", i)));
            notices.add(notice);
        }

        // Study Materials: 10 published items
        List<StudyMaterial> materials = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            StudyMaterial mat =
                    new StudyMaterial(
                            schoolId,
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            "Study Unit Notes #" + i,
                            "Std " + ((i % 8) + 1) + " Subject Material Details",
                            "PDF",
                            "school-platform-bucket",
                            String.format("schools/%s/materials/unit-%d.pdf", schoolId, i),
                            "unit-" + i + ".pdf",
                            "application/pdf",
                            102400L,
                            "sha256-dummy",
                            "file-id-" + i);
            mat.publish(Instant.parse("2026-02-10T10:00:00Z").plusSeconds(i * 3600));
            ReflectionTestUtils.setField(
                    mat, "id", UUID.fromString(String.format("a1e00000-0000-0000-0000-%012d", i)));
            materials.add(mat);
        }

        // Downloads: 6 published documents
        List<Download> downloads = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            Download dl =
                    new Download(
                            schoolId,
                            UUID.randomUUID(),
                            "Official Form / Circular #" + i,
                            "PDF Document description " + i,
                            "General",
                            "school-platform-bucket",
                            String.format("schools/%s/downloads/form-%d.pdf", schoolId, i),
                            "form-" + i + ".pdf",
                            "application/pdf",
                            204800L,
                            "sha256-dl-dummy",
                            "dl-file-id-" + i);
            dl.publish(Instant.parse("2026-02-12T10:00:00Z").plusSeconds(i * 3600));
            ReflectionTestUtils.setField(
                    dl, "id", UUID.fromString(String.format("a1f00000-0000-0000-0000-%012d", i)));
            downloads.add(dl);
        }

        // Academic & Students: Standards 1-8, 20 synthetic students
        List<Standard> standards = new ArrayList<>();
        for (short s = 1; s <= 8; s++) {
            standards.add(new Standard(schoolId, "STD_" + s, "Standard " + s, s));
        }

        List<AnnualExamResult> results = new ArrayList<>();
        for (int r = 1; r <= 20; r++) {
            AnnualExamResult res = new AnnualExamResult();
            res.setId(UUID.fromString(String.format("a1a00000-0000-0000-0000-%012d", r)));
            res.setSchoolId(schoolId);
            res.setResultType("ANNUAL");
            res.setStandard(String.valueOf((r % 8) + 1));
            res.setStudentClass("A");
            res.setStudentName("Synthetic Student " + r);
            res.setRollNumber(r);
            res.setGeneralRegisterNumber(String.valueOf(1000 + r));
            res.setTotalWorkingDays("200");
            res.setAttendedDays("190");
            res.setTotalMarks(400);
            res.setObtainedMarks(320);

            List<AnnualExamResultSubject> subjects = new ArrayList<>();
            String[] subNames = {"Mathematics", "Science", "Gujarati", "English"};
            int[] marks = {80, 85, 75, 80};
            for (int s = 0; s < subNames.length; s++) {
                AnnualExamResultSubject sub = new AnnualExamResultSubject();
                sub.setId(UUID.randomUUID());
                sub.setExamResult(res);
                sub.setSubjectName(subNames[s]);
                sub.setMaximumMarks(100);
                sub.setObtainedMarks(marks[s]);
                sub.setGrade("A");
                sub.setStatus("PRESENT");
                sub.setSortOrder(s + 1);
                subjects.add(sub);
            }
            res.setSubjects(subjects);
            results.add(res);
        }

        return new TenantDataset(
                schoolId,
                "school-a",
                "School A Primary (Gallery & Content Rich)",
                TenantTopology.FULL_FEATURED_GALLERY_RICH,
                school,
                profile,
                context,
                features,
                albums,
                images,
                notices,
                materials,
                downloads,
                standards,
                results);
    }

    public TenantDataset buildTenantB() {
        UUID schoolId = baseFixtures.schoolIdB;
        School school = baseFixtures.schoolB;
        PrincipalProfile profile = baseFixtures.profileB;
        TenantContext context = baseFixtures.contextB;

        // Features: Results enabled, Gallery disabled, Materials enabled
        List<SchoolFeature> features = new ArrayList<>();
        features.add(new SchoolFeature(schoolId, FeatureKey.GALLERY, false));
        features.add(new SchoolFeature(schoolId, FeatureKey.EXAM_RESULTS, true));
        features.add(new SchoolFeature(schoolId, FeatureKey.MATERIALS, true));
        features.add(new SchoolFeature(schoolId, FeatureKey.NOTICES, true));
        features.add(new SchoolFeature(schoolId, FeatureKey.DOWNLOADS, true));

        // Minimal Gallery: 1 album, 4 photos
        List<GalleryAlbum> albums = new ArrayList<>();
        List<GalleryImage> images = new ArrayList<>();
        UUID albumId = UUID.fromString("b1b00000-0000-0000-0000-000000000001");
        GalleryAlbum album = new GalleryAlbum(schoolId, "Campus Tour", "School B campus views");
        album.publish(Instant.parse("2026-01-10T00:00:00Z"));
        ReflectionTestUtils.setField(album, "id", albumId);
        albums.add(album);

        for (int j = 1; j <= 4; j++) {
            UUID imgId = UUID.fromString(String.format("b1c00000-0000-0000-0001-%08d", j));
            GalleryImage img =
                    new GalleryImage(
                            schoolId,
                            albumId,
                            "school-platform-bucket",
                            String.format("schools/%s/gallery/album-1/img-%d.webp", schoolId, j),
                            String.format("campus-%d.webp", j),
                            "image/webp",
                            102400L,
                            "sha256-img-b-dummy",
                            "img-b-1-" + j,
                            "Campus photo " + j,
                            "Campus photo " + j,
                            j);
            ReflectionTestUtils.setField(img, "id", imgId);
            images.add(img);
        }

        // Notices: 5 notices
        List<Notice> notices = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Notice notice =
                    new Notice(
                            schoolId,
                            "School B Notice #" + i,
                            "Important circular regarding results and examinations #" + i);
            notice.update(
                    "School B Notice #" + i,
                    "Important circular regarding results and examinations #" + i,
                    i == 1,
                    null);
            notice.publish(Instant.parse("2026-02-15T09:00:00Z").plusSeconds(i * 3600));
            ReflectionTestUtils.setField(
                    notice,
                    "id",
                    UUID.fromString(String.format("b1d00000-0000-0000-0000-%012d", i)));
            notices.add(notice);
        }

        List<StudyMaterial> materials = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            StudyMaterial mat =
                    new StudyMaterial(
                            schoolId,
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            "Exam Revision Notes #" + i,
                            "Revision guide for upcoming assessments",
                            "PDF",
                            "school-platform-bucket",
                            String.format("schools/%s/materials/rev-%d.pdf", schoolId, i),
                            "rev-" + i + ".pdf",
                            "application/pdf",
                            150000L,
                            "sha256-mat-b-dummy",
                            "mat-b-id-" + i);
            mat.publish(Instant.parse("2026-02-16T10:00:00Z").plusSeconds(i * 3600));
            ReflectionTestUtils.setField(
                    mat, "id", UUID.fromString(String.format("b1e00000-0000-0000-0000-%012d", i)));
            materials.add(mat);
        }

        List<Download> downloads = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Download dl =
                    new Download(
                            schoolId,
                            UUID.randomUUID(),
                            "Academic Calendar & Exam Schedule #" + i,
                            "Annual examination circular",
                            "Exams",
                            "school-platform-bucket",
                            String.format("schools/%s/downloads/exam-%d.pdf", schoolId, i),
                            "exam-" + i + ".pdf",
                            "application/pdf",
                            180000L,
                            "sha256-exam-b-dummy",
                            "dl-b-file-id-" + i);
            dl.publish(Instant.parse("2026-02-18T10:00:00Z").plusSeconds(i * 3600));
            ReflectionTestUtils.setField(
                    dl, "id", UUID.fromString(String.format("b1f00000-0000-0000-0000-%012d", i)));
            downloads.add(dl);
        }

        // Standards 1-10
        List<Standard> standards = new ArrayList<>();
        for (short s = 1; s <= 10; s++) {
            standards.add(new Standard(schoolId, "STD_" + s, "Standard " + s, s));
        }

        // Results heavy: 50 synthetic student results
        List<AnnualExamResult> results = new ArrayList<>();
        for (int r = 1; r <= 50; r++) {
            AnnualExamResult res = new AnnualExamResult();
            res.setId(UUID.fromString(String.format("b1a00000-0000-0000-0000-%012d", r)));
            res.setSchoolId(schoolId);
            res.setResultType("ANNUAL");
            res.setStandard(String.valueOf((r % 10) + 1));
            res.setStudentClass("A");
            res.setStudentName("Synthetic Scholar " + r);
            res.setRollNumber(r);
            res.setGeneralRegisterNumber(String.valueOf(2000 + r));
            res.setTotalWorkingDays("210");
            res.setAttendedDays("200");
            res.setTotalMarks(500);
            res.setObtainedMarks(420);

            List<AnnualExamResultSubject> subjects = new ArrayList<>();
            String[] subNames = {"Mathematics", "Science", "Gujarati", "Social Science", "English"};
            for (int s = 0; s < subNames.length; s++) {
                AnnualExamResultSubject sub = new AnnualExamResultSubject();
                sub.setId(UUID.randomUUID());
                sub.setExamResult(res);
                sub.setSubjectName(subNames[s]);
                sub.setMaximumMarks(100);
                sub.setObtainedMarks(84);
                sub.setGrade("A");
                sub.setStatus("PRESENT");
                sub.setSortOrder(s + 1);
                subjects.add(sub);
            }
            res.setSubjects(subjects);
            results.add(res);
        }

        return new TenantDataset(
                schoolId,
                "school-b",
                "School B Academy (Results & Academic Heavy)",
                TenantTopology.RESULTS_RICH_MODERATE_MEDIA,
                school,
                profile,
                context,
                features,
                albums,
                images,
                notices,
                materials,
                downloads,
                standards,
                results);
    }

    public TenantDataset buildTenantC() {
        UUID schoolId = baseFixtures.schoolIdC;
        School school = baseFixtures.schoolC;
        PrincipalProfile profile = baseFixtures.profileC;
        TenantContext context = baseFixtures.contextC;

        // Features: Minimal (Notices only, Gallery/Materials/Downloads/Results disabled)
        List<SchoolFeature> features = new ArrayList<>();
        features.add(new SchoolFeature(schoolId, FeatureKey.GALLERY, false));
        features.add(new SchoolFeature(schoolId, FeatureKey.MATERIALS, false));
        features.add(new SchoolFeature(schoolId, FeatureKey.DOWNLOADS, false));
        features.add(new SchoolFeature(schoolId, FeatureKey.EXAM_RESULTS, false));
        features.add(new SchoolFeature(schoolId, FeatureKey.NOTICES, true));

        List<Notice> notices = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Notice notice =
                    new Notice(
                            schoolId,
                            "General School Announcement #" + i,
                            "Basic portal announcement for parents and students");
            notice.update(
                    "General School Announcement #" + i,
                    "Basic portal announcement for parents and students",
                    i == 1,
                    null);
            notice.publish(Instant.parse("2026-03-01T08:00:00Z").plusSeconds(i * 3600));
            ReflectionTestUtils.setField(
                    notice,
                    "id",
                    UUID.fromString(String.format("c1d00000-0000-0000-0000-%012d", i)));
            notices.add(notice);
        }

        return new TenantDataset(
                schoolId,
                "school-c",
                "School C High (Minimal Presence)",
                TenantTopology.MINIMAL_PUBLIC_PRESENCE,
                school,
                profile,
                context,
                features,
                Collections.emptyList(),
                Collections.emptyList(),
                notices,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList());
    }

    public MultiTenantBenchmarkDataset buildAll() {
        TenantDataset a = buildTenantA();
        TenantDataset b = buildTenantB();
        TenantDataset c = buildTenantC();
        return new MultiTenantBenchmarkDataset(a, b, c);
    }

    public enum TenantTopology {
        FULL_FEATURED_GALLERY_RICH,
        RESULTS_RICH_MODERATE_MEDIA,
        MINIMAL_PUBLIC_PRESENCE
    }

    public record TenantDataset(
            UUID schoolId,
            String slug,
            String displayName,
            TenantTopology topology,
            School school,
            PrincipalProfile profile,
            TenantContext context,
            List<SchoolFeature> features,
            List<GalleryAlbum> albums,
            List<GalleryImage> images,
            List<Notice> notices,
            List<StudyMaterial> materials,
            List<Download> downloads,
            List<Standard> standards,
            List<AnnualExamResult> results) {

        public Map<String, Integer> getRowCounts() {
            Map<String, Integer> counts = new HashMap<>();
            counts.put("features", features.size());
            counts.put("galleryAlbums", albums.size());
            counts.put("galleryImages", images.size());
            counts.put("notices", notices.size());
            counts.put("studyMaterials", materials.size());
            counts.put("downloads", downloads.size());
            counts.put("standards", standards.size());
            counts.put("examResults", results.size());
            return counts;
        }
    }

    public record MultiTenantBenchmarkDataset(
            TenantDataset tenantA, TenantDataset tenantB, TenantDataset tenantC) {

        public List<TenantDataset> allTenants() {
            return List.of(tenantA, tenantB, tenantC);
        }

        public int totalRowCounts() {
            return allTenants().stream()
                    .mapToInt(
                            t ->
                                    t.getRowCounts().values().stream()
                                            .mapToInt(Integer::intValue)
                                            .sum())
                    .sum();
        }
    }
}
