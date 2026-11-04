"use strict";

const API_BASE = `${window.API_BASE_URL}/api/exam-approval`;

let allRequests = [];
let selectedRequest = null;
let toastTimer = null;


// ==========================================
// INITIALIZATION
// ==========================================

document.addEventListener("DOMContentLoaded", () => {

    initializePage();
    bindEvents();
    loadApprovalRequests();

});


function initializePage() {

    const dateElement = document.getElementById("currentDate");

    if (dateElement) {

        dateElement.textContent =
            new Date().toLocaleDateString("en-IN", {
                day: "2-digit",
                month: "short",
                year: "numeric"
            });

    }

    const yearElement = document.getElementById("currentYear");

    if (yearElement) {
        yearElement.textContent = new Date().getFullYear();
    }

    const user = JSON.parse(
        localStorage.getItem("user") || "null"
    );

    const email = localStorage.getItem("email") || "";

    const name =
        user?.name ||
        user?.fullName ||
        user?.username ||
        email ||
        "Faculty";

    const avatar = document.getElementById("topAvatar");

    if (avatar) {
        avatar.textContent = name.charAt(0).toUpperCase();
    }

}


// ==========================================
// EVENT LISTENERS
// ==========================================

function bindEvents() {

    document.getElementById("refreshButton")
        .addEventListener("click", loadApprovalRequests);

    document.getElementById("searchInput")
        .addEventListener("input", renderRequests);

    document.getElementById("statusFilter")
        .addEventListener("change", renderRequests);

    document.getElementById("sortFilter")
        .addEventListener("change", renderRequests);

    document.getElementById("clearFilters")
        .addEventListener("click", clearFilters);

    document.getElementById("closeDetails")
        .addEventListener("click", closeDetailsModal);

    document.getElementById("closeDetailsFooter")
        .addEventListener("click", closeDetailsModal);

    document.getElementById("modalApprove")
        .addEventListener("click", () => {

            if (selectedRequest) {
                approveRequest(selectedRequest.id);
            }

        });

    document.getElementById("modalReject")
        .addEventListener("click", () => {

            if (selectedRequest) {
                rejectRequest(selectedRequest.id);
            }

        });

    document.getElementById("detailsModal")
        .addEventListener("click", event => {

            if (event.target.id === "detailsModal") {
                closeDetailsModal();
            }

        });

    document.addEventListener("keydown", event => {

        if (event.key === "Escape") {
            closeDetailsModal();
        }

    });

}


// ==========================================
// AUTHENTICATION
// ==========================================

function getToken() {

    return localStorage.getItem("token");

}


// ==========================================
// API REQUEST
// ==========================================

