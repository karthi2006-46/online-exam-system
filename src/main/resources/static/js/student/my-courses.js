// =====================================================
// STUDENT MY COURSES MODULE
// Course Progress + Study Materials + Coding Challenges
// Exam Approval + Course Extension
// =====================================================

const API_BASE = `${window.API_BASE_URL}`;
const EXTENSION_API = `${API_BASE}/api/extensions`;

let selectedExtensionEnrollment = null;

const extensionCoursesByEnrollment = new Map();


// =====================================================
// INITIALIZE PAGE
// =====================================================

document.addEventListener("DOMContentLoaded", () => {

    const form = document.getElementById("extensionForm");
    const cancelButton = document.getElementById("cancelExtensionBtn");
    const logoutLink = document.getElementById("logoutLink");
    const modal = document.getElementById("extensionModal");

    if (form) {
        form.addEventListener("submit", submitExtensionRequest);
    }

    if (cancelButton) {
        cancelButton.addEventListener("click", closeExtensionModal);
    }

    if (logoutLink) {
        logoutLink.addEventListener("click", () => {
            localStorage.removeItem("token");
            localStorage.removeItem("userId");
            localStorage.removeItem("role");
        });
    }

    // Close modal when clicking outside the form.
    if (modal) {
        modal.addEventListener("click", event => {
            if (event.target === modal) {
                closeExtensionModal();
            }
        });
    }

    // Load extension history.
    loadStudentExtensionHistory();

});


// =====================================================
// LOAD STUDENT COURSES
// =====================================================

