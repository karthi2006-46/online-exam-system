
const API_BASE = `${window.API_BASE_URL}/api`;

const studentId = localStorage.getItem("userId");
const token = localStorage.getItem("token");
const courseId = Number(localStorage.getItem("courseId") || 2);

let challenges = [];
let completedChallengeIds = [];

// ========================================
// COMMON API REQUEST
// ========================================

async function apiRequest(url, options = {}) {
    const headers = { ...(options.headers || {}) };

    if (token) {
        headers.Authorization = `Bearer ${token}`;
    }

    const response = await fetch(url, { ...options, headers });
    const responseText = await response.text();

    if (!response.ok) {
        throw new Error(
            responseText || `Request failed: ${response.status}`
        );
    }

    if (!responseText) return null;

    try {
        return JSON.parse(responseText);
    } catch {
        return responseText;
    }
}

// ========================================
// LOAD CODING CHALLENGES
// ========================================

async function loadChallenges() {
    const container = document.getElementById("challengesContainer");

    try {
        if (!studentId) {
            container.innerHTML = `
                <div class="empty">
                    <h3>Login Required</h3>
                    <p>Please log in again to access coding challenges.</p>
                </div>`;
            return;
        }

        document.getElementById("courseTitle").textContent =
            courseId === 2
                ? "Spring Boot Masterclass"
                : `Course ${courseId}`;

        container.innerHTML = `
            <div class="empty">Loading coding challenges...</div>`;

        const [challengeData, progressRecords] = await Promise.all([
            apiRequest(
                `${API_BASE}/coding-challenges/course/${courseId}`
            ),
            apiRequest(
                `${API_BASE}/coding-challenge-progress/student/${studentId}`
            )
        ]);

        challenges = Array.isArray(challengeData)
            ? challengeData
            : [];

        completedChallengeIds = (
            Array.isArray(progressRecords) ? progressRecords : []
        )
            .filter(item => item.completed === true)
            .map(item =>
                Number(item.challenge?.id ?? item.challengeId)
            )
            .filter(Number.isFinite);

        renderChallenges();
        await updateEligibility();

    } catch (error) {
        console.error("Load Challenges Error:", error);

        container.innerHTML = `
            <div class="empty">
                <h3>Unable to Load Coding Challenges</h3>
                <p>${escapeHtml(error.message)}</p>
                <p>Check the backend and refresh the page.</p>
            </div>`;
    }
}

// ========================================
// DISPLAY CODING CHALLENGES
// ========================================

