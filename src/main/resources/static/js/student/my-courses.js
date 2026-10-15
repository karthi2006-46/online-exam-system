// =====================================================
// LOAD STUDENT COURSES
// =====================================================

async function loadCourses() {

    try {

        const studentId = localStorage.getItem("userId");
        const token = localStorage.getItem("token");

        if (!studentId) {
            alert("Please login again.");
            window.location.href = "../login.html";
            return;
        }

        const response = await fetch(
            `http://localhost:8080/api/student/courses/${studentId}`,
            {
                method: "GET",
                headers: {
                    "Authorization": `Bearer ${token}`
                }
            }
        );

        if (!response.ok) {
            throw new Error(
                `Failed to load courses. Status: ${response.status}`
            );
        }

        const courses = await response.json();

        console.log("Student Courses:", courses);

        const container = document.getElementById("coursesContainer");

        if (!container) {
            console.error("coursesContainer not found in HTML.");
            return;
        }

        container.innerHTML = "";

        if (!Array.isArray(courses) || courses.length === 0) {

            container.innerHTML = `
                <h3>No Enrolled Courses Found</h3>
            `;

            return;
        }

        // Render each enrolled course
        for (const course of courses) {

            const courseId = course.courseId ?? course.id;

            if (!courseId) {
                console.error("Course ID missing:", course);
                continue;
            }

            // Get course progress
            const progress = Number(
                course.progress ??
                course.completionPercentage ??
                0
            );

            // Get coding/material eligibility
            const eligibility = await getExamEligibility(
                studentId,
                courseId,
                token
            );

            // Get faculty approval status
            const approvalStatus = await getApprovalStatus(
                studentId,
                courseId,
                token
            );

            // Exam requires faculty approval
            const examApproved = approvalStatus === "APPROVED";

            const card = document.createElement("div");
            card.className = "course-card";

            card.innerHTML = `
                <h3>${escapeHTML(course.courseTitle ?? course.courseName ?? "Course")}</h3>

                <p>
                    <strong>Duration:</strong>
                    ${course.durationDays ?? "-"} Days
                </p>

                <p>
                    <strong>Material Progress:</strong>
                    ${progress}%
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
                        width:${Math.min(100, Math.max(0, progress))}%;
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
                    ${
                        course.dueDate
                            ? escapeHTML(course.dueDate.substring(0, 10))
                            : "-"
                    }
                </p>

                <div class="course-actions" style="
                    display:flex;
                    flex-wrap:wrap;
                    gap:8px;
                    margin-top:12px;
                ">

                    <button type="button" class="materials-btn">
                        Study Materials
                    </button>

                    <button type="button" class="coding-btn">
                        Coding Challenges
                    </button>

                    <button type="button" class="approval-btn">
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
                        ${examApproved ? "" : "disabled"}
                    >
                        ${
                            examApproved
                                ? "Take Exam"
                                : "Await Faculty Approval"
                        }
                    </button>

                </div>

                <div class="eligibility-message" style="
                    margin-top:10px;
                    font-size:14px;
                    color:#555;
                ">
                    ${getEligibilityMessage(eligibility, approvalStatus)}
                </div>
            `;

            // Study Materials button
            card.querySelector(".materials-btn").addEventListener(
                "click",
                () => openMaterials(courseId)
            );

            // Coding Challenges button
            card.querySelector(".coding-btn").addEventListener(
                "click",
                () => openCodingChallenges(courseId)
            );

            // Faculty approval button
            const approvalButton = card.querySelector(".approval-btn");

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

            // Exam button
            card.querySelector(".exam-btn").addEventListener(
                "click",
                () => startExam(courseId, studentId, token)
            );

            container.appendChild(card);
        }

    } catch (error) {

        console.error("Load Courses Error:", error);

        alert("Unable to load courses. Check the browser console.");
    }
}


// =====================================================
// CHECK EXAM ELIGIBILITY
// =====================================================

async function getExamEligibility(studentId, courseId, token) {

    try {

        const response = await fetch(
            `http://localhost:8080/api/exam-eligibility/student/${studentId}/course/${courseId}`,
            {
                method: "GET",
                headers: {
                    "Authorization": `Bearer ${token}`
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
// CHECK FACULTY APPROVAL STATUS
// =====================================================

async function getApprovalStatus(studentId, courseId, token) {

    try {

        const response = await fetch(
            `http://localhost:8080/api/exam-approval/student/${studentId}/course/${courseId}`,
            {
                method: "GET",
                headers: {
                    "Authorization": `Bearer ${token}`
                }
            }
        );

        if (!response.ok) {
            console.error(
                "Approval status API failed:",
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
// DISPLAY ELIGIBILITY MESSAGE
// =====================================================

function getEligibilityMessage(eligibility, approvalStatus) {

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

    console.log("Opening materials for course:", courseId);

    localStorage.setItem("courseId", String(courseId));

    window.location.href = "study-material.html";
}


// =====================================================
// OPEN CODING CHALLENGES
// =====================================================

function openCodingChallenges(courseId) {

    console.log("Opening coding challenges for course:", courseId);

    // Save selected course ID
    localStorage.setItem("courseId", String(courseId));

    // Navigate to the coding challenge page
    window.location.href = "coding-challenges.html";
}


// =====================================================
// REQUEST FACULTY EXAM APPROVAL
// =====================================================

async function requestExamApproval(studentId, courseId, token) {

    try {

        const confirmed = confirm(
            "Have you completed all study materials and coding challenges? Submit an exam approval request?"
        );

        if (!confirmed) {
            return;
        }

        const response = await fetch(
            `http://localhost:8080/api/exam-approval/request?studentId=${studentId}&courseId=${courseId}`,
            {
                method: "POST",
                headers: {
                    "Authorization": `Bearer ${token}`
                }
            }
        );

        const message = await response.text();

        if (!response.ok) {
            alert(message || "Unable to request exam approval.");
            return;
        }

        alert("Exam approval request submitted successfully.");

        await loadCourses();

    } catch (error) {

        console.error("Request Approval Error:", error);

        alert("Unable to submit the approval request.");
    }
}


// =====================================================
// START EXAM
// =====================================================

async function startExam(courseId, studentId, token) {

    try {

        // Check approval before starting
        const approvalStatus = await getApprovalStatus(
            studentId,
            courseId,
            token
        );

        if (approvalStatus !== "APPROVED") {
            alert("Your exam must be approved by faculty first.");
            return;
        }

        const response = await fetch(
            `http://localhost:8080/api/exams/course/${courseId}/published`,
            {
                method: "GET",
                headers: {
                    "Authorization": `Bearer ${token}`
                }
            }
        );

        if (!response.ok) {
            throw new Error(
                `Failed to load published exams: ${response.status}`
            );
        }

        const result = await response.json();

        console.log("Published Exam Response:", result);

        // Supports APIs returning either an array or { data: [...] }
        const exams = Array.isArray(result)
            ? result
            : result.data;

        if (!Array.isArray(exams) || exams.length === 0) {
            alert("No published exam found for this course.");
            return;
        }

        const examId = exams[0].id;

        if (!examId) {
            alert("Exam ID was not returned by the server.");
            return;
        }

        localStorage.setItem("examId", String(examId));
        localStorage.setItem("courseId", String(courseId));

        window.location.href = `exam.html?examId=${examId}`;

    } catch (error) {

        console.error("Start Exam Error:", error);

        alert("Unable to start the exam. Check the console.");
    }
}


// =====================================================
// ESCAPE HTML TEXT
// =====================================================

function escapeHTML(value) {

    return String(value)
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