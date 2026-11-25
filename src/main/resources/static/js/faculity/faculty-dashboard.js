"use strict";

const API_BASE = `${window.API_BASE_URL}/api`;


// ----------------------------------------
// AUTHENTICATION
// ----------------------------------------

function getToken() {
    return localStorage.getItem("token");
}

function logout() {
    localStorage.removeItem("token");
    localStorage.removeItem("userId");
    localStorage.removeItem("role");
    localStorage.removeItem("email");

    window.location.href = "/login.html";
}

// ----------------------------------------
// FACULTY ACCESS CHECK
// ----------------------------------------

function checkFacultyAccess() {
    const token = getToken();

    const role = (localStorage.getItem("role") || "")
        .replace(/^ROLE_/i, "")
        .toUpperCase();

    if (!token) {
        window.location.href = "/login.html";
        return false;
    }

    if (role !== "FACULTY" && role !== "ADMIN") {
        alert("Access denied. Please log in with a faculty account.");
        window.location.href = "/login.html";
        return false;
    }

    return true;
}

// ----------------------------------------
// API GET
// ----------------------------------------

async function apiGet(endpoint) {
    const token = getToken();

    if (!token) {
        throw new Error(
            "Your login session has expired. Please log in again."
        );
    }

    const response = await fetch(`${API_BASE}${endpoint}`, {
        method: "GET",
        headers: {
            "Authorization": `Bearer ${token}`,
            "Accept": "application/json"
        }
    });

    if (response.status === 401) {
        throw new Error("Authentication failed. Please log in again.");
    }

    if (response.status === 403) {
        throw new Error(
            "Access denied. Check your account role and API permissions."
        );
    }

    if (!response.ok) {
        throw new Error(
            `Dashboard request failed with HTTP ${response.status}.`
        );
    }

    return response.json();
}

// ----------------------------------------
// UPDATE HTML ELEMENT SAFELY
// ----------------------------------------

function setValue(id, value) {
    const element = document.getElementById(id);

    if (!element) {
        return;
    }

    element.textContent = value ?? 0;
}

// ----------------------------------------
// ESCAPE HTML
// ----------------------------------------

function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>"']/g, character => {
        const entities = {
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            '"': "&quot;",
            "'": "&#039;"
        };

        return entities[character];
    });
}

// ----------------------------------------
// FORMAT PERCENTAGE
// ----------------------------------------

function formatPercentage(value) {
    const number = Number(value ?? 0);

    if (!Number.isFinite(number)) {
        return "0.00%";
    }

    return `${number.toFixed(2)}%`;
}

// ----------------------------------------
// LOAD FACULTY DASHBOARD
// ----------------------------------------

async function loadDashboard() {

    const message = document.getElementById("dashboardMessage");
    const refreshButton = document.getElementById("refreshDashboard");

    if (message) {
        message.textContent = "Loading dashboard statistics...";
    }

    if (refreshButton) {
        refreshButton.disabled = true;
        refreshButton.textContent = "Loading...";
    }

    const statisticIds = [
        "courseCount",
        "studentCount",
        "activeStudentCount",
        "inactiveStudentCount",
        "materialCount",
        "examCount",
        "pendingApprovalCount",
        "pendingCount",
        "totalResults",
        "passedResults",
        "failedResults",
        "averageScore",
        "passPercentage",
        "overviewPending"
    ];

    statisticIds.forEach(id => setValue(id, "—"));

    setValue("extensionBadge", "0");
    setValue("examApprovalBadge", "0");

    const progress = document.getElementById("passProgress");

    if (progress) {
        progress.style.width = "0%";
    }

    try {

        const data = await apiGet("/faculty/dashboard");

        if (!data || typeof data !== "object") {
            throw new Error(
                "The dashboard API returned an invalid response."
            );
        }

        // COURSES AND STUDENTS

        setValue("courseCount", data.totalCourses);
        setValue("studentCount", data.totalStudents);
        setValue("activeStudentCount", data.activeStudents);
        setValue("inactiveStudentCount", data.inactiveStudents);

        // MATERIALS AND EXAMS

        setValue("materialCount", data.totalMaterials);
        setValue("examCount", data.totalExams);

        // PENDING REQUESTS

        const pendingExtensions =
            Number(data.pendingExtensionRequests ?? 0);

        const pendingApprovals =
            Number(data.pendingExamApprovals ?? 0);

        setValue("pendingCount", pendingExtensions);
        setValue("extensionBadge", pendingExtensions);
        setValue("overviewPending", pendingExtensions);

        setValue("pendingApprovalCount", pendingApprovals);
        setValue("examApprovalBadge", pendingApprovals);

        // EXAM RESULTS

        setValue("totalResults", data.totalResults);
        setValue("passedResults", data.passedResults);
        setValue("failedResults", data.failedResults);

        setValue(
            "averageScore",
            formatPercentage(data.averageScore)
        );

        const passPercentage = Math.min(
            100,
            Math.max(0, Number(data.passPercentage ?? 0))
        );

        setValue(
            "passPercentage",
            formatPercentage(passPercentage)
        );

        if (progress) {
            progress.style.width = `${passPercentage}%`;
        }

        // EXTENSION APPROVED / REJECTED

        setValue("approvedCount", "Not available");
        setValue("rejectedCount", "Not available");
        setValue("overviewApproved", "Not available");
        setValue("overviewRejected", "Not available");

        if (message) {
            message.textContent =
                "Dashboard statistics loaded successfully.";
        }

    } catch (error) {

        console.error("Faculty dashboard error:", error);

        if (message) {
            message.textContent =
                error.message ||
                "Unable to load dashboard statistics.";
        }

    } finally {

        if (refreshButton) {
            refreshButton.disabled = false;
            refreshButton.textContent = "Refresh ↻";
        }
    }
}

