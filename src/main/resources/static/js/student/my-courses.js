// =====================================================
// STUDENT MY COURSES MODULE
// Six-step path per course:
// 1 Study materials  2 Coding challenges  3 Coding approval
// 4 Request exam approval  5 Approval status  6 Take exam
// =====================================================

const API_BASE = `${window.API_BASE_URL}`;
const EXTENSION_API = `${API_BASE}/api/extensions`;

let selectedExtensionEnrollment = null;
const extensionCoursesByEnrollment = new Map();

const CHECK_ICON = `<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12.5l4.5 4.5L19 7.5"/></svg>`;


// =====================================================
// COURSE ID RESOLVER (permanent fix)
// Always returns the REAL course ID, never the enrollment ID.
// The backend must send `courseId` (or a nested `course.id`).
// We deliberately do NOT fall back to `course.id`, because in
// an enrollment row that is the enrollment/row ID (this was
// the cause of /api/materials/course/1 instead of /course/2).
// =====================================================

function resolveCourseId(course) {
    const id = course?.courseId ?? course?.course?.id;
    return id !== undefined && id !== null && id !== "" ? Number(id) : null;
}


// =====================================================
// INITIALIZE PAGE
// =====================================================

document.addEventListener("DOMContentLoaded", () => {

    const form = document.getElementById("extensionForm");
    const cancelButton = document.getElementById("cancelExtensionBtn");
    const closeButton = document.getElementById("closeExtensionModalBtn");
    const logoutLink = document.getElementById("logoutLink");
    const modal = document.getElementById("extensionModal");

    if (form) form.addEventListener("submit", submitExtensionRequest);
    if (cancelButton) cancelButton.addEventListener("click", closeExtensionModal);
    if (closeButton) closeButton.addEventListener("click", closeExtensionModal);

    if (logoutLink) {
        logoutLink.addEventListener("click", () => {
            localStorage.removeItem("token");
            localStorage.removeItem("userId");
            localStorage.removeItem("role");
            localStorage.removeItem("courseId");
            localStorage.removeItem("examId");
        });
    }

    if (modal) {
        modal.addEventListener("click", event => {
            if (event.target === modal) closeExtensionModal();
        });
    }

    document.addEventListener("keydown", event => {
        if (event.key === "Escape" && modal && modal.classList.contains("show")) {
            closeExtensionModal();
        }
    });

    loadStudentExtensionHistory();

});


// =====================================================
// LOAD STUDENT COURSES
// =====================================================

async function loadCourses() {

    try {

        const studentId = localStorage.getItem("userId");
        const token = localStorage.getItem("token");

        if (!studentId || !token) {
            alert("Your session has expired. Please log in again.");
            window.location.href = "../login.html";
            return;
        }

        const response = await fetch(
            `${API_BASE}/api/student/courses/${encodeURIComponent(studentId)}`,
            {
                method: "GET",
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Accept": "application/json"
                }
            }
        );

        if (!response.ok) {
            throw new Error(`Failed to load courses. HTTP ${response.status}`);
        }

        const courses = await response.json();

        const container = document.getElementById("coursesContainer");

        if (!container) {
            console.error("coursesContainer not found.");
            return;
        }

        if (!Array.isArray(courses) || courses.length === 0) {
            container.innerHTML = `<div class="courses-empty">You are not enrolled in any courses yet.</div>`;
            return;
        }

        const validCourses = courses.filter(course => {
            const ok = resolveCourseId(course) != null;
            if (!ok) {
                console.error("Course ID missing (backend must send courseId):", course);
            }
            return ok;
        });

        if (validCourses.length === 0) {
            container.innerHTML = `<div class="courses-empty">Course data is incomplete. Please contact support.</div>`;
            return;
        }

        // Fetch eligibility and approval for all courses in parallel.
        const cards = await Promise.all(validCourses.map(async course => {

            const courseId = resolveCourseId(course);

            if (course.enrollmentId != null) {
                extensionCoursesByEnrollment.set(
                    Number(course.enrollmentId),
                    course.courseTitle ?? course.courseName ?? "Course"
                );
            }

            const [eligibility, approvalStatus] = await Promise.all([
                getExamEligibility(studentId, courseId, token),
                getApprovalStatus(studentId, courseId, token)
            ]);

            return buildCourseCard(course, courseId, eligibility, approvalStatus, studentId, token);

        }));

        container.innerHTML = "";
        cards.forEach(card => container.appendChild(card));

    } catch (error) {

        console.error("Load Courses Error:", error);
        showPageMessage("Unable to load courses. Please refresh the page.");

    }

}


