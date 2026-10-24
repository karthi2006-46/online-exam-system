let courseProgressChart = null;

/* =========================================================
   1. LOAD STUDENT DASHBOARD
========================================================= */
const API_BASE_URL = window.API_BASE_URL;


async function loadDashboard() {
    const token = localStorage.getItem("token");
    const studentId = localStorage.getItem("userId");

    if (!token || !studentId) {
        console.error("Missing authentication token or student ID.");
        return;
    }

    try {
        const response = await fetch(
            `${API_BASE_URL}/api/student/dashboard?studentId=${encodeURIComponent(studentId)}`,
            {
                method: "GET",
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Accept": "application/json"
                }
            }
        );

        if (!response.ok) {
            throw new Error(`Dashboard request failed: ${response.status}`);
        }

        const data = await response.json();

        // Update dashboard cards
        document.getElementById("totalCourses").textContent =
            data.totalCourses ?? 0;

        document.getElementById("completedCourses").textContent =
            data.completedCourses ?? 0;

        document.getElementById("pendingExams").textContent =
            data.pendingExams ?? 0;

        const overallProgress = Math.max(
            0,
            Math.min(100, Number(data.overallProgress) || 0)
        );

        document.getElementById("overallProgress").textContent =
            `${overallProgress.toFixed(1)}%`;

        // Update doughnut chart
        renderOverallChart(overallProgress);

        // Update individual course progress
        renderCourseProgress(
            Array.isArray(data.courses) ? data.courses : []
        );

        // Update percentage displayed below the chart
        const progressElement =
            document.getElementById("progressPercentage");

        if (progressElement) {
            progressElement.textContent =
                `${overallProgress.toFixed(1)}% Completed`;
        }

    } catch (error) {
        console.error("Dashboard Error:", error);

        const progressElement =
            document.getElementById("progressPercentage");

        if (progressElement) {
            progressElement.textContent =
                "Unable to load progress.";
        }
    }
}


/* =========================================================
   2. RENDER OVERALL PROGRESS DOUGHNUT CHART
========================================================= */

function renderOverallChart(progress) {
    const canvas = document.getElementById("courseProgressChart");

    if (!canvas) {
        console.error("Course progress chart canvas not found.");
        return;
    }

    if (typeof Chart === "undefined") {
        console.error("Chart.js is not loaded.");
        return;
    }

    progress = Math.max(
        0,
        Math.min(100, Number(progress) || 0)
    );

    // Destroy the previous chart before creating another
    if (courseProgressChart) {
        courseProgressChart.destroy();
        courseProgressChart = null;
    }

    courseProgressChart = new Chart(canvas, {
        type: "doughnut",

        data: {
            labels: [
                "Completed",
                "Remaining"
            ],

            datasets: [{
                data: [
                    progress,
                    100 - progress
                ],

                backgroundColor: [
                    "#2563eb",
                    "#e5e7eb"
                ],

                borderWidth: 0,
                hoverOffset: 5
            }]
        },

        options: {
            responsive: true,
            maintainAspectRatio: true,
            cutout: "72%",

            plugins: {
                legend: {
                    position: "bottom"
                },

                tooltip: {
                    callbacks: {
                        label: function (context) {
                            return `${context.label}: ${Number(context.raw).toFixed(1)}%`;
                        }
                    }
                }
            }
        }
    });
}


/* =========================================================
   3. DISPLAY INDIVIDUAL COURSE PROGRESS
========================================================= */

function renderCourseProgress(courses) {
    const container = document.getElementById("courseProgressList");

    if (!container) {
        console.error("Course progress container not found.");
        return;
    }

    container.replaceChildren();

    if (!Array.isArray(courses) || courses.length === 0) {
        container.textContent = "No enrolled courses found.";
        return;
    }

    courses.forEach(course => {
        const card = document.createElement("section");

        card.className = "course-progress-item";

        // Course title
        const title = document.createElement("h3");

        title.textContent =
            course.courseTitle ?? "Untitled course";

        card.appendChild(title);

        // Course duration progress
        addProgressRow(
            card,
            "Course duration",
            course.durationProgress,
            `${course.elapsedDays ?? 0}/${course.courseDurationDays ?? 0} days`
        );

        // Study material progress
        addProgressRow(
            card,
            "Study materials",
            course.materialsProgress,
            `${course.completedMaterials ?? 0}/${course.totalMaterials ?? 0}`
        );

        // Coding challenge progress
        addProgressRow(
            card,
            "Coding challenges",
            course.challengesProgress,
            `${course.completedChallenges ?? 0}/${course.totalChallenges ?? 0}`
        );

        // Exam progress
        addProgressRow(
            card,
            "Exams",
            course.examsProgress,
            `${course.completedExams ?? 0}/${course.totalExams ?? 0}`
        );

        // Overall course progress
        addProgressRow(
            card,
            "Overall progress",
            course.overallProgress,
            `${Number(course.overallProgress ?? 0).toFixed(1)}%`
        );

        // Course status
        const status = document.createElement("p");

        status.textContent =
            `Status: ${course.status ?? "ACTIVE"}`;

        card.appendChild(status);

        container.appendChild(card);
    });
}


/* =========================================================
   4. CREATE A PROGRESS ROW
========================================================= */