// ----------------------------------------
// DISPLAY FACULTY PROFILE AND DATE
// ----------------------------------------

function displayFaculty() {

    const email = localStorage.getItem("email") || "Faculty";

    const firstLetter = email.charAt(0).toUpperCase() || "F";

    setValue("facultyName", email);
    setValue("avatar", firstLetter);
    setValue("topAvatar", firstLetter);

    loadFacultyIdentity();

    setValue(
        "currentDate",
        new Date().toLocaleDateString("en-IN", {
            day: "numeric",
            month: "short",
            year: "numeric"
        })
    );

    setValue("year", new Date().getFullYear());
}

// ----------------------------------------
// LOAD FACULTY NAME FROM THE DATABASE
// ----------------------------------------

async function loadFacultyIdentity() {

    const token = localStorage.getItem("token");
    const facultyId = localStorage.getItem("userId");

    if (!token || !facultyId) {
        return;
    }

    try {

        const response = await fetch(
            `${API_BASE}/auth/profile/${encodeURIComponent(facultyId)}`,
            {
                headers: {
                    Authorization: `Bearer ${token}`,
                    Accept: "application/json"
                }
            }
        );

        if (!response.ok) {
            return;
        }

        const result = await response.json();
        const profile = result.data || result;

        const fullName = `${profile.firstName || ""} ${profile.lastName || ""}`
            .replace(/\s+/g, " ")
            .trim();

        if (!fullName) {
            return;
        }

        const initial = fullName.charAt(0).toUpperCase();

        setValue("facultyWelcomeName", fullName);
        setValue("facultyName", fullName);
        setValue("avatar", initial);
        setValue("topAvatar", initial);

    } catch (error) {
        console.error("Faculty identity error:", error);
    }
}

// ----------------------------------------
// STUDENT TABLE
// ----------------------------------------

let allFacultyStudents = [];

async function loadFacultyStudents() {

    const tableBody = document.getElementById("studentTableBody");
    const countElement = document.getElementById("studentTableCount");
    const message = document.getElementById("studentTableMessage");
    const refreshButton = document.getElementById("refreshStudents");

    if (!tableBody) {
        return;
    }

    tableBody.innerHTML = `
        <tr>
            <td colspan="8">Loading student details...</td>
        </tr>
    `;

    if (message) {
        message.textContent = "";
    }

    if (refreshButton) {
        refreshButton.disabled = true;
    }

    try {

        const data = await apiGet("/faculty/dashboard/students");

        if (!Array.isArray(data)) {
            throw new Error(
                "Invalid student list returned by the server."
            );
        }

        allFacultyStudents = data;

        renderFacultyStudents();

        if (message) {
            message.textContent =
                "Student details loaded successfully.";
        }

    } catch (error) {

        console.error("Student list loading error:", error);

        tableBody.innerHTML = `
            <tr>
                <td colspan="8">
                    Unable to load student details.
                </td>
            </tr>
        `;

        if (countElement) {
            countElement.textContent = "Unable to load students";
        }

        if (message) {
            message.textContent = error.message;
        }

    } finally {

        if (refreshButton) {
            refreshButton.disabled = false;
        }
    }
}

// ----------------------------------------
// RENDER STUDENTS
// ----------------------------------------