// =====================================================
// BUILD ONE COURSE CARD
// =====================================================

function buildCourseCard(course, courseId, eligibility, approvalStatus, studentId, token) {

    const enrollmentId = course.enrollmentId;
    const title = course.courseTitle ?? course.courseName ?? "Course";

    const progress = Math.min(100, Math.max(0, Number(course.progress ?? 0)));

    const dueDate = course.dueDate ? new Date(course.dueDate) : null;
    const deadlineExpired =
        dueDate !== null &&
        !Number.isNaN(dueDate.getTime()) &&
        Date.now() >= dueDate.getTime();

    const card = document.createElement("article");
    card.className = "course-card";

    card.innerHTML = `
        <div class="course-head">
            <div>
                <h3 class="course-title">${escapeHTML(title)}</h3>
                <ul class="course-meta">
                    <li><strong>Duration:</strong> ${escapeHTML(course.durationDays ?? "-")} days</li>
                    <li><strong>Status:</strong> ${escapeHTML(course.status ?? "-")}</li>
                    <li>
                        <strong>Due:</strong>
                        <span class="${deadlineExpired ? "overdue" : ""}">
                            ${course.dueDate ? escapeHTML(formatExtensionDateTime(course.dueDate)) : "-"}
                            ${deadlineExpired ? "(expired)" : ""}
                        </span>
                    </li>
                </ul>
            </div>
            ${enrollmentId != null && deadlineExpired
                ? `<button type="button" class="extension-btn">Request extension</button>`
                : ""}
        </div>

        <div class="course-progress">
            <div class="course-progress-row">
                <span>Overall progress</span>
                <strong>${progress.toFixed(1)}%</strong>
            </div>
            <div class="progress-bar"
                 role="progressbar" aria-valuemin="0" aria-valuemax="100"
                 aria-valuenow="${progress.toFixed(0)}">
                <div class="progress" style="width:${progress}%"></div>
            </div>
        </div>

        <ol class="steps" aria-label="Exam path for ${escapeHTML(title)}">
            ${renderSteps(eligibility, approvalStatus)}
        </ol>
    `;

    // Actions
    const on = (selector, handler) => {
        const el = card.querySelector(selector);
        if (el && !el.disabled) el.addEventListener("click", handler);
    };

    on('[data-action="materials"]', () => openMaterials(courseId));
    on('[data-action="coding"]', () => openCodingChallenges(courseId));
    on('[data-action="request"]', () => requestExamApproval(studentId, courseId, token));
    on('[data-action="exam"]', () => startExam(courseId, studentId, token));
    on(".extension-btn", () => openExtensionModal(course));

    return card;

}


// =====================================================
// STEP PATH
// =====================================================

