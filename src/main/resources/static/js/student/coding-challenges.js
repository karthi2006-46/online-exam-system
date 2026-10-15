const API_BASE = "http://localhost:8080/api";

const studentId = localStorage.getItem("userId");
const token = localStorage.getItem("token");

// Course 2 = Spring Boot Masterclass.
// The selected course is read from localStorage when available.
const courseId = Number(
    localStorage.getItem("courseId") || 2
);

let challenges = [];
let completedChallengeIds = [];


// ========================================
// COMMON API REQUEST
// ========================================

async function apiRequest(url, options = {}) {

    const headers = {
        ...(options.headers || {})
    };

    if (token) {
        headers["Authorization"] = `Bearer ${token}`;
    }

    const response = await fetch(url, {
        ...options,
        headers
    });

    if (!response.ok) {

        const errorText = await response.text();

        throw new Error(
            errorText || `Request failed: ${response.status}`
        );
    }

    const text = await response.text();

    if (!text) {
        return null;
    }

    try {
        return JSON.parse(text);
    } catch {
        return text;
    }
}


// ========================================
// LOAD CODING CHALLENGES
// ========================================

async function loadChallenges() {

    const container = document.getElementById(
        "challengesContainer"
    );

    try {

        if (!studentId) {
            container.innerHTML = `
                <div class="empty">
                    Please log in again to access coding challenges.
                </div>
            `;
            return;
        }

        document.getElementById("courseTitle").textContent =
            courseId === 2
                ? "Spring Boot Masterclass"
                : `Course ${courseId}`;

        container.innerHTML = `
            <div class="empty">
                Loading coding challenges...
            </div>
        `;

        // Load all active challenges for the selected course.
        challenges = await apiRequest(
            `${API_BASE}/coding-challenges/course/${courseId}`
        );

        // Load the student's coding progress.
        const progressRecords = await apiRequest(
            `${API_BASE}/coding-challenge-progress/student/${studentId}`
        );

        completedChallengeIds = (progressRecords || [])
            .filter(item => item.completed === true)
            .map(item => Number(
                item.challenge?.id ?? item.challengeId
            ))
            .filter(id => Number.isFinite(id));

        renderChallenges();

        await updateEligibility();

    } catch (error) {

        console.error("Load Challenges Error:", error);

        container.innerHTML = `
            <div class="empty">
                <h3>Unable to load coding challenges</h3>
                <p>${escapeHtml(error.message)}</p>
                <p>Check the backend and browser console.</p>
            </div>
        `;
    }
}


// ========================================
// DISPLAY CHALLENGES
// ========================================

function renderChallenges() {

    const container = document.getElementById(
        "challengesContainer"
    );

    if (!Array.isArray(challenges) || challenges.length === 0) {

        container.innerHTML = `
            <div class="empty">
                <h3>No coding challenges found</h3>
                <p>
                    No active coding challenges are available
                    for Course ${courseId}.
                </p>
            </div>
        `;

        updateProgressDisplay();

        return;
    }

    container.innerHTML = challenges.map((challenge, index) => {

        const challengeId = Number(challenge.id);

        const completed =
            completedChallengeIds.includes(challengeId);

        const title =
            challenge.title ||
            challenge.name ||
            `Coding Challenge ${index + 1}`;

        const description =
            challenge.description ||
            challenge.problemStatement ||
            "No problem statement has been added yet.";

        const starterCode =
            challenge.starterCode ||
            challenge.templateCode ||
            "";

        return `
            <div class="challenge-card">

                <h2>
                    ${escapeHtml(title)}
                </h2>

                <span class="badge ${completed ? "completed" : ""}">
                    ${completed ? "COMPLETED" : "NOT COMPLETED"}
                </span>

                <div class="description">
                    ${escapeHtml(description)}
                </div>

                <label
                    class="editor-label"
                    for="code-${challengeId}">
                    Write Your Solution
                </label>

                <textarea
                    id="code-${challengeId}"
                    class="code-editor"
                    spellcheck="false"
                    placeholder="Write your solution here..."
                    ${completed ? "disabled" : ""}
                >${escapeHtml(starterCode)}</textarea>

                <div class="actions">

                    <button
                        class="btn-secondary"
                        onclick="copyCode(${challengeId})">
                        Copy Code
                    </button>

                    <button
                        class="btn-success"
                        id="complete-${challengeId}"
                        onclick="markChallengeCompleted(${challengeId})"
                        ${completed ? "disabled" : ""}>
                        ${completed ? "Completed ✓" : "Mark as Completed"}
                    </button>

                </div>

                <div
                    class="message"
                    id="message-${challengeId}">
                </div>

            </div>
        `;

    }).join("");

    updateProgressDisplay();
}