async function loadCourses() {

    try {

        const studentId = localStorage.getItem("userId");
        const token = localStorage.getItem("token");

        if (!studentId || !token) {
            alert("Your session has expired. Please log in again.");
            window.location.href = "../login.html";
            return;
        }

        const response = await fetch(
            `${API_BASE}/api/student/courses/${encodeURIComponent(studentId)}`,
            {
                method: "GET",
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Accept": "application/json"
                }
            }
        );

        if (!response.ok) {
            throw new Error(
                `Failed to load courses. HTTP ${response.status}`
            );
        }

        const courses = await response.json();

        console.log("Student Courses:", courses);

        const container = document.getElementById("coursesContainer");

        if (!container) {
            console.error("coursesContainer not found.");
            return;
        }

        container.innerHTML = "";

        if (!Array.isArray(courses) || courses.length === 0) {

            container.innerHTML = `
                <h3>No Enrolled Courses Found</h3>
            `;

            return;
        }

        // Render each enrolled course.
        for (const course of courses) {

            const courseId = course.courseId ?? course.id;
            const enrollmentId = course.enrollmentId;

            if (!courseId) {
                console.error("Course ID missing:", course);
                continue;
            }

            // Remember course name for extension history.
            if (enrollmentId != null) {
                extensionCoursesByEnrollment.set(
                    Number(enrollmentId),
                    course.courseTitle ??
                    course.courseName ??
                    "Course"
                );
            }

            // Calculate displayed progress.
            const progress = Math.min(
                100,
                Math.max(0, Number(course.progress ?? 0))
            );

            // Get material and coding eligibility.
            const eligibility = await getExamEligibility(
                studentId,
                courseId,
                token
            );

            // Get faculty exam approval.
            const approvalStatus = await getApprovalStatus(
                studentId,
                courseId,
                token
            );

            const examApproved = approvalStatus === "APPROVED";

            // Check whether the enrollment deadline has expired.
            const dueDate = course.dueDate
                ? new Date(course.dueDate)
                : null;

            const deadlineExpired =
                dueDate !== null &&
                !Number.isNaN(dueDate.getTime()) &&
                Date.now() >= dueDate.getTime();

            // Build the course card.
            const card = document.createElement("div");
            card.className = "course-card";

            card.innerHTML = `

                <h3>
                    ${escapeHTML(
                        course.courseTitle ??
                        course.courseName ??
                        "Course"
                    )}
                </h3>

                <p>
                    <strong>Duration:</strong>
                    ${escapeHTML(course.durationDays ?? "-")} Days
                </p>

                <p>
                    <strong>Overall Progress:</strong>
                    ${progress.toFixed(1)}%
                </p>

                <div style="
                    width:100%;
                    background:#ddd;
                    height:20px;
                    border-radius:10px;
                    overflow:hidden;
                    margin-bottom:12px;
                ">
                    <div style="
                        width:${progress}%;
                        height:100%;
                        background:green;
                        transition:width 0.4s;
                    "></div>
                </div>

                <p>
                    <strong>Status:</strong>
                    ${escapeHTML(course.status ?? "-")}
                </p>

                <p>
                    <strong>Due Date:</strong>
                    ${course.dueDate
                        ? escapeHTML(formatExtensionDateTime(course.dueDate))
                        : "-"
                    }
                </p>

                <div class="course-actions" style="
                    display:flex;
                    flex-wrap:wrap;
                    gap:8px;
                    margin-top:12px;
                ">

                    <button
                        type="button"
                        class="materials-btn">
                        Study Materials
                    </button>

                    <button
                        type="button"
                        class="coding-btn">
                        Coding Challenges
                    </button>

                    <button
                        type="button"
                        class="approval-btn">
                        ${
                            examApproved
                                ? "Exam Approved"
                                : approvalStatus === "PENDING"
                                    ? "Approval Pending"
                                    : approvalStatus === "REJECTED"
                                        ? "Request Approval Again"
                                        : "Complete Materials & Coding"
                        }
                    </button>

                    <button
                        type="button"
                        class="exam-btn"
                        ${examApproved ? "" : "disabled"}>
                        ${
                            examApproved
                                ? "Take Exam"
                                : "Await Faculty Approval"
                        }
                    </button>

                    ${
                        enrollmentId != null && deadlineExpired
                            ? `
                                <button
                                    type="button"
                                    class="extension-btn">
                                    Request Extension
                                </button>
                            `
                            : ""
                    }

                </div>

                <div class="eligibility-message" style="
                    margin-top:10px;
                    font-size:14px;
                    color:#555;
                ">
                    ${escapeHTML(
                        getEligibilityMessage(
                            eligibility,
                            approvalStatus
                        )
                    )}
                </div>

            `;

            // ---------------------------------------------
            // STUDY MATERIALS
            // ---------------------------------------------

            card.querySelector(".materials-btn").addEventListener(
                "click",
                () => openMaterials(courseId)
            );

            // ---------------------------------------------
            // CODING CHALLENGES
            // ---------------------------------------------

            card.querySelector(".coding-btn").addEventListener(
                "click",
                () => openCodingChallenges(courseId)
            );

            // ---------------------------------------------
            // EXAM APPROVAL
            // ---------------------------------------------

            const approvalButton =
                card.querySelector(".approval-btn");

            if (
                approvalStatus === "PENDING" ||
                approvalStatus === "APPROVED"
            ) {

                approvalButton.disabled = true;

            } else {

                approvalButton.addEventListener(
                    "click",
                    () => requestExamApproval(
                        studentId,
                        courseId,
                        token
                    )
                );

            }

            // ---------------------------------------------
            // TAKE EXAM
            // ---------------------------------------------

            card.querySelector(".exam-btn").addEventListener(
                "click",
                () => startExam(courseId, studentId, token)
            );

            // ---------------------------------------------
            // REQUEST COURSE EXTENSION
            // ---------------------------------------------

            const extensionButton =
                card.querySelector(".extension-btn");

            if (extensionButton) {

                extensionButton.addEventListener("click", () => {

                    openExtensionModal(course);

                });

            }

            container.appendChild(card);

        }

    } catch (error) {

        console.error("Load Courses Error:", error);

        showPageMessage(
            "Unable to load courses. Check the browser console."
        );

    }

}


// =====================================================
// EXTENSION API HELPER
// =====================================================

async function extensionApiRequest(url, options = {}) {

    const token = localStorage.getItem("token");

    if (!token) {
        throw new Error(
            "Your session has expired. Please log in again."
        );
    }

    const response = await fetch(url, {

        ...options,

        headers: {
            "Authorization": `Bearer ${token}`,
            "Accept": "application/json",
            ...(options.headers || {})
        }

    });

    const responseText = await response.text();

    let result = null;

    try {

        result = responseText
            ? JSON.parse(responseText)
            : null;

    } catch {

        result = responseText;

    }

    if (!response.ok) {

        let errorMessage;

        if (typeof result === "string") {

            errorMessage = result;

        } else if (result && typeof result === "object") {

            errorMessage =
                result.message ||
                result.error ||
                result.data?.message;

        }

        throw new Error(
            errorMessage ||
            `Request failed. HTTP ${response.status}`
        );

    }

    return result;

}


