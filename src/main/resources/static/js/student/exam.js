const params = new URLSearchParams(window.location.search);

const examId = params.get("examId");
const studentId = localStorage.getItem("userId");
const token = localStorage.getItem("token");

let attemptId = null;
let answers = {};
let timerInterval = null;
let isSubmitting = false;

// Start the exam
async function startExam() {
    if (!examId || !studentId || !token) {
        alert("Exam or student information is missing. Please log in again.");
        return;
    }

    try {
        const response = await fetch(
            `${window.API_BASE_URL}/api/exam-attempts/start?examId=${encodeURIComponent(examId)}&studentId=${encodeURIComponent(studentId)}`,
            {
                method: "POST",
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Accept": "application/json"
                }
            }
        );

        const responseText = await response.text();

        let result = {};

        try {
            result = responseText ? JSON.parse(responseText) : {};
        } catch {
            throw new Error("The server returned an invalid response.");
        }

        // Handle a completed exam.
        if (!response.ok || result.success === false) {
            const message = result.message || "Unable to start the exam.";

            if (
                message.toLowerCase().includes("already completed")
            ) {
                alert(
                    "Exam Already Completed!\n\n" +
                    "You have already completed this exam. " +
                    "You can view your result."
                );

                // Look up the completed attempt to get its ID.
                await openCompletedResult();

                return;
            }

            throw new Error(message);
        }

        const attempt = result.data;

        if (!attempt || !attempt.id) {
            throw new Error("The server did not return a valid exam attempt.");
        }

        attemptId = attempt.id;

        // Do not load an exam that is already submitted.
        if (
            attempt.status === "SUBMITTED" ||
            attempt.status === "AUTO_SUBMITTED"
        ) {
            await openCompletedResult();
            return;
        }

        await loadExamDetails();
        await loadQuestions();

    } catch (error) {
        console.error("Start Exam Error:", error);
        alert(error.message || "Unable to start exam.");
    }
}


// Find the existing completed attempt and open its result.
async function openCompletedResult() {
    try {
        const response = await fetch(
            `${window.API_BASE_URL}/api/exam-attempts/student/${studentId}`,
            {
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Accept": "application/json"
                }
            }
        );

        if (!response.ok) {
            throw new Error("Unable to retrieve your previous exam attempt.");
        }

        const result = await response.json();

        const attempts = Array.isArray(result.data)
            ? result.data
            : [];

        const completedAttempt = attempts.find(attempt =>
            String(attempt.examId) === String(examId) &&
            (
                attempt.status === "SUBMITTED" ||
                attempt.status === "AUTO_SUBMITTED"
            )
        );

        if (completedAttempt) {
            attemptId = completedAttempt.id;

            // Ask the student before opening the result.
            if (confirm(
                "Exam Already Completed!\n\n" +
                "Would you like to view your result?"
            )) {
                window.location.href =
                    `result.html?attemptId=${encodeURIComponent(attemptId)}`;
            }

            return;
        }

        alert(
            "The exam is marked as completed, but its result could not be found. " +
            "Please contact your administrator."
        );

    } catch (error) {
        console.error("Completed Exam Error:", error);

        alert(
            "Your exam is already completed. " +
            "Please open your results page to view your result."
        );
    }
}


// Load exam details.
async function loadExamDetails() {
    try {
        const response = await fetch(
            `${window.API_BASE_URL}/api/exams/${examId}`,
            {
                headers: {
                    "Authorization": `Bearer ${token}`
                }
            }
        );

        if (!response.ok) {
            throw new Error("Unable to load exam details.");
        }

        const result = await response.json();
        const exam = result.data;

        if (!exam) {
            throw new Error("Exam details were not found.");
        }

        document.getElementById("examTitle").innerText = exam.title;

        startTimer(exam.durationMinutes);

    } catch (error) {
        console.error("Exam Details Error:", error);
        alert(error.message || "Unable to load exam details.");
    }
}


