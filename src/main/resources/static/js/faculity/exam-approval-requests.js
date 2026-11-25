"use strict";

const API_BASE = `${window.API_BASE_URL}/api`;

const EXAM_API = `${API_BASE}/exam-approval`;
const CODING_API = `${API_BASE}/coding-submissions`;

let allRequests = [];
let codingSubmissions = [];
let selectedCodingSubmission = null;
let examHistory = [];
let codingHistory = [];

// ========================================
// INITIALIZATION
// ========================================

document.addEventListener("DOMContentLoaded", () => {

    bindEvents();

    loadApprovalRequests();
    loadCodingSubmissions();
    loadExamHistory();
    loadCodingHistory();

});

// ========================================
// AUTHENTICATION AND API
// ========================================

function getToken() {
    return localStorage.getItem("token");
}

async function apiRequest(url, options = {}) {

    const token = getToken();

    if (!token) {
        window.location.href = "/login.html";
        throw new Error("Authentication required.");
    }

    const headers = {
        Authorization: `Bearer ${token}`,
        Accept: "application/json",
        ...(options.body ? {
            "Content-Type": "application/json"
        } : {}),
        ...options.headers
    };

    const response = await fetch(url, {
        ...options,
        headers
    });

    const text = await response.text();

    let data = null;

    if (text) {
        try {
            data = JSON.parse(text);
        } catch {
            data = text;
        }
    }

    if (response.status === 401) {
        throw new Error("Session expired. Please log in again.");
    }

    if (response.status === 403) {
        throw new Error("You are not authorized to perform this action.");
    }

    if (!response.ok) {

        const message =
            typeof data === "object"
                ? data?.message || data?.error
                : data;

        throw new Error(
            message || `Request failed: ${response.status}`
        );
    }

    return data;
}

function unwrapResponse(data) {

    if (Array.isArray(data)) return data;

    if (data?.data !== undefined) {
        return unwrapResponse(data.data);
    }

    if (data?.result !== undefined) {
        return unwrapResponse(data.result);
    }

    return data;
}

// ========================================
// EVENT LISTENERS
// ========================================

function bindEvents() {

    document.getElementById("refreshButton")
        .addEventListener("click", refreshAll);

    document.querySelectorAll(".review-tab")
        .forEach(tab => {

            tab.addEventListener("click", () => {

                const target = tab.dataset.tab;

                document.querySelectorAll(".review-tab")
                    .forEach(item => item.classList.remove("active"));

                document.querySelectorAll(".review-panel")
                    .forEach(panel => panel.classList.remove("active"));

                tab.classList.add("active");

                document.getElementById(target)
                    .classList.add("active");

            });

        });

    document.getElementById("closeCodingModal")
        .addEventListener("click", closeCodingModal);

    document.getElementById("codingModal")
        .addEventListener("click", event => {

            if (event.target.id === "codingModal") {
                closeCodingModal();
            }

        });

    document.getElementById("approveCoding")
        .addEventListener("click", () => {

            if (selectedCodingSubmission) {
                reviewCodingSubmission("approve");
            }

        });

    document.getElementById("rejectCoding")
        .addEventListener("click", () => {

            if (selectedCodingSubmission) {
                reviewCodingSubmission("reject");
            }

        });

    document.addEventListener("keydown", event => {

        if (event.key === "Escape") {
            closeCodingModal();
        }

    });

}

async function refreshAll() {

    const button = document.getElementById("refreshButton");

    button.disabled = true;
    button.textContent = "Loading...";

    try {
        await Promise.all([
            loadApprovalRequests(),
            loadCodingSubmissions(),
            loadExamHistory(),
            loadCodingHistory()
        ]);
    } finally {
        button.disabled = false;
        button.textContent = "↻ Refresh";
    }

}

// ========================================
// EXAM APPROVAL REQUESTS
// ========================================

async function loadApprovalRequests() {

    const container =
        document.getElementById("requestsContainer");

    container.innerHTML =
        `<p class="loading">Loading exam requests...</p>`;

    try {

        const response = await apiRequest(
            `${EXAM_API}/pending`
        );

        const requests = unwrapResponse(response);

        if (!Array.isArray(requests)) {
            throw new Error("Invalid exam request response.");
        }

        allRequests = requests;

        updateExamStatistics();

        renderExamRequests();

    } catch (error) {

        console.error("Exam requests error:", error);

        container.innerHTML = `
            <div class="empty-state">
                Unable to load exam requests.
                <br>
                ${escapeHtml(error.message)}
            </div>
        `;

        showToast(error.message, "error");

    }

}