// =====================================================
// LOAD STUDENT EXTENSION HISTORY
// =====================================================

async function loadStudentExtensionHistory() {

    const studentId = localStorage.getItem("userId");

    const tbody = document.getElementById(
        "extensionHistoryBody"
    );

    const message = document.getElementById(
        "extensionHistoryMessage"
    );

    if (!studentId || !tbody) {
        return;
    }

    tbody.innerHTML = `
        <tr>
            <td colspan="5">Loading extension history...</td>
        </tr>
    `;

    try {

        const result = await extensionApiRequest(
            `${EXTENSION_API}/student/${encodeURIComponent(studentId)}`
        );

        // Support direct arrays and common response wrappers.
        const requests = Array.isArray(result)
            ? result
            : Array.isArray(result?.data)
                ? result.data
                : Array.isArray(result?.data?.content)
                    ? result.data.content
                    : [];

        renderExtensionHistory(requests);

        if (message) {
            message.textContent = "";
        }

    } catch (error) {

        console.error("Extension History Error:", error);

        tbody.innerHTML = `
            <tr>
                <td colspan="5">
                    Unable to load extension history.
                </td>
            </tr>
        `;

        if (message) {
            message.textContent = error.message;
        }

    }

}


// =====================================================
// RENDER EXTENSION HISTORY
// =====================================================

function renderExtensionHistory(requests) {

    const tbody = document.getElementById(
        "extensionHistoryBody"
    );

    if (!tbody) {
        return;
    }

    if (!requests.length) {

        tbody.innerHTML = `
            <tr>
                <td colspan="5">
                    You have not submitted any extension requests.
                </td>
            </tr>
        `;

        return;
    }

    tbody.innerHTML = requests.map(request => {

        const status = String(
            request.status || "PENDING"
        ).toUpperCase();

        const statusClass = {

            PENDING: "status-pending",
            APPROVED: "status-approved",
            REJECTED: "status-rejected"

        }[status] || "";

        const enrollmentId = request.enrollmentId;

        const courseName =
            request.courseTitle ||
            request.courseName ||
            extensionCoursesByEnrollment.get(
                Number(enrollmentId)
            ) ||
            `Enrollment #${enrollmentId ?? "-"}`;

        return `

            <tr>

                <td>
                    ${escapeHTML(courseName)}
                </td>

                <td>
                    ${escapeHTML(request.daysRequested ?? "-")}
                </td>

                <td>
                    ${escapeHTML(request.reason || "-")}
                </td>

                <td>
                    <span class="extension-status ${statusClass}">
                        ${escapeHTML(status)}
                    </span>
                </td>

                <td>
                    ${escapeHTML(request.rejectionReason || "-")}
                </td>

            </tr>

        `;

    }).join("");

}


// =====================================================
// OPEN EXTENSION REQUEST MODAL
// =====================================================

function openExtensionModal(course) {

    const modal = document.getElementById(
        "extensionModal"
    );

    if (!modal) {
        return;
    }

    if (!course.enrollmentId) {

        alert(
            "Enrollment ID is missing. Please refresh the page."
        );

        return;
    }

    // Verify the deadline again before opening.
    const dueDate = course.dueDate
        ? new Date(course.dueDate)
        : null;

    if (
        !dueDate ||
        Number.isNaN(dueDate.getTime()) ||
        Date.now() < dueDate.getTime()
    ) {

        alert(
            "You can request an extension only after the course deadline expires."
        );

        return;
    }

    selectedExtensionEnrollment = Number(
        course.enrollmentId
    );

    document.getElementById(
        "extensionEnrollmentId"
    ).value = String(course.enrollmentId);

    document.getElementById(
        "extensionCourseName"
    ).textContent =
        `Course: ${course.courseTitle || course.courseName || "Course"}`;

    document.getElementById(
        "extensionDueDate"
    ).textContent =
        `Due date: ${formatExtensionDateTime(course.dueDate)}`;

    document.getElementById(
        "daysRequested"
    ).value = "";

    document.getElementById(
        "extensionReason"
    ).value = "";

    document.getElementById(
        "extensionFormMessage"
    ).textContent = "";

    document.getElementById(
        "submitExtensionBtn"
    ).disabled = false;

    modal.classList.add("show");

}


// =====================================================
// CLOSE EXTENSION MODAL
// =====================================================

function closeExtensionModal() {

    const modal = document.getElementById(
        "extensionModal"
    );

    if (modal) {
        modal.classList.remove("show");
    }

    selectedExtensionEnrollment = null;

}


