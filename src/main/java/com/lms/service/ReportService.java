package com.lms.service;

import com.lms.entity.Result;
import com.lms.repository.ResultRepository;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReportService {

    @Autowired
    private ResultRepository resultRepository;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    // =========================================================
    // 1. EXPORT ALL RESULTS FOR AN INDIVIDUAL STUDENT
    // =========================================================

    public byte[] generateStudentResultsExcel(Long studentId)
            throws IOException {

        List<Result> results =
                resultRepository.findByStudent_Id(studentId);

        return generateExcel(results, "Student Results");
    }

    // =========================================================
    // 2. EXPORT ALL RESULTS FOR AN EXAM
    // =========================================================

    public byte[] generateExamResultsExcel(Long examId)
            throws IOException {

        List<Result> results =
                resultRepository.findByExam_Id(examId);

        return generateExcel(results, "Exam Results");
    }

    // =========================================================
    // 3. EXPORT ALL RESULTS FOR A COURSE
    // =========================================================

    public byte[] generateCourseResultsExcel(Long courseId)
            throws IOException {

        List<Result> results =
                resultRepository.findByExam_Course_Id(courseId);

        return generateExcel(results, "Course Results");
    }

    // =========================================================
    // 4. EXPORT FILTERED RESULTS FOR A COURSE
    // =========================================================

    public byte[] generateFilteredCourseResultsExcel(
            Long courseId,
            Long examId,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            String status
    ) throws IOException {

        validateFilters(fromDate, toDate, status);

        List<Result> results =
                resultRepository.findFilteredCourseResults(
                        courseId,
                        examId,
                        fromDate,
                        toDate,
                        status == null || status.isBlank()
                                ? null
                                : status.trim()
                );

        return generateExcel(results, "Filtered Results");
    }

    // =========================================================
    // 5. COMMON EXCEL GENERATOR
    // =========================================================

    private byte[] generateExcel(
            List<Result> results,
            String sheetName
    ) throws IOException {

        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet(sheetName);

            String[] headers = {
                    "Student ID",
                    "Student Name",
                    "Email",
                    "Course Name",
                    "Exam Name",
                    "Marks Obtained",
                    "Total Marks",
                    "Percentage",
                    "Grade",
                    "Pass/Fail",
                    "Attempt Date"
            };

            // -------------------------------------------------
            // HEADER STYLE
            // -------------------------------------------------

            CellStyle headerStyle = workbook.createCellStyle();

            headerStyle.setFillForegroundColor(
                    IndexedColors.DARK_BLUE.getIndex()
            );

            headerStyle.setFillPattern(
                    FillPatternType.SOLID_FOREGROUND
            );

            Font headerFont = workbook.createFont();

            headerFont.setColor(
                    IndexedColors.WHITE.getIndex()
            );

            headerFont.setBold(true);

            headerStyle.setFont(headerFont);

            headerStyle.setAlignment(
                    HorizontalAlignment.CENTER
            );

            // -------------------------------------------------
            // CREATE HEADER
            // -------------------------------------------------

            Row headerRow = sheet.createRow(0);

            for (int i = 0; i < headers.length; i++) {

                Cell cell = headerRow.createCell(i);

                cell.setCellValue(headers[i]);

                cell.setCellStyle(headerStyle);
            }

            // -------------------------------------------------
            // DATE CELL STYLE
            // -------------------------------------------------

            CellStyle dateStyle = workbook.createCellStyle();

            dateStyle.setDataFormat(
                    workbook.createDataFormat()
                            .getFormat("dd-mm-yyyy hh:mm")
            );

            // -------------------------------------------------
            // DATA ROWS
            // -------------------------------------------------

            int rowNumber = 1;

            for (Result result : results) {

                Row row = sheet.createRow(rowNumber++);

                // Student ID
                row.createCell(0).setCellValue(
                        result.getStudent().getId()
                );

                // Student Name
                String firstName =
                        result.getStudent().getFirstName() == null
                                ? ""
                                : result.getStudent().getFirstName();

                String lastName =
                        result.getStudent().getLastName() == null
                                ? ""
                                : result.getStudent().getLastName();

                row.createCell(1).setCellValue(
                        (firstName + " " + lastName).trim()
                );

                // Email
                row.createCell(2).setCellValue(
                        safeString(result.getStudent().getEmail())
                );

                // Course
                row.createCell(3).setCellValue(
                        safeString(
                                result.getExam().getCourse().getTitle()
                        )
                );

                // Exam
                row.createCell(4).setCellValue(
                        safeString(result.getExam().getTitle())
                );

                // Obtained Marks
                row.createCell(5).setCellValue(
                        result.getObtainedMarks() == null
                                ? 0
                                : result.getObtainedMarks()
                );

                // Total Marks
                row.createCell(6).setCellValue(
                        result.getExam().getTotalMarks() == null
                                ? 0
                                : result.getExam().getTotalMarks()
                );

                // Percentage
                double percentage =
                        result.getPercentage() == null
                                ? 0.0
                                : result.getPercentage();

                row.createCell(7).setCellValue(
                        Math.round(percentage * 100.0) / 100.0
                );

                // Grade
                row.createCell(8).setCellValue(
                        calculateGrade(percentage)
                );

                // Pass/Fail
                row.createCell(9).setCellValue(
                        safeString(result.getStatus())
                );

                // Attempt Date
                LocalDateTime attemptDate = getAttemptDate(result);

                if (attemptDate != null) {

                    Cell dateCell = row.createCell(10);

                    dateCell.setCellValue(
                            java.sql.Timestamp.valueOf(attemptDate)
                    );

                    dateCell.setCellStyle(dateStyle);

                } else {

                    row.createCell(10).setCellValue("N/A");
                }
            }

            // -------------------------------------------------
            // COLUMN WIDTHS
            // -------------------------------------------------

            for (int i = 0; i < headers.length; i++) {

                sheet.autoSizeColumn(i);

                // Prevent excessively wide columns.
                if (sheet.getColumnWidth(i) > 15000) {
                    sheet.setColumnWidth(i, 15000);
                }
            }

            sheet.createFreezePane(0, 1);

            // -------------------------------------------------
            // RETURN EXCEL BYTES
            // -------------------------------------------------

            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();

            workbook.write(outputStream);

            return outputStream.toByteArray();
        }
    }

    // =========================================================
    // 6. CALCULATE GRADE
    // =========================================================

    private String calculateGrade(double percentage) {

        if (percentage >= 90) {
            return "A+";
        } else if (percentage >= 80) {
            return "A";
        } else if (percentage >= 70) {
            return "B";
        } else if (percentage >= 60) {
            return "C";
        } else if (percentage >= 50) {
            return "D";
        } else {
            return "F";
        }
    }

    // =========================================================
    // 7. GET ATTEMPT DATE
    // =========================================================

    private LocalDateTime getAttemptDate(Result result) {

        if (result.getExamAttempt() == null) {
            return result.getCreatedAt();
        }

        if (result.getExamAttempt().getSubmissionTime() != null) {
            return result.getExamAttempt().getSubmissionTime();
        }

        if (result.getExamAttempt().getEndTime() != null) {
            return result.getExamAttempt().getEndTime();
        }

        if (result.getExamAttempt().getCreatedAt() != null) {
            return result.getExamAttempt().getCreatedAt();
        }

        return result.getCreatedAt();
    }

    // =========================================================
    // 8. VALIDATE FILTERS
    // =========================================================

    private void validateFilters(
            LocalDateTime fromDate,
            LocalDateTime toDate,
            String status
    ) {

        if (fromDate != null
                && toDate != null
                && fromDate.isAfter(toDate)) {

            throw new IllegalArgumentException(
                    "From date must be before or equal to the to date."
            );
        }

        if (status != null && !status.isBlank()
                && !status.equalsIgnoreCase("PASS")
                && !status.equalsIgnoreCase("FAIL")) {

            throw new IllegalArgumentException(
                    "Status must be PASS or FAIL."
            );
        }
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }
    public byte[] generateResultsExcelForRecords(
        List<Result> results,
        String sheetName
) throws IOException {

    return generateExcel(results, sheetName);
}
}