function renderSteps(eligibility, approvalStatus) {

    const e = eligibility || {};
    const eligible = e.eligibleForExamApproval === true;

    const materialsDone = e.materialsCompleted === true;
    const codingApproved = e.codingCompleted === true;

    // Coding is "completed" once all challenges are submitted, or once approved.
    const submitted = e.submittedCodingChallenges;
    const codingDone =
        codingApproved ||
        (submitted != null && e.totalCodingChallenges > 0 && submitted >= e.totalCodingChallenges);

    const requested = ["PENDING", "APPROVED"].includes(approvalStatus);
    const approved = approvalStatus === "APPROVED";
    const rejected = approvalStatus === "REJECTED";

    const mDone = e.completedMaterials ?? 0, mTotal = e.totalMaterials ?? 0;
    const cDone = e.completedCodingChallenges ?? 0, cTotal = e.totalCodingChallenges ?? 0;

    // done flags, in order
    const done = [materialsDone, codingDone, codingApproved, requested, approved, false];

    // The first step that is not done is the current one.
    const currentIndex = done.findIndex(flag => !flag);

    const defs = [
        {
            title: "Study materials",
            desc: materialsDone ? "All materials completed." : `${mDone}/${mTotal} completed.`,
            action: `<button type="button" class="step-btn ${materialsDone ? "secondary" : ""}" data-action="materials">
                        ${materialsDone ? "Review materials" : "Open materials"}</button>`
        },
        {
            title: "Coding challenges",
            desc: codingDone ? "All challenges completed." : (submitted != null
                ? `${submitted}/${cTotal} submitted.` : "Complete every challenge."),
            action: `<button type="button" class="step-btn ${codingDone ? "secondary" : ""}" data-action="coding">
                        ${codingDone ? "Review challenges" : "Open challenges"}</button>`
        },
        {
            title: "Coding approval",
            desc: codingApproved ? "Faculty approved all challenges." : `${cDone}/${cTotal} approved by faculty.`,
            action: codingApproved
                ? `<span class="pill pill-approved">Approved</span>`
                : codingDone
                    ? `<span class="pill pill-pending">Awaiting review</span>`
                    : `<span class="pill pill-wait">Not approved</span>`
        },
        {
            title: "Request approval",
            desc: requested ? "Request sent to faculty."
                : rejected ? "Your last request was rejected."
                : eligible ? "You can request now." : "Finish steps 1 to 3 first.",
            action: `<button type="button" class="step-btn" data-action="request" ${!eligible || requested ? "disabled" : ""}>
                        ${requested ? "Requested" : rejected ? "Request again" : "Request approval"}</button>`
        },
        {
            title: "Approval status",
            desc: approved ? "Faculty approved your exam."
                : approvalStatus === "PENDING" ? "Waiting for faculty review."
                : rejected ? "Check your progress, then request again."
                : "No request sent yet.",
            action: approved ? `<span class="pill pill-approved">Approved</span>`
                : approvalStatus === "PENDING" ? `<span class="pill pill-pending">Pending</span>`
                : rejected ? `<span class="pill pill-rejected">Rejected</span>`
                : `<span class="pill pill-wait">Not requested</span>`
        },
        {
            title: "Take exam",
            desc: approved ? "You are cleared to start." : "Unlocks after approval.",
            action: `<button type="button" class="step-btn success" data-action="exam" ${approved ? "" : "disabled"}>
                        Take exam</button>`
        }
    ];

    return defs.map((step, i) => {

        let state = "is-locked";
        if (done[i]) state = "is-done";
        else if (i === currentIndex) state = "is-current";

        // Step 5 shows the red state when the request was rejected.
        if (i === 4 && rejected) state = "is-error";

        const marker = state === "is-done" ? CHECK_ICON : i + 1;
        const current = state === "is-current" ? ' aria-current="step"' : "";

        return `
            <li class="step ${state}"${current}>
                <span class="step-marker">${marker}</span>
                <span class="step-title">${step.title}</span>
                <p class="step-desc">${escapeHTML(step.desc)}</p>
                <div class="step-action">${step.action}</div>
            </li>
        `;

    }).join("");

}


// =====================================================
// API RESPONSE HELPERS
// =====================================================

async function parseApiResponse(response) {

    const responseText = await response.text();

    if (!responseText) return null;

    try {
        return JSON.parse(responseText);
    } catch {
        return responseText;
    }

}

