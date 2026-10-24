const params = new URLSearchParams(window.location.search);

const attemptId = params.get("attemptId");

const token = localStorage.getItem("token");

const loadingMessage = document.getElementById("loadingMessage");
const errorMessage = document.getElementById("errorMessage");
const resultContent = document.getElementById("resultContent");
const answersContainer = document.getElementById("answersContainer");
const reviewMessage = document.getElementById("reviewMessage");

document.addEventListener("DOMContentLoaded", () => {

    document.getElementById("backBtn")
        ?.addEventListener("click", goToExamPortal);

    document.getElementById("backToPortalBtn")
        ?.addEventListener("click", goToExamPortal);

    loadResult();

});

function goToExamPortal() {
    window.location.href = "exam-portal.html";
}

async function loadResult() {

    if (!attemptId) {
        showError("Attempt ID is missing from the URL.");
        return;
    }

    if (!token) {
        showError("Your login session has expired. Please log in again.");
        return;
    }

    try {

        loadingMessage.classList.remove("hidden");
        errorMessage.classList.add("hidden");
        resultContent.classList.add("hidden");

        const response = await fetch(
            `${window.API_BASE_URL}/api/results/attempt/${encodeURIComponent(attemptId)}/details`,
            {
                method: "GET",
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Accept": "application/json"
                }
            }
        );

        const responseText = await response.text();

        let apiResponse;

        try {
            apiResponse = responseText
                ? JSON.parse(responseText)
                : {};
        } catch {
            throw new Error("The server returned an invalid response.");
        }

        if (!response.ok) {
            throw new Error(
                apiResponse.message ||
                `Unable to load result. HTTP ${response.status}`
            );
        }

        if (apiResponse.success === false) {
            throw new Error(
                apiResponse.message || "Unable to load the result."
            );
        }

        const data = apiResponse.data;

        if (!data || !data.result) {
            throw new Error("The server did not return the result details.");
        }

        renderSummary(data.result, data.questions || []);

        renderQuestionReview(data.questions || []);

        loadingMessage.classList.add("hidden");
        resultContent.classList.remove("hidden");

    } catch (error) {

        console.error("Result loading error:", error);

        showError(error.message || "Failed to load your exam result.");
    }
}

function renderSummary(data, questions) {

    const percentage = Number(data.percentage || 0);

    const correct = Number(data.correctAnswers || 0);
    const wrong = Number(data.wrongAnswers || 0);
    const skipped = Number(data.skippedQuestions || 0);

    document.getElementById("examTitle").textContent =
        data.examTitle || "Exam Result";

    document.getElementById("marks").textContent =
        `${data.obtainedMarks ?? 0} / ${data.totalMarks ?? 0}`;

    document.getElementById("percentage").textContent =
        `${percentage.toFixed(percentage % 1 === 0 ? 0 : 1)}%`;

    document.getElementById("correct").textContent = correct;
    document.getElementById("wrong").textContent = wrong;
    document.getElementById("skipped").textContent = skipped;

    document.getElementById("totalQuestions").textContent =
        questions.length;

    const statusBadge = document.getElementById("statusBadge");

    const status = String(data.status || "COMPLETED").toUpperCase();

    statusBadge.textContent = status;
    statusBadge.className = "status-badge";

    if (status === "PASS") {
        statusBadge.classList.add("pass");
    } else if (status === "FAIL") {
        statusBadge.classList.add("fail");
    }

    const scoreCircle = document.getElementById("scoreCircle");

    const safePercentage = Math.max(0, Math.min(100, percentage));

    scoreCircle.style.background = `
        conic-gradient(
            var(--primary) 0%,
            var(--primary) ${safePercentage}%,
            #e2e8f0 ${safePercentage}%,
            #e2e8f0 100%
        )
    `;

    const performanceMessage =
        document.getElementById("performanceMessage");

    if (status === "PASS") {
        performanceMessage.textContent =
            "Congratulations! You passed this examination.";
    } else if (status === "FAIL") {
        performanceMessage.textContent =
            "You did not reach the passing mark. Review your answers below.";
    } else {
        performanceMessage.textContent =
            "Your examination result is ready.";
    }
}

