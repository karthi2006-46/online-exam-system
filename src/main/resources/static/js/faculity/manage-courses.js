
const API_BASE = "/api";

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

function checkFacultyAccess() {
    const token = getToken();

    const role = (localStorage.getItem("role") || "")
        .replace(/^ROLE_/i, "")
        .toUpperCase();

    if (!token) {
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

async function apiGet(endpoint) {
    const response = await fetch(`${API_BASE}${endpoint}`, {
        method: "GET",
        headers: {
            Authorization: `Bearer ${getToken()}`,
            Accept: "application/json"
        }
    });

    if (response.status === 401 || response.status === 403) {
        throw new Error(
            `Access denied (${response.status}). Please verify your login and permissions.`
        );
    }

    if (!response.ok) {
        throw new Error(`Request failed: ${response.status}`);
    }

    return response.json();
}

function unwrapResponse(response) {
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

let assignedCourses = [];

function renderCourses(courses) {
    const grid = document.getElementById("courseGrid");
    const message = document.getElementById("courseMessage");

    grid.replaceChildren();

    document.getElementById("courseSummary").textContent =
        `${courses.length} course${courses.length === 1 ? "" : "s"}`;

    if (courses.length === 0) {
        message.hidden = false;
        message.textContent =
            "No matching courses found. Try another search.";
        return;
    }

    message.hidden = true;

    courses.forEach(course => {
        const card = document.createElement("article");
        card.className = "course-card";

        const courseId = encodeURIComponent(course.id);

        card.innerHTML = `
            <span class="course-code">
                ${escapeHTML(course.courseCode || "COURSE")}
            </span>

            <h2>${escapeHTML(course.title || "Untitled Course")}</h2>

            <p class="course-description">
                ${escapeHTML(course.description || "No description available.")}
            </p>

            <div class="course-details">
                <div>
                    <small>Duration</small>
                    <strong>${escapeHTML(course.durationDays ?? "—")} days</strong>
                </div>

                <div>
                    <small>Course Fee</small>
                    <strong>${course.totalFees == null
                        ? "—"
                        : "₹" + escapeHTML(course.totalFees)}</strong>
                </div>
            </div>

            <div class="course-actions">
                <a href="/faculty/manage-exams.html?courseId=${courseId}">
                    View Exams
                </a>

                <a href="/faculty/manage-materials.html?courseId=${courseId}">
                    Materials
                </a>
            </div>
        `;

        grid.appendChild(card);
    });
}

async function loadCourses() {
    const message = document.getElementById("courseMessage");

    message.hidden = false;
    message.textContent = "Loading your assigned courses...";

    try {
        const response = await apiGet("/courses/my-courses");

        assignedCourses = unwrapResponse(response);

        renderCourses(assignedCourses);

    } catch (error) {
        console.error("Unable to load courses:", error);

        document.getElementById("courseGrid").replaceChildren();
        document.getElementById("courseSummary").textContent =
            "Courses unavailable";

        message.hidden = false;
        message.textContent =
            error.message || "Unable to load courses. Please try again.";
    }
}

function displayFaculty() {
    const email = localStorage.getItem("email") || "Faculty";
    const initial = email.charAt(0).toUpperCase() || "F";

    document.getElementById("facultyName").textContent = email;
    document.getElementById("avatar").textContent = initial;
    document.getElementById("year").textContent = new Date().getFullYear();

    document.getElementById("courseSearch").addEventListener("input", event => {
        const query = event.target.value.trim().toLowerCase();

        const filteredCourses = assignedCourses.filter(course => {
            const title = String(course.title || "").toLowerCase();
            const code = String(course.courseCode || "").toLowerCase();

            return title.includes(query) || code.includes(query);
        });

        renderCourses(filteredCourses);
    });
}

document.addEventListener("DOMContentLoaded", () => {
    if (!checkFacultyAccess()) return;

    displayFaculty();
    loadCourses();
});

