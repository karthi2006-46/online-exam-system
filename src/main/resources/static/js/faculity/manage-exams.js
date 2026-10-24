
const API_BASE = "/api";

let assignedCourses = [];
let exams = [];

function getToken() {
    return localStorage.getItem("token");
}

function logout() {
    ["token", "userId", "role", "email"].forEach(key => {
        localStorage.removeItem(key);
    });

    window.location.href = "/login.html";
}

function checkFacultyAccess() {
    const role = (localStorage.getItem("role") || "")
        .replace(/^ROLE_/i, "")
        .toUpperCase();

    if (!getToken()) {
        window.location.href = "/login.html";
        return false;
    }

    if (!["FACULTY", "ADMIN"].includes(role)) {
        alert("Access denied. Please log in with an authorized account.");
        window.location.href = "/login.html";
        return false;
    }

    return true;
}

async function apiRequest(endpoint, options = {}) {
    const response = await fetch(`${API_BASE}${endpoint}`, {
        ...options,
        headers: {
            Authorization: `Bearer ${getToken()}`,
            ...(options.body instanceof FormData
                ? {}
                : { "Content-Type": "application/json" }),
            Accept: "application/json",
            ...options.headers
        }
    });

    const text = await response.text();
    let body = null;

    if (text) {
        try {
            body = JSON.parse(text);
        } catch {
            body = text;
        }
    }

    if (!response.ok) {
        const message =
            typeof body === "object"
                ? body?.message || body?.error
                : body;

        throw new Error(message || `Request failed: ${response.status}`);
    }

    return body;
}

function unwrapArray(response) {
    if (Array.isArray(response)) return response;
    if (Array.isArray(response?.data)) return response.data;
    if (Array.isArray(response?.data?.content)) {
        return response.data.content;
    }

    return [];
}

function escapeHTML(value) {
    return String(value ?? "").replace(/[&<>"']/g, character => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;"
    })[character]);
}

function showMessage(message) {
    const element = document.getElementById("examMessage");
    element.textContent = message;
    element.hidden = false;
}

function closeModal() {
    document.getElementById("examModal").classList.remove("open");
}

function openModal() {
    document.getElementById("formError").textContent = "";
    document.getElementById("examForm").reset();

    const queryCourseId = new URLSearchParams(location.search).get("courseId");

    if (queryCourseId &&
        assignedCourses.some(course => String(course.id) === queryCourseId)) {
        document.getElementById("courseId").value = queryCourseId;
    }

    document.getElementById("examModal").classList.add("open");
}

function populateCourseSelects() {
    const selects = [
        document.getElementById("courseId"),
        document.getElementById("courseFilter")
    ];

    selects.forEach(select => {
        const firstOption = select.options[0];
        select.replaceChildren(firstOption);

        assignedCourses.forEach(course => {
            const option = document.createElement("option");
            option.value = course.id;
            option.textContent =
                `${course.title || "Untitled"} (${course.courseCode || course.id})`;

            select.appendChild(option);
        });
    });
}

async function loadCourses() {
    const response = await apiRequest("/courses/my-courses");
    assignedCourses = unwrapArray(response);

    populateCourseSelects();
}

async function loadExams() {
    const message = document.getElementById("examMessage");
    const grid = document.getElementById("examGrid");

    grid.replaceChildren();
    showMessage("Loading exams...");

    try {
        const results = await Promise.all(
            assignedCourses.map(async course => {
                const response = await apiRequest(
                    `/exams/course/${encodeURIComponent(course.id)}`
                );

                return unwrapArray(response).map(exam => ({
                    ...exam,
                    courseId: exam.courseId ?? course.id,
                    courseTitle: course.title || "Untitled Course"
                }));
            })
        );

        exams = results.flat();

        renderExams();

    } catch (error) {
        console.error("Failed to load exams:", error);

        showMessage(
            "Unable to load exams. Check your API access and server logs."
        );
    }
}

function renderExams() {
    const grid = document.getElementById("examGrid");
    const message = document.getElementById("examMessage");

    const query = document.getElementById("examSearch")
        .value.trim().toLowerCase();

    const courseFilter = document.getElementById("courseFilter").value;
    const statusFilter = document.getElementById("statusFilter").value;

    const filtered = exams.filter(exam => {
        const matchesSearch =
            String(exam.title || "").toLowerCase().includes(query) ||
            String(exam.description || "").toLowerCase().includes(query) ||
            String(exam.courseTitle || "").toLowerCase().includes(query);

        const matchesCourse =
            !courseFilter || String(exam.courseId) === courseFilter;

        const isPublished = exam.published === true;
        const matchesStatus =
            !statusFilter ||
            (statusFilter === "published" && isPublished) ||
            (statusFilter === "draft" && !isPublished);

        return matchesSearch && matchesCourse && matchesStatus;
    });

    grid.replaceChildren();

    if (!filtered.length) {
        message.hidden = false;
        message.textContent = exams.length
            ? "No exams match your filters."
            : "No exams found for your assigned courses.";

        return;
    }

    message.hidden = true;

    filtered.forEach(exam => {
        const card = document.createElement("article");
        card.className = "exam-card";

        const published = exam.published === true;
        const statusLabel = published ? "Published" : "Draft";

        card.innerHTML = `
            <span class="status-badge ${published ? "published" : "draft"}">
                ${statusLabel}
            </span>

            <h2>${escapeHTML(exam.title || "Untitled Exam")}</h2>

            <p class="exam-description">
                ${escapeHTML(exam.description || "No description available.")}
            </p>

            <p class="muted" style="margin-top:12px">
                Course: ${escapeHTML(exam.courseTitle)}
            </p>

            <div class="exam-stats">
                <div>
                    <small>Duration</small>
                    <strong>${escapeHTML(exam.durationMinutes ?? "—")} min</strong>
                </div>

                <div>
                    <small>Questions</small>
                    <strong>${escapeHTML(exam.totalQuestions ?? "—")}</strong>
                </div>

                <div>
                    <small>Total Marks</small>
                    <strong>${escapeHTML(exam.totalMarks ?? "—")}</strong>
                </div>

                <div>
                    <small>Passing Marks</small>
                    <strong>${escapeHTML(exam.passingMarks ?? "—")}</strong>
                </div>
            </div>

            <div class="exam-actions">
                <button
                    class="secondary-button"
                    data-action="${published ? "unpublish" : "publish"}"
                    data-id="${escapeHTML(exam.id)}">
                    ${published ? "Unpublish" : "Publish"}
                </button>

                <button
                    class="danger-button"
                    data-action="delete"
                    data-id="${escapeHTML(exam.id)}">
                    Delete
                </button>
            </div>
        `;

        grid.appendChild(card);
    });
}

