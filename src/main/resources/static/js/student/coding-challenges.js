const API_BASE = `${window.API_BASE_URL}/api`;

const studentId = localStorage.getItem("userId");
const token = localStorage.getItem("token");
const courseId = Number(
    new URLSearchParams(window.location.search).get("courseId") ||
    localStorage.getItem("courseId")
);

let challenges = [];
let submissionsByChallenge = {};
let completedChallengeIds = [];

// Which challenge cards are expanded. Kept across re-renders.
const openIds = new Set();
let openInitialised = false;

const CHECK_ICON = `<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12.5l4.5 4.5L19 7.5"/></svg>`;
const CHEVRON_ICON = `<svg class="chevron" viewBox="0 0 24 24" aria-hidden="true"><path d="M6 9l6 6 6-6"/></svg>`;


// ========================================
// COMMON API REQUEST
// ========================================

async function apiRequest(url, options = {}) {
    const headers = { ...(options.headers || {}) };

    if (token) headers.Authorization = `Bearer ${token}`;

    if (options.body && !headers["Content-Type"]) {
        headers["Content-Type"] = "application/json";
    }

    const response = await fetch(url, { ...options, headers });
    const responseText = await response.text();

    if (!response.ok) {
        let errorMessage = responseText;

        try {
            const errorData = JSON.parse(responseText);
            errorMessage = errorData.message || errorData.error || responseText;
        } catch {
            // Keep original response text.
        }

        throw new Error(errorMessage || `Request failed: ${response.status}`);
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
        if (!token || !studentId) {
            container.innerHTML = `
                <div class="empty">
                    <h3>Login required</h3>
                    <p>Please log in again to access coding challenges.</p>
                </div>`;
            return;
        }

document.getElementById("courseTitle").textContent =
    localStorage.getItem("courseTitle") || `Course ${courseId}`;

            if (!Number.isInteger(courseId) || courseId <= 0) {
    container.innerHTML = `
        <div class="empty">
            <h3>No course selected</h3>
            <p>Please open this page from My Courses.</p>
        </div>`;
    return;
}

        container.setAttribute("aria-busy", "true");
        container.innerHTML = `<div class="empty"><p>Loading coding challenges...</p></div>`;

        const challengeData = await apiRequest(`${API_BASE}/coding-challenges/course/${courseId}`);

        challenges = Array.isArray(challengeData) ? challengeData : [];

        // Submissions and eligibility are independent, so load them together.
        await Promise.all([loadAllSubmissions(), updateEligibility()]);

        // Open the first challenge that still needs a submission (once).
        if (!openInitialised) {
            const next = challenges.find(c => !submissionsByChallenge[Number(c.id)]);
            if (next) openIds.add(Number(next.id));
            openInitialised = true;
        }

        renderChallenges();

    } catch (error) {
        console.error("Load Challenges Error:", error);

        container.innerHTML = `
            <div class="empty">
                <h3>Unable to load coding challenges</h3>
                <p>${escapeHtml(error.message)}</p>
                <p>Check your connection and refresh the page.</p>
            </div>`;
    } finally {
        container.setAttribute("aria-busy", "false");
    }
}


// ========================================
// LOAD STUDENT SUBMISSIONS
// ========================================

async function loadAllSubmissions() {
    submissionsByChallenge = {};

    const results = await Promise.allSettled(
        challenges.map(async challenge => {
            const challengeId = Number(challenge.id);

            const data = await apiRequest(
                `${API_BASE}/coding-submissions/student/challenge/${challengeId}`
            );

            const submissions = Array.isArray(data) ? data : data ? [data] : [];

            submissionsByChallenge[challengeId] = submissions.length > 0 ? submissions[0] : null;
        })
    );

    results.forEach((result, index) => {
        if (result.status === "rejected") {
            console.error(`Unable to load submission for challenge ${challenges[index].id}:`, result.reason);

            // A failed history request must not unlock the editor.
            submissionsByChallenge[Number(challenges[index].id)] = {
                status: "LOAD_ERROR",
                error: result.reason.message
            };
        }
    });

    recomputeCompleted();
}

// Only APPROVED submissions count as completed.
function recomputeCompleted() {
    completedChallengeIds = Object.entries(submissionsByChallenge)
        .filter(([, s]) => s && String(s.status).toUpperCase() === "APPROVED")
        .map(([id]) => Number(id));
}