function extractErrorMessage(result, status) {

    if (typeof result === "string") return result;

    if (result && typeof result === "object") {
        return (
            result.message ||
            result.error ||
            result.data?.message ||
            `Request failed. HTTP ${status}`
        );
    }

    return `Request failed. HTTP ${status}`;

}

async function extensionApiRequest(url, options = {}) {

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

    const result = await parseApiResponse(response);

    if (!response.ok) {
        throw new Error(extractErrorMessage(result, response.status));
    }

    return result;

}


// =====================================================
// EXTENSION HISTORY
// =====================================================

async function loadStudentExtensionHistory() {

    const studentId = localStorage.getItem("userId");
    const tbody = document.getElementById("extensionHistoryBody");
    const message = document.getElementById("extensionHistoryMessage");

    if (!studentId || !tbody) return;

    tbody.innerHTML = `<tr><td colspan="5">Loading extension history...</td></tr>`;

    try {

        const result = await extensionApiRequest(
            `${EXTENSION_API}/student/${encodeURIComponent(studentId)}`
        );

        const requests = Array.isArray(result)
            ? result
            : Array.isArray(result?.data)
                ? result.data
                : Array.isArray(result?.data?.content)
                    ? result.data.content
                    : [];

        renderExtensionHistory(requests);

        if (message) message.textContent = "";

    } catch (error) {

        console.error("Extension History Error:", error);

        tbody.innerHTML = `<tr><td colspan="5">Unable to load extension history.</td></tr>`;

        if (message) message.textContent = error.message;

    }

}

function renderExtensionHistory(requests) {

    const tbody = document.getElementById("extensionHistoryBody");

    if (!tbody) return;

    if (!requests.length) {
        tbody.innerHTML = `<tr><td colspan="5">You have not submitted any extension requests.</td></tr>`;
        return;
    }

    tbody.innerHTML = requests.map(request => {

        const status = String(request.status || "PENDING").toUpperCase();

        const statusClass = {
            PENDING: "status-pending",
            APPROVED: "status-approved",
            REJECTED: "status-rejected"
        }[status] || "";

        const enrollmentId = request.enrollmentId;

        const courseName =
            request.courseTitle ||
            request.courseName ||
            extensionCoursesByEnrollment.get(Number(enrollmentId)) ||
            `Enrollment #${enrollmentId ?? "-"}`;

        return `
            <tr>
                <td>${escapeHTML(courseName)}</td>
                <td>${escapeHTML(request.daysRequested ?? "-")}</td>
                <td>${escapeHTML(request.reason || "-")}</td>
                <td><span class="extension-status ${statusClass}">${escapeHTML(status)}</span></td>
                <td>${escapeHTML(request.rejectionReason || "-")}</td>
            </tr>
        `;

    }).join("");

}


// =====================================================
// EXTENSION MODAL
// =====================================================

function openExtensionModal(course) {

    const modal = document.getElementById("extensionModal");

    if (!modal) return;

    if (!course.enrollmentId) {
        alert("Enrollment ID is missing. Please refresh the page.");
        return;
    }

    const dueDate = course.dueDate ? new Date(course.dueDate) : null;

    if (!dueDate || Number.isNaN(dueDate.getTime()) || Date.now() < dueDate.getTime()) {
        alert("You can request an extension only after the course deadline expires.");
        return;
    }

    selectedExtensionEnrollment = Number(course.enrollmentId);

    document.getElementById("extensionEnrollmentId").value = String(course.enrollmentId);
    document.getElementById("extensionCourseName").textContent =
        course.courseTitle || course.courseName || "Course";
    document.getElementById("extensionDueDate").textContent =
        `Due date: ${formatExtensionDateTime(course.dueDate)}`;

    document.getElementById("daysRequested").value = "";
    document.getElementById("extensionReason").value = "";
    document.getElementById("extensionFormMessage").textContent = "";
    document.getElementById("submitExtensionBtn").disabled = false;

    modal.classList.add("show");
    modal.setAttribute("aria-hidden", "false");
    document.body.classList.add("modal-open");

}