function addProgressRow(parent, label, value, detail) {
    const percentage = Math.max(
        0,
        Math.min(100, Number(value) || 0)
    );

    const row = document.createElement("div");

    row.className = "progress-row";

    // Label
    const labelElement = document.createElement("span");

    labelElement.textContent = label;

    // Percentage and details
    const valueElement = document.createElement("span");

    valueElement.textContent =
        `${percentage.toFixed(1)}% (${detail})`;

    row.append(labelElement, valueElement);

    // Progress bar background
    const track = document.createElement("div");

    track.className = "progress-track";

    // Progress bar fill
    const fill = document.createElement("div");

    fill.className = "progress-fill";

    fill.style.width = `${percentage}%`;

    track.appendChild(fill);

    parent.append(row, track);
}


/* =========================================================
   5. LOAD STUDENT EXAM RESULTS
========================================================= */

async function loadResults() {
    const studentId = localStorage.getItem("userId");
    const token = localStorage.getItem("token");

    const tableBody =
        document.getElementById("resultTableBody");

    if (!tableBody) {
        console.error("Exam results table not found.");
        return;
    }

    if (!studentId || !token) {
        showTableMessage(
            tableBody,
            4,
            "Please log in to view exam results."
        );

        return;
    }

    try {
        const response = await fetch(
            `${API_BASE_URL}/api/results/student/${encodeURIComponent(studentId)}`,
            {
                method: "GET",

                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Accept": "application/json"
                }
            }
        );

        if (!response.ok) {
            throw new Error(
                `Results request failed: ${response.status}`
            );
        }

        const result = await response.json();

        // Support direct arrays and wrapped API responses
        const results = Array.isArray(result)
            ? result
            : result.data ?? result.payload ?? [];

        tableBody.replaceChildren();

        if (!Array.isArray(results) || results.length === 0) {
            showTableMessage(
                tableBody,
                4,
                "No results available."
            );

            return;
        }

        results.forEach(item => {
            const row = tableBody.insertRow();

            const examTitle =
                item.examTitle ?? "Untitled exam";

            const obtainedMarks =
                item.obtainedMarks ?? 0;

            const totalMarks =
                item.totalMarks ?? 0;

            const percentage =
                Number(item.percentage ?? 0);

            const status =
                item.status ?? "N/A";

            const values = [
                examTitle,
                `${obtainedMarks}/${totalMarks}`,
                `${percentage.toFixed(1)}%`,
                status
            ];

            values.forEach(value => {
                const cell = row.insertCell();

                cell.textContent = value;
            });
        });

    } catch (error) {
        console.error("Results Error:", error);

        showTableMessage(
            tableBody,
            4,
            "Unable to load exam results."
        );
    }
}


/* =========================================================
   6. LOAD ENROLLED COURSES TABLE
   FIXES: "Loading courses..." NEVER DISAPPEARING
========================================================= */

async function loadStudentCourses() {
    const studentId = localStorage.getItem("userId");
    const token = localStorage.getItem("token");

    const tableBody =
        document.getElementById("courseTableBody");

    if (!tableBody) {
        console.error("Student courses table not found.");
        return;
    }

    if (!studentId || !token) {
        showTableMessage(
            tableBody,
            3,
            "Please log in to view your courses."
        );

        return;
    }

    // Display loading message while fetching data
    showTableMessage(
        tableBody,
        3,
        "Loading courses..."
    );

    try {
        const response = await fetch(
            `${API_BASE_URL}/api/student/courses/${encodeURIComponent(studentId)}`,
            {
                method: "GET",

                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Accept": "application/json"
                }
            }
        );

        if (!response.ok) {
            throw new Error(
                `Courses request failed: ${response.status}`
            );
        }

        const courses = await response.json();

        tableBody.replaceChildren();

        if (!Array.isArray(courses) || courses.length === 0) {
            showTableMessage(
                tableBody,
                3,
                "No enrolled courses found."
            );

            return;
        }

        courses.forEach(course => {
            const row = tableBody.insertRow();

            // Column 1: Course name
            const titleCell = row.insertCell();

            titleCell.textContent =
                course.courseTitle ?? "Untitled course";

            // Column 2: Completion percentage and progress bar
            const progressCell = row.insertCell();

            const progress = Math.max(
                0,
                Math.min(100, Number(course.progress) || 0)
            );

            const progressText = document.createElement("span");

            progressText.textContent =
                `${progress.toFixed(1)}%`;

            const progressTrack =
                document.createElement("div");

            progressTrack.className = "progress-track";

            const progressFill =
                document.createElement("div");

            progressFill.className = "progress-fill";

            progressFill.style.width =
                `${progress}%`;

            progressTrack.appendChild(progressFill);

            progressCell.append(
                progressText,
                progressTrack
            );

            // Column 3: Enrollment status
            const statusCell = row.insertCell();

            statusCell.textContent =
                course.status ?? "ACTIVE";
        });

    } catch (error) {
        console.error("Student Courses Error:", error);

        showTableMessage(
            tableBody,
            3,
            "Unable to load courses. Please refresh the page."
        );
    }
}


/* =========================================================
   7. REUSABLE TABLE MESSAGE
========================================================= */

function showTableMessage(tableBody, columnCount, message) {
    if (!tableBody) {
        return;
    }

    tableBody.replaceChildren();

    const row = tableBody.insertRow();

    const cell = row.insertCell();

    cell.colSpan = columnCount;

    cell.textContent = message;
}


/* =========================================================
   8. INITIALIZE DASHBOARD
========================================================= */

document.addEventListener("DOMContentLoaded", () => {
    loadDashboard();
    loadResults();
    loadStudentCourses();
});