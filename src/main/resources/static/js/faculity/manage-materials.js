"use strict";

/*
 * Faculty Manage Materials
 *
 * Existing backend endpoints:
 *
 * GET    /api/materials/course/{courseId}
 * GET    /api/materials/course/{courseId}/type/{fileType}
 * POST   /api/materials/upload
 * PUT    /api/materials/{materialId}
 * DELETE /api/materials/{materialId}
 *
 * Upload fields:
 * courseId, title, description, fileType, file
 */

const API_BASE = `${window.API_BASE_URL}/api`;
const MAX_FILE_SIZE = 50 * 1024 * 1024;

const ALLOWED_EXTENSIONS = {
    PDF: [".pdf"],
    VIDEO: [".mp4", ".webm", ".mov", ".m4v"],
    ASSIGNMENT: [".pdf", ".doc", ".docx", ".txt", ".zip"]
};

let currentMaterials = [];
let currentCourseId = null;
let editingMaterialId = null;
let materialPendingDelete = null;
let notificationTimer = null;


/* =========================================================
   INITIALIZATION
========================================================= */

document.addEventListener("DOMContentLoaded", () => {

    if (!checkFacultyAccess()) {
        return;
    }

    initializePage();
    bindEvents();

    const savedCourseId = sessionStorage.getItem(
        "facultyMaterialsCourseId"
    );

    if (savedCourseId && Number(savedCourseId) > 0) {
        document.getElementById("courseId").value = savedCourseId;
        loadCourseMaterials(savedCourseId);
    }

});


/* =========================================================
   AUTHENTICATION
========================================================= */