function closeExtensionModal() {

    const modal = document.getElementById("extensionModal");

    if (modal) {
        modal.classList.remove("show");
        modal.setAttribute("aria-hidden", "true");
    }

    document.body.classList.remove("modal-open");
    selectedExtensionEnrollment = null;

}

async function submitExtensionRequest(event) {

    event.preventDefault();

    const enrollmentId = selectedExtensionEnrollment;
    const daysRequested = Number(document.getElementById("daysRequested").value);
    const reason = document.getElementById("extensionReason").value.trim();
    const message = document.getElementById("extensionFormMessage");
    const submitButton = document.getElementById("submitExtensionBtn");

    if (!enrollmentId) {
        message.textContent = "Please select a course.";
        return;
    }

    if (!Number.isInteger(daysRequested) || daysRequested < 1 || daysRequested > 90) {
        message.textContent = "Enter a number of days between 1 and 90.";
        return;
    }

    if (!reason) {
        message.textContent = "Please enter a reason for your request.";
        return;
    }

    if (!confirm(`Submit a request for ${daysRequested} additional days?`)) return;

    submitButton.disabled = true;
    message.textContent = "Submitting request...";

    try {

        const params = new URLSearchParams({
            enrollmentId: String(enrollmentId),
            daysRequested: String(daysRequested),
            reason: reason
        });

        const result = await extensionApiRequest(
            `${EXTENSION_API}/request?${params.toString()}`,
            { method: "POST" }
        );

        alert(
            result?.message ||
            result?.data?.message ||
            "Extension request submitted successfully."
        );

        closeExtensionModal();

        await loadStudentExtensionHistory();
        await loadCourses();

    } catch (error) {

        console.error("Submit Extension Request Error:", error);
        message.textContent = error.message;
        alert(error.message);

    } finally {

        submitButton.disabled = false;

    }

}


// =====================================================
// ELIGIBILITY + APPROVAL APIs
// =====================================================

async function getExamEligibility(studentId, courseId, token) {

    try {

        const response = await fetch(
            `${API_BASE}/api/exam-eligibility/student/${encodeURIComponent(studentId)}/course/${encodeURIComponent(courseId)}`,
            {
                method: "GET",
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Accept": "application/json"
                }
            }
        );

        const result = await parseApiResponse(response);

        if (!response.ok) {
            console.error("Eligibility API failed:", response.status, result);
            return null;
        }

        return result;

    } catch (error) {

        console.error("Eligibility Error:", error);
        return null;

    }

}

// GET /api/exam-approval/course/{courseId}
async function getApprovalStatus(studentId, courseId, token) {

    try {

        const response = await fetch(
            `${API_BASE}/api/exam-approval/course/${encodeURIComponent(courseId)}`,
            {
                method: "GET",
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Accept": "application/json, text/plain"
                }
            }
        );

        const result = await parseApiResponse(response);

        if (!response.ok) {
            console.error("Approval Status API failed:", response.status, result);
            return "NOT_REQUESTED";
        }

        const status = String(result ?? "NOT_REQUESTED")
            .trim()
            .replace(/^"|"$/g, "")
            .toUpperCase();

        return status || "NOT_REQUESTED";

    } catch (error) {

        console.error("Approval Status Error:", error);
        return "NOT_REQUESTED";

    }

}


// =====================================================
// NAVIGATION
// The course ID is passed in the URL (source of truth) and
// also saved to localStorage as a fallback. Destination pages
// must read the URL first (see getSelectedCourseId below).
// =====================================================

function openMaterials(courseId) {

    if (courseId == null) {
        alert("Course ID is missing. Please refresh the page.");
        return;
    }

    localStorage.setItem("courseId", String(courseId));
    window.location.href = `study-material.html?courseId=${encodeURIComponent(courseId)}`;

}