// ========================================
// STEP TRACKER
// 1 Write & submit code  2 Faculty review  3 Approved
// ========================================

function renderSteps(status) {
    const map = {
        NOT_SUBMITTED:  [["is-current", "Write your solution, then submit"],  ["is-locked", "Not started"],  ["is-locked", "Not approved"]],
        PENDING_REVIEW: [["is-done", "Submitted"],                            ["is-pending", "Pending"],     ["is-locked", "Not approved"]],
        APPROVED:       [["is-done", "Submitted"],                            ["is-done", "Reviewed"],       ["is-done", "Approved"]],
        REJECTED:       [["is-done", "Submitted"],                            ["is-error", "Rejected"],      ["is-locked", "Not approved"]],
        LOAD_ERROR:     [["is-locked", "Unavailable"],                        ["is-locked", "Unavailable"],  ["is-locked", "Unavailable"]]
    };

    const titles = ["Write code & submit", "Faculty review", "Approved"];
    const rows = map[status] || map.NOT_SUBMITTED;

    return rows.map(([state, note], i) => {
        const marker = state === "is-done" ? CHECK_ICON : i + 1;
        const current = state === "is-current" || state === "is-pending" ? ' aria-current="step"' : "";

        return `
            <li class="step ${state}"${current}>
                <span class="step-marker">${marker}</span>
                <span class="step-text">
                    <span class="step-title">${titles[i]}</span>
                    <span class="step-note">${escapeHtml(note)}</span>
                </span>
            </li>`;
    }).join("");
}


// ========================================
// DISPLAY CODING CHALLENGES
// ========================================

function renderChallenges() {
    const container = document.getElementById("challengesContainer");

    if (!challenges.length) {
        container.innerHTML = `
            <div class="empty">
                <h3>No coding challenges yet</h3>
                <p>No active coding challenges are available for this course.</p>
            </div>`;

        updateProgressDisplay();
        return;
    }

    container.innerHTML = challenges.map((challenge, index) => challengeCardHTML(challenge, index)).join("");

    updateProgressDisplay();
}

