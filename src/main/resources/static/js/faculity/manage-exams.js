const API_BASE = `${window.API_BASE_URL}/api`;

let assignedCourses = [];
let exams = [];

let activeQuestionExam = null;
let currentQuestions = [];

// ==============================
// AUTHENTICATION
// ==============================

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

// ==============================
// API REQUEST
// ==============================

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

        throw new Error(
            message || `Request failed: ${response.status}`
        );
    }

    return body;
}

// ==============================
// HELPER FUNCTIONS
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

    const element = document.getElementById("examMessage");

    element.textContent = message;
    element.hidden = false;
}

// ==============================
// CREATE EXAM MODAL
// ==============================

function closeModal() {

    document.getElementById("examModal")
        .classList.remove("open");
}

function openModal() {

    document.getElementById("formError").textContent = "";

    document.getElementById("examForm").reset();

    const queryCourseId =
        new URLSearchParams(location.search).get("courseId");

    if (
        queryCourseId &&
        assignedCourses.some(
            course => String(course.id) === queryCourseId
        )
    ) {
        document.getElementById("courseId").value =
            queryCourseId;
    }

    document.getElementById("examModal")
        .classList.add("open");
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

    const response = await apiRequest(
        "/courses/my-courses"
    );

    assignedCourses = unwrapArray(response);

    populateCourseSelects();
}

// ==============================
// LOAD EXAMS
// ==============================

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

                    courseTitle:
                        course.title || "Untitled Course"
                }));
            })
        );

        exams = results.flat();

        renderExams();

    } catch (error) {

        console.error("Failed to load exams:", error);

        showMessage(
            error.message ||
            "Unable to load exams. Check your API access and server logs."
        );
    }
}

// ==============================
// DISPLAY EXAMS
// ==============================

