package com.lms.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lms.dto.StudentProgressReportDTO;
import com.lms.dto.StudentProgressReportDTO.ExamPerformanceDTO;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;

@Component
public class StudentProgressPdfGenerator {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private static final Font TITLE_FONT =
            new Font(Font.HELVETICA, 20, Font.BOLD);

    private static final Font SECTION_FONT =
            new Font(Font.HELVETICA, 13, Font.BOLD);

    private static final Font NORMAL_FONT =
            new Font(Font.HELVETICA, 10, Font.NORMAL);

    private static final Font SMALL_FONT =
            new Font(Font.HELVETICA, 8, Font.NORMAL);

    /**
     * Generates a student progress PDF.
     */
    public byte[] generatePdf(StudentProgressReportDTO report) {

        try (ByteArrayOutputStream outputStream =
                     new ByteArrayOutputStream()) {

            Document document = new Document(PageSize.A4, 40, 40, 45, 45);

            PdfWriter writer = PdfWriter.getInstance(document, outputStream);

            writer.setPageEvent(new ReportPageEvent());

            document.open();

            addHeader(document, report);
            addStudentDetails(document, report);
            addEnrollmentDetails(document, report);
            addCourseDetails(document, report);
            addAcademicSummary(document, report);
            addExamResults(document, report);
            addPerformanceChart(document, report);
            addStrengthsAndImprovements(document, report);
            addVerificationSection(document, report);

            document.close();

            return outputStream.toByteArray();

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to generate student progress PDF",
                    exception
            );
        }
    }

    private void addHeader(
            Document document,
            StudentProgressReportDTO report
    ) throws DocumentException {

        Paragraph title = new Paragraph(
                "LEARNHUB LMS",
                TITLE_FONT
        );

        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph subtitle = new Paragraph(
                "STUDENT PROFILE & ACADEMIC PROGRESS REPORT",
                SECTION_FONT
        );

        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(8);

        document.add(subtitle);

        document.add(new Paragraph(
                "Report ID: " + safe(report.getReportId()),
                NORMAL_FONT
        ));

        String generatedAt = report.getGeneratedAt() == null
                ? "Not available"
                : report.getGeneratedAt().format(DATE_TIME_FORMATTER);

        document.add(new Paragraph(
                "Generated: " + generatedAt,
                NORMAL_FONT
        ));

        document.add(Chunk.NEWLINE);
    }

    private void addStudentDetails(
            Document document,
            StudentProgressReportDTO report
    ) throws DocumentException {

        addSectionTitle(document, "1. STUDENT PERSONAL INFORMATION");

        PdfPTable table = createTwoColumnTable();

        addRow(table, "Student ID", value(report.getStudentId()));
        addRow(table, "Full Name", report.getStudentName());
        addRow(table, "Email", report.getEmail());
        addRow(table, "Phone", report.getPhone());
        addRow(table, "Address", report.getAddress());
        addRow(table, "Account Status", report.getAccountStatus());

        addRow(
                table,
                "Registration Date",
                formatDateTime(report.getRegistrationDate())
        );

        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    private void addEnrollmentDetails(
            Document document,
            StudentProgressReportDTO report
    ) throws DocumentException {

        addSectionTitle(document, "2. ENROLLMENT INFORMATION");

        PdfPTable table = createTwoColumnTable();

        addRow(table, "Enrollment ID", value(report.getEnrollmentId()));
        addRow(
                table,
                "Enrollment Date",
                value(report.getEnrollmentDate())
        );

        addRow(table, "Due Date", value(report.getDueDate()));

        addRow(
                table,
                "Enrollment Status",
                report.getEnrollmentStatus()
        );

addRow(
        table,
        "Overall Progress",
        report.getOverallProgress() == null
                ? "Not available"
                : String.format(
                        "%.2f%%",
                        report.getOverallProgress()
                )
);

        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    private void addCourseDetails(
            Document document,
            StudentProgressReportDTO report
    ) throws DocumentException {

        addSectionTitle(document, "3. COURSE INFORMATION");

        PdfPTable table = createTwoColumnTable();

        addRow(table, "Course ID", value(report.getCourseId()));
        addRow(table, "Course Title", report.getCourseTitle());
        addRow(table, "Course Code", report.getCourseCode());
        addRow(
                table,
                "Duration",
                report.getCourseDurationDays() == null
                        ? "Not available"
                        : report.getCourseDurationDays() + " days"
        );

        addRow(
                table,
                "Course Fees",
                report.getCourseFees() == null
                        ? "Not available"
                        : String.format(
                                "INR %.2f",
                                report.getCourseFees()
                        )
        );

        addRow(table, "Faculty", report.getFacultyName());

        addRow(
                table,
                "Description",
                report.getCourseDescription()
        );

        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    private void addAcademicSummary(
            Document document,
            StudentProgressReportDTO report
    ) throws DocumentException {

        addSectionTitle(document, "4. ACADEMIC PERFORMANCE SUMMARY");

        PdfPTable table = createTwoColumnTable();

        addRow(
                table,
                "Exams Attempted",
                value(report.getTotalExamsAttempted())
        );

        addRow(table, "Exams Passed", value(report.getExamsPassed()));
        addRow(table, "Exams Failed", value(report.getExamsFailed()));

        addRow(
                table,
                "Average Percentage",
                String.format(
                        "%.2f%%",
                        report.getAveragePercentage() == null
                                ? 0.0
                                : report.getAveragePercentage()
                )
        );

        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    private void addExamResults(
            Document document,
            StudentProgressReportDTO report
    ) throws DocumentException {

        addSectionTitle(document, "5. EXAM-WISE RESULTS");

        List<ExamPerformanceDTO> performances =
                report.getExamPerformances();

        if (performances == null || performances.isEmpty()) {

            document.add(new Paragraph(
                    "No exam results are available for this student.",
                    NORMAL_FONT
            ));

            document.add(Chunk.NEWLINE);
            return;
        }

        PdfPTable table = new PdfPTable(
                new float[]{2.5f, 1f, 1f, 1f, 1f, 1f}
        );

        table.setWidthPercentage(100);
        table.setHeaderRows(1);

        addHeaderCell(table, "Exam");
        addHeaderCell(table, "Marks");
        addHeaderCell(table, "Score");
        addHeaderCell(table, "Correct");
        addHeaderCell(table, "Wrong");
        addHeaderCell(table, "Skipped");

        for (ExamPerformanceDTO exam : performances) {

            table.addCell(cell(exam.getExamTitle()));

            table.addCell(
                    cell(
                            value(exam.getObtainedMarks())
                                    + " / "
                                    + value(exam.getTotalMarks())
                    )
            );

            table.addCell(
                    cell(
                            exam.getPercentage() == null
                                    ? "N/A"
                                    : String.format(
                                            "%.1f%%",
                                            exam.getPercentage()
                                    )
                    )
            );

            table.addCell(cell(value(exam.getCorrectAnswers())));
            table.addCell(cell(value(exam.getWrongAnswers())));
            table.addCell(cell(value(exam.getSkippedQuestions())));
        }

        document.add(table);
        document.add(Chunk.NEWLINE);

        PdfPTable submissionTable = new PdfPTable(
                new float[]{3f, 2f, 1f}
        );

        submissionTable.setWidthPercentage(100);
        submissionTable.setHeaderRows(1);

        addHeaderCell(submissionTable, "Exam");
        addHeaderCell(submissionTable, "Submission Date");
        addHeaderCell(submissionTable, "Status");

        for (ExamPerformanceDTO exam : performances) {

            submissionTable.addCell(cell(exam.getExamTitle()));

            submissionTable.addCell(
                    cell(formatDateTime(exam.getSubmissionDate()))
            );

            submissionTable.addCell(cell(exam.getStatus()));
        }

        document.add(submissionTable);
        document.add(Chunk.NEWLINE);
    }

    private void addPerformanceChart(
            Document document,
            StudentProgressReportDTO report
    ) throws Exception {

        addSectionTitle(document, "6. PERFORMANCE TREND");

        List<ExamPerformanceDTO> performances =
                report.getExamPerformances();

        if (performances == null || performances.isEmpty()) {

            document.add(new Paragraph(
                    "The performance graph will be available after exam "
                            + "results have been recorded.",
                    NORMAL_FONT
            ));

            document.add(Chunk.NEWLINE);
            return;
        }

        DefaultCategoryDataset dataset =
                new DefaultCategoryDataset();

        int index = 1;

        for (ExamPerformanceDTO exam : performances) {

            if (exam.getPercentage() != null) {

                dataset.addValue(
                        exam.getPercentage(),
                        "Score",
                        "Exam " + index
                );

                index++;
            }
        }

        if (dataset.getColumnCount() == 0) {
            document.add(new Paragraph(
                    "No percentage data is available for the graph.",
                    NORMAL_FONT
            ));
            document.add(Chunk.NEWLINE);
            return;
        }

        JFreeChart chart = ChartFactory.createLineChart(
                "Student Exam Performance",
                "Exam Sequence",
                "Percentage (%)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        BufferedImage chartImage = chart.createBufferedImage(500, 280);

        Image image = Image.getInstance(
                chartImage,
                null
        );

        image.scaleToFit(480, 260);
        image.setAlignment(Element.ALIGN_CENTER);

        document.add(image);
        document.add(Chunk.NEWLINE);
    }

    private void addStrengthsAndImprovements(
            Document document,
            StudentProgressReportDTO report
    ) throws DocumentException {

        addSectionTitle(
                document,
                "7. STRENGTHS & AREAS FOR IMPROVEMENT"
        );

        List<ExamPerformanceDTO> performances =
                report.getExamPerformances();

        if (performances == null || performances.isEmpty()) {

            document.add(new Paragraph(
                    "Insufficient exam data is available to assess "
                            + "strengths or areas for improvement.",
                    NORMAL_FONT
            ));

            document.add(Chunk.NEWLINE);
            return;
        }

        double average = report.getAveragePercentage() == null
                ? 0.0
                : report.getAveragePercentage();

        if (average >= 80.0) {

            document.add(new Paragraph(
                    "Strength: Strong overall exam performance.",
                    NORMAL_FONT
            ));

        } else if (average >= 60.0) {

            document.add(new Paragraph(
                    "Strength: Satisfactory overall performance. "
                            + "Continue practising to improve consistency.",
                    NORMAL_FONT
            ));

        } else {

            document.add(new Paragraph(
                    "Area for improvement: Review course materials "
                            + "and practise additional questions.",
                    NORMAL_FONT
            ));
        }

        int totalWrong = 0;
        int totalSkipped = 0;

        for (ExamPerformanceDTO exam : performances) {

            totalWrong += exam.getWrongAnswers() == null
                    ? 0
                    : exam.getWrongAnswers();

            totalSkipped += exam.getSkippedQuestions() == null
                    ? 0
                    : exam.getSkippedQuestions();
        }

        document.add(new Paragraph(
                "Total incorrect answers: " + totalWrong,
                NORMAL_FONT
        ));

        document.add(new Paragraph(
                "Total skipped questions: " + totalSkipped,
                NORMAL_FONT
        ));

        document.add(new Paragraph(
                "Recommendation: Review incorrect answers and revisit "
                        + "the topics associated with those questions.",
                NORMAL_FONT
        ));

        document.add(Chunk.NEWLINE);
    }

    private void addVerificationSection(
            Document document,
            StudentProgressReportDTO report
    ) throws Exception {

        addSectionTitle(document, "8. REPORT VERIFICATION");

        document.add(new Paragraph(
                "Report ID: " + safe(report.getReportId()),
                NORMAL_FONT
        ));

        document.add(new Paragraph(
                "Verification Code: "
                        + safe(report.getVerificationCode()),
                NORMAL_FONT
        ));

        document.add(new Paragraph(
                "Use the LearnHub report verification endpoint to check "
                        + "whether this report ID exists in the system.",
                SMALL_FONT
        ));

        if (report.getReportId() != null) {

            String qrContent =
                    "LearnHub Report ID: " + report.getReportId();

            QRCodeWriter qrCodeWriter = new QRCodeWriter();

            BitMatrix matrix = qrCodeWriter.encode(
                    qrContent,
                    BarcodeFormat.QR_CODE,
                    150,
                    150
            );

            BufferedImage qrImage =
                    MatrixToImageWriter.toBufferedImage(matrix);

            Image image = Image.getInstance(qrImage, null);

            image.scaleToFit(100, 100);
            image.setAlignment(Element.ALIGN_LEFT);

            document.add(image);
        }

        document.add(Chunk.NEWLINE);

        Paragraph footer = new Paragraph(
                "This report was generated electronically by LearnHub LMS.",
                SMALL_FONT
        );

        footer.setAlignment(Element.ALIGN_CENTER);

        document.add(footer);
    }

    private void addSectionTitle(
            Document document,
            String title
    ) throws DocumentException {

        Paragraph paragraph = new Paragraph(
                title,
                SECTION_FONT
        );

        paragraph.setSpacingBefore(5);
        paragraph.setSpacingAfter(7);

        document.add(paragraph);
    }

    private PdfPTable createTwoColumnTable()
            throws DocumentException {

        PdfPTable table = new PdfPTable(
                new float[]{1.5f, 2.5f}
        );

        table.setWidthPercentage(100);
        table.setSpacingAfter(5);

        return table;
    }

    private void addRow(
            PdfPTable table,
            String label,
            String value
    ) {

        PdfPCell labelCell = cell(label);
        PdfPCell valueCell = cell(value);

        labelCell.setBackgroundColor(
                new java.awt.Color(230, 235, 245)
        );

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addHeaderCell(
            PdfPTable table,
            String text
    ) {

        PdfPCell cell = cell(text);

        cell.setBackgroundColor(
                new java.awt.Color(35, 55, 90)
        );

        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Font headerFont = new Font(
                Font.HELVETICA,
                8,
                Font.BOLD,
                java.awt.Color.WHITE
        );

        cell.setPhrase(new Phrase(text, headerFont));

        table.addCell(cell);
    }

    private PdfPCell cell(String text) {

        PdfPCell cell = new PdfPCell(
                new Phrase(safe(text), NORMAL_FONT)
        );

        cell.setPadding(5);

        return cell;
    }

    private String formatDateTime(
            java.time.LocalDateTime dateTime
    ) {

        if (dateTime == null) {
            return "Not available";
        }

        return dateTime.format(DATE_TIME_FORMATTER);
    }

    private String value(Object object) {

        return object == null
                ? "Not available"
                : String.valueOf(object);
    }

    private String safe(String value) {

        return value == null || value.isBlank()
                ? "Not available"
                : value;
    }

    /**
     * Adds page numbers to every page.
     */
    private static class ReportPageEvent extends PdfPageEventHelper {

        @Override
        public void onEndPage(
                PdfWriter writer,
                Document document
        ) {

            PdfContentByte canvas = writer.getDirectContent();

            Phrase footer = new Phrase(
                    "LearnHub LMS | Page "
                            + writer.getPageNumber(),
                    new Font(Font.HELVETICA, 8, Font.NORMAL)
            );

            ColumnText.showTextAligned(
                    canvas,
                    Element.ALIGN_CENTER,
                    footer,
                    (document.right() + document.left()) / 2,
                    document.bottom() - 20,
                    0
            );
        }
    }

    /**
     * Calculates a SHA-256 hash for a generated PDF.
     * The service will store this hash for report verification.
     */
    public String calculateSha256(byte[] pdfBytes) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(pdfBytes);

            return HexFormat.of().formatHex(hash);

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Unable to calculate PDF hash",
                    exception
            );
        }
    }
}
