package com.lms.service;

import com.lms.dto.admin.AdminStudentReportDTO;
import com.lms.dto.admin.AdminStudentReportDTO.CodingRow;
import com.lms.dto.admin.AdminStudentReportDTO.CourseSection;
import com.lms.dto.admin.AdminStudentReportDTO.ExamRow;
import com.lms.dto.admin.AdminStudentReportDTO.ExtensionRow;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Complete student report PDF used by the admin downloads.
 * It renders every course section from the AdminStudentReportDTO.
 */
@Component
public class AdminStudentFullReportPdfGenerator {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private static final Font TITLE_FONT =
            new Font(Font.HELVETICA, 18, Font.BOLD);

    private static final Font SECTION_FONT =
            new Font(Font.HELVETICA, 12, Font.BOLD);

    private static final Font NORMAL_FONT =
            new Font(Font.HELVETICA, 9, Font.NORMAL);

    private static final Font SMALL_FONT =
            new Font(Font.HELVETICA, 8, Font.NORMAL);

    public byte[] generate(AdminStudentReportDTO report) {

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            Document document = new Document(
                    PageSize.A4, 36, 36, 40, 40);

            PdfWriter writer = PdfWriter.getInstance(document, output);
            writer.setPageEvent(new PageEvent());

            document.open();

            Paragraph title = new Paragraph(
                    "Student Full Report", TITLE_FONT);
            title.setSpacingAfter(4);
            document.add(title);

            Paragraph generated = new Paragraph(
                    "Generated on " + formatDateTime(LocalDateTime.now())
                            + "  |  LearnHub LMS",
                    SMALL_FONT);
            generated.setSpacingAfter(10);
            document.add(generated);

            addProfile(document, report);
            addSummary(document, report);

            for (CourseSection course : report.getCourses()) {
                addCourse(document, course);
            }

            document.close();

            return output.toByteArray();

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to generate student full report PDF",
                    exception);
        }
    }

    private void addProfile(
            Document document,
            AdminStudentReportDTO report) throws DocumentException {

        section(document, "1. Student information");

        PdfPTable table = twoColumn();
        row(table, "Student name", value(report.getStudentName()));
        row(table, "Student ID", value(report.getStudentId()));
        row(table, "Email", value(report.getEmail()));
        row(table, "Phone", value(report.getPhone()));
        row(table, "Address", value(report.getAddress()));
        row(table, "Account status", value(report.getAccountStatus()));
        row(table, "Registered", formatDateTime(report.getRegisteredAt()));

        document.add(table);
    }

    private void addSummary(
            Document document,
            AdminStudentReportDTO report) throws DocumentException {

        section(document, "2. Academic summary");

        PdfPTable table = twoColumn();
        row(table, "Enrolled courses", value(report.getEnrolledCourses()));
        row(table, "Active enrollments", value(report.getActiveCourses()));
        row(table, "Inactive enrollments", value(report.getInactiveCourses()));
        row(table, "Completed courses", value(report.getCompletedCourses()));
        row(table, "Exams attempted", value(report.getTotalResults()));
        row(table, "Exams passed", value(report.getPassedResults()));
        row(table, "Exams failed", value(report.getFailedResults()));
        row(table, "Average score",
                formatPercent(report.getAveragePercentage()));

        document.add(table);
    }

    private void addCourse(
            Document document,
            CourseSection course) throws DocumentException {

        section(document, "Course: " + value(course.getCourseTitle()));

        PdfPTable info = twoColumn();
        row(info, "Course code", value(course.getCourseCode()));
        row(info, "Faculty", value(course.getFacultyName()));
        row(info, "Enrollment status", value(course.getEnrollmentStatus()));
        row(info, "Enrolled on", formatDateTime(course.getEnrollmentDate()));
        row(info, "Due date", formatDateTime(course.getDueDate()));
        row(info, "Completion",
                formatPercent(course.getCompletionPercentage()));
        row(info, "Overall progress",
                formatPercent(course.getOverallProgress()));
        row(info, "Materials completed",
                value(course.getMaterialsCompleted()) + " / "
                        + value(course.getMaterialsTotal()));
        row(info, "Exam approval status",
                value(course.getExamApprovalStatus()));
        row(info, "Coding submissions",
                value(course.getCodingSubmissions())
                        + " (" + value(course.getCodingApproved())
                        + " approved)");
        document.add(info);

        addExamResults(document, course.getExamResults());
        addCodingHistory(document, course.getCodingHistory());
        addExtensionHistory(document, course.getExtensionHistory());
    }

    private void addExamResults(
            Document document,
            List<ExamRow> rows) throws DocumentException {

        section(document, "Exam participation and results");

        if (rows == null || rows.isEmpty()) {
            document.add(new Paragraph(
                    "No exam attempts recorded.", NORMAL_FONT));
            return;
        }

        List<String[]> data = new ArrayList<>();

        for (ExamRow row : rows) {
            data.add(new String[]{
                    value(row.getExamTitle()),
                    value(row.getObtainedMarks()) + " / "
                            + value(row.getTotalMarks()),
                    formatPercent(row.getPercentage()),
                    value(row.getStatus()),
                    formatDateTime(row.getSubmittedAt())
            });
        }

        document.add(dataTable(
                new float[]{3.2f, 1.4f, 1.1f, 1.1f, 2.2f},
                new String[]{"Exam", "Marks", "Score", "Status", "Submitted"},
                data));
    }

    private void addCodingHistory(
            Document document,
            List<CodingRow> rows) throws DocumentException {

        section(document, "Coding submission history");

        if (rows == null || rows.isEmpty()) {
            document.add(new Paragraph(
                    "No coding submissions recorded.", NORMAL_FONT));
            return;
        }

        List<String[]> data = new ArrayList<>();

        for (CodingRow row : rows) {
            data.add(new String[]{
                    value(row.getChallengeTitle()),
                    formatDateTime(row.getSubmittedAt()),
                    value(row.getTestCasesPassed()) + " / "
                            + value(row.getTotalTestCases()),
                    formatPercent(row.getPercentage()),
                    value(row.getReviewStatus()),
                    value(row.getFacultyFeedback())
            });
        }

        document.add(dataTable(
                new float[]{2.6f, 2.0f, 1.3f, 1.0f, 1.5f, 2.4f},
                new String[]{
                        "Challenge", "Submitted", "Test cases",
                        "Score", "Review", "Feedback"
                },
                data));
    }

    private void addExtensionHistory(
            Document document,
            List<ExtensionRow> rows) throws DocumentException {

        section(document, "Extension history");

        if (rows == null || rows.isEmpty()) {
            document.add(new Paragraph(
                    "No extension requests recorded.", NORMAL_FONT));
            return;
        }

        List<String[]> data = new ArrayList<>();

        for (ExtensionRow row : rows) {
            data.add(new String[]{
                    formatDateTime(row.getOriginalDueDate()),
                    value(row.getDaysRequested()),
                    value(row.getReason()),
                    formatDateTime(row.getRequestedAt()),
                    value(row.getStatus()),
                    value(row.getReviewedByName()),
                    formatDateTime(row.getReviewedAt())
            });
        }

        document.add(dataTable(
                new float[]{1.9f, 0.7f, 2.4f, 1.9f, 1.1f, 1.4f, 1.9f},
                new String[]{
                        "Original due date", "Days", "Reason", "Requested",
                        "Status", "Reviewed by", "Reviewed on"
                },
                data));
    }

    private void section(Document document, String text)
            throws DocumentException {

        Paragraph paragraph = new Paragraph(text, SECTION_FONT);
        paragraph.setSpacingBefore(10);
        paragraph.setSpacingAfter(6);

        document.add(paragraph);
    }

    private PdfPTable twoColumn() throws DocumentException {

        PdfPTable table = new PdfPTable(new float[]{1.4f, 2.6f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(6);

        return table;
    }

    private void row(PdfPTable table, String label, String value) {

        PdfPCell labelCell = cell(label);
        labelCell.setBackgroundColor(
                new java.awt.Color(232, 237, 246));

        table.addCell(labelCell);
        table.addCell(cell(value));
    }

    private PdfPTable dataTable(
            float[] widths,
            String[] headers,
            List<String[]> rows) throws DocumentException {

        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100);
        table.setSpacingAfter(6);

        for (String header : headers) {
            PdfPCell cell = cell(header);
            cell.setBackgroundColor(new java.awt.Color(35, 55, 90));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPhrase(new Phrase(header, new Font(
                    Font.HELVETICA, 8, Font.BOLD, java.awt.Color.WHITE)));
            table.addCell(cell);
        }

        for (String[] row : rows) {
            for (String value : row) {
                table.addCell(cell(value));
            }
        }

        return table;
    }

    private PdfPCell cell(String text) {

        PdfPCell cell = new PdfPCell(
                new Phrase(text == null ? "" : text, NORMAL_FONT));

        cell.setPadding(4);

        return cell;
    }

    private String value(Object value) {
        return value == null ? "—" : String.valueOf(value);
    }

    private String formatPercent(Double value) {
        return value == null
                ? "—"
                : String.format(java.util.Locale.ROOT, "%.2f%%", value);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "—" : value.format(DATE_TIME);
    }

    private static class PageEvent extends PdfPageEventHelper {

        @Override
        public void onEndPage(PdfWriter writer, Document document) {

            Phrase footer = new Phrase(
                    "LearnHub LMS | Page " + writer.getPageNumber(),
                    new Font(Font.HELVETICA, 8, Font.NORMAL));

            com.lowagie.text.pdf.ColumnText.showTextAligned(
                    writer.getDirectContent(),
                    Element.ALIGN_CENTER,
                    footer,
                    (document.right() + document.left()) / 2,
                    document.bottom() - 18,
                    0);
        }
    }
}