function renderFacultyStudents() {

    const tableBody = document.getElementById("studentTableBody");
    const countElement = document.getElementById("studentTableCount");
    const searchInput = document.getElementById("studentSearch");

    if (!tableBody) {
        return;
    }

    const searchTerm = (searchInput?.value || "")
        .trim()
        .toLowerCase();

    const filteredStudents = allFacultyStudents.filter(student => {

        const searchableText = [
            student.studentName,
            student.email,
            student.courseName,
            student.status
        ].join(" ").toLowerCase();

        return searchableText.includes(searchTerm);
    });

    if (countElement) {
        countElement.textContent =
            `${filteredStudents.length} enrollment(s)`;
    }

    if (filteredStudents.length === 0) {

        tableBody.innerHTML = `
            <tr>
                <td colspan="8">
                    No matching student enrollments found.
                </td>
            </tr>
        `;

        return;
    }

    tableBody.innerHTML = filteredStudents.map(student => {

        const name = escapeHtml(student.studentName || "Student");
        const email = escapeHtml(student.email || "—");
        const course = escapeHtml(student.courseName || "—");
        const status = escapeHtml(student.status || "UNKNOWN");

        const percentage = Math.min(
            100,
            Math.max(0, Number(student.overallProgress ?? 0))
        );

        const enrollmentDate = formatDate(student.enrollmentDate);
        const dueDate = formatDate(student.dueDate);

        const statusClass = status.toUpperCase() === "ACTIVE"
            ? "student-status-active"
            : "student-status-inactive";

        const studentId = Number(student.studentId);
        const courseId = Number(student.courseId);

        const canDownload =
            Number.isInteger(studentId) && studentId > 0 &&
            Number.isInteger(courseId) && courseId > 0;

        return `
            <tr>

                <td>
                    <div class="student-name-cell">

                        <div class="student-avatar">
                            ${escapeHtml(name.charAt(0).toUpperCase())}
                        </div>

                        <strong>${name}</strong>

                    </div>
                </td>

                <td>${email}</td>

                <td>${course}</td>

                <td>
                    <div class="student-progress">

                        <div class="student-progress-track">

                            <div
                                class="student-progress-fill"
                                style="width: ${percentage}%">
                            </div>

                        </div>

                        <small>${percentage.toFixed(1)}%</small>

                    </div>
                </td>

                <td>
                    <span class="student-status ${statusClass}">
                        ${status}
                    </span>
                </td>

                <td>${enrollmentDate}</td>

                <td>${dueDate}</td>

                <td>
                    <button
                        type="button"
                        class="button button-primary student-report-btn"
                        data-student-id="${canDownload ? studentId : ""}"
                        data-course-id="${canDownload ? courseId : ""}"
                        ${canDownload ? "" : "disabled"}>

                        Download PDF

                    </button>
                </td>

            </tr>
        `;

    }).join("");
}

// ----------------------------------------
// FORMAT DATE
// ----------------------------------------

function formatDate(value) {

    if (!value) {
        return "—";
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return "—";
    }

    return date.toLocaleDateString("en-IN", {
        day: "2-digit",
        month: "short",
        year: "numeric"
    });
}

// ----------------------------------------
// DOWNLOAD STUDENT PROGRESS PDF
// ----------------------------------------

async function downloadStudentProgressPDF(
    studentId,
    courseId,
    button
) {

    const token = getToken();

    if (!token) {
        alert("Your session has expired. Please log in again.");
        return;
    }

    const originalText = button.textContent;

    button.disabled = true;
    button.textContent = "Generating...";

    try {

        const url =
            `/api/reports/student-progress/${studentId}` +
            `/course/${courseId}/pdf`;

        const response = await fetch(url, {
            method: "GET",
            headers: {
                "Authorization": `Bearer ${token}`,
                "Accept": "application/pdf"
            }
        });

        if (response.status === 401) {
            throw new Error(
                "Your session has expired. Please log in again."
            );
        }

        if (response.status === 403) {
            throw new Error(
                "You do not have permission to download this report."
            );
        }

        if (!response.ok) {
            throw new Error(
                `Unable to generate PDF. HTTP ${response.status}`
            );
        }

        const blob = await response.blob();

        if (blob.size === 0) {
            throw new Error("The generated PDF is empty.");
        }

        const objectUrl = URL.createObjectURL(blob);

        const link = document.createElement("a");

        link.href = objectUrl;

        link.download =
            `Student_${studentId}_Progress_Report.pdf`;

        document.body.appendChild(link);

        link.click();

        link.remove();

        setTimeout(() => {
            URL.revokeObjectURL(objectUrl);
        }, 1000);

    } catch (error) {

        console.error("PDF download error:", error);

        alert(error.message);

    } finally {

        button.disabled = false;
        button.textContent = originalText;

    }
}

// ----------------------------------------
// STUDENT TABLE EVENTS
// ----------------------------------------

document.getElementById("refreshStudents")
    ?.addEventListener("click", loadFacultyStudents);

document.getElementById("studentSearch")
    ?.addEventListener("input", renderFacultyStudents);

// ----------------------------------------
// PDF BUTTON EVENT DELEGATION
// ----------------------------------------

document.getElementById("studentTableBody")
    ?.addEventListener("click", function(event) {

        const button = event.target.closest(".student-report-btn");

        if (!button || button.disabled) {
            return;
        }

        const studentId = Number(button.dataset.studentId);
        const courseId = Number(button.dataset.courseId);

        if (!studentId || !courseId) {
            alert("Student ID or Course ID is missing.");
            return;
        }

        downloadStudentProgressPDF(
            studentId,
            courseId,
            button
        );
    });

// ----------------------------------------
// INITIALIZE DASHBOARD
// ----------------------------------------

document.addEventListener("DOMContentLoaded", function() {

    if (!checkFacultyAccess()) {
        return;
    }

    displayFaculty();

    loadDashboard();

    loadFacultyStudents();

    const refreshButton =
        document.getElementById("refreshDashboard");

    if (refreshButton) {
        refreshButton.addEventListener("click", loadDashboard);
    }

});
