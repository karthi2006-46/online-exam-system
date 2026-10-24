const API_BASE = "/api";

function getToken() {
    return localStorage.getItem("token");
}

function logout() {
    localStorage.removeItem("token");
    localStorage.removeItem("userId");
    localStorage.removeItem("role");
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

    if (role !== "FACULTY" && role !== "ADMIN") {
        alert("Access denied. Please log in with a faculty account.");
        window.location.href = "/login.html";
        return false;
    }

    return true;
}

async function apiGet(endpoint) {
    const response = await fetch(`${API_BASE}${endpoint}`, {
        method: "GET",
        headers: {
            "Authorization": `Bearer ${getToken()}`,
            "Content-Type": "application/json"
        }
    });

    if (!response.ok) {
        throw new Error(`Request failed: ${response.status}`);
    }

    return response.json();
}

function getArray(response) {
    if (Array.isArray(response)) return response;
    if (Array.isArray(response?.data)) return response.data;
    if (Array.isArray(response?.data?.content)) {
        return response.data.content;
    }
    return [];
}


async function loadDashboard() {
    const message = document.getElementById("dashboardMessage");

    const elements = {
        courseCount: "courseCount",
        pendingCount: "pendingCount",
        approvedCount: "approvedCount",
        rejectedCount: "rejectedCount"
    };

    Object.values(elements).forEach(id => {
        const element = document.getElementById(id);
        if (element) element.textContent = "—";
    });

    try {
        const data = await apiGet("/faculty/dashboard");

        // Dashboard statistics
        document.getElementById("courseCount").textContent =
            data.totalCourses ?? 0;

        document.getElementById("pendingCount").textContent =
            data.pendingExtensionRequests ?? 0;

        document.getElementById("extensionBadge").textContent =
            data.pendingExtensionRequests ?? 0;

        // Update the extension overview
        document.getElementById("overviewPending").textContent =
            data.pendingExtensionRequests ?? 0;

        // The dashboard API currently doesn't return separate
        // approved and rejected extension counts.
        document.getElementById("approvedCount").textContent = "—";
        document.getElementById("rejectedCount").textContent = "—";
        document.getElementById("overviewApproved").textContent = "—";
        document.getElementById("overviewRejected").textContent = "—";

        message.textContent = "Dashboard loaded successfully.";

    } catch (error) {
        console.error("Dashboard loading error:", error);

        message.textContent =
            "Unable to load dashboard statistics. Please check your login and API access.";
    }
}



function displayFaculty() {
    const email = localStorage.getItem("email") || "Faculty";
    const firstLetter = email.charAt(0).toUpperCase() || "F";

    document.getElementById("facultyName").textContent = email;
    document.getElementById("avatar").textContent = firstLetter;
    document.getElementById("topAvatar").textContent = firstLetter;
    document.getElementById("currentDate").textContent =
        new Date().toLocaleDateString("en-IN", {
            day: "numeric",
            month: "short",
            year: "numeric"
        });

    document.getElementById("year").textContent =
        new Date().getFullYear();
}

document.addEventListener("DOMContentLoaded", () => {
    if (!checkFacultyAccess()) return;

    displayFaculty();
    loadDashboard();
});