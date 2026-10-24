// ============================================
// STUDENT EXAM PORTAL
// ============================================

const API_BASE_URL = `${window.API_BASE_URL}/api`;

const examContainer = document.getElementById("examsContainer");
const examMessage = document.getElementById("examMessage");

const totalExamsElement = document.getElementById("totalExams");
const availableExamsElement = document.getElementById("availableExams");

const refreshButton = document.getElementById("refreshExamsBtn");


// ============================================
// API RESPONSE HELPER
// ============================================

async function fetchJson(url, options = {}) {
    const token = localStorage.getItem("token");

    if (!token) {
        throw new Error("Your session has expired. Please log in again.");
    }

    const response = await fetch(url, {
        ...options,

        headers: {
            "Authorization": `Bearer ${token}`,
            "Accept": "application/json",
            ...(options.headers || {})
        }
    });

    const responseText = await response.text();

    let result = {};

    if (responseText) {
        try {
            result = JSON.parse(responseText);
        } catch {
            throw new Error("The server returned an invalid response.");
        }
    }

    if (!response.ok) {
        throw new Error(
            result.message ||
            `Request failed with status ${response.status}.`
        );
    }

    return result;
}


// ============================================
// EXTRACT DATA FROM API RESPONSE
// ============================================

function extractData(result) {
    if (Array.isArray(result)) {
        return result;
    }

    if (result && Array.isArray(result.data)) {
        return result.data;
    }

    return [];
}


// ============================================
// GET COURSE IDS
// ============================================

async function getCourseIds() {

    // Use the selected course if available.
    const selectedCourseId = localStorage.getItem("courseId");

    if (selectedCourseId) {
        return [selectedCourseId];
    }

    // Otherwise, load the student's enrolled courses.
    const studentId = localStorage.getItem("userId");

    if (!studentId) {
        throw new Error(
            "Student information was not found. Please log in again."
        );
    }

    const result = await fetchJson(
        `${API_BASE_URL}/enrollments/student/${encodeURIComponent(studentId)}`
    );

    const enrollments = extractData(result);

    const courseIds = enrollments
        .map(enrollment => {
            if (enrollment.courseId != null) {
                return enrollment.courseId;
            }

            if (enrollment.course && enrollment.course.id != null) {
                return enrollment.course.id;
            }

            return null;
        })
        .filter(id => id != null);

    return [...new Set(courseIds.map(String))];
}


// ============================================
// CHECK EXISTING EXAM ATTEMPT
// ============================================

async function checkExamStatus(examId) {

    const studentId = localStorage.getItem("userId");

    if (!studentId) {
        throw new Error("Student ID not found. Please log in again.");
    }

    const result = await fetchJson(
        `${API_BASE_URL}/exam-attempts/student/` +
        `${encodeURIComponent(studentId)}/exam/` +
        `${encodeURIComponent(examId)}`
    );

    return result.data || null;
}


// ============================================
// LOAD PUBLISHED EXAMS
// ============================================

async function loadPublishedExams() {

    examContainer.innerHTML = "";

    examMessage.className = "message";
    examMessage.textContent = "Loading available examinations...";

    refreshButton.disabled = true;

    try {

        const courseIds = await getCourseIds();

        if (!courseIds.length) {

            totalExamsElement.textContent = "0";
            availableExamsElement.textContent = "0";

            examMessage.textContent =
                "No enrolled courses were found. Enroll in a course to view its examinations.";

            return;
        }

        const examRequests = courseIds.map(async courseId => {

            const result = await fetchJson(
                `${API_BASE_URL}/exams/course/${encodeURIComponent(courseId)}/published`
            );

            const exams = extractData(result);

            return exams.map(exam => ({
                ...exam,

                courseId:
                    exam.courseId ??
                    exam.course?.id ??
                    courseId,

                courseName:
                    exam.courseName ??
                    exam.course?.title ??
                    exam.course?.name ??
                    ""
            }));
        });

        const examResults = await Promise.all(examRequests);

        // Remove duplicate exams.
        const examMap = new Map();

        examResults.flat().forEach(exam => {
            if (exam.id != null) {
                examMap.set(String(exam.id), exam);
            }
        });

        const exams = [...examMap.values()];

        totalExamsElement.textContent = exams.length;
        availableExamsElement.textContent = exams.length;

        if (!exams.length) {

            examMessage.textContent = "";

            examContainer.innerHTML = `
                <div class="empty-state">
                    <div class="empty-icon">▣</div>
                    <h3>No Published Exams Yet</h3>
                    <p>
                        There are currently no published examinations
                        for your enrolled courses. Please check again later.
                    </p>
                </div>
            `;

            return;
        }

        examMessage.textContent = "";

        exams.forEach((exam, index) => {
            const card = createExamCard(exam, index);
            examContainer.appendChild(card);
        });

    } catch (error) {

        console.error("Load Published Exams Error:", error);

        totalExamsElement.textContent = "—";
        availableExamsElement.textContent = "—";

        examMessage.className = "message error";

        examMessage.textContent =
            error.message ||
            "Unable to load examinations. Please try again.";

    } finally {

        refreshButton.disabled = false;
    }
}