// ========================================
// EXAM STATISTICS
// ========================================

function updateExamStatistics() {

    const pending = allRequests.filter(
        item => String(item.status).toUpperCase() === "PENDING"
    ).length;

    document.getElementById("totalRequests").textContent =
        examHistory.length || allRequests.length;

    document.getElementById("pendingRequests").textContent =
        pending;

}

// ========================================
// EXAM APPROVAL HISTORY
// ========================================

async function loadExamHistory() {

    const container =
        document.getElementById("examHistoryContainer");

    if (!container) return;

    try {

        const response = await apiRequest(
            `${EXAM_API}/faculty/history`
        );

        const requests = unwrapResponse(response);

        examHistory = Array.isArray(requests) ? requests : [];

        renderExamHistory();

        updateExamStatistics();

    } catch (error) {

        console.error("Exam history error:", error);

        container.innerHTML = `
            <div class="empty-state">
                Unable to load exam approval history.
                <br>
                ${escapeHtml(error.message)}
            </div>
        `;

    }

}

function renderExamHistory() {

    const container =
        document.getElementById("examHistoryContainer");

    if (!container) return;

    if (!examHistory.length) {

        container.innerHTML = `
            <div class="empty-state">
                <h3>No exam approval history</h3>
                <p>Reviewed exam requests will appear here.</p>
            </div>
        `;

        return;
    }

    container.innerHTML = examHistory.map(request => {

        const status =
            String(request.status || "PENDING").toUpperCase();

        const requested = request.requestedAt
            ? new Date(request.requestedAt).toLocaleString("en-IN")
            : "—";

        const reviewed = request.reviewedAt
            ? new Date(request.reviewedAt).toLocaleString("en-IN")
            : "—";

        return `
            <article class="review-card">

                <h3>${escapeHtml(request.studentName || request.studentEmail || "Student")}</h3>

                <div class="review-meta">

                    <span>
                        <strong>Course:</strong>
                        ${escapeHtml(request.courseTitle || request.courseCode || "-")}
                    </span>

                    <span>
                        <strong>Requested:</strong>
                        ${escapeHtml(requested)}
                    </span>

                    <span>
                        <strong>Reviewed by:</strong>
                        ${escapeHtml(request.reviewedByName || "—")}
                    </span>

                    <span>
                        <strong>Reviewed on:</strong>
                        ${escapeHtml(reviewed)}
                    </span>

                    ${
                        request.comment
                            ? `<span>
                                   <strong>Comment:</strong>
                                   ${escapeHtml(request.comment)}
                               </span>`
                            : ""
                    }

                </div>

                <span class="review-status ${status.toLowerCase()}">
                    ${escapeHtml(status)}
                </span>

            </article>
        `;

    }).join("");

}

// ========================================
// RENDER EXAM REQUESTS
// ========================================

function renderExamRequests() {

    const container =
        document.getElementById("requestsContainer");

    if (!allRequests.length) {

        container.innerHTML = `
            <div class="empty-state">
                <h3>No Exam Requests</h3>
                <p>There are currently no exam approval requests.</p>
            </div>
        `;

        return;
    }

    container.innerHTML = allRequests.map(request => {

        const student = request.student || {};
        const course = request.course || {};

        const studentName =
            `${student.firstName || ""} ${student.lastName || ""}`.trim()
            || student.name
            || request.studentName
            || `Student #${student.id || request.studentId || "-"}`;

        const courseName =
            course.title
            || course.name
            || request.courseName
            || `Course #${course.id || request.courseId || "-"}`;

        const status =
            String(request.status || "PENDING").toUpperCase();

        const date = request.requestedAt
            ? new Date(request.requestedAt).toLocaleString("en-IN")
            : "Not available";

        return `
            <article class="review-card">

                <h3>${escapeHtml(studentName)}</h3>

                <div class="review-meta">

                    <span>
                        <strong>Student ID:</strong>
                        ${escapeHtml(student.id || request.studentId || "-")}
                    </span>

                    <span>
                        <strong>Email:</strong>
                        ${escapeHtml(student.email || request.studentEmail || "-")}
                    </span>

                    <span>
                        <strong>Course:</strong>
                        ${escapeHtml(courseName)}
                    </span>

                    <span>
                        <strong>Requested:</strong>
                        ${escapeHtml(date)}
                    </span>

                </div>

                <span class="review-status ${status.toLowerCase()}">
                    ${escapeHtml(status)}
                </span>

                ${
                    status === "PENDING"
                        ? `
                            <div class="review-actions">

                                <button
                                    class="btn-approve"
                                    data-exam-approve="${escapeHtml(request.id)}">
                                    Approve
                                </button>

                                <button
                                    class="btn-reject"
                                    data-exam-reject="${escapeHtml(request.id)}">
                                    Reject
                                </button>

                            </div>
                        `
                        : ""
                }

            </article>
        `;

    }).join("");

    container.querySelectorAll("[data-exam-approve]")
        .forEach(button => {

            button.addEventListener("click", () => {
                approveExamRequest(button.dataset.examApprove);
            });

        });

    container.querySelectorAll("[data-exam-reject]")
        .forEach(button => {

            button.addEventListener("click", () => {
                rejectExamRequest(button.dataset.examReject);
            });

        });

}