function renderQuestionReview(questions) {

    answersContainer.innerHTML = "";
    reviewMessage.classList.add("hidden");
    reviewMessage.textContent = "";

    if (!Array.isArray(questions) || questions.length === 0) {

        reviewMessage.textContent =
            "Your score is available, but no question-review details were returned.";

        reviewMessage.classList.remove("hidden");

        return;
    }

    questions.forEach((question, index) => {

        const status = String(
            question.answerStatus || "SKIPPED"
        ).toUpperCase();

        const isCorrect = status === "CORRECT";
        const isWrong = status === "WRONG";
        const isSkipped = status === "SKIPPED";

        const card = document.createElement("article");

        card.className =
            "question-review " +
            (isCorrect ? "correct" : isWrong ? "wrong" : "skipped");

        const header = document.createElement("div");
        header.className = "question-header";

        const number = document.createElement("span");
        number.className = "question-number";

        number.textContent =
            `Question ${question.questionNumber ?? index + 1}`;

        const badge = document.createElement("span");

        badge.className =
            "answer-status " +
            (isCorrect ? "correct" : isWrong ? "wrong" : "skipped");

        badge.textContent =
            isCorrect ? "✓ Correct" :
            isWrong ? "✕ Incorrect" :
            "↷ Skipped";

        header.append(number, badge);

        const body = document.createElement("div");
        body.className = "question-body";

        const questionText = document.createElement("p");
        questionText.className = "question-text";
        questionText.textContent = question.questionText || "Question text unavailable.";

        const optionsList = document.createElement("div");
        optionsList.className = "options-list";

        const options = [
            ["A", question.optionA],
            ["B", question.optionB],
            ["C", question.optionC],
            ["D", question.optionD]
        ];

        options.forEach(([letter, optionText]) => {

            const option = document.createElement("div");
            option.className = "review-option";

            const selected =
                question.studentAnswer &&
                String(question.studentAnswer).toUpperCase() === letter;

            const correctAnswer =
                question.correctAnswer &&
                String(question.correctAnswer).toUpperCase() === letter;

            if (selected && correctAnswer) {
                option.classList.add("student-correct");
            } else if (selected && !correctAnswer) {
                option.classList.add("student-wrong");
            } else if (correctAnswer) {
                option.classList.add("correct-answer");
            }

            const optionLetter = document.createElement("span");
            optionLetter.className = "option-letter";
            optionLetter.textContent = letter;

            const content = document.createElement("div");
            content.textContent = optionText || "";

            if (selected || correctAnswer) {

                const label = document.createElement("span");
                label.className = "option-label";

                if (selected && correctAnswer) {
                    label.textContent = "Your answer · Correct answer";
                } else if (selected) {
                    label.textContent = "Your answer";
                } else {
                    label.textContent = "Correct answer";
                }

                content.appendChild(label);
            }

            option.append(optionLetter, content);
            optionsList.appendChild(option);
        });

        const feedback = document.createElement("div");

        feedback.className =
            "question-feedback " +
            (isCorrect ? "correct" : isWrong ? "wrong" : "skipped");

        if (isCorrect) {

            feedback.textContent =
                `Correct! You earned ${question.marks ?? 0} mark(s).`;

        } else if (isWrong) {

            feedback.textContent =
                `Your answer: ${question.studentAnswer}. ` +
                `Correct answer: ${question.correctAnswer}.`;

        } else {

            feedback.textContent =
                `You skipped this question. ` +
                `Correct answer: ${question.correctAnswer}.`;
        }

        body.append(questionText, optionsList, feedback);

        card.append(header, body);

        answersContainer.appendChild(card);
    });
}

function showError(message) {

    loadingMessage.classList.add("hidden");

    resultContent.classList.add("hidden");

    errorMessage.textContent = message;
    errorMessage.classList.remove("hidden");
}