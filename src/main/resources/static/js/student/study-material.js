// =====================================================
// STUDY MATERIALS - STUDENT MODULE
// =====================================================

const MATERIALS_API = `${window.API_BASE_URL}/api/materials`;
const MATERIAL_PROGRESS_API =
    `${window.API_BASE_URL}/api/material-progress`;


// =====================================================
// API RESPONSE HELPER
// =====================================================

async function readApiResponse(response) {
    const contentType = response.headers.get("content-type") || "";

    if (contentType.includes("application/json")) {
        return await response.json();
    }

    return await response.text();
}


// =====================================================
// NORMALIZE MATERIAL URL
// =====================================================

function getMaterialUrl(fileUrl) {
    if (!fileUrl || typeof fileUrl !== "string") {
        throw new Error("Material file URL is missing.");
    }

    const url = fileUrl.trim();

    if (!url) {
        throw new Error("Material file URL is empty.");
    }

    // External URL: return it without adding localhost.
    if (/^https?:\/\//i.test(url)) {
        return url;
    }

    // Backend URL from api-config.js
    const baseUrl = String(window.API_BASE_URL || "")
        .replace(/\/+$/, "");

    if (!baseUrl) {
        throw new Error("Backend API URL is not configured.");
    }

    // If the URL starts with "/", append it to the backend URL.
    if (url.startsWith("/")) {
        return `${baseUrl}${url}`;
    }

    // Otherwise, append the relative path.
    return `${baseUrl}/${url}`;
}




// =====================================================
// LOAD STUDY MATERIALS
// =====================================================

