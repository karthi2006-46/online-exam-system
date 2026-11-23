package com.lms.service;

import com.lms.dto.admin.AdminCourseReportRowDTO;
import com.lms.dto.admin.AdminFacultyWorkloadDTO;
import com.lms.dto.admin.AdminReportOverviewDTO;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Printable platform report used by the admin Reports page.
 */
@Component
public class AdminReportPdfGenerator {

    private static final Color INK = new Color(24, 32, 45);
    private static final Color MUTED = new Color(108, 118, 132);
    private static final Color BRAND = new Color(15, 61, 62);
    private static final Color HEADER_TEXT = Color.WHITE;
    private static final Color STRIPE = new Color(244, 247, 249);
    private static final Color BORDER = new Color(220, 226, 232);

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    public byte[] generate(AdminReportOverviewDTO report) {

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            Document document = new Document(
                    PageSize.A4, 34, 34, 42, 46);

            PdfWriter.getInstance(document, output);

            document.open();

            addTitle(document, report);
            addSummary(document, report);
            addCourseTable(document, report.getCourses());
            addFacultyTable(document, report.getFaculty());

            document.close();

            return output.toByteArray();

        } catch (DocumentException exception) {
            throw new IllegalStateException(
                    "Failed to build the platform report PDF", exception);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(
                    "Failed to build the platform report PDF", exception);
        }
    }

    /* ---------------- sections ---------------- */

    private void addTitle(
            Document document,
            AdminReportOverviewDTO report) throws DocumentException {

        Paragraph eyebrow = new Paragraph(
                "LEARNHUB LMS  /  ADMIN",
                FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD, 8, BRAND));
        eyebrow.setSpacingAfter(4);
        document.add(eyebrow);

        Paragraph title = new Paragraph(
                "Platform Report",
                FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD, 22, INK));
        title.setSpacingAfter(2);
        document.add(title);

        Paragraph meta = new Paragraph(
                "Generated "
                        + (report.getGeneratedAt() == null
                        ? ""
                        : report.getGeneratedAt().format(STAMP)),
                FontFactory.getFont(
                        FontFactory.HELVETICA, 9, MUTED));
        meta.setSpacingAfter(14);
        document.add(meta);
    }

    private void addSummary(
            Document document,
            AdminReportOverviewDTO report) throws DocumentException {

        PdfPTable table = new PdfPTable(new float[]{1.2f, 1f, 1.2f, 1f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(14);

        addMetric(table, "Courses", report.getActiveCourses()
                + " active / " + report.getTotalCourses());
        addMetric(table, "Faculty", String.valueOf(report.getTotalFaculty()));
        addMetric(table, "Students", String.valueOf(report.getTotalStudents()));
        addMetric(table, "Enrollments",
                report.getActiveEnrollments() + " active / "
                        + report.getTotalEnrollments());
        addMetric(table, "Pass rate", formatPercent(report.getPassPercentage()));
        addMetric(table, "Results", String.valueOf(report.getTotalResults()));
        addMetric(table, "Pending approvals",
                String.valueOf(report.getPendingExamApprovals()));
        addMetric(table, "Pending extensions",
                String.valueOf(report.getPendingExtensionRequests()));

        document.add(table);
    }

    private void addMetric(
            PdfPTable table,
            String label,
            String value) {

        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(BORDER);
        cell.setPadding(8);
        cell.setBackgroundColor(STRIPE);

        Paragraph labelParagraph = new Paragraph(
                label.toUpperCase(),
                FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD, 7, MUTED));
        labelParagraph.setSpacingAfter(2);
        cell.addElement(labelParagraph);

        cell.addElement(new Paragraph(
                value,
                FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD, 13, INK)));

        table.addCell(cell);
    }

    private void addCourseTable(
            Document document,
            List<AdminCourseReportRowDTO> courses) throws DocumentException {

        document.add(sectionHeading("Course performance"));

        PdfPTable table = new PdfPTable(
                new float[]{2.4f, 1.7f, 1f, 1f, 1f, 1f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(16);

        addHeader(table, "Course", "Faculty", "Students",
                "Results", "Pass %", "Avg %");

        boolean stripe = false;

        for (AdminCourseReportRowDTO course : courses) {

            addBody(table, course.getTitle(), stripe);
            addBody(table, course.getFacultyName(), stripe);
            addBody(table, String.valueOf(course.getStudents()), stripe);
            addBody(table, String.valueOf(course.getResults()), stripe);
            addBody(table, formatPercent(course.getPassPercentage()), stripe);
            addBody(table, formatPercent(course.getAveragePercentage()), stripe);

            stripe = !stripe;
        }

        if (courses.isEmpty()) {
            addBody(table, "No courses yet", false, 6);
        }

        document.add(table);
    }

    private void addFacultyTable(
            Document document,
            List<AdminFacultyWorkloadDTO> faculty) throws DocumentException {

        document.add(sectionHeading("Faculty workload"));

        PdfPTable table = new PdfPTable(
                new float[]{2.1f, 1.7f, 0.9f, 1f, 1f, 1.1f});
        table.setWidthPercentage(100);

        addHeader(table, "Faculty", "Email", "Courses",
                "Students", "Pending", "Coding");

        boolean stripe = false;

        for (AdminFacultyWorkloadDTO row : faculty) {

            long pending = row.getPendingExamApprovals()
                    + row.getPendingExtensions();

            addBody(table, row.getName(), stripe);
            addBody(table, row.getEmail(), stripe);
            addBody(table, String.valueOf(row.getCourses()), stripe);
            addBody(table, String.valueOf(row.getStudents()), stripe);
            addBody(table, String.valueOf(pending), stripe);
            addBody(table, String.valueOf(row.getPendingCodingReviews()), stripe);

            stripe = !stripe;
        }

        if (faculty.isEmpty()) {
            addBody(table, "No faculty yet", false, 6);
        }

        document.add(table);
    }

    /* ---------------- cell helpers ---------------- */

    private Paragraph sectionHeading(String text) {

        Paragraph heading = new Paragraph(
                text,
                FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD, 12, INK));
        heading.setSpacingBefore(4);
        heading.setSpacingAfter(6);

        return heading;
    }

    private void addHeader(PdfPTable table, String... labels) {

        Font font = FontFactory.getFont(
                FontFactory.HELVETICA_BOLD, 8, HEADER_TEXT);

        for (String label : labels) {

            PdfPCell cell = new PdfPCell(
                    new Phrase(label, font));
            cell.setBackgroundColor(BRAND);
            cell.setBorderColor(BRAND);
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);

            table.addCell(cell);
        }
    }

    private void addBody(
            PdfPTable table,
            String value,
            boolean stripe) {

        addBody(table, value, stripe, 1);
    }

    private void addBody(
            PdfPTable table,
            String value,
            boolean stripe,
            int colSpan) {

        PdfPCell cell = new PdfPCell(new Phrase(
                value == null ? "" : value,
                FontFactory.getFont(
                        FontFactory.HELVETICA, 8, INK)));

        cell.setPadding(6);
        cell.setBorderColor(BORDER);
        cell.setBackgroundColor(stripe ? STRIPE : Color.WHITE);
        cell.setColspan(colSpan);

        table.addCell(cell);
    }

    private String formatPercent(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f%%", value);
    }
}
