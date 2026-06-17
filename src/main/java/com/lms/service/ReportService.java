package com.lms.service;

import com.lms.entity.Result;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.ResultRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ReportService {

    @Autowired
    private ResultRepository resultRepository;

    public byte[] generateStudentResultsExcel(Long studentId) throws IOException {
        List<Result> results = resultRepository.findByStudent_Id(studentId);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Student Results");

            // Header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Exam Title", "Total Marks", "Obtained Marks", "Percentage", "Status", "Correct Answers", "Wrong Answers", "Skipped Questions"};

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 1;
            for (Result result : results) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(result.getExam().getTitle());
                row.createCell(1).setCellValue(result.getExam().getTotalMarks());
                row.createCell(2).setCellValue(result.getObtainedMarks());
                row.createCell(3).setCellValue(String.format("%.2f%%", result.getPercentage()));
                row.createCell(4).setCellValue(result.getStatus());
                row.createCell(5).setCellValue(result.getCorrectAnswers());
                row.createCell(6).setCellValue(result.getWrongAnswers());
                row.createCell(7).setCellValue(result.getSkippedQuestions());
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    public byte[] generateExamResultsExcel(Long examId) throws IOException {
        List<Result> results = resultRepository.findByExam_Id(examId);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Exam Results");

            // Header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Student Name", "Email", "Total Marks", "Obtained Marks", "Percentage", "Status", "Correct Answers", "Wrong Answers"};

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 1;
            for (Result result : results) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(result.getStudent().getFirstName() + " " + result.getStudent().getLastName());
                row.createCell(1).setCellValue(result.getStudent().getEmail());
                row.createCell(2).setCellValue(result.getExam().getTotalMarks());
                row.createCell(3).setCellValue(result.getObtainedMarks());
                row.createCell(4).setCellValue(String.format("%.2f%%", result.getPercentage()));
                row.createCell(5).setCellValue(result.getStatus());
                row.createCell(6).setCellValue(result.getCorrectAnswers());
                row.createCell(7).setCellValue(result.getWrongAnswers());
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