// ========================================
// MARK CHALLENGE COMPLETED
// ========================================

async function markChallengeCompleted(challengeId) {

    const button = document.getElementById(
        `complete-${challengeId}`
    );

    const message = document.getElementById(
        `message-${challengeId}`
    );

    try {

        if (!studentId) {
            alert("Please log in again.");
            return;
        }

        if (completedChallengeIds.includes(Number(challengeId))) {
            return;
        }

        const editor = document.getElementById(
            `code-${challengeId}`
        );

        if (!editor || !editor.value.trim()) {

            message.textContent =
                "Please write your solution before marking this challenge complete.";

            return;
        }

        const confirmed = confirm(
            "Have you finished this coding challenge? " +
            "Click OK to record it as completed."
        );

        if (!confirmed) {
            return;
        }

        button.disabled = true;
        button.textContent = "Saving...";

        await apiRequest(
            `${API_BASE}/coding-challenge-progress/complete/${challengeId}?studentId=${studentId}`,
            {
                method: "POST"
            }
        );

        completedChallengeIds.push(Number(challengeId));

        renderChallenges();

        await updateEligibility();

        alert("Coding challenge marked as completed.");

    } catch (error) {

        console.error("Complete Challenge Error:", error);

        message.textContent =
            `Unable to save completion: ${error.message}`;

        button.disabled = false;
        button.textContent = "Mark as Completed";
    }
}


// ========================================
// CHECK EXAM ELIGIBILITY
// ========================================

async function updateEligibility() {

    const eligibilityText = document.getElementById(
        "eligibilityText"
    );

    try {

        const eligibility = await apiRequest(
            `${API_BASE}/exam-eligibility/student/${studentId}/course/${courseId}`
        );

        console.log(
            "Exam Eligibility:",
            eligibility
        );

        if (eligibility.eligibleForExamApproval === true) {

            eligibilityText.textContent =
                "You have completed the required materials and coding challenges. You can request exam approval.";

        } else {

            const materialPercentage =
                eligibility.materialPercentage ?? 0;

            const codingPercentage =
                eligibility.codingPercentage ?? 0;

            eligibilityText.textContent =
                `Exam approval requirements not completed. ` +
                `Materials: ${materialPercentage}%. ` +
                `Coding challenges: ${codingPercentage}%.`;
        }

    } catch (error) {

        console.error("Eligibility Error:", error);

        eligibilityText.textContent =
            "Unable to check exam eligibility. Please refresh the page.";
    }
}


// ========================================
// UPDATE PROGRESS DISPLAY
// ========================================

function updateProgressDisplay() {

    const total = challenges.length;

    const completed = challenges.filter(challenge =>
        completedChallengeIds.includes(Number(challenge.id))
    ).length;

    const percentage = total > 0
        ? Math.round((completed / total) * 100)
        : 0;

    document.getElementById("completedCount").textContent =
        `${completed} / ${total}`;

    document.getElementById("progressText").textContent =
        `Coding Challenge Progress: ${percentage}%`;

    document.getElementById("progressFill").style.width =
        `${percentage}%`;
}


// ========================================
// COPY CODE
// ========================================

async function copyCode(challengeId) {

    const editor = document.getElementById(
        `code-${challengeId}`
    );

    try {

        await navigator.clipboard.writeText(editor.value);

        alert("Code copied.");

    } catch (error) {

        editor.select();
        document.execCommand("copy");

        alert("Code copied.");
    }
}


// ========================================
// HTML ESCAPING
// ========================================

function escapeHtml(value) {

    return String(value ?? "").replace(
        /[&<>"']/g,
        character => ({
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            '"': "&quot;",
            "'": "&#039;"
        })[character]
    );
}


// ========================================
// LOGOUT
// ========================================

function logout() {

    localStorage.removeItem("token");
    localStorage.removeItem("userId");
    localStorage.removeItem("courseId");

    window.location.href = "../login.html";
}


// ========================================
// START
// ========================================

loadChallenges();