async function apiRequest(endpoint, options = {}) {

    const token = getToken();

    if (!token) {

        window.location.href = "/login.html";

        throw new Error("Authentication required.");

    }

    const response = await fetch(
        `${API_BASE}${endpoint}`,
        {
            ...options,

            headers: {
                "Authorization": `Bearer ${token}`,
                "Accept": "application/json",
                ...options.headers
            }
        }
    );

    if (response.status === 401) {

        throw new Error("Session expired. Please log in again.");

    }

    if (response.status === 403) {

        throw new Error("You are not authorized to perform this action.");

    }

    const text = await response.text();

    let data = null;

    if (text) {

        try {
            data = JSON.parse(text);
        } catch {
            data = text;
        }

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


// ==========================================
// RESPONSE HANDLING
// ==========================================

function unwrapResponse(data) {

    if (Array.isArray(data)) {
        return data;
    }

    if (data?.data !== undefined) {
        return unwrapResponse(data.data);
    }

    if (data?.result !== undefined) {
        return unwrapResponse(data.result);
    }

    return data;

}


// ==========================================
// LOAD PENDING REQUESTS
// ==========================================

async function loadApprovalRequests() {

    const container =
        document.getElementById("requestsContainer");

    const refreshButton =
        document.getElementById("refreshButton");

    container.innerHTML = `
        <tr>
            <td colspan="5" class="loading-cell">
                Loading approval requests...
            </td>
        </tr>
    `;

    refreshButton.disabled = true;
    refreshButton.textContent = "Loading...";

    try {

        const response = await apiRequest("/pending");

        const requests = unwrapResponse(response);
        document.getElementById("totalRequests").textContent =
    requests.length;

document.getElementById("pendingRequests").textContent =
    requests.filter(r => r.status === "PENDING").length;

document.getElementById("approvedRequests").textContent =
    requests.filter(r => r.status === "APPROVED").length;

document.getElementById("rejectedRequests").textContent =
    requests.filter(r => r.status === "REJECTED").length;

        if (!Array.isArray(requests)) {

            throw new Error(
                "Invalid approval requests response from server."
            );

        }

        allRequests = requests;

        updateStatistics();

        renderRequests();

    } catch (error) {

        console.error("Approval requests error:", error);

        container.innerHTML = `
            <tr>
                <td colspan="5" class="empty-cell">
                    Unable to load approval requests.
                    ${escapeHtml(error.message)}
                </td>
            </tr>
        `;

        showToast(error.message, "error");

    } finally {

        refreshButton.disabled = false;
        refreshButton.textContent = "↻ Refresh";

    }

}


// ==========================================
// UPDATE STATISTICS
// ==========================================

function updateStatistics() {

    const pending = allRequests.filter(request =>
        String(request.status || "").toUpperCase() === "PENDING"
    ).length;

    document.getElementById("pendingCount").textContent =
        pending;

    document.getElementById("totalCount").textContent =
        allRequests.length;

    document.getElementById("pendingBadge").textContent =
        `${pending} Pending`;

}


// ==========================================
// GET REQUEST INFORMATION
// ==========================================

function getRequestInfo(request) {

    const student = request.student || {};
    const course = request.course || {};

    const studentName =
        `${student.firstName || ""} ${student.lastName || ""}`.trim() ||
        student.name ||
        request.studentName ||
        `Student #${request.studentId || student.id || "-"}`;

    const studentId =
        student.id ??
        request.studentId ??
        "-";

    const email =
        student.email ||
        request.studentEmail ||
        "Not available";

    const courseName =
        course.title ||
        course.name ||
        request.courseName ||
        `Course #${request.courseId || course.id || "-"}`;

    const courseId =
        course.id ??
        request.courseId ??
        "-";

    const requestedAt =
        request.requestedAt
            ? new Date(request.requestedAt)
            : null;

    return {
        id: request.id,
        studentName,
        studentId,
        email,
        courseName,
        courseId,
        requestedAt,
        status: String(request.status || "PENDING").toUpperCase()
    };

}


// ==========================================
// RENDER REQUEST TABLE
// ==========================================

function renderRequests() {

    const container =
        document.getElementById("requestsContainer");

    const search =
        document.getElementById("searchInput")
            .value.trim().toLowerCase();

    const status =
        document.getElementById("statusFilter").value;

    const sort =
        document.getElementById("sortFilter").value;

    let filtered = allRequests.filter(request => {

        const info = getRequestInfo(request);

        const searchableText = [
            info.studentName,
            info.studentId,
            info.email,
            info.courseName,
            info.courseId
        ].join(" ").toLowerCase();

        const matchesSearch =
            searchableText.includes(search);

        const matchesStatus =
            status === "ALL" ||
            info.status === status;

        return matchesSearch && matchesStatus;

    });


    filtered.sort((a, b) => {

        const dateA =
            getRequestInfo(a).requestedAt?.getTime() || 0;

        const dateB =
            getRequestInfo(b).requestedAt?.getTime() || 0;

        return sort === "NEWEST"
            ? dateB - dateA
            : dateA - dateB;

    });


    if (filtered.length === 0) {

        container.innerHTML = `
            <tr>
                <td colspan="5" class="empty-cell">
                    <strong>No requests found</strong>
                    <br><br>
                    No approval requests match your current search or filter.
                </td>
            </tr>
        `;

        document.getElementById("resultInfo").textContent =
            "No matching requests.";

        return;

    }


    container.innerHTML = filtered.map(request => {

        const info = getRequestInfo(request);

        const initial =
            info.studentName.charAt(0).toUpperCase();

        const date =
            info.requestedAt &&
            !isNaN(info.requestedAt.getTime())
                ? info.requestedAt.toLocaleString("en-IN", {
                    day: "2-digit",
                    month: "short",
                    year: "numeric",
                    hour: "2-digit",
                    minute: "2-digit"
                })
                : "Not available";

        const statusClass =
            info.status.toLowerCase();

        return `

            <tr>

                <td>

                    <div class="student-cell">

                        <div class="student-small-avatar">
                            ${escapeHtml(initial)}
                        </div>

                        <div class="student-details">

                            <strong>
                                ${escapeHtml(info.studentName)}
                            </strong>

                            <small>
                                ID: ${escapeHtml(info.studentId)}
                            </small>

                        </div>

                    </div>

                </td>


                <td>

                    <span class="course-name">
                        ${escapeHtml(info.courseName)}
                    </span>

                    <span class="course-id">
                        Course ID: ${escapeHtml(info.courseId)}
                    </span>

                </td>


                <td>
                    ${escapeHtml(date)}
                </td>


                <td>

                    <span class="status-pill ${escapeHtml(statusClass)}">
                        ${escapeHtml(info.status)}
                    </span>

                </td>


                <td>

                    <button
                        class="view-btn"
                        data-request-id="${escapeHtml(info.id)}"
                    >
                        Review →
                    </button>

                </td>

            </tr>

        `;

    }).join("");


    // Bind review buttons without inline JavaScript.
    container.querySelectorAll("[data-request-id]")
        .forEach(button => {

            button.addEventListener("click", () => {

                const requestId = button.dataset.requestId;

                openDetailsModal(requestId);

            });

        });


    document.getElementById("resultInfo").textContent =
        `Showing ${filtered.length} of ${allRequests.length} requests`;

}


// ==========================================
// REQUEST DETAILS MODAL
// ==========================================

function openDetailsModal(requestId) {

    const request = allRequests.find(item =>
        String(item.id) === String(requestId)
    );

    if (!request) {

        showToast("Request not found.", "error");
        return;

    }

    selectedRequest = request;

    const info = getRequestInfo(request);

    document.getElementById("modalStudentName").textContent =
        info.studentName;

    document.getElementById("modalStudentId").textContent =
        `Student ID: ${info.studentId}`;

    document.getElementById("modalAvatar").textContent =
        info.studentName.charAt(0).toUpperCase();

    document.getElementById("modalEmail").textContent =
        info.email;

    document.getElementById("modalCourse").textContent =
        info.courseName;

    document.getElementById("modalCourseId").textContent =
        info.courseId;

    document.getElementById("modalRequestedAt").textContent =
        info.requestedAt &&
        !isNaN(info.requestedAt.getTime())
            ? info.requestedAt.toLocaleString("en-IN")
            : "Not available";

    document.getElementById("modalStatus").textContent =
        info.status;

    document.getElementById("modalRequestId").textContent =
        info.id;

    const pending =
        info.status === "PENDING";

    document.getElementById("modalApprove").hidden =
        !pending;

    document.getElementById("modalReject").hidden =
        !pending;

    document.getElementById("detailsModal")
        .classList.add("show");

}


function closeDetailsModal() {

    document.getElementById("detailsModal")
        .classList.remove("show");

    selectedRequest = null;

}


// ==========================================
// APPROVE REQUEST
// ==========================================

async function approveRequest(requestId) {

    const confirmed = confirm(
        "Are you sure you want to approve this student's exam access request?"
    );

    if (!confirmed) {
        return;
    }

    const comment = prompt(
        "Enter approval comment (optional):"
    );

    if (comment === null) {
        return;
    }

    try {

        let url = `/${encodeURIComponent(requestId)}/approve`;

        if (comment.trim()) {

            url += `?comment=${encodeURIComponent(comment.trim())}`;

        }

        await apiRequest(url, {
            method: "PUT"
        });

        closeDetailsModal();

        showToast(
            "Exam approval request approved successfully.",
            "success"
        );

        await loadApprovalRequests();

    } catch (error) {

        console.error("Approval error:", error);

        showToast(error.message, "error");

    }

}


// ==========================================
// REJECT REQUEST
// ==========================================

async function rejectRequest(requestId) {

    const comment = prompt(
        "Please enter the reason for rejecting this request:"
    );

    if (comment === null) {
        return;
    }

    if (!comment.trim()) {

        showToast(
            "Please provide a rejection reason.",
            "error"
        );

        return;

    }

    const confirmed = confirm(
        "Are you sure you want to reject this exam approval request?"
    );

    if (!confirmed) {
        return;
    }

    try {

        const url =
            `/${encodeURIComponent(requestId)}/reject` +
            `?comment=${encodeURIComponent(comment.trim())}`;

        await apiRequest(url, {
            method: "PUT"
        });

        closeDetailsModal();

        showToast(
            "Exam approval request rejected successfully.",
            "success"
        );

        await loadApprovalRequests();

    } catch (error) {

        console.error("Rejection error:", error);

        showToast(error.message, "error");

    }

}


// ==========================================
// CLEAR FILTERS
// ==========================================

function clearFilters() {

    document.getElementById("searchInput").value = "";

    document.getElementById("statusFilter").value = "ALL";

    document.getElementById("sortFilter").value = "NEWEST";

    renderRequests();

}


// ==========================================
// TOAST NOTIFICATIONS
// ==========================================

function showToast(message, type = "success") {

    const toast = document.getElementById("toast");

    clearTimeout(toastTimer);

    toast.textContent = message;

    toast.className = `toast show ${type}`;

    toastTimer = setTimeout(() => {

        toast.classList.remove("show");

    }, 3500);

}


// ==========================================
// HTML ESCAPE
// ==========================================

function escapeHtml(value) {

    return String(value ?? "").replace(/[&<>"']/g, character => {

        const entities = {
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            '"': "&quot;",
            "'": "&#39;"
        };

        return entities[character];

    });

}