async function createExam(event) {
    event.preventDefault();

    const errorElement = document.getElementById("formError");
    const saveButton = document.getElementById("saveExamButton");

    errorElement.textContent = "";

    const courseId = document.getElementById("courseId").value;
    const title = document.getElementById("title").value.trim();
    const description = document.getElementById("description").value.trim();

    const totalQuestions = Number(
        document.getElementById("totalQuestions").value
    );

    const durationMinutes = Number(
        document.getElementById("durationMinutes").value
    );

    const totalMarks = Number(
        document.getElementById("totalMarks").value
    );

    const passingMarks = Number(
        document.getElementById("passingMarks").value
    );

    if (!courseId || !title) {
        errorElement.textContent = "Please select a course and enter a title.";
        return;
    }

    if (
        !Number.isInteger(totalQuestions) || totalQuestions < 1 ||
        !Number.isInteger(durationMinutes) || durationMinutes < 1 ||
        !Number.isInteger(totalMarks) || totalMarks < 1 ||
        !Number.isInteger(passingMarks) ||
        passingMarks < 0 || passingMarks > totalMarks
    ) {
        errorElement.textContent =
            "Enter valid values. Passing marks must not exceed total marks.";
        return;
    }

    const payload = {
        courseId: Number(courseId),
        title,
        description,
        totalQuestions,
        totalMarks,
        passingMarks,
        durationMinutes
    };

    saveButton.disabled = true;
    saveButton.textContent = "Creating...";

    try {
        await apiRequest("/exams", {
            method: "POST",
            body: JSON.stringify(payload)
        });

        closeModal();
        await loadExams();

        alert("Exam created successfully.");

    } catch (error) {
        console.error("Exam creation failed:", error);
        errorElement.textContent = error.message ||
            "Unable to create exam.";

    } finally {
        saveButton.disabled = false;
        saveButton.textContent = "Create Exam";
    }
}

async function handleExamAction(action, examId) {
    const exam = exams.find(item => String(item.id) === String(examId));

    if (!exam) return;

    let endpoint;
    let method = "PUT";
    let successMessage;

    if (action === "publish") {
        endpoint = `/exams/${encodeURIComponent(examId)}/publish`;
        successMessage = "Exam published successfully.";
    } else if (action === "unpublish") {
        endpoint = `/exams/${encodeURIComponent(examId)}/unpublish`;
        successMessage = "Exam unpublished successfully.";
    } else if (action === "delete") {
        if (!confirm(`Delete exam "${exam.title}"?`)) return;

        endpoint = `/exams/${encodeURIComponent(examId)}`;
        method = "DELETE";
        successMessage = "Exam deleted successfully.";
    } else {
        return;
    }

    try {
        await apiRequest(endpoint, { method });
        alert(successMessage);
        await loadExams();

    } catch (error) {
        console.error("Exam action failed:", error);
        alert(error.message || "Unable to complete the action.");
    }
}

function displayFaculty() {
    const email = localStorage.getItem("email") || "Faculty";

    document.getElementById("facultyName").textContent = email;
    document.getElementById("avatar").textContent =
        email.charAt(0).toUpperCase() || "F";

    document.getElementById("year").textContent = new Date().getFullYear();
}

document.addEventListener("DOMContentLoaded", async () => {
    if (!checkFacultyAccess()) return;

    displayFaculty();

    document.getElementById("openCreateModal")
        .addEventListener("click", openModal);

    document.getElementById("closeCreateModal")
        .addEventListener("click", closeModal);

    document.getElementById("cancelCreate")
        .addEventListener("click", closeModal);

    document.getElementById("examForm")
        .addEventListener("submit", createExam);

    document.getElementById("examSearch")
        .addEventListener("input", renderExams);

    document.getElementById("courseFilter")
        .addEventListener("change", renderExams);

    document.getElementById("statusFilter")
        .addEventListener("change", renderExams);

    document.getElementById("examGrid").addEventListener("click", event => {
        const button = event.target.closest("button[data-action]");
        if (!button) return;

        handleExamAction(button.dataset.action, button.dataset.id);
    });

    document.getElementById("examModal").addEventListener("click", event => {
        if (event.target.id === "examModal") closeModal();
    });

    try {
        await loadCourses();
        await loadExams();
    } catch (error) {
        console.error("Initialization failed:", error);
        showMessage(
            error.message || "Unable to initialize the exam management page."
        );
    }
});