function openCodingChallenges(courseId) {

    if (courseId == null) {
        alert("Course ID is missing. Please refresh the page.");
        return;
    }

    localStorage.setItem("courseId", String(courseId));
    window.location.href = `coding-challenges.html?courseId=${encodeURIComponent(courseId)}`;

}


// =====================================================
// REQUEST FACULTY EXAM APPROVAL
// POST /api/exam-approval/request?courseId={courseId}
// =====================================================

async function requestExamApproval(studentId, courseId, token) {

    if (!courseId || !token) {
        alert("Course ID or authentication token is missing.");
        return;
    }

    const eligibility = await getExamEligibility(studentId, courseId, token);

    if (!eligibility?.eligibleForExamApproval) {
        alert("Complete 100% of study materials and get 100% of coding challenges approved by faculty first.");
        await loadCourses();
        return;
    }

    if (!confirm(
        "You have completed all study materials and received faculty approval for all coding challenges. Submit your exam approval request?"
    )) {
        return;
    }

    try {

        const response = await fetch(
            `${API_BASE}/api/exam-approval/request?courseId=${encodeURIComponent(courseId)}`,
            {
                method: "POST",
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Accept": "application/json"
                }
            }
        );

        const result = await parseApiResponse(response);

        if (!response.ok) {
            throw new Error(extractErrorMessage(result, response.status));
        }

        alert(
            result?.message ||
            result?.data?.message ||
            "Exam approval request submitted successfully."
        );

        await loadCourses();

    } catch (error) {

        console.error("Request Approval Error:", error);
        alert(error.message);

    }

}


// =====================================================
// START EXAM
// =====================================================

async function startExam(courseId, studentId, token) {

    try {

        const approvalStatus = await getApprovalStatus(studentId, courseId, token);

        if (approvalStatus !== "APPROVED") {
            alert("Your exam must be approved by faculty first.");
            return;
        }

        const response = await fetch(
            `${API_BASE}/api/exams/course/${encodeURIComponent(courseId)}/published`,
            {
                method: "GET",
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Accept": "application/json"
                }
            }
        );

        const result = await parseApiResponse(response);

        if (!response.ok) {
            throw new Error(extractErrorMessage(result, response.status));
        }

        const exams = Array.isArray(result) ? result : result?.data;

        if (!Array.isArray(exams) || exams.length === 0) {
            alert("No published exam found for this course.");
            return;
        }

        const examId = exams[0].id;

        if (!examId) {
            alert("Exam ID was not returned by the server.");
            return;
        }

        localStorage.setItem("examId", String(examId));
        localStorage.setItem("courseId", String(courseId));

        window.location.href =
            `exam-portal.html?examId=${encodeURIComponent(examId)}&courseId=${encodeURIComponent(courseId)}`;

    } catch (error) {

        console.error("Start Exam Error:", error);
        alert(error.message || "Unable to start the exam.");

    }

}


// =====================================================
// HELPERS
// =====================================================

// Copy this into study-material.js, coding-challenges.js and
// exam-portal.js (or a shared utils file). URL first, then localStorage.
function getSelectedCourseId() {

    const fromUrl = new URLSearchParams(window.location.search).get("courseId");
    const id = fromUrl || localStorage.getItem("courseId");

    if (!id) {
        alert("No course selected. Please open the course from My Courses.");
        window.location.href = "my-courses.html";
        return null;
    }

    return id;

}

function formatExtensionDateTime(value) {

    if (!value) return "Not available";

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) return String(value);

    return date.toLocaleString();

}

function showPageMessage(message) {

    const pageMessage = document.getElementById("pageMessage");

    if (pageMessage) {
        pageMessage.textContent = message;
    } else {
        console.error(message);
    }

}

function escapeHTML(value) {

    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");

}


// =====================================================
// LOAD COURSES WHEN PAGE OPENS
// =====================================================

loadCourses();