// ============================================
// CREATE EXAM CARD
// ============================================

function createExamCard(exam, index) {

    const card = document.createElement("article");
    card.className = "exam-card";

    card.style.animationDelay =
        `${Math.min(index * 60, 300)}ms`;

    // Header
    const header = document.createElement("div");
    header.className = "exam-card-header";

    const icon = document.createElement("div");
    icon.className = "exam-type-icon";
    icon.textContent = "▣";
    icon.setAttribute("aria-hidden", "true");

    const badge = document.createElement("span");
    badge.className = "published-badge";
    badge.textContent = "● Published";

    header.append(icon, badge);

    // Title
    const title = document.createElement("h3");
    title.textContent = exam.title || "Untitled Examination";

    // Description
    const description = document.createElement("p");
    description.className = "exam-description";

    description.textContent =
        exam.description ||
        "Complete this examination to assess your understanding of the course.";

    // Exam metadata
    const meta = document.createElement("div");
    meta.className = "exam-meta";

    const durationItem = document.createElement("div");
    durationItem.className = "exam-meta-item";

    const durationLabel = document.createElement("span");
    durationLabel.textContent = "EXAM DURATION";

    const durationValue = document.createElement("strong");

    const duration = Number(exam.durationMinutes);

    durationValue.textContent =
        Number.isFinite(duration) && duration > 0
            ? `${duration} minutes`
            : "Timed examination";

    durationItem.append(durationLabel, durationValue);

    const courseItem = document.createElement("div");
    courseItem.className = "exam-meta-item";

    const courseLabel = document.createElement("span");
    courseLabel.textContent = "COURSE";

    const courseValue = document.createElement("strong");
    courseValue.textContent = exam.courseName || "Enrolled Course";

    courseItem.append(courseLabel, courseValue);

    meta.append(durationItem, courseItem);

    // Footer
    const footer = document.createElement("div");
    footer.className = "exam-card-footer";

    const footerText = document.createElement("p");
    footerText.textContent = "Checking exam status...";

    const actionButton = document.createElement("button");
    actionButton.type = "button";
    actionButton.className = "start-exam-btn";
    actionButton.textContent = "Please wait...";
    actionButton.disabled = true;

    footer.append(footerText, actionButton);

    card.append(header, title, description, meta, footer);

    // Check the existing attempt before allowing an action.
    checkExamStatus(exam.id)
        .then(attempt => {

            if (!card.isConnected) {
                return;
            }

            // ========================================
            // COMPLETED EXAM
            // ========================================

            if (
                attempt &&
                (
                    attempt.status === "SUBMITTED" ||
                    attempt.status === "AUTO_SUBMITTED"
                )
            ) {

                badge.textContent = "✓ Completed";
                badge.classList.add("completed-badge");

                footerText.textContent =
                    "You have already completed this exam.";

                actionButton.disabled = false;
                actionButton.textContent = "View Result";

                actionButton.addEventListener("click", () => {
                    viewExamResult(attempt.id);
                });

                return;
            }

            // ========================================
            // EXAM IN PROGRESS
            // ========================================

            if (
                attempt &&
                attempt.status === "IN_PROGRESS"
            ) {

                badge.textContent = "● In Progress";

                footerText.textContent =
                    "You have an exam attempt in progress.";

                actionButton.disabled = false;
                actionButton.textContent = "Resume Exam →";

                actionButton.addEventListener("click", () => {
                    startSelectedExam(exam);
                });

                return;
            }

            // ========================================
            // NOT YET ATTEMPTED
            // ========================================

            footerText.textContent = "Ready when you are.";

            actionButton.disabled = false;
            actionButton.textContent = "Start Exam →";

            actionButton.addEventListener("click", () => {
                startSelectedExam(exam);
            });

        })
        .catch(error => {

            console.error(
                `Unable to check status for exam ${exam.id}:`,
                error
            );

            if (!card.isConnected) {
                return;
            }

            footerText.textContent =
                "Unable to check exam status.";

            actionButton.disabled = false;
            actionButton.textContent = "Retry";

            actionButton.addEventListener("click", () => {
                loadPublishedExams();
            });
        });

    return card;
}


// ============================================
// VIEW RESULT
// ============================================

function viewExamResult(attemptId) {

    if (!attemptId) {
        alert("Unable to find your exam result.");
        return;
    }

    window.location.href =
        `result.html?attemptId=${encodeURIComponent(attemptId)}`;
}


// ============================================
// START OR RESUME THE SELECTED EXAM
// ============================================

function startSelectedExam(exam) {

    if (!exam.id) {
        alert("This examination does not have a valid exam ID.");
        return;
    }

    if (exam.courseId != null) {
        localStorage.setItem("courseId", String(exam.courseId));
    }

    localStorage.setItem("selectedExamId", String(exam.id));

    window.location.href =
        `exam.html?examId=${encodeURIComponent(exam.id)}`;
}


// ============================================
// REFRESH BUTTON
// ============================================

refreshButton.addEventListener("click", loadPublishedExams);


// ============================================
// PAGE INITIALIZATION
// ============================================

document.addEventListener("DOMContentLoaded", loadPublishedExams);