// ========================================
// APPROVE EXAM REQUEST
// ========================================

async function approveExamRequest(requestId) {

    if (!confirm("Approve this student's exam request?")) return;

    const comment = prompt("Enter approval comment (optional):");

    if (comment === null) return;

    try {

        const url =
            `${EXAM_API}/${encodeURIComponent(requestId)}/approve` +
            `?comment=${encodeURIComponent(comment.trim())}`;

        await apiRequest(url, {
            method: "PUT"
        });

        showToast("Exam request approved successfully.", "success");

        await loadApprovalRequests();
        await loadExamHistory();

    } catch (error) {

        console.error(error);

        showToast(error.message, "error");

    }

}

// ========================================
// REJECT EXAM REQUEST
// ========================================

async function rejectExamRequest(requestId) {

    const comment = prompt(
        "Enter the reason for rejecting this exam request:"
    );

    if (comment === null) return;

    if (!comment.trim()) {

        showToast("Rejection reason is required.", "error");

        return;
    }

    if (!confirm("Reject this student's exam request?")) return;

    try {

        const url =
            `${EXAM_API}/${encodeURIComponent(requestId)}/reject` +
            `?comment=${encodeURIComponent(comment.trim())}`;

        await apiRequest(url, {
            method: "PUT"
        });

        showToast("Exam request rejected.", "success");

        await loadApprovalRequests();
        await loadExamHistory();

    } catch (error) {

        console.error(error);

        showToast(error.message, "error");

    }

}

// ========================================
// CODING SUBMISSION REVIEWS
// ========================================

async function loadCodingSubmissions() {

    const container =
        document.getElementById("codingContainer");

    container.innerHTML =
        `<p class="loading">Loading coding submissions...</p>`;

    try {

        const response = await apiRequest(
            `${CODING_API}/faculty/pending`
        );

        const submissions = unwrapResponse(response);

        if (!Array.isArray(submissions)) {
            throw new Error("Invalid coding submissions response.");
        }

        codingSubmissions = submissions;

        updateCodingStatistics();

        renderCodingSubmissions();

    } catch (error) {

        console.error("Coding submissions error:", error);

        container.innerHTML = `
            <div class="empty-state">
                Unable to load coding submissions.
                <br>
                ${escapeHtml(error.message)}
            </div>
        `;

        showToast(error.message, "error");

    }

}

// ========================================
// CODING STATISTICS
// ========================================

function updateCodingStatistics() {

    document.getElementById("pendingCoding").textContent =
        codingSubmissions.filter(
            item => item.status === "PENDING_REVIEW"
        ).length;

    document.getElementById("approvedCoding").textContent =
        codingHistory.filter(
            item => String(item.status).toUpperCase() === "APPROVED"
        ).length;

}

// ========================================
// CODING SUBMISSION HISTORY
// ========================================

async function loadCodingHistory() {

    const container =
        document.getElementById("codingHistoryContainer");

    if (!container) return;

    try {

        const response = await apiRequest(
            `${CODING_API}/faculty/history`
        );

        const submissions = unwrapResponse(response);

        codingHistory = Array.isArray(submissions) ? submissions : [];

        renderCodingHistory();

        updateCodingStatistics();

    } catch (error) {

        console.error("Coding history error:", error);

        container.innerHTML = `
            <div class="empty-state">
                Unable to load coding submission history.
                <br>
                ${escapeHtml(error.message)}
            </div>
        `;

    }

}

