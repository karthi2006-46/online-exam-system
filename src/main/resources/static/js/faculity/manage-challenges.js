const API_BASE = `${window.API_BASE_URL}/api`;

let assignedCourses = [];
let challenges = [];

// ==============================
// AUTHENTICATION
// ==============================

function getToken() {
    return localStorage.getItem("token");
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

// ==============================
// API REQUEST
// ==============================

async function apiRequest(endpoint, options = {}) {

    const response = await fetch(`${API_BASE}${endpoint}`, {
        ...options,

        headers: {
            Authorization: `Bearer ${getToken()}`,
            "Content-Type": "application/json",
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

// ==============================
// HELPERS
// ==============================

function unwrapArray(response) {

    if (Array.isArray(response)) {
        return response;
    }

    if (Array.isArray(response?.data)) {
        return response.data;
    }

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

    const element = document.getElementById("challengeMessage");

    element.textContent = message;
    element.hidden = false;
}

// ==============================
// LOAD ASSIGNED COURSES
// ==============================

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

// ==============================
// LOAD CHALLENGES
// ==============================

async function loadChallenges() {

    const message = document.getElementById("challengeMessage");
    const grid = document.getElementById("challengeGrid");

    grid.replaceChildren();

    if (!assignedCourses.length) {

        message.hidden = false;
        message.textContent =
            "No courses are assigned to your account yet. Create a course first.";

        return;
    }

    showMessage("Loading challenges...");

    try {

        const results = await Promise.all(

            assignedCourses.map(async course => {

                const response = await apiRequest(
                    `/coding-challenges/faculty/course/${encodeURIComponent(course.id)}`
                );

                return unwrapArray(response).map(challenge => ({
                    ...challenge,

                    courseId: challenge.courseId ?? course.id,

                    courseTitle:
                        challenge.courseTitle || course.title || "Untitled Course"
                }));
            })
        );

        challenges = results.flat();

        renderChallenges();

    } catch (error) {

        console.error("Failed to load challenges:", error);

        showMessage(
            error.message ||
            "Unable to load coding challenges. Check your API access and server logs."
        );
    }
}

// ==============================
// RENDER CHALLENGES
// ==============================

function renderChallenges() {

    const grid = document.getElementById("challengeGrid");
    const message = document.getElementById("challengeMessage");

    const query = document.getElementById("challengeSearch")
        .value.trim().toLowerCase();

    const courseFilter = document.getElementById("courseFilter").value;
    const statusFilter = document.getElementById("statusFilter").value;

    const filtered = challenges.filter(challenge => {

        const matchesSearch =
            String(challenge.title || "").toLowerCase().includes(query) ||
            String(challenge.description || "").toLowerCase().includes(query) ||
            String(challenge.courseTitle || "").toLowerCase().includes(query);

        const matchesCourse =
            !courseFilter || String(challenge.courseId) === courseFilter;

        const isActive = challenge.active === true;

        const matchesStatus =
            !statusFilter ||
            (statusFilter === "active" && isActive) ||
            (statusFilter === "inactive" && !isActive);

        return matchesSearch && matchesCourse && matchesStatus;
    });

    grid.replaceChildren();

    if (!filtered.length) {

        message.hidden = false;

        message.textContent = challenges.length
            ? "No challenges match your filters."
            : "No coding challenges yet. Use “+ New challenge” to add one.";

        return;
    }

    message.hidden = true;

    filtered.forEach(challenge => {

        const card = document.createElement("article");

        card.className = "challenge-card";

        const active = challenge.active === true;

        const difficulty = String(challenge.difficulty || "EASY").toUpperCase();

        card.innerHTML = `

            <div class="challenge-card-top">

                <span class="status-badge ${active ? "published" : "draft"}">
                    ${active ? "Active" : "Inactive"}
                </span>

                <span class="difficulty-badge ${difficulty.toLowerCase()}">
                    ${escapeHTML(difficulty)}
                </span>
            </div>

            <h2>${escapeHTML(challenge.title || "Untitled Challenge")}</h2>

            <p class="challenge-description">
                ${escapeHTML(
                    challenge.description ||
                    challenge.problemStatement ||
                    "No description available."
                )}
            </p>

            <p class="muted challenge-course">
                Course: ${escapeHTML(challenge.courseTitle)}
            </p>

            <div class="challenge-stats">
                <div>
                    <small>Marks</small>
                    <strong>${escapeHTML(challenge.marks ?? "—")}</strong>
                </div>

                <div>
                    <small>Difficulty</small>
                    <strong>${escapeHTML(difficulty)}</strong>
                </div>
            </div>

            <div class="challenge-actions">

                <button
                    type="button"
                    class="secondary-button"
                    data-action="edit"
                    data-id="${escapeHTML(challenge.id)}">

                    Edit

                </button>

                <button
                    type="button"
                    class="secondary-button"
                    data-action="${active ? "deactivate" : "activate"}"
                    data-id="${escapeHTML(challenge.id)}">

                    ${active ? "Deactivate" : "Activate"}

                </button>

                <button
                    type="button"
                    class="danger-button"
                    data-action="delete"
                    data-id="${escapeHTML(challenge.id)}">

                    Delete

                </button>

            </div>
        `;

        grid.appendChild(card);
    });
}

// ==============================
// MODAL
// ==============================

function resetForm() {

    document.getElementById("challengeForm").reset();
    document.getElementById("challengeId").value = "";
    document.getElementById("formError").textContent = "";
    document.getElementById("active").checked = true;
    document.getElementById("difficulty").value = "EASY";
}

function closeModal() {

    document.getElementById("challengeModal").classList.remove("open");
}

function openCreateModal() {

    resetForm();

    document.getElementById("modalTitle").textContent = "New coding challenge";
    document.getElementById("modalSubtitle").textContent =
        "Enter the challenge details for your course.";
    document.getElementById("saveChallengeButton").textContent = "Create challenge";

    const queryCourseId =
        new URLSearchParams(location.search).get("courseId");

    if (
        queryCourseId &&
        assignedCourses.some(course => String(course.id) === queryCourseId)
    ) {
        document.getElementById("courseId").value = queryCourseId;
    }

    document.getElementById("challengeModal").classList.add("open");
}

function fillForm(challenge) {

    resetForm();

    document.getElementById("modalTitle").textContent = "Edit coding challenge";
    document.getElementById("modalSubtitle").textContent =
        `Update “${challenge.title || "Untitled"}”.`;
    document.getElementById("saveChallengeButton").textContent = "Save changes";

    document.getElementById("challengeId").value = challenge.id ?? "";
    document.getElementById("courseId").value = challenge.courseId ?? "";
    document.getElementById("title").value = challenge.title ?? "";
    document.getElementById("difficulty").value =
        String(challenge.difficulty || "EASY").toUpperCase();
    document.getElementById("marks").value = challenge.marks ?? "";
    document.getElementById("description").value = challenge.description ?? "";
    document.getElementById("problemStatement").value = challenge.problemStatement ?? "";
    document.getElementById("starterCode").value = challenge.starterCode ?? "";
    document.getElementById("sampleInput").value = challenge.sampleInput ?? "";
    document.getElementById("sampleOutput").value = challenge.sampleOutput ?? "";
    document.getElementById("expectedOutput").value = challenge.expectedOutput ?? "";
    document.getElementById("constraints").value = challenge.constraints ?? "";
    document.getElementById("active").checked = challenge.active === true;

    document.getElementById("challengeModal").classList.add("open");
}

// ==============================
// CREATE / UPDATE
// ==============================

function readForm() {

    return {
        courseId: Number(document.getElementById("courseId").value) || null,
        title: document.getElementById("title").value.trim(),
        difficulty: document.getElementById("difficulty").value,
        marks: document.getElementById("marks").value === ""
            ? null
            : Number(document.getElementById("marks").value),
        description: document.getElementById("description").value.trim(),
        problemStatement: document.getElementById("problemStatement").value.trim(),
        starterCode: document.getElementById("starterCode").value,
        sampleInput: document.getElementById("sampleInput").value,
        sampleOutput: document.getElementById("sampleOutput").value,
        expectedOutput: document.getElementById("expectedOutput").value,
        constraints: document.getElementById("constraints").value.trim(),
        active: document.getElementById("active").checked
    };
}

async function saveChallenge(event) {

    event.preventDefault();

    const errorElement = document.getElementById("formError");
    const saveButton = document.getElementById("saveChallengeButton");

    errorElement.textContent = "";

    const challengeId = document.getElementById("challengeId").value;
    const payload = readForm();

    if (!payload.courseId) {
        errorElement.textContent = "Please select a course.";
        return;
    }

    if (!payload.title) {
        errorElement.textContent = "Please enter a challenge title.";
        return;
    }

    if (payload.marks !== null && (!Number.isInteger(payload.marks) || payload.marks < 0)) {
        errorElement.textContent = "Marks must be a whole number of 0 or more.";
        return;
    }

    const originalLabel = saveButton.textContent;

    saveButton.disabled = true;
    saveButton.textContent = "Saving...";

    try {

        if (challengeId) {

            await apiRequest(`/coding-challenges/${encodeURIComponent(challengeId)}`, {
                method: "PUT",
                body: JSON.stringify(payload)
            });

        } else {

            await apiRequest("/coding-challenges", {
                method: "POST",
                body: JSON.stringify(payload)
            });
        }

        closeModal();

        await loadChallenges();

    } catch (error) {

        console.error("Failed to save challenge:", error);

        errorElement.textContent =
            error.message || "Unable to save the challenge.";

    } finally {

        saveButton.disabled = false;
        saveButton.textContent = originalLabel;
    }
}

// ==============================
// ACTIVATE / DEACTIVATE / DELETE
// ==============================

async function toggleChallenge(challengeId, active) {

    try {

        await apiRequest(
            `/coding-challenges/${encodeURIComponent(challengeId)}/active`,
            {
                method: "PATCH",
                body: JSON.stringify({ active })
            }
        );

        await loadChallenges();

    } catch (error) {

        console.error("Failed to change challenge status:", error);
        alert(error.message || "Unable to update the challenge status.");
    }
}

async function deleteChallenge(challenge) {

    const confirmed = window.confirm(
        `Delete “${challenge.title || "this challenge"}”?\n\n` +
        "Challenges that already have student submissions cannot be deleted — " +
        "deactivate those instead."
    );

    if (!confirmed) {
        return;
    }

    try {

        await apiRequest(
            `/coding-challenges/${encodeURIComponent(challenge.id)}`,
            { method: "DELETE" }
        );

        await loadChallenges();

    } catch (error) {

        console.error("Failed to delete challenge:", error);
        alert(error.message || "Unable to delete the challenge.");
    }
}

// ==============================
// EVENT WIRING
// ==============================

function wireEvents() {

    document.getElementById("openCreateModal")
        .addEventListener("click", openCreateModal);

    document.getElementById("closeChallengeModal")
        .addEventListener("click", closeModal);

    document.getElementById("cancelChallenge")
        .addEventListener("click", closeModal);

    document.getElementById("challengeModal")
        .addEventListener("click", event => {
            if (event.target.id === "challengeModal") {
                closeModal();
            }
        });

    document.addEventListener("keydown", event => {
        if (event.key === "Escape") {
            closeModal();
        }
    });

    document.getElementById("challengeForm")
        .addEventListener("submit", saveChallenge);

    ["challengeSearch", "courseFilter", "statusFilter"]
        .forEach(id => {
            document.getElementById(id)
                .addEventListener("input", renderChallenges);
            document.getElementById(id)
                .addEventListener("change", renderChallenges);
        });

    document.getElementById("challengeGrid")
        .addEventListener("click", event => {

            const button = event.target.closest("button[data-action]");

            if (!button) {
                return;
            }

            const id = button.dataset.id;
            const challenge = challenges.find(
                item => String(item.id) === String(id)
            );

            if (!challenge) {
                return;
            }

            switch (button.dataset.action) {

                case "edit":
                    fillForm(challenge);
                    break;

                case "activate":
                    toggleChallenge(id, true);
                    break;

                case "deactivate":
                    toggleChallenge(id, false);
                    break;

                case "delete":
                    deleteChallenge(challenge);
                    break;
            }
        });
}

// ==============================
// INIT
// ==============================

async function initialize() {

    if (!checkFacultyAccess()) {
        return;
    }

    const yearElement = document.getElementById("year");

    if (yearElement) {
        yearElement.textContent = new Date().getFullYear();
    }

    wireEvents();

    try {

        await loadCourses();
        await loadChallenges();

    } catch (error) {

        console.error("Initialization failed:", error);

        showMessage(
            error.message ||
            "Unable to load your courses. Check your API access and server logs."
        );
    }
}

document.addEventListener("DOMContentLoaded", initialize);
