(() => {
    "use strict";

    const API_BASE_URL = `${window.API_BASE_URL}/api/reports`;
    

    const $ = (id) => document.getElementById(id);

    const courseInput = $("courseId");
    const examInput = $("examId");
    const fromDateInput = $("fromDate");
    const toDateInput = $("toDate");
    const statusInput = $("status");

    const pageMessage = $("pageMessage");

    /* =========================================================
       DISPLAY MESSAGE
       ========================================================= */

    function showMessage(text, type = "") {
        pageMessage.textContent = text;
        pageMessage.className = `page-message ${type}`.trim();
    }

    /* =========================================================
       VALIDATE IDS
       ========================================================= */

    function requireId(input, label) {
        const raw = input.value.trim();
        const id = Number(raw);

        if (!raw || !Number.isSafeInteger(id) || id < 1) {
            throw new Error(`Enter a valid ${label}.`);
        }

        return id;
    }

    /* =========================================================
       AUTHENTICATION
       Uses the existing login.js localStorage keys.
       ========================================================= */

    function getAuthHeaders() {
        const token = localStorage.getItem("token");

        const role = String(
            localStorage.getItem("role") || ""
        )
            .replace(/^ROLE_/i, "")
            .toUpperCase();

        if (!token) {
            throw new Error(
                "Your login session was not found. Please sign in again."
            );
        }

        if (role !== "FACULTY") {
            throw new Error(
                "This page is available only to faculty accounts."
            );
        }

        return {
            Authorization: `Bearer ${token}`
        };
    }

    /* =========================================================
       FACULTY PROFILE AND DATE
       ========================================================= */

    function setFacultyProfile() {
        const facultyName =
            localStorage.getItem("userName") ||
            localStorage.getItem("name") ||
            "Faculty";

        $("facultyName").textContent = facultyName;

        const initial =
            facultyName.trim().charAt(0).toUpperCase() || "F";

        $("facultyAvatar").textContent = initial;
        $("topAvatar").textContent = initial;

        $("currentDate").textContent =
            new Intl.DateTimeFormat("en-IN", {
                weekday: "short",
                day: "numeric",
                month: "short",
                year: "numeric"
            }).format(new Date());

        $("year").textContent = new Date().getFullYear();
    }

    /* =========================================================
       DOWNLOAD EXCEL FILE
       ========================================================= */

    async function downloadExcel(url, filename) {
        let response;

        try {
            showMessage("Preparing your Excel file...");

            const headers = getAuthHeaders();

            response = await fetch(url, {
                method: "GET",
                headers: headers
            });

        } catch (error) {
            showMessage(
                error.message || "Unable to connect to the server.",
                "error"
            );

            return;
        }

        if (!response.ok) {
            let errorDetails = "";

            try {
                const contentType =
                    response.headers.get("content-type") || "";

                if (contentType.includes("application/json")) {
                    errorDetails = JSON.stringify(
                        await response.json()
                    );
                } else {
                    errorDetails = await response.text();
                }
            } catch (error) {
                // Use the HTTP status message below.
            }

            if (response.status === 401) {
                showMessage(
                    "Your session has expired. Please sign in again.",
                    "error"
                );

            } else if (response.status === 403) {
                showMessage(
                    "Access denied. Check whether you are assigned to this course.",
                    "error"
                );

            } else if (response.status === 404) {
                showMessage(
                    "No matching records were found, or the API endpoint does not exist.",
                    "error"
                );

            } else {
                showMessage(
                    errorDetails ||
                    `Export failed. HTTP status: ${response.status}`,
                    "error"
                );
            }

            return;
        }

        try {
            const blob = await response.blob();

            if (blob.size === 0) {
                showMessage(
                    "The server returned an empty file.",
                    "warning"
                );
                return;
            }

            const objectURL = URL.createObjectURL(blob);

            const downloadLink = document.createElement("a");

            downloadLink.href = objectURL;
            downloadLink.download = filename;

            document.body.appendChild(downloadLink);

            downloadLink.click();
            downloadLink.remove();

            setTimeout(() => {
                URL.revokeObjectURL(objectURL);
            }, 1500);

            showMessage(
                "Excel file downloaded successfully.",
                "success"
            );

        } catch (error) {
            showMessage(
                "Unable to download the Excel file.",
                "error"
            );
        }
    }

    /* =========================================================
       DATE VALIDATION
       ========================================================= */

    function validateDates() {
        const fromDate = fromDateInput.value;
        const toDate = toDateInput.value;

        if (fromDate && toDate && fromDate > toDate) {
            throw new Error(
                "From date cannot be later than To date."
            );
        }
    }

    /* =========================================================
       UPDATE SUMMARY CARDS
       ========================================================= */

    function updateSummary() {
        $("selectedCourseLabel").textContent =
            courseInput.value || "—";

        $("selectedExamLabel").textContent =
            examInput.value || "All";

        $("selectedStatusLabel").textContent =
            statusInput.value || "All";
    }

    /* =========================================================
       EXPORT FILTERED COURSE RESULTS
       Endpoint:
       GET /api/reports/export/course-results/{courseId}/filtered
       ========================================================= */

    $("filterForm").addEventListener("submit", async (event) => {
        event.preventDefault();

        try {
            const courseId = requireId(
                courseInput,
                "Course ID"
            );

            validateDates();

            const params = new URLSearchParams();

            if (examInput.value.trim()) {
                const examId = requireId(
                    examInput,
                    "Exam ID"
                );

                params.set("examId", examId);
            }

            if (fromDateInput.value) {
                params.set(
                    "fromDate",
                    fromDateInput.value
                );
            }

            if (toDateInput.value) {
                params.set(
                    "toDate",
                    toDateInput.value
                );
            }

            if (statusInput.value) {
                params.set(
                    "status",
                    statusInput.value
                );
            }

            updateSummary();

            const queryString = params.toString();

            const url =
                `${API_BASE_URL}/export/course-results/${courseId}/filtered` +
                (queryString ? `?${queryString}` : "");

            await downloadExcel(
                url,
                `course-${courseId}-filtered-results.xlsx`
            );

        } catch (error) {
            showMessage(error.message, "error");
        }
    });

    /* =========================================================
       EXPORT ALL COURSE RESULTS
       Endpoint:
       GET /api/reports/export/course-results/{courseId}
       ========================================================= */

    $("exportCourseBtn").addEventListener("click", async () => {
        try {
            const courseId = requireId(
                courseInput,
                "Course ID"
            );

            updateSummary();

            const url =
                `${API_BASE_URL}/export/course-results/${courseId}`;

            await downloadExcel(
                url,
                `course-${courseId}-results.xlsx`
            );

        } catch (error) {
            showMessage(error.message, "error");
        }
    });

    /* =========================================================
       EXPORT EXAM RESULTS
       Endpoint:
       GET /api/reports/export/exam-results/{examId}
       ========================================================= */

    $("exportExamBtn").addEventListener("click", async () => {
        try {
            const examId = requireId(
                examInput,
                "Exam ID"
            );

            const url =
                `${API_BASE_URL}/export/exam-results/${examId}`;

            await downloadExcel(
                url,
                `exam-${examId}-results.xlsx`
            );

        } catch (error) {
            showMessage(error.message, "error");
        }
    });

    /* =========================================================
       EXPORT INDIVIDUAL STUDENT RESULTS
       Endpoint:
       GET /api/reports/export/student-results/{studentId}/course/{courseId}
       ========================================================= */

    $("studentForm").addEventListener("submit", async (event) => {
        event.preventDefault();

        try {
            const courseId = requireId(
                courseInput,
                "Course ID"
            );

            const studentId = requireId(
                $("studentId"),
                "Student ID"
            );

            const url =
                `${API_BASE_URL}/export/student-results/${studentId}/course/${courseId}`;

            await downloadExcel(
                url,
                `student-${studentId}-course-${courseId}-results.xlsx`
            );

        } catch (error) {
            showMessage(error.message, "error");
        }
    });
// ==========================================
// STUDENT PROGRESS REPORT DOWNLOADS
// ==========================================

function getReportToken() {
    // Change this key if your existing login code uses another key.
    return localStorage.getItem("token");
}

function showProgressReportMessage(message, isError = false) {
    const element = document.getElementById("progressReportMessage");

    if (!element) return;

    element.textContent = message;
    element.style.color = isError ? "#dc2626" : "#15803d";
}

async function downloadProgressReport(url, filename, button) {
    const token = getReportToken();

    if (!token) {
        showProgressReportMessage(
            "Your login session was not found. Please sign in again.",
            true
        );
        return;
    }

    const originalText = button.textContent;

    button.disabled = true;
    button.textContent = "Generating report...";

    showProgressReportMessage("Generating your report. Please wait...");

    try {
        const response = await fetch(url, {
            method: "GET",
            headers: {
                Authorization: `Bearer ${token}`
            }
        });

        if (!response.ok) {
            let message = `Request failed (${response.status}).`;

            try {
                const contentType = response.headers.get("content-type") || "";

                if (contentType.includes("application/json")) {
                    const data = await response.json();
                    message = data.message || data.error || message;
                } else {
                    const text = await response.text();

                    if (text) {
                        message = text;
                    }
                }
            } catch (error) {
                // Keep the default error message if the response is unreadable.
            }

            throw new Error(message);
        }

        const blob = await response.blob();

        if (blob.size === 0) {
            throw new Error("The server returned an empty file.");
        }

        const objectUrl = URL.createObjectURL(blob);
        const link = document.createElement("a");

        link.href = objectUrl;
        link.download = filename;

        document.body.appendChild(link);
        link.click();
        link.remove();

        setTimeout(() => URL.revokeObjectURL(objectUrl), 1000);

        showProgressReportMessage(
            `Download started successfully: ${filename}`
        );

    } catch (error) {
        console.error("Progress report download failed:", error);

        showProgressReportMessage(
            error.message || "Unable to download the report.",
            true
        );

    } finally {
        button.disabled = false;
        button.textContent = originalText;
    }
}

// Download one student's PDF.
document
    .getElementById("downloadStudentProgressBtn")
    ?.addEventListener("click", function () {

        const courseId = document
            .getElementById("progressCourseId")
            .value.trim();

        const studentId = document
            .getElementById("progressStudentId")
            .value.trim();

        if (!courseId || Number(courseId) < 1) {
            showProgressReportMessage(
                "Please enter a valid Course ID.",
                true
            );
            return;
        }

        if (!studentId || Number(studentId) < 1) {
            showProgressReportMessage(
                "Please enter a valid Student ID.",
                true
            );
            return;
        }

        const url =
            `/api/reports/student-progress/${encodeURIComponent(studentId)}` +
            `/course/${encodeURIComponent(courseId)}/pdf`;

        downloadProgressReport(
            url,
            `Student_${studentId}_Progress_Report.pdf`,
            this
        );
    });

// Download all student reports for a course as a ZIP.
document
    .getElementById("downloadCourseProgressZipBtn")
    ?.addEventListener("click", function () {

        const courseId = document
            .getElementById("progressCourseId")
            .value.trim();

        if (!courseId || Number(courseId) < 1) {
            showProgressReportMessage(
                "Please enter a valid Course ID.",
                true
            );
            return;
        }

        const url =
            `/api/reports/course-progress/${encodeURIComponent(courseId)}` +
            `/pdf/zip`;

        downloadProgressReport(
            url,
            `Course_${courseId}_Student_Reports.zip`,
            this
        );
    });

    /* =========================================================
       LOGOUT
       ========================================================= */

    function logout() {
        localStorage.removeItem("token");
        localStorage.removeItem("userId");
        localStorage.removeItem("role");
        localStorage.removeItem("userName");
        localStorage.removeItem("name");

        // Change this path if your login page uses another URL.
        window.location.href = "/login.html";
    }

    $("logoutBtn").addEventListener("click", logout);

    /* =========================================================
       LIVE SUMMARY UPDATES
       ========================================================= */

    courseInput.addEventListener("input", updateSummary);

    examInput.addEventListener("input", updateSummary);

    statusInput.addEventListener("change", updateSummary);

    /* =========================================================
       INITIALIZE PAGE
       ========================================================= */

    setFacultyProfile();

    const storedRole = String(
        localStorage.getItem("role") || ""
    )
        .replace(/^ROLE_/i, "")
        .toUpperCase();

    if (!localStorage.getItem("token")) {
        showMessage(
            "No login token found. Please sign in first.",
            "error"
        );

    } else if (storedRole !== "FACULTY") {
        showMessage(
            "This page is restricted to faculty accounts.",
            "error"
        );

    } else {
        showMessage(
            "Signed in as faculty. Enter a course ID to export results.",
            "success"
        );
    }

    // Keep logout available for other page scripts if needed.
    window.logout = logout;

})();