function renderCodingHistory() {

    const container =
        document.getElementById("codingHistoryContainer");

    if (!container) return;

    if (!codingHistory.length) {

        container.innerHTML = `
            <div class="empty-state">
                <h3>No coding submission history</h3>
                <p>Student coding submissions will appear here.</p>
            </div>
        `;

        return;
    }

    container.innerHTML = codingHistory.map(submission => {

        const status =
            String(submission.reviewStatus || submission.status || "PENDING_REVIEW")
                .toUpperCase();

        const submitted = submission.submittedAt
            ? new Date(submission.submittedAt).toLocaleString("en-IN")
            : "—";

        const reviewed = submission.reviewedAt
            ? new Date(submission.reviewedAt).toLocaleString("en-IN")
            : "—";

        const passed = Number(submission.testCasesPassed || 0);
        const total = Number(submission.totalTestCases || 0);
        const percentage = Number(submission.percentage || 0);

        return `
            <article class="review-card">

                <h3>${escapeHtml(submission.studentName || submission.studentEmail || "Student")}</h3>

                <div class="review-meta">

                    <span>
                        <strong>Course:</strong>
                        ${escapeHtml(submission.courseTitle || "-")}
                    </span>

                    <span>
                        <strong>Challenge:</strong>
                        ${escapeHtml(submission.challengeTitle || "-")}
                    </span>

                    <span>
                        <strong>Submitted:</strong>
                        ${escapeHtml(submitted)}
                    </span>

                    <span>
                        <strong>Test cases passed:</strong>
                        ${escapeHtml(`${passed} / ${total}`)}
                    </span>

                    <span>
                        <strong>Percentage:</strong>
                        ${escapeHtml(`${percentage.toFixed(2)}%`)}
                    </span>

                    <span>
                        <strong>Reviewed by:</strong>
                        ${escapeHtml(submission.reviewedByName || "—")}
                    </span>

                    <span>
                        <strong>Reviewed on:</strong>
                        ${escapeHtml(reviewed)}
                    </span>

                    ${
                        submission.facultyFeedback
                            ? `<span>
                                   <strong>Feedback:</strong>
                                   ${escapeHtml(submission.facultyFeedback)}
                               </span>`
                            : ""
                    }

                </div>

                <span class="review-status ${status === "APPROVED" ? "" : status.toLowerCase()}">
                    ${escapeHtml(status.replace("_", " "))}
                </span>

            </article>
        `;

    }).join("");

}

// ========================================
// RENDER CODING SUBMISSIONS
// ========================================

function renderCodingSubmissions() {

    const container =
        document.getElementById("codingContainer");

    if (!codingSubmissions.length) {

        container.innerHTML = `
            <div class="empty-state">
                <h3>No Pending Coding Submissions</h3>
                <p>All available coding submissions have been reviewed.</p>
            </div>
        `;

        return;
    }

    container.innerHTML = codingSubmissions.map(submission => {

        const student = submission.student || {};
        const challenge = submission.challenge || {};
        const course = challenge.course || {};

        const studentName =
            `${student.firstName || ""} ${student.lastName || ""}`.trim()
            || student.name
            || `Student #${student.id || "-"}`;

        const challengeName =
            challenge.title
            || challenge.name
            || `Challenge #${challenge.id || "-"}`;

        const courseName =
            course.title
            || course.name
            || "Course";

        const date = submission.submittedAt
            ? new Date(submission.submittedAt).toLocaleString("en-IN")
            : "Not available";

        return `
            <article class="review-card">

                <h3>${escapeHtml(studentName)}</h3>

                <div class="review-meta">

                    <span>
                        <strong>Student ID:</strong>
                        ${escapeHtml(student.id || "-")}
                    </span>

                    <span>
                        <strong>Challenge:</strong>
                        ${escapeHtml(challengeName)}
                    </span>

                    <span>
                        <strong>Course:</strong>
                        ${escapeHtml(courseName)}
                    </span>

                    <span>
                        <strong>Language:</strong>
                        ${escapeHtml(submission.language || "-")}
                    </span>

                    <span>
                        <strong>Submitted:</strong>
                        ${escapeHtml(date)}
                    </span>

                </div>

                <span class="review-status">
                    PENDING REVIEW
                </span>

                <div class="review-actions">

                    <button
                        class="btn-review"
                        data-review-submission="${escapeHtml(submission.id)}">
                        Review Code →
                    </button>

                </div>

            </article>
        `;

    }).join("");

    container.querySelectorAll("[data-review-submission]")
        .forEach(button => {

            button.addEventListener("click", () => {

                openCodingReview(
                    button.dataset.reviewSubmission
                );

            });

        });

}