async function loadMaterials() {
    const studentId = localStorage.getItem("userId");
    const courseId = localStorage.getItem("courseId");
    const token = localStorage.getItem("token");

    const container = document.getElementById("materialsContainer");

    if (!container) {
        console.error("materialsContainer not found.");
        return;
    }

    if (!studentId || !token) {
        container.innerHTML = `
            <div class="empty-state">
                <h3>Session expired</h3>
                <p>Please log in again to access your study materials.</p>
            </div>
        `;
        return;
    }

    if (!courseId) {
        container.innerHTML = `
            <div class="empty-state">
                <h3>No course selected</h3>
                <p>
                    Please select an enrolled course before viewing materials.
                </p>
            </div>
        `;
        return;
    }

    container.innerHTML = `
        <div class="materials-loading">
            <div class="loading-spinner"></div>
            <p>Loading your study materials...</p>
        </div>
    `;

    try {
        // ---------------------------------------------
        // 1. Fetch course materials
        // ---------------------------------------------

        const materialsResponse = await fetch(
            `${MATERIALS_API}/course/${encodeURIComponent(courseId)}`,
            {
                method: "GET",
                headers: {
                    Authorization: `Bearer ${token}`,
                    Accept: "application/json"
                }
            }
        );

        if (!materialsResponse.ok) {
            throw new Error(
                `Failed to load materials: HTTP ${materialsResponse.status}`
            );
        }

        const materialsResult = await materialsResponse.json();

        const materials = Array.isArray(materialsResult)
            ? materialsResult
            : Array.isArray(materialsResult.data)
                ? materialsResult.data
                : [];

        // ---------------------------------------------
        // 2. Fetch completed material IDs
        // ---------------------------------------------

        const completedResponse = await fetch(
            `${MATERIAL_PROGRESS_API}/student/${encodeURIComponent(studentId)}`,
            {
                method: "GET",
                headers: {
                    Authorization: `Bearer ${token}`,
                    Accept: "application/json"
                }
            }
        );

        if (!completedResponse.ok) {
            throw new Error(
                `Failed to load completion status: HTTP ${completedResponse.status}`
            );
        }

        const completedResult = await completedResponse.json();

        const completedIds = Array.isArray(completedResult)
            ? completedResult
            : Array.isArray(completedResult.data)
                ? completedResult.data
                : [];

        const completedSet = new Set(
            completedIds
                .map(item => {
                    if (typeof item === "object" && item !== null) {
                        return Number(item.materialId ?? item.id);
                    }

                    return Number(item);
                })
                .filter(Number.isFinite)
        );

        // ---------------------------------------------
        // 3. Render materials
        // ---------------------------------------------

        container.replaceChildren();

        if (materials.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <h3>No study materials available</h3>
                    <p>
                        Your faculty has not added any materials
                        for this course yet.
                    </p>
                </div>
            `;
            return;
        }

        materials.forEach(material => {
            const materialId = Number(material.id);
            const isCompleted = completedSet.has(materialId);

            const card = document.createElement("article");
            card.className = "material-card";

            // Material icon
            const icon = document.createElement("div");
            icon.className = "material-icon";
            icon.setAttribute("aria-hidden", "true");
            icon.textContent = getMaterialIcon(material.fileType);

            // Title
            const title = document.createElement("h3");
            title.className = "material-title";
            title.textContent = material.title || "Untitled Material";

            // Type
            const type = document.createElement("span");
            type.className = "material-type";
            type.textContent = material.fileType || "Study Material";

            // Description
            const description = document.createElement("p");
            description.className = "material-desc";
            description.textContent =
                material.description || "No description provided.";

            // Completion status
            const status = document.createElement("div");

            status.className = `material-status ${
                isCompleted ? "is-completed" : "is-pending"
            }`;

            const statusDot = document.createElement("span");
            statusDot.className = "status-dot";

            const statusText = document.createElement("span");
            statusText.textContent = isCompleted
                ? "Completed"
                : "Not completed";

            status.append(statusDot, statusText);

            // Actions
            const actions = document.createElement("div");
            actions.className = "material-actions";

            // -----------------------------------------
            // Open material button
            // -----------------------------------------

            const openButton = document.createElement("button");

            openButton.type = "button";
            openButton.className = "download-btn";
            openButton.textContent = "Open Material";

            // IMPORTANT:
            // Pass the complete material object.
            openButton.addEventListener("click", () => {
                openStudyMaterial(material);
            });

            actions.appendChild(openButton);

            // -----------------------------------------
            // Completion button
            // -----------------------------------------

            if (isCompleted) {
                const completedButton = document.createElement("button");

                completedButton.type = "button";
                completedButton.className =
                    "complete-btn completed-btn";

                completedButton.disabled = true;
                completedButton.textContent = "✓ Completed";

                actions.appendChild(completedButton);

            } else {
                const completeButton = document.createElement("button");

                completeButton.type = "button";
                completeButton.className = "complete-btn";
                completeButton.textContent = "Mark Complete";

                completeButton.addEventListener("click", () => {
                    completeMaterial(materialId, completeButton);
                });

                actions.appendChild(completeButton);
            }

            // Assemble card
            card.append(
                icon,
                title,
                type,
                description,
                status,
                actions
            );

            container.appendChild(card);
        });

    } catch (error) {
        console.error("Load Materials Error:", error);

        container.innerHTML = `
            <div class="empty-state error-state">
                <h3>Unable to load study materials</h3>
                <p>
                    Please check your connection and try again.
                    If the problem continues, contact your administrator.
                </p>
                <button
                    type="button"
                    class="download-btn retry-btn"
                    id="retryMaterialsBtn">
                    Try Again
                </button>
            </div>
        `;

        document
            .getElementById("retryMaterialsBtn")
            ?.addEventListener("click", loadMaterials);
    }
}


// =====================================================
// MATERIAL ICON
// =====================================================

function getMaterialIcon(fileType) {
    const type = String(fileType || "").toLowerCase();

    if (type.includes("pdf")) {
        return "PDF";
    }

    if (type.includes("video") || type.includes("mp4")) {
        return "▶";
    }

    if (type.includes("ppt") || type.includes("presentation")) {
        return "PPT";
    }

    if (type.includes("doc") || type.includes("word")) {
        return "DOC";
    }

    if (
        type.includes("image") ||
        type.includes("jpg") ||
        type.includes("png")
    ) {
        return "IMG";
    }

    return "FILE";
}


// =====================================================
// OPEN STUDY MATERIAL
// =====================================================

async function openStudyMaterial(material) {
    try {
        const fileUrl = getMaterialUrl(material.fileUrl);

        // Open external resources directly.
        if (/^https?:\/\//i.test(fileUrl) &&
            !fileUrl.startsWith(window.API_BASE_URL)) {
            window.open(fileUrl, "_blank", "noopener,noreferrer");
            return;
        }

        // Open files hosted by your Spring Boot backend.
        const token = localStorage.getItem("token");

        const response = await fetch(fileUrl, {
            method: "GET",
            headers: token
                ? { Authorization: `Bearer ${token}` }
                : {}
        });

        if (!response.ok) {
            throw new Error(`Unable to open material. HTTP ${response.status}`);
        }

        const blob = await response.blob();
        const blobUrl = URL.createObjectURL(blob);

        window.open(blobUrl, "_blank", "noopener,noreferrer");

        // Keep the object URL available while the browser opens the file.
        setTimeout(() => URL.revokeObjectURL(blobUrl), 60000);

    } catch (error) {
        console.error("Error opening material:", error);
        alert(error.message || "Unable to open the study material.");
    }
}


// =====================================================
// MARK MATERIAL AS COMPLETED
// =====================================================

async function completeMaterial(materialId, button) {
    const token = localStorage.getItem("token");

    if (!token) {
        alert("Please log in again.");
        return;
    }

    if (button) {
        button.disabled = true;
        button.textContent = "Saving...";
    }

    try {
        const response = await fetch(
            `${MATERIAL_PROGRESS_API}/complete/${encodeURIComponent(materialId)}`,
            {
                method: "POST",
                headers: {
                    Authorization: `Bearer ${token}`,
                    Accept: "application/json"
                }
            }
        );

        if (!response.ok) {
            const errorText = await response.text();

            throw new Error(
                `HTTP ${response.status}: ${errorText}`
            );
        }

        // Reload from the backend to confirm saved status.
        await loadMaterials();

    } catch (error) {
        console.error("Error completing material:", error);

        alert(
            "Unable to mark this material as completed. Please try again."
        );

        if (button) {
            button.disabled = false;
            button.textContent = "Mark Complete";
        }
    }
}


// =====================================================
// PAGE LOAD
// =====================================================

document.addEventListener("DOMContentLoaded", () => {
    loadMaterials();
});