// Load questions.
async function loadQuestions() {
    try {
        const response = await fetch(
            `${window.API_BASE_URL}/api/questions/exam/${examId}`,
            {
                headers: {
                    "Authorization": `Bearer ${token}`
                }
            }
        );

        if (!response.ok) {
            throw new Error("Unable to load exam questions.");
        }

        const result = await response.json();
        const questions = result.data;

        const container =
            document.getElementById("questionsContainer");

        container.innerHTML = "";

        if (!questions || questions.length === 0) {
            container.innerHTML = "<h3>No questions available.</h3>";
            return;
        }

        questions.forEach(question => {
            container.innerHTML += `
                <div class="question-card">
                    <div class="question">
                        ${question.questionNumber}.
                        ${question.questionText}
                    </div>

                    <div class="option">
                        <input type="radio"
                            name="q${question.id}"
                            value="A"
                            onchange="saveAnswer(${question.id}, 'A')">
                        ${question.optionA}
                    </div>

                    <div class="option">
                        <input type="radio"
                            name="q${question.id}"
                            value="B"
                            onchange="saveAnswer(${question.id}, 'B')">
                        ${question.optionB}
                    </div>

                    <div class="option">
                        <input type="radio"
                            name="q${question.id}"
                            value="C"
                            onchange="saveAnswer(${question.id}, 'C')">
                        ${question.optionC}
                    </div>

                    <div class="option">
                        <input type="radio"
                            name="q${question.id}"
                            value="D"
                            onchange="saveAnswer(${question.id}, 'D')">
                        ${question.optionD}
                    </div>
                </div>
            `;
        });

    } catch (error) {
        console.error("Questions Error:", error);
        alert(error.message || "Unable to load questions.");
    }
}


// Save an answer locally.
function saveAnswer(questionId, answer) {
    answers[questionId] = answer;
}


// Save answers to the backend.
async function saveAnswersToServer() {
    if (!attemptId) {
        return;
    }

    const response = await fetch(
        `${window.API_BASE_URL}/api/exam-attempts/${attemptId}/save-answers`,
        {
            method: "PUT",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`
            },
            body: JSON.stringify(answers)
        }
    );

    if (!response.ok) {
        throw new Error("Unable to save your answers.");
    }
}


// Submit the exam.
document.getElementById("submitBtn")
    .addEventListener("click", submitExam);

async function submitExam() {
    if (!attemptId || isSubmitting) {
        return;
    }

    isSubmitting = true;

    const submitButton = document.getElementById("submitBtn");

    if (submitButton) {
        submitButton.disabled = true;
    }

    try {
        await saveAnswersToServer();

        const response = await fetch(
            `${window.API_BASE_URL}/api/exam-attempts/${attemptId}/submit`,
            {
                method: "PUT",
                headers: {
                    "Authorization": `Bearer ${token}`
                }
            }
        );

        const result = await response.json();

        if (!response.ok || result.success === false) {
            throw new Error(result.message || "Unable to submit the exam.");
        }

        clearInterval(timerInterval);

        alert("Exam submitted successfully!");

        window.location.href =
            `result.html?attemptId=${encodeURIComponent(attemptId)}`;

    } catch (error) {
        console.error("Submit Exam Error:", error);

        alert(error.message || "Unable to submit exam.");

        isSubmitting = false;

        if (submitButton) {
            submitButton.disabled = false;
        }
    }
}


// Start the exam timer.
function startTimer(durationMinutes) {
    let time = durationMinutes * 60;

    const timer = document.getElementById("timer");

    if (!timer) {
        return;
    }

    clearInterval(timerInterval);

    function updateTimer() {
        const minutes = Math.floor(time / 60);
        const seconds = time % 60;

        timer.innerText =
            `${String(minutes).padStart(2, "0")}:` +
            `${String(seconds).padStart(2, "0")}`;

        if (time <= 0) {
            clearInterval(timerInterval);
            autoSubmitExam();
            return;
        }

        time--;
    }

    updateTimer();

    timerInterval = setInterval(updateTimer, 1000);
}


// Automatically submit when the timer ends.
async function autoSubmitExam() {
    if (!attemptId || isSubmitting) {
        return;
    }

    isSubmitting = true;

    try {
        await saveAnswersToServer();

        const response = await fetch(
            `${window.API_BASE_URL}/api/exam-attempts/${attemptId}/auto-submit`,
            {
                method: "PUT",
                headers: {
                    "Authorization": `Bearer ${token}`
                }
            }
        );

        const result = await response.json();

        if (!response.ok || result.success === false) {
            throw new Error(result.message || "Unable to auto-submit exam.");
        }

        alert("Time is up! Your exam has been submitted automatically.");

        window.location.href =
            `result.html?attemptId=${encodeURIComponent(attemptId)}`;

    } catch (error) {
        console.error("Auto Submit Error:", error);

        alert(error.message || "Unable to auto-submit exam.");

        isSubmitting = false;
    }
}


// Start when the page loads.
startExam();