function renderExams() {

    const grid = document.getElementById("examGrid");
    const message = document.getElementById("examMessage");

    const query = document.getElementById("examSearch")
        .value.trim().toLowerCase();

    const courseFilter =
        document.getElementById("courseFilter").value;

    const statusFilter =
        document.getElementById("statusFilter").value;

    const filtered = exams.filter(exam => {

        const matchesSearch =
            String(exam.title || "")
                .toLowerCase().includes(query) ||

            String(exam.description || "")
                .toLowerCase().includes(query) ||

            String(exam.courseTitle || "")
                .toLowerCase().includes(query);

        const matchesCourse =
            !courseFilter ||
            String(exam.courseId) === courseFilter;

        const isPublished = exam.published === true;

        const matchesStatus =
            !statusFilter ||
            (statusFilter === "published" && isPublished) ||
            (statusFilter === "draft" && !isPublished);

        return matchesSearch &&
            matchesCourse &&
            matchesStatus;
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

        const statusLabel =
            published ? "Published" : "Draft";

        card.innerHTML = `

            <span class="status-badge ${published ? "published" : "draft"}">
                ${statusLabel}
            </span>

            <h2>
                ${escapeHTML(exam.title || "Untitled Exam")}
            </h2>

            <p class="exam-description">
                ${escapeHTML(exam.description || "No description available.")}
            </p>

            <p class="muted" style="margin-top:12px">
                Course: ${escapeHTML(exam.courseTitle)}
            </p>

            <div class="exam-stats">

                <div>
                    <small>Duration</small>
                    <strong>
                        ${escapeHTML(exam.durationMinutes ?? "—")} min
                    </strong>
                </div>

                <div>
                    <small>Questions</small>
                    <strong>
                        ${escapeHTML(exam.totalQuestions ?? "—")}
                    </strong>
                </div>

                <div>
                    <small>Total Marks</small>
                    <strong>
                        ${escapeHTML(exam.totalMarks ?? "—")}
                    </strong>
                </div>

                <div>
                    <small>Passing Marks</small>
                    <strong>
                        ${escapeHTML(exam.passingMarks ?? "—")}
                    </strong>
                </div>

            </div>

            <div class="exam-actions">

                <button
                    type="button"
                    class="secondary-button"
                    data-action="questions"
                    data-id="${escapeHTML(exam.id)}">

                    Manage Questions

                </button>

                <button
                    type="button"
                    class="secondary-button"
                    data-action="${published ? "unpublish" : "publish"}"
                    data-id="${escapeHTML(exam.id)}">

                    ${published ? "Unpublish" : "Publish"}

                </button>

                <button
                    type="button"
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

// ==============================
// CREATE EXAM
// ==============================

async function createExam(event) {

    event.preventDefault();

    const errorElement =
        document.getElementById("formError");

    const saveButton =
        document.getElementById("saveExamButton");

    errorElement.textContent = "";

    const courseId =
        document.getElementById("courseId").value;

    const title =
        document.getElementById("title").value.trim();

    const description =
        document.getElementById("description").value.trim();

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

        errorElement.textContent =
            "Please select a course and enter a title.";

        return;
    }

    if (
        !Number.isInteger(totalQuestions) ||
        totalQuestions < 1 ||

        !Number.isInteger(durationMinutes) ||
        durationMinutes < 1 ||

        !Number.isInteger(totalMarks) ||
        totalMarks < 1 ||

        !Number.isInteger(passingMarks) ||
        passingMarks < 0 ||
        passingMarks > totalMarks
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

        errorElement.textContent =
            error.message || "Unable to create exam.";

    } finally {

        saveButton.disabled = false;

        saveButton.textContent = "Create Exam";
    }
}

// ==============================
// PUBLISH / UNPUBLISH / DELETE
// ==============================

async function handleExamAction(action, examId) {

    const exam = exams.find(
        item => String(item.id) === String(examId)
    );

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

        if (!confirm(`Delete exam "${exam.title}"?`)) {
            return;
        }

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

        alert(
            error.message || "Unable to complete the action."
        );
    }
}

// ==============================
// MANAGE QUESTIONS
// ==============================

function openQuestionsModal(examId) {

    activeQuestionExam = exams.find(
        item => String(item.id) === String(examId)
    );

    if (!activeQuestionExam) return;

    document.getElementById("questionsExamTitle").textContent =
        `${activeQuestionExam.title || "Exam"} · ${activeQuestionExam.courseTitle || "Course"}`;

    document.getElementById("questionsModal")
        .classList.add("open");

    resetQuestionForm();

    loadQuestions();
}

function closeQuestionsModal() {

    document.getElementById("questionsModal")
        .classList.remove("open");

    activeQuestionExam = null;

    currentQuestions = [];
}

// ==============================
// RESET QUESTION FORM
// ==============================

function resetQuestionForm() {

    document.getElementById("questionForm").reset();

    document.getElementById("questionId").value = "";

    document.getElementById("questionFormError").textContent = "";

    document.getElementById("questionFormTitle").textContent =
        "Add a question";

    document.getElementById("saveQuestionButton").textContent =
        "Add Question";
}

// ==============================
// LOAD QUESTIONS
// ==============================

async function loadQuestions() {

    const message =
        document.getElementById("questionsMessage");

    const list =
        document.getElementById("questionList");

    if (!activeQuestionExam) return;

    list.replaceChildren();

    message.textContent = "Loading questions...";

    try {

        const response = await apiRequest(
            `/questions/exam/${encodeURIComponent(activeQuestionExam.id)}`
        );

        currentQuestions = unwrapArray(response);

        renderQuestions();

    } catch (error) {

        console.error("Failed to load questions:", error);

        message.textContent =
            error.message || "Unable to load questions.";
    }
}

// ==============================
// DISPLAY QUESTIONS
// ==============================

function renderQuestions() {

    const list =
        document.getElementById("questionList");

    const message =
        document.getElementById("questionsMessage");

    list.replaceChildren();

    message.textContent =
        `${currentQuestions.length} question(s) added. ` +
        `Exam target: ${activeQuestionExam?.totalQuestions ?? "—"} questions, ` +
        `${activeQuestionExam?.totalMarks ?? "—"} total marks.`;

    if (!currentQuestions.length) {

        const empty = document.createElement("div");

        empty.className = "state-message";

        empty.textContent =
            "No questions added yet. Use the form below to add your first MCQ.";

        list.appendChild(empty);

        return;
    }

    currentQuestions
        .slice()
        .sort(
            (a, b) =>
                Number(a.questionNumber || 0) -
                Number(b.questionNumber || 0)
        )
        .forEach(question => {

            const item = document.createElement("article");

            item.className = "question-item";

            item.innerHTML = `

                <h3>
                    Q${escapeHTML(question.questionNumber ?? "—")}.
                    ${escapeHTML(question.questionText)}
                </h3>

                <div class="question-options">

                    <div>
                        A. ${escapeHTML(question.optionA)}
                    </div>

                    <div>
                        B. ${escapeHTML(question.optionB)}
                    </div>

                    <div>
                        C. ${escapeHTML(question.optionC)}
                    </div>

                    <div>
                        D. ${escapeHTML(question.optionD)}
                    </div>

                </div>

                <p class="question-summary">
                    Correct answer:
                    <strong>
                        ${escapeHTML(question.correctAnswer || "—")}
                    </strong>

                    · Marks:

                    <strong>
                        ${escapeHTML(question.marks ?? "—")}
                    </strong>
                </p>

                <div class="question-item-actions">

                    <button
                        type="button"
                        class="secondary-button"
                        data-question-action="edit"
                        data-id="${escapeHTML(question.id)}">

                        Edit

                    </button>

                    <button
                        type="button"
                        class="danger-button"
                        data-question-action="delete"
                        data-id="${escapeHTML(question.id)}">

                        Delete

                    </button>

                </div>
            `;

            list.appendChild(item);
        });
}

// ==============================
// EDIT QUESTION
// ==============================

function editQuestion(questionId) {

    const question = currentQuestions.find(
        item => String(item.id) === String(questionId)
    );

    if (!question) return;

    document.getElementById("questionId").value =
        question.id;

    document.getElementById("questionText").value =
        question.questionText || "";

    document.getElementById("optionA").value =
        question.optionA || "";

    document.getElementById("optionB").value =
        question.optionB || "";

    document.getElementById("optionC").value =
        question.optionC || "";

    document.getElementById("optionD").value =
        question.optionD || "";

    document.getElementById("correctAnswer").value =
        question.correctAnswer || "";

    document.getElementById("questionMarks").value =
        question.marks ?? 1;

    document.getElementById("questionFormError").textContent = "";

    document.getElementById("questionFormTitle").textContent =
        `Edit question ${question.questionNumber || ""}`;

    document.getElementById("saveQuestionButton").textContent =
        "Save Changes";

    document.getElementById("questionText").focus();
}

// ==============================
// DELETE QUESTION
// ==============================

async function deleteQuestion(questionId) {

    const question = currentQuestions.find(
        item => String(item.id) === String(questionId)
    );

    if (
        !question ||
        !confirm(`Delete question ${question.questionNumber || ""}?`)
    ) {
        return;
    }

    try {

        await apiRequest(
            `/questions/${encodeURIComponent(questionId)}`,
            { method: "DELETE" }
        );

        resetQuestionForm();

        await loadQuestions();

        await loadExams();

        alert("Question deleted successfully.");

    } catch (error) {

        console.error("Delete question failed:", error);

        alert(
            error.message || "Unable to delete question."
        );
    }
}

// ==============================
// ADD / UPDATE QUESTION
// ==============================

async function saveQuestion(event) {

    event.preventDefault();

    if (!activeQuestionExam) return;

    const errorElement =
        document.getElementById("questionFormError");

    const button =
        document.getElementById("saveQuestionButton");

    errorElement.textContent = "";

    const questionId =
        document.getElementById("questionId").value;

    const payload = {

        examId: Number(activeQuestionExam.id),

        questionText:
            document.getElementById("questionText").value.trim(),

        optionA:
            document.getElementById("optionA").value.trim(),

        optionB:
            document.getElementById("optionB").value.trim(),

        optionC:
            document.getElementById("optionC").value.trim(),

        optionD:
            document.getElementById("optionD").value.trim(),

        correctAnswer:
            document.getElementById("correctAnswer").value,

        marks:
            Number(document.getElementById("questionMarks").value)
    };

    // Validate question and options

    if (
        !payload.questionText ||
        !payload.optionA ||
        !payload.optionB ||
        !payload.optionC ||
        !payload.optionD
    ) {

        errorElement.textContent =
            "Please fill in the question and all four options.";

        return;
    }

    // Validate correct answer

    if (!["A", "B", "C", "D"].includes(payload.correctAnswer)) {

        errorElement.textContent =
            "Select a valid correct answer.";

        return;
    }

    // Validate marks

    if (
        !Number.isInteger(payload.marks) ||
        payload.marks < 1
    ) {

        errorElement.textContent =
            "Marks must be a positive whole number.";

        return;
    }

    // Validate maximum question count when adding

    if (
        !questionId &&
        currentQuestions.length >=
        Number(activeQuestionExam.totalQuestions)
    ) {

        errorElement.textContent =
            `This exam allows ${activeQuestionExam.totalQuestions} question(s). ` +
            "Edit an existing question or update the exam configuration.";

        return;
    }

    button.disabled = true;

    button.textContent = questionId
        ? "Saving..."
        : "Adding...";

    try {

        if (questionId) {

            // Update existing question

            await apiRequest(
                `/questions/${encodeURIComponent(questionId)}`,
                {
                    method: "PUT",
                    body: JSON.stringify(payload)
                }
            );

        } else {

            // Add new question

            await apiRequest("/questions", {
                method: "POST",
                body: JSON.stringify(payload)
            });
        }

        resetQuestionForm();

        await loadQuestions();

        await loadExams();

        alert(
            questionId
                ? "Question updated successfully."
                : "Question added successfully."
        );

    } catch (error) {

        console.error("Saving question failed:", error);

        errorElement.textContent =
            error.message || "Unable to save question.";

    } finally {

        button.disabled = false;

        button.textContent =
            document.getElementById("questionId").value
                ? "Save Changes"
                : "Add Question";
    }
}

// ==============================
// FACULTY PROFILE
// ==============================

function displayFaculty() {

    const email =
        localStorage.getItem("email") || "Faculty";

    document.getElementById("facultyName").textContent =
        email;

    document.getElementById("avatar").textContent =
        email.charAt(0).toUpperCase() || "F";

    document.getElementById("year").textContent =
        new Date().getFullYear();
}

// ==============================
// INITIALIZE PAGE
// ==============================

document.addEventListener("DOMContentLoaded", async () => {

    if (!checkFacultyAccess()) return;

    displayFaculty();

    // Create exam modal

    document.getElementById("openCreateModal")
        .addEventListener("click", openModal);

    document.getElementById("closeCreateModal")
        .addEventListener("click", closeModal);

    document.getElementById("cancelCreate")
        .addEventListener("click", closeModal);

    document.getElementById("examForm")
        .addEventListener("submit", createExam);

    // Search and filters

    document.getElementById("examSearch")
        .addEventListener("input", renderExams);

    document.getElementById("courseFilter")
        .addEventListener("change", renderExams);

    document.getElementById("statusFilter")
        .addEventListener("change", renderExams);

    // Exam card actions

    document.getElementById("examGrid")
        .addEventListener("click", event => {

            const button =
                event.target.closest("button[data-action]");

            if (!button) return;

            if (button.dataset.action === "questions") {

                openQuestionsModal(button.dataset.id);

                return;
            }

            handleExamAction(
                button.dataset.action,
                button.dataset.id
            );
        });

    // Close create exam modal on backdrop click

    document.getElementById("examModal")
        .addEventListener("click", event => {

            if (event.target.id === "examModal") {
                closeModal();
            }
        });

    // Question modal actions

    document.getElementById("closeQuestionsModal")
        .addEventListener("click", closeQuestionsModal);

    document.getElementById("cancelQuestionEdit")
        .addEventListener("click", resetQuestionForm);

    document.getElementById("questionForm")
        .addEventListener("submit", saveQuestion);

    document.getElementById("questionList")
        .addEventListener("click", event => {

            const button =
                event.target.closest("button[data-question-action]");

            if (!button) return;

            if (button.dataset.questionAction === "edit") {

                editQuestion(button.dataset.id);
            }

            if (button.dataset.questionAction === "delete") {

                deleteQuestion(button.dataset.id);
            }
        });

    // Close question modal on backdrop click

    document.getElementById("questionsModal")
        .addEventListener("click", event => {

            if (event.target.id === "questionsModal") {
                closeQuestionsModal();
            }
        });

    // Load page data

    try {

        await loadCourses();

        await loadExams();

    } catch (error) {

        console.error("Initialization failed:", error);

        showMessage(
            error.message ||
            "Unable to initialize the exam management page."
        );
    }
});