function getToken() {
    return localStorage.getItem("token");
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


function logout() {

    localStorage.removeItem("token");
    localStorage.removeItem("userId");
    localStorage.removeItem("role");
    localStorage.removeItem("email");

    sessionStorage.removeItem("facultyMaterialsCourseId");

    window.location.href = "/login.html";
}


/* =========================================================
   INITIALIZE PAGE
========================================================= */
function initializePage() {

    const email = localStorage.getItem("email") || "Faculty";
    const storedUser = JSON.parse(localStorage.getItem("user") || "null");

    const facultyName =
        storedUser?.name ||
        storedUser?.fullName ||
        storedUser?.username ||
        email;

    const firstLetter = facultyName.charAt(0).toUpperCase() || "F";

    // Dynamically loaded faculty sidebar
    const sidebarName = document.getElementById("facultyName");
    const sidebarAvatar = document.getElementById("avatar");

    if (sidebarName) {
        sidebarName.textContent = facultyName;
    }

    if (sidebarAvatar) {
        sidebarAvatar.textContent = firstLetter;
    }

    // Top avatar
    const topAvatar = document.getElementById("topAvatar");

    if (topAvatar) {
        topAvatar.textContent = firstLetter;
    }

    // Current date
    const currentDate = document.getElementById("currentDate");

    if (currentDate) {
        currentDate.textContent = new Date().toLocaleDateString("en-IN", {
            day: "2-digit",
            month: "short",
            year: "numeric"
        });
    }

    // Current year
    const currentYear = document.getElementById("currentYear");

    if (currentYear) {
        currentYear.textContent = new Date().getFullYear();
    }

    updateStatistics([]);
    updateFileHelp();
}
/* =========================================================
   EVENT LISTENERS
========================================================= */

function bindEvents() {

// document.addEventListener("click", function(event) {
//     if (event.target.closest("#logoutButton")) {
//         logout();
//     }
// });

    document.getElementById("courseForm")
        .addEventListener("submit", handleCourseSubmit);

    document.getElementById("refreshButton")
        .addEventListener("click", refreshMaterials);

    document.getElementById("tableRefreshButton")
        .addEventListener("click", refreshMaterials);

    document.getElementById("materialForm")
        .addEventListener("submit", handleMaterialSubmit);

    document.getElementById("materialFile")
        .addEventListener("change", handleFileSelection);

    document.getElementById("fileType")
        .addEventListener("change", updateFileHelp);

    document.getElementById("materialSearch")
        .addEventListener("input", renderMaterials);

    document.getElementById("typeFilter")
        .addEventListener("change", renderMaterials);

    document.getElementById("cancelEditButton")
        .addEventListener("click", resetMaterialForm);

    document.getElementById("cancelDeleteButton")
        .addEventListener("click", closeDeleteModal);

    document.getElementById("confirmDeleteButton")
        .addEventListener("click", confirmDeleteMaterial);

    document.getElementById("materialsTableBody")
        .addEventListener("click", handleTableAction);

    document.getElementById("deleteModal")
        .addEventListener("click", event => {
            if (event.target.id === "deleteModal") {
                closeDeleteModal();
            }
        });

}


/* =========================================================
   API REQUEST HELPER
========================================================= */

async function apiRequest(endpoint, options = {}) {

    const token = getToken();

    if (!token) {
        throw new Error(
            "Your session has expired. Please log in again."
        );
    }

    const headers = new Headers(options.headers || {});

    headers.set("Authorization", `Bearer ${token}`);
    headers.set("Accept", "application/json");

    /*
     * Do not set Content-Type manually for FormData.
     * The browser adds the multipart boundary automatically.
     */

    const response = await fetch(`${API_BASE}${endpoint}`, {
        ...options,
        headers
    });

    if (response.status === 401) {
        throw new Error(
            "Authentication failed. Please log in again."
        );
    }

    if (response.status === 403) {
        throw new Error(
            "Access denied. Verify your role and course assignment."
        );
    }

    const contentType = response.headers.get("content-type") || "";

    let body = null;

    if (contentType.includes("application/json")) {
        body = await response.json();
    } else {
        const text = await response.text();
        body = text ? { message: text } : null;
    }

    if (!response.ok) {
        throw new Error(
            extractErrorMessage(body) ||
            `Request failed with HTTP ${response.status}.`
        );
    }

    return body;
}


function extractErrorMessage(body) {

    if (!body) {
        return "";
    }

    if (typeof body === "string") {
        return body;
    }

    return body.message ||
        body.error ||
        body.detail ||
        body.data?.message ||
        "";
}


/*
 * ApiResponseUtil response envelopes can vary.
 * This function supports common response formats.
 */

function unwrapResponse(body) {

    if (body == null) {
        return body;
    }

    if (Array.isArray(body)) {
        return body;
    }

    if (Object.prototype.hasOwnProperty.call(body, "data")) {
        return body.data;
    }

    if (Object.prototype.hasOwnProperty.call(body, "result")) {
        return body.result;
    }

    return body;
}


/* =========================================================
   COURSE SELECTION
========================================================= */

async function handleCourseSubmit(event) {

    event.preventDefault();

    const input = document.getElementById("courseId");
    const courseId = Number(input.value);

    if (!Number.isSafeInteger(courseId) || courseId <= 0) {
        showNotification(
            "Please enter a valid positive course ID.",
            "error"
        );
        return;
    }

    await loadCourseMaterials(courseId);
}


async function loadCourseMaterials(courseId) {

    const loadButton = document.getElementById("loadCourseButton");
    const tableBody = document.getElementById("materialsTableBody");

    loadButton.disabled = true;
    loadButton.textContent = "Loading...";

    tableBody.innerHTML = `
        <tr>
            <td colspan="4" class="empty-cell">
                Loading course materials...
            </td>
        </tr>
    `;

    setTableMessage("Loading materials...");

    try {

        const body = await apiRequest(
            `/materials/course/${encodeURIComponent(courseId)}`
        );

        const data = unwrapResponse(body);

        if (!Array.isArray(data)) {
            throw new Error(
                "The server did not return a valid materials list."
            );
        }

        currentCourseId = courseId;
        currentMaterials = data;

        sessionStorage.setItem(
            "facultyMaterialsCourseId",
            String(courseId)
        );

        resetMaterialForm();

        renderMaterials();

        document.getElementById("formNote").textContent =
            `Selected course ID: ${courseId}`;

        showNotification(
            `Loaded ${data.length} material(s) successfully.`,
            "success"
        );

    } catch (error) {

        console.error("Unable to load course materials:", error);

        currentCourseId = null;
        currentMaterials = [];

        updateStatistics([]);
        renderMaterials();

        setTableMessage(error.message);

        showNotification(error.message, "error");

    } finally {

        loadButton.disabled = false;
        loadButton.textContent = "Load Materials";

    }
}


async function refreshMaterials() {

    if (!currentCourseId) {

        const value = document.getElementById("courseId").value;

        if (!value) {
            showNotification(
                "Please enter a course ID first.",
                "info"
            );
            return;
        }

        await loadCourseMaterials(value);
        return;
    }

    await loadCourseMaterials(currentCourseId);
}


/* =========================================================
   FILE VALIDATION
========================================================= */

function updateFileHelp() {

    const fileType = document.getElementById("fileType").value;
    const help = document.getElementById("fileHelp");

    const extensions = ALLOWED_EXTENSIONS[fileType] || [];

    help.textContent =
        `Allowed formats: ${extensions.join(", ")}. Maximum size: 50 MB.`;

}


function handleFileSelection() {

    const input = document.getElementById("materialFile");
    const file = input.files[0];

    const label = document.getElementById("fileLabel");

    if (!file) {
        label.textContent = "Click to select a file";
        return;
    }

    label.textContent = file.name;

    if (file.size > MAX_FILE_SIZE) {

        showNotification(
            "The selected file exceeds the 50 MB limit.",
            "error"
        );

        input.value = "";
        label.textContent = "Click to select a file";
        return;
    }

    const fileType = document.getElementById("fileType").value;

    if (!isAllowedFile(file, fileType)) {

        showNotification(
            `This file extension is not allowed for ${fileType}.`,
            "error"
        );

        input.value = "";
        label.textContent = "Click to select a file";
        return;
    }

    showNotification(
        `Selected ${file.name} (${formatFileSize(file.size)}).`,
        "info"
    );
}


function isAllowedFile(file, fileType) {

    const extensions = ALLOWED_EXTENSIONS[fileType] || [];

    const name = file.name.toLowerCase();

    return extensions.some(extension =>
        name.endsWith(extension)
    );
}


/* =========================================================
   UPLOAD / UPDATE MATERIAL
========================================================= */

async function handleMaterialSubmit(event) {

    event.preventDefault();

    if (!currentCourseId) {

        showNotification(
            "Load an authorized course before managing materials.",
            "error"
        );

        return;
    }

    const title = document.getElementById("materialTitle")
        .value.trim();

    const description = document.getElementById("materialDescription")
        .value.trim();

    const fileType = document.getElementById("fileType")
        .value;

    const fileInput = document.getElementById("materialFile");

    const file = fileInput.files[0];

    if (!title) {

        showNotification(
            "Please enter a material title.",
            "error"
        );

        return;
    }

    if (title.length > 200) {

        showNotification(
            "The material title must not exceed 200 characters.",
            "error"
        );

        return;
    }

    if (description.length > 1000) {

        showNotification(
            "The description must not exceed 1000 characters.",
            "error"
        );

        return;
    }

    const isEditing = editingMaterialId !== null;

    /*
     * The current backend update endpoint changes only title
     * and description. File replacement is not implemented.
     */

    if (isEditing && file) {

        showNotification(
            "File replacement is not supported by the current update API. Remove the selected file and update the text fields.",
            "error"
        );

        return;
    }

    if (!isEditing) {

        if (!file) {

            showNotification(
                "Please select a file to upload.",
                "error"
            );

            return;
        }

        if (file.size > MAX_FILE_SIZE) {

            showNotification(
                "File size must not exceed 50 MB.",
                "error"
            );

            return;
        }

        if (!isAllowedFile(file, fileType)) {

            showNotification(
                "The selected file extension does not match the material type.",
                "error"
            );

            return;
        }
    }

    const submitButton = document.getElementById("saveMaterialButton");
    const submitText = document.getElementById("saveButtonText");

    submitButton.disabled = true;

    submitText.textContent = isEditing
        ? "Saving Changes..."
        : "Uploading...";

    try {

        if (isEditing) {

            const params = new URLSearchParams();

            params.set("title", title);
            params.set("description", description);

            await apiRequest(
                `/materials/${editingMaterialId}`,
                {
                    method: "PUT",
                    body: params
                }
            );

            showNotification(
                "Material updated successfully.",
                "success"
            );

        } else {

            const formData = new FormData();

            formData.append("courseId", String(currentCourseId));
            formData.append("title", title);
            formData.append("description", description);
            formData.append("fileType", fileType);
            formData.append("file", file);

            await apiRequest(
                "/materials/upload",
                {
                    method: "POST",
                    body: formData
                }
            );

            showNotification(
                "Material uploaded successfully.",
                "success"
            );
        }

        resetMaterialForm();

        await loadCourseMaterials(currentCourseId);

    } catch (error) {

        console.error("Material save error:", error);

        showNotification(error.message, "error");

    } finally {

        submitButton.disabled = false;

        submitText.textContent = editingMaterialId !== null
            ? "Save Changes"
            : "↑ Upload Material";

    }
}


/* =========================================================
   RENDER MATERIALS
========================================================= */

function renderMaterials() {

    const tableBody = document.getElementById("materialsTableBody");

    const search = document.getElementById("materialSearch")
        .value.trim().toLowerCase();

    const typeFilter = document.getElementById("typeFilter").value;

    const filteredMaterials = currentMaterials.filter(material => {

        const title = String(material.title || "").toLowerCase();

        const fileName = String(material.fileName || "").toLowerCase();

        const description = String(material.description || "").toLowerCase();

        const matchesSearch =
            title.includes(search) ||
            fileName.includes(search) ||
            description.includes(search);

        const matchesType =
            typeFilter === "ALL" ||
            String(material.fileType || "").toUpperCase() === typeFilter;

        return matchesSearch && matchesType;
    });

    updateStatistics(currentMaterials);

    document.getElementById("resultCount").textContent =
        `${filteredMaterials.length} material(s)`;

    if (!currentCourseId) {

        tableBody.innerHTML = `
            <tr>
                <td colspan="4" class="empty-cell">
                    Select a course to view materials.
                </td>
            </tr>
        `;

        return;
    }

    if (filteredMaterials.length === 0) {

        tableBody.innerHTML = `
            <tr>
                <td colspan="4" class="empty-cell">
                    No materials found. Try another search or upload a material.
                </td>
            </tr>
        `;

        setTableMessage("No matching materials.");
        return;
    }

    tableBody.innerHTML = filteredMaterials.map(material => {

        const type = String(material.fileType || "").toUpperCase();

        const badgeClass = getTypeClass(type);

        const iconLabel = getTypeLabel(type);

        return `
            <tr>

                <td>
                    <div class="material-name">

                        <div class="material-file-icon ${badgeClass}">
                            ${iconLabel}
                        </div>

                        <div class="material-details">

                            <strong>${escapeHtml(material.title || "Untitled")}</strong>

                            <span>
                                ${escapeHtml(material.fileName || "No filename")}
                            </span>

                        </div>

                    </div>
                </td>

                <td>
                    <span class="type-badge ${badgeClass}">
                        ${escapeHtml(type || "UNKNOWN")}
                    </span>
                </td>

                <td>
                    <span class="file-size">
                        ${formatFileSize(material.fileSize)}
                    </span>
                </td>

                <td>
                    <div class="action-buttons">

                        <button
                            type="button"
                            class="action-button"
                            data-action="edit"
                            data-id="${Number(material.id)}"
                            title="Edit material"
                            aria-label="Edit material">
                            ✎
                        </button>

                        <button
                            type="button"
                            class="action-button delete"
                            data-action="delete"
                            data-id="${Number(material.id)}"
                            title="Delete material"
                            aria-label="Delete material">
                            ×
                        </button>

                    </div>
                </td>

            </tr>
        `;

    }).join("");

    setTableMessage(
        `Showing ${filteredMaterials.length} of ${currentMaterials.length} material(s).`
    );
}


/* =========================================================
   STATISTICS
========================================================= */

function updateStatistics(materials) {

    const list = Array.isArray(materials) ? materials : [];

    const pdfCount = list.filter(material =>
        String(material.fileType || "").toUpperCase() === "PDF"
    ).length;

    const videoCount = list.filter(material =>
        String(material.fileType || "").toUpperCase() === "VIDEO"
    ).length;

    const assignmentCount = list.filter(material =>
        String(material.fileType || "").toUpperCase() === "ASSIGNMENT"
    ).length;

    document.getElementById("totalMaterials").textContent =
        list.length;

    document.getElementById("pdfMaterials").textContent =
        pdfCount;

    document.getElementById("videoMaterials").textContent =
        videoCount;

    document.getElementById("assignmentMaterials").textContent =
        assignmentCount;
}


/* =========================================================
   EDIT MATERIAL
========================================================= */

function handleTableAction(event) {

    const button = event.target.closest("button[data-action]");

    if (!button) {
        return;
    }

    const action = button.dataset.action;

    const materialId = Number(button.dataset.id);

    if (!Number.isSafeInteger(materialId) || materialId <= 0) {
        showNotification("Invalid material ID.", "error");
        return;
    }

    const material = currentMaterials.find(item =>
        Number(item.id) === materialId
    );

    if (!material) {
        showNotification("Material not found in the current list.", "error");
        return;
    }

    if (action === "edit") {
        startEditMaterial(material);
    }

    if (action === "delete") {
        openDeleteModal(material);
    }
}


function startEditMaterial(material) {

    editingMaterialId = Number(material.id);

    document.getElementById("materialTitle").value =
        material.title || "";

    document.getElementById("materialDescription").value =
        material.description || "";

    document.getElementById("fileType").value =
        material.fileType || "PDF";

    document.getElementById("materialFile").value = "";

    document.getElementById("fileLabel").textContent =
        "File replacement is not supported";

    document.getElementById("fileHelp").textContent =
        `Current file: ${material.fileName || "Unknown"}`;

    document.getElementById("materialFile").required = false;

    document.getElementById("formHeading").textContent =
        "Edit Material";

    document.getElementById("saveButtonText").textContent =
        "Save Changes";

    document.getElementById("cancelEditButton").hidden = false;

    document.getElementById("formNote").textContent =
        `Editing material ID: ${material.id}`;

    document.getElementById("fileType").disabled = true;

    document.getElementById("fileType").title =
        "The current backend does not support replacing an uploaded file.";

    document.getElementById("materialTitle").focus();

    document.getElementById("formHeading").scrollIntoView({
        behavior: "smooth",
        block: "center"
    });
}


function resetMaterialForm() {

    editingMaterialId = null;

    document.getElementById("materialForm").reset();

    document.getElementById("materialFile").required = true;

    document.getElementById("fileType").disabled = false;

    document.getElementById("fileType").title = "";

    document.getElementById("formHeading").textContent =
        "Upload New Material";

    document.getElementById("saveButtonText").textContent =
        "↑ Upload Material";

    document.getElementById("cancelEditButton").hidden = true;

    document.getElementById("fileLabel").textContent =
        "Click to select a file";

    document.getElementById("formNote").textContent =
        currentCourseId
            ? `Selected course ID: ${currentCourseId}`
            : "Select a course before uploading.";

    updateFileHelp();
}


/* =========================================================
   DELETE MATERIAL
========================================================= */

function openDeleteModal(material) {

    materialPendingDelete = material;

    document.getElementById("deleteMessage").textContent =
        `Are you sure you want to delete "${material.title}"? It will be removed from the active materials list.`;

    document.getElementById("deleteModal").hidden = false;
}


function closeDeleteModal() {

    materialPendingDelete = null;

    document.getElementById("deleteModal").hidden = true;
}


async function confirmDeleteMaterial() {

    if (!materialPendingDelete) {
        return;
    }

    const materialId = materialPendingDelete.id;

    const button = document.getElementById("confirmDeleteButton");

    button.disabled = true;
    button.textContent = "Deleting...";

    try {

        await apiRequest(
            `/materials/${materialId}`,
            {
                method: "DELETE"
            }
        );

        closeDeleteModal();

        if (editingMaterialId === Number(materialId)) {
            resetMaterialForm();
        }

        showNotification(
            "Material deleted successfully.",
            "success"
        );

        await loadCourseMaterials(currentCourseId);

    } catch (error) {

        console.error("Delete material error:", error);

        showNotification(error.message, "error");

    } finally {

        button.disabled = false;
        button.textContent = "Delete Material";

    }
}


/* =========================================================
   UTILITIES
========================================================= */

function getTypeClass(type) {

    switch (type) {

        case "PDF":
            return "pdf";

        case "VIDEO":
            return "video";

        case "ASSIGNMENT":
            return "assignment";

        default:
            return "";
    }
}


function getTypeLabel(type) {

    switch (type) {

        case "PDF":
            return "PDF";

        case "VIDEO":
            return "▶";

        case "ASSIGNMENT":
            return "DOC";

        default:
            return "FILE";
    }
}


function formatFileSize(size) {

    const bytes = Number(size);

    if (!Number.isFinite(bytes) || bytes < 0) {
        return "—";
    }

    if (bytes < 1024) {
        return `${bytes} B`;
    }

    if (bytes < 1024 * 1024) {
        return `${(bytes / 1024).toFixed(1)} KB`;
    }

    if (bytes < 1024 * 1024 * 1024) {
        return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
    }

    return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`;
}


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


function setTableMessage(message) {

    document.getElementById("tableMessage").textContent =
        message || "";
}


function showNotification(message, type = "info") {

    const notification = document.getElementById("notification");

    clearTimeout(notificationTimer);

    notification.textContent = message;
    notification.className = `notification ${type}`;
    notification.hidden = false;

    notificationTimer = setTimeout(() => {
        notification.hidden = true;
    }, 5000);
}