function renderChallenges() {
    const container = document.getElementById("challengesContainer");

    if (!challenges.length) {
        container.innerHTML = `
            <div class="empty">
                <h3>No Coding Challenges Found</h3>
                <p>No active coding challenges are available
                   for Course ${courseId}.</p>
            </div>`;

        updateProgressDisplay();
        return;
    }

    container.innerHTML = challenges.map((challenge, index) => {
        const id = Number(challenge.id);

        const completed = completedChallengeIds.includes(id);

        const title = challenge.title ||
            challenge.name ||
            `Coding Challenge ${index + 1}`;

        const description = challenge.description || "";

        const problem = challenge.problemStatement ||
            description ||
            "No problem statement has been added yet.";

        const starterCode = challenge.starterCode ||
            challenge.templateCode ||
            "";

        const difficulty = String(
            challenge.difficulty || "EASY"
        ).toUpperCase();

        const difficultyClass =
            ["EASY", "MEDIUM", "HARD"].includes(difficulty)
                ? difficulty.toLowerCase()
                : "easy";

        return `
            <article class="challenge-card">

                <div class="challenge-card-header">
                    <div>
                        <div class="challenge-kicker">
                            CHALLENGE ${index + 1}
                        </div>

                        <h2>${escapeHtml(title)}</h2>
                    </div>

                    <div class="challenge-badges">
                        <span class="difficulty-badge difficulty-${difficultyClass}">
                            ${escapeHtml(difficulty)}
                        </span>

                        <span class="marks-badge">
                            ${escapeHtml(challenge.marks ?? 0)} Marks
                        </span>
                    </div>
                </div>

                <div class="challenge-status ${
                    completed
                        ? "status-completed"
                        : "status-pending"
                }">
                    ${completed
                        ? "✓ Completed"
                        : "○ Not completed"}
                </div>

                ${
                    description && description !== problem
                    ? `
                        <section class="challenge-section">
                            <h3>Overview</h3>
                            <p class="challenge-description">
                                ${escapeHtml(description)}
                            </p>
                        </section>
                    `
                    : ""
                }

                <section class="challenge-section">
                    <h3>Problem Statement</h3>
                    <p class="challenge-description">
                        ${escapeHtml(problem)}
                    </p>
                </section>

                ${
                    challenge.sampleInput
                    ? `
                        <section class="challenge-section">
                            <h3>Sample Input</h3>
                            <pre class="sample-code">${escapeHtml(
                                challenge.sampleInput
                            )}</pre>
                        </section>
                    `
                    : ""
                }

                ${
                    challenge.sampleOutput || challenge.expectedOutput
                    ? `
                        <section class="challenge-section">
                            <h3>Expected Output</h3>
                            <pre class="sample-code">${escapeHtml(
                                challenge.sampleOutput ||
                                challenge.expectedOutput
                            )}</pre>
                        </section>
                    `
                    : ""
                }

                ${
                    challenge.constraints
                    ? `
                        <section class="challenge-section">
                            <h3>Constraints</h3>
                            <p class="challenge-description">
                                ${escapeHtml(challenge.constraints)}
                            </p>
                        </section>
                    `
                    : ""
                }

                <label class="editor-label" for="code-${id}">
                    Write Your Solution
                </label>

                <textarea
                    id="code-${id}"
                    class="code-editor"
                    spellcheck="false"
                    autocapitalize="off"
                    autocomplete="off"
                    placeholder="Write your solution here..."
                    ${completed ? "disabled" : ""}
                >${escapeHtml(starterCode)}</textarea>

                <div class="actions">
                    <button
                        type="button"
                        class="btn-secondary"
                        onclick="copyCode(${id})">
                        Copy Code
                    </button>

                    <button
                        type="button"
                        class="btn-success"
                        id="complete-${id}"
                        onclick="markChallengeCompleted(${id})"
                        ${completed ? "disabled" : ""}>
                        ${completed
                            ? "Completed ✓"
                            : "Mark as Completed"}
                    </button>
                </div>

                <p
                    class="message"
                    id="message-${id}"
                    role="status"
                    aria-live="polite">
                </p>

                ${
                    !completed
                    ? `
                        <p class="completion-note">
                            Marking this complete records your progress.
                            It does not execute or validate your code.
                        </p>
                    `
                    : ""
                }

            </article>
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

            message.className = "message message-error";
            return;
        }

        const confirmed = confirm(
            "Have you finished this coding challenge? " +
            "Click OK to record it as completed."
        );

        if (!confirmed) return;

        button.disabled = true;
        button.textContent = "Saving...";

        await apiRequest(
            `${API_BASE}/coding-challenge-progress/complete/` +
            `${challengeId}?studentId=${encodeURIComponent(studentId)}`,
            { method: "POST" }
        );

        completedChallengeIds.push(Number(challengeId));

        renderChallenges();
        await updateEligibility();

    } catch (error) {
        console.error("Complete Challenge Error:", error);

        if (message) {
            message.textContent =
                `Unable to save completion: ${error.message}`;

            message.className = "message message-error";
        }

        if (button) {
            button.disabled = false;
            button.textContent = "Mark as Completed";
        }
    }
}

// ========================================
// CHECK EXAM ELIGIBILITY
// ========================================

async function updateEligibility() {
    const eligibilityText = document.getElementById(
        "eligibilityText"
    );

    if (!eligibilityText || !studentId) return;

    try {
        const eligibility = await apiRequest(
            `${API_BASE}/exam-eligibility/student/` +
            `${encodeURIComponent(studentId)}/course/${courseId}`
        );

        if (eligibility?.eligibleForExamApproval === true) {
            eligibilityText.textContent =
                "You have completed the required materials and coding challenges. " +
                "You can request exam approval.";
        } else {
            const materialPercentage =
                eligibility?.materialPercentage ?? 0;

            const codingPercentage =
                eligibility?.codingPercentage ?? 0;

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

    const count = document.getElementById("completedCount");
    const progressText = document.getElementById("progressText");
    const progressFill = document.getElementById("progressFill");

    if (count) {
        count.textContent = `${completed} / ${total}`;
    }

    if (progressText) {
        progressText.textContent =
            `Coding Challenge Progress: ${percentage}%`;
    }

    if (progressFill) {
        progressFill.style.width = `${percentage}%`;
        progressFill.setAttribute(
            "aria-valuenow",
            String(percentage)
        );
    }
}

// ========================================
// COPY CODE
// ========================================

async function copyCode(challengeId) {
    const editor = document.getElementById(
        `code-${challengeId}`
    );

    if (!editor) return;

    try {
        await navigator.clipboard.writeText(editor.value);

        showMessage(
            challengeId,
            "Code copied to clipboard.",
            false
        );

    } catch {
        editor.focus();
        editor.select();

        const copied = document.execCommand("copy");

        showMessage(
            challengeId,
            copied
                ? "Code copied to clipboard."
                : "Unable to copy automatically. Select and copy the code manually.",
            !copied
        );
    }
}

function showMessage(challengeId, text, isError) {
    const message = document.getElementById(
        `message-${challengeId}`
    );

    if (!message) return;

    message.textContent = text;
    message.className = isError
        ? "message message-error"
        : "message message-success";
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