function challengeCardHTML(challenge, index) {
    const id = Number(challenge.id);
    const submission = submissionsByChallenge[id];

    const status = submission
        ? String(submission.status || "").toUpperCase()
        : "NOT_SUBMITTED";

    const pending = status === "PENDING_REVIEW";
    const approved = status === "APPROVED";
    const rejected = status === "REJECTED";
    const loadError = status === "LOAD_ERROR";
    const alreadySubmitted = pending || approved || rejected;
    const editorDisabled = alreadySubmitted || loadError;

    const title = challenge.title || challenge.name || `Coding Challenge ${index + 1}`;
    const description = challenge.description || "";
    const problem = challenge.problemStatement || description || "No problem statement has been added yet.";
    const starterCode = challenge.starterCode || challenge.templateCode || "";

    const difficulty = String(challenge.difficulty || "EASY").toUpperCase();
    const difficultyClass = ["EASY", "MEDIUM", "HARD"].includes(difficulty) ? difficulty.toLowerCase() : "easy";

    const sampleInput = challenge.sampleInput || "";
    const sampleOutput = challenge.sampleOutput || challenge.expectedOutput || "";

    const feedback = submission?.facultyFeedback || "";
    const submittedAt = submission?.submittedAt ? formatDate(submission.submittedAt) : "";

    const isOpen = openIds.has(id);

    const submitLabel = pending ? "Pending review"
        : approved ? "Approved"
        : rejected ? "Rejected"
        : loadError ? "Status unavailable"
        : "Submit for review";

    return `
        <article class="challenge-card ${isOpen ? "is-open" : ""}" data-id="${id}">

            <button type="button" class="challenge-toggle" data-toggle="${id}"
                    aria-expanded="${isOpen}" aria-controls="body-${id}">
                <span class="challenge-title-wrap">
                    <span class="challenge-index">${index + 1}</span>
                    <h2 class="challenge-title">${escapeHtml(title)}</h2>
                </span>
                <span class="challenge-badges">
                    <span class="difficulty-badge difficulty-${difficultyClass}">${escapeHtml(difficulty)}</span>
                    <span class="marks-badge">${escapeHtml(challenge.marks ?? 0)} marks</span>
                    ${CHEVRON_ICON}
                </span>
            </button>

            <ol class="steps" aria-label="Progress for ${escapeHtml(title)}">
                ${renderSteps(status)}
            </ol>

            <div class="challenge-body" id="body-${id}" ${isOpen ? "" : "hidden"}>

                ${description && description !== problem ? `
                    <section class="challenge-section">
                        <h3>Overview</h3>
                        <p class="challenge-description">${escapeHtml(description)}</p>
                    </section>` : ""}

                <section class="challenge-section">
                    <h3>Problem statement</h3>
                    <p class="challenge-description">${escapeHtml(problem)}</p>
                </section>

                ${sampleInput || sampleOutput ? `
                    <div class="io-grid ${sampleInput && sampleOutput ? "" : "single"}">
                        ${sampleInput ? `
                            <section class="challenge-section">
                                <h3>Sample input</h3>
                                <pre class="sample-code">${escapeHtml(sampleInput)}</pre>
                            </section>` : ""}
                        ${sampleOutput ? `
                            <section class="challenge-section">
                                <h3>Expected output</h3>
                                <pre class="sample-code">${escapeHtml(sampleOutput)}</pre>
                            </section>` : ""}
                    </div>` : ""}

                ${challenge.constraints ? `
                    <section class="challenge-section">
                        <h3>Constraints</h3>
                        <p class="challenge-description">${escapeHtml(challenge.constraints)}</p>
                    </section>` : ""}

                <div class="terminal">
                    <div class="terminal-bar">
                        <span class="terminal-dots" aria-hidden="true"><i></i><i></i><i></i></span>
                        <label class="terminal-name" for="code-${id}">Solution.java</label>
                        <button type="button" class="copy-btn" data-copy="${id}">Copy</button>
                    </div>
                    <textarea
                        id="code-${id}"
                        class="code-editor"
                        spellcheck="false"
                        autocapitalize="off"
                        autocomplete="off"
                        placeholder="Write your solution here..."
                        ${editorDisabled ? "disabled" : ""}
                    >${escapeHtml(alreadySubmitted ? submission.sourceCode || "" : starterCode)}</textarea>
                </div>

                ${alreadySubmitted ? `
                    <div class="submission-feedback">
                        <p><strong>Language:</strong> ${escapeHtml(submission.language || "Not specified")}
                           ${submittedAt ? ` &nbsp;|&nbsp; <strong>Submitted:</strong> ${escapeHtml(submittedAt)}` : ""}</p>
                        <p><strong>Faculty feedback:</strong>
                           ${feedback ? escapeHtml(feedback) : "None yet."}</p>
                        ${rejected ? `<p>This submission was rejected. Resubmission is not allowed.</p>` : ""}
                    </div>` : ""}

                ${loadError ? `
                    <div class="submission-feedback">
                        <p>We could not verify your submission status. Refresh the page to try again.</p>
                    </div>` : ""}

                <div class="actions">
                    <button type="button" class="btn-success" id="submit-${id}" data-submit="${id}"
                            ${editorDisabled ? "disabled" : ""}>${submitLabel}</button>
                    ${!alreadySubmitted && !loadError ? `
                        <p class="hint">You can submit once. After that, your code cannot be edited.</p>` : ""}
                </div>

                <p class="message" id="message-${id}" role="status" aria-live="polite"></p>

            </div>
        </article>`;
}


// ========================================
// EVENTS (delegated, so re-renders need no re-binding)
// ========================================

document.addEventListener("DOMContentLoaded", () => {
    const container = document.getElementById("challengesContainer");
    const refresh = document.getElementById("refreshBtn");

    if (refresh) refresh.addEventListener("click", loadChallenges);

    if (!container) return;

    container.addEventListener("click", event => {
        const toggle = event.target.closest("[data-toggle]");
        const copy = event.target.closest("[data-copy]");
        const submit = event.target.closest("[data-submit]");

        if (toggle) toggleCard(Number(toggle.dataset.toggle));
        else if (copy) copyCode(Number(copy.dataset.copy));
        else if (submit && !submit.disabled) submitCodingSolution(Number(submit.dataset.submit));
    });
});

function toggleCard(id) {
    const card = document.querySelector(`.challenge-card[data-id="${id}"]`);
    if (!card) return;

    const body = card.querySelector(".challenge-body");
    const button = card.querySelector("[data-toggle]");
    const willOpen = body.hasAttribute("hidden");

    body.toggleAttribute("hidden", !willOpen);
    card.classList.toggle("is-open", willOpen);
    button.setAttribute("aria-expanded", String(willOpen));

    if (willOpen) openIds.add(id); else openIds.delete(id);
}