// =====================================================
// SUBMIT EXTENSION REQUEST
// =====================================================

async function submitExtensionRequest(event) {

    event.preventDefault();

    const enrollmentId = selectedExtensionEnrollment;

    const daysRequested = Number(
        document.getElementById("daysRequested").value
    );

    const reason = document.getElementById(
        "extensionReason"
    ).value.trim();

    const message = document.getElementById(
        "extensionFormMessage"
    );

    const submitButton = document.getElementById(
        "submitExtensionBtn"
    );

    if (!enrollmentId) {

        message.textContent = "Please select a course.";

        return;

    }

    if (
        !Number.isInteger(daysRequested) ||
        daysRequested < 1 ||
        daysRequested > 90
    ) {

        message.textContent =
            "Enter a number of days between 1 and 90.";

        return;

    }

    if (!reason) {

        message.textContent =
            "Please enter a reason for your request.";

        return;

    }

    const confirmed = confirm(
        `Submit a request for ${daysRequested} additional days?`
    );

    if (!confirmed) {
        return;
    }

    submitButton.disabled = true;

    message.textContent = "Submitting request...";

    try {

        const params = new URLSearchParams({

            enrollmentId: String(enrollmentId),

            daysRequested: String(daysRequested),

            reason: reason

        });

        const result = await extensionApiRequest(
            `${EXTENSION_API}/request?${params.toString()}`,
            {
                method: "POST"
            }
        );

        const successMessage =
            result?.message ||
            result?.data?.message ||
            "Extension request submitted successfully.";

        alert(successMessage);

        closeExtensionModal();

        // Refresh request history.
        await loadStudentExtensionHistory();

        // Refresh courses and deadline information.
        await loadCourses();

    } catch (error) {

        console.error(
            "Submit Extension Request Error:",
            error
        );

        message.textContent = error.message;

        alert(error.message);

    } finally {

        submitButton.disabled = false;

    }

}


// =====================================================
// CHECK EXAM ELIGIBILITY
// =====================================================

async function getExamEligibility(
    studentId,
    courseId,
    token
) {

    try {

        const response = await fetch(
            `${API_BASE}/api/exam-eligibility/student/${studentId}/course/${courseId}`,
            {
                method: "GET",
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Accept": "application/json"
                }
            }
        );

        if (!response.ok) {

            console.error(
                "Eligibility API failed:",
                response.status,
                courseId
            );

            return null;

        }

        const result = await response.json();

        console.log(
            `Eligibility for course ${courseId}:`,
            result
        );

        return result;

    } catch (error) {

        console.error("Eligibility Error:", error);

        return null;

    }

}


// =====================================================
// CHECK FACULTY EXAM APPROVAL STATUS
// =====================================================

async function getApprovalStatus(
    studentId,
    courseId,
    token
) {

    try {

        const response = await fetch(
            `${API_BASE}/api/exam-approval/student/${studentId}/course/${courseId}`,
            {
                method: "GET",
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Accept": "application/json, text/plain"
                }
            }
        );

        if (!response.ok) {

            console.error(
                "Approval Status API failed:",
                response.status
            );

            return "NOT_REQUESTED";

        }

        const status = await response.text();

        const cleanStatus = status
            .trim()
            .replace(/^"|"$/g, "");

        console.log(
            `Approval Status for course ${courseId}:`,
            cleanStatus
        );

        return cleanStatus;

    } catch (error) {

        console.error("Approval Status Error:", error);

        return "NOT_REQUESTED";

    }

}


// =====================================================
// DISPLAY EXAM ELIGIBILITY MESSAGE
// =====================================================

function getEligibilityMessage(
    eligibility,
    approvalStatus
) {

    if (!eligibility) {

        return "Unable to check exam eligibility. Please refresh.";

    }

    if (approvalStatus === "APPROVED") {

        return "Faculty approved your exam. You can take the exam.";

    }

    if (approvalStatus === "PENDING") {

        return "Your exam approval request is waiting for faculty review.";

    }

    if (approvalStatus === "REJECTED") {

        return "Your previous approval request was rejected. You can request approval again after checking your progress.";

    }

    if (!eligibility.materialsCompleted) {

        return "Complete all study materials first.";

    }

    if (!eligibility.codingCompleted) {

        return "Complete all coding challenges first.";

    }

    return "You have completed the requirements. Request faculty approval.";

}