// ========================================
// OPEN CODING REVIEW
// ========================================

async function openCodingReview(submissionId) {

    try {

        const response = await apiRequest(
            `${CODING_API}/faculty/${encodeURIComponent(submissionId)}`
        );

        const submission = unwrapResponse(response);

        if (!submission || typeof submission !== "object") {
            throw new Error("Invalid submission details.");
        }

        selectedCodingSubmission = submission;

        const student = submission.student || {};
        const challenge = submission.challenge || {};
        const course = challenge.course || {};

        const studentName =
            `${student.firstName || ""} ${student.lastName || ""}`.trim()
            || student.name
            || `Student #${student.id || "-"}`;

        const details = document.getElementById("codingDetails");

        details.innerHTML = `

            <div class="review-meta">

                <span>
                    <strong>Student:</strong>
                    ${escapeHtml(studentName)}
                </span>

                <span>
                    <strong>Student ID:</strong>
                    ${escapeHtml(student.id || "-")}
                </span>

                <span>
                    <strong>Course:</strong>
                    ${escapeHtml(course.title || course.name || "-")}
                </span>

                <span>
                    <strong>Challenge:</strong>
                    ${escapeHtml(challenge.title || challenge.name || "-")}
                </span>

                <span>
                    <strong>Language:</strong>
                    ${escapeHtml(submission.language || "-")}
                </span>

                <span>
                    <strong>Submitted:</strong>
                    ${escapeHtml(
                        submission.submittedAt
                            ? new Date(submission.submittedAt).toLocaleString("en-IN")
                            : "-"
                    )}
                </span>

            </div>

            <h3>Student Source Code</h3>

            <pre class="code-preview">${escapeHtml(
                submission.sourceCode || "No source code available."
            )}</pre>

        `;

        document.getElementById("codingFeedback").value = "";

        const isPending =
            submission.status === "PENDING_REVIEW";

        document.getElementById("approveCoding").hidden =
            !isPending;

        document.getElementById("rejectCoding").hidden =
            !isPending;

        document.getElementById("codingFeedback").disabled =
            !isPending;

        document.getElementById("codingModal")
            .classList.add("show");

    } catch (error) {

        console.error("Coding review error:", error);

        showToast(error.message, "error");

    }

}

// ========================================
// APPROVE / REJECT CODING SUBMISSION
// ========================================

async function reviewCodingSubmission(action) {

    if (!selectedCodingSubmission) return;

    const feedback =
        document.getElementById("codingFeedback").value.trim();

    if (action === "reject" && !feedback) {
        showToast("Please provide a rejection reason.", "error");
        return;
    }

    const message = action === "approve"
        ? "Approve this coding submission?"
        : "Reject this coding submission?";

    if (!confirm(message)) return;

    const submissionId = selectedCodingSubmission.id;

    try {

        const url =
            `${CODING_API}/faculty/${encodeURIComponent(submissionId)}/${action}` +
            `?feedback=${encodeURIComponent(feedback)}`;

        await apiRequest(url, {
            method: "PUT"
        });

        closeCodingModal();

        showToast(
            action === "approve"
                ? "Coding submission approved successfully."
                : "Coding submission rejected. Student can submit again.",
            "success"
        );

        await loadCodingSubmissions();
        await loadApprovalRequests();
        await loadCodingHistory();
        await loadExamHistory();

    } catch (error) {

        console.error("Coding review action error:", error);

        showToast(error.message, "error");
    }
}

// ========================================
// CLOSE CODING MODAL
// ========================================

function closeCodingModal() {

    document.getElementById("codingModal")
        .classList.remove("show");

    selectedCodingSubmission = null;

}

// ========================================
// TOAST
// ========================================

function showToast(message, type = "success") {

    const toast = document.getElementById("toast");

    toast.textContent = message;

    toast.className = `toast show ${type}`;

    setTimeout(() => {
        toast.classList.remove("show");
    }, 3500);

}

// ========================================
// HTML ESCAPE
// ========================================

function escapeHtml(value) {

    return String(value ?? "").replace(
        /[&<>"']/g,
        character => ({
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            '"': "&quot;",
            "'": "&#39;"
        })[character]
    );

}