// ========================================
// SUBMIT CODING SOLUTION
// ========================================

async function submitCodingSolution(challengeId) {
    const button = document.getElementById(`submit-${challengeId}`);
    const editor = document.getElementById(`code-${challengeId}`);

    if (!button || !editor) return;

    try {
        if (!token) throw new Error("Please log in again.");

        if (submissionsByChallenge[challengeId]) {
            throw new Error("You have already submitted this challenge.");
        }

        if (!editor.value.trim()) {
            showMessage(challengeId, "Please write your solution before submitting.", true);
            return;
        }

        const confirmed = confirm(
            "Are you sure you want to submit your solution?\n\n" +
            "After submission, you cannot edit or resubmit your code."
        );

        if (!confirmed) return;

        button.disabled = true;
        button.textContent = "Submitting...";

        const result = await apiRequest(`${API_BASE}/coding-submissions/submit`, {
            method: "POST",
            body: JSON.stringify({
                challengeId: challengeId,
                language: "JAVA",
                sourceCode: editor.value
            })
        });

        submissionsByChallenge[challengeId] = result || {
            status: "PENDING_REVIEW",
            sourceCode: editor.value,
            language: "JAVA"
        };

        // Update the page from local state, without reloading every challenge.
        recomputeCompleted();
        renderChallenges();
        updateEligibility();

        showMessage(challengeId, "Submitted. Waiting for faculty review.", false);

    } catch (error) {
        console.error("Submit Coding Solution Error:", error);

        showMessage(challengeId, error.message || "Unable to submit your solution.", true);

        button.disabled = false;
        button.textContent = "Submit for review";
    }
}


// ========================================
// CHECK EXAM ELIGIBILITY
// ========================================

async function updateEligibility() {
    const eligibilityText = document.getElementById("eligibilityText");

    if (!eligibilityText || !studentId) return;

    try {
        const eligibility = await apiRequest(
            `${API_BASE}/exam-eligibility/student/${encodeURIComponent(studentId)}/course/${courseId}`
        );

        if (eligibility?.eligibleForExamApproval === true) {
            eligibilityText.textContent =
                "All requirements complete. You can request exam approval from My Courses.";
        } else {
            eligibilityText.textContent =
                `Exam approval needs 100% of both. Materials: ${eligibility?.materialPercentage ?? 0}%. ` +
                `Coding challenges: ${eligibility?.codingPercentage ?? 0}%.`;
        }

    } catch (error) {
        console.error("Eligibility Error:", error);
        eligibilityText.textContent = "Unable to check exam eligibility. Please refresh the page.";
    }
}


// ========================================
// UPDATE PROGRESS DISPLAY
// ========================================

function updateProgressDisplay() {
    const total = challenges.length;
    const completed = completedChallengeIds.length;
    const percentage = total > 0 ? Math.round((completed / total) * 100) : 0;

    const count = document.getElementById("completedCount");
    const fill = document.getElementById("progressFill");
    const track = document.getElementById("progressTrack");

    if (count) count.textContent = `${completed} / ${total}`;
    if (fill) fill.style.width = `${percentage}%`;
    if (track) track.setAttribute("aria-valuenow", String(percentage));
}


// ========================================
// COPY CODE
// ========================================

async function copyCode(challengeId) {
    const editor = document.getElementById(`code-${challengeId}`);

    if (!editor) return;

    try {
        await navigator.clipboard.writeText(editor.value);
        showMessage(challengeId, "Code copied.", false);
    } catch {
        editor.focus();
        editor.select();

        const copied = document.execCommand("copy");

        showMessage(
            challengeId,
            copied ? "Code copied." : "Unable to copy automatically. Select and copy the code manually.",
            !copied
        );
    }
}


// ========================================
// HELPERS
// ========================================

function showMessage(challengeId, messageText, isError) {
    const message = document.getElementById(`message-${challengeId}`);

    if (!message) return;

    message.textContent = messageText;
    message.className = isError ? "message message-error" : "message message-success";
}

function formatDate(dateValue) {
    const date = new Date(dateValue);

    return Number.isNaN(date.getTime()) ? String(dateValue) : date.toLocaleString();
}

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