// =====================================================
// OPEN STUDY MATERIALS
// =====================================================

function openMaterials(courseId) {

    console.log(
        "Opening materials for course:",
        courseId
    );

    localStorage.setItem(
        "courseId",
        String(courseId)
    );

    window.location.href = "study-material.html";

}


// =====================================================
// OPEN CODING CHALLENGES
// =====================================================

function openCodingChallenges(courseId) {

    console.log(
        "Opening coding challenges for course:",
        courseId
    );

    localStorage.setItem(
        "courseId",
        String(courseId)
    );

    window.location.href = "coding-challenges.html";

}


// =====================================================
// REQUEST FACULTY EXAM APPROVAL
// =====================================================

async function requestExamApproval(
    studentId,
    courseId,
    token
) {

    if (!studentId || !courseId || !token) {

        alert(
            "Student ID, Course ID, or authentication token is missing."
        );

        return;

    }

    const confirmed = confirm(
        "Have you completed all study materials and coding challenges? " +
        "Submit an exam approval request?"
    );

    if (!confirmed) {
        return;
    }

    try {

        const params = new URLSearchParams({
            studentId: String(studentId),
            courseId: String(courseId)
        });

        const url =
            `${API_BASE}/api/exam-approval/request?${params.toString()}`;

        const response = await fetch(url, {

            method: "POST",

            headers: {
                "Authorization": `Bearer ${token}`,
                "Accept": "application/json, text/plain"
            }

        });

        const responseText = await response.text();

        console.log(
            "Approval request HTTP status:",
            response.status
        );

        console.log(
            "Approval request response:",
            responseText
        );

        if (!response.ok) {

            alert(
                `Approval request failed (${response.status}).\n\n` +
                (responseText || "The server did not provide an error message.")
            );

            return;

        }

        alert(
            responseText ||
            "Exam approval request submitted successfully."
        );

        await loadCourses();

    } catch (error) {

        console.error(
            "Request Approval Error:",
            error
        );

        alert(
            "Unable to connect to the server. Check whether Spring Boot is running."
        );

    }

}


// =====================================================
// START EXAM
// =====================================================

async function startExam(
    courseId,
    studentId,
    token
) {

    try {

        // Always recheck faculty approval.
        const approvalStatus = await getApprovalStatus(
            studentId,
            courseId,
            token
        );

        if (approvalStatus !== "APPROVED") {

            alert(
                "Your exam must be approved by faculty first."
            );

            return;

        }

        const response = await fetch(
            `${API_BASE}/api/exams/course/${courseId}/published`,
            {
                method: "GET",
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Accept": "application/json"
                }
            }
        );

        if (!response.ok) {

            throw new Error(
                `Failed to load published exams: ${response.status}`
            );

        }

        const result = await response.json();

        console.log(
            "Published Exam Response:",
            result
        );

        const exams = Array.isArray(result)
            ? result
            : result?.data;

        if (!Array.isArray(exams) || exams.length === 0) {

            alert(
                "No published exam found for this course."
            );

            return;

        }

        const examId = exams[0].id;

        if (!examId) {

            alert(
                "Exam ID was not returned by the server."
            );

            return;

        }

        localStorage.setItem(
            "examId",
            String(examId)
        );

        localStorage.setItem(
            "courseId",
            String(courseId)
        );

        window.location.href =
            `exam-portal.html?examId=${encodeURIComponent(examId)}`;

    } catch (error) {

        console.error(
            "Start Exam Error:",
            error
        );

        alert(
            "Unable to start the exam. Check the browser console."
        );

    }

}


// =====================================================
// DATE HELPERS
// =====================================================

function formatExtensionDate(value) {

    if (!value) {
        return "Not available";
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return String(value);
    }

    return date.toLocaleDateString();

}


function formatExtensionDateTime(value) {

    if (!value) {
        return "Not available";
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return String(value);
    }

    return date.toLocaleString();

}


// =====================================================
// PAGE MESSAGE
// =====================================================

function showPageMessage(message) {

    const pageMessage = document.getElementById(
        "pageMessage"
    );

    if (pageMessage) {
        pageMessage.textContent = message;
    } else {
        console.error(message);
    }

}


// =====================================================
// ESCAPE HTML
// =====================================================

function escapeHTML(value) {

    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");

}


// =====================================================
// LOAD COURSES WHEN PAGE OPENS
// =====================================================

loadCourses();