const params = new URLSearchParams(window.location.search);

const examId = params.get("examId");

const studentId = localStorage.getItem("userId");

let attemptId = null;
let answers = {};

async function startExam() {

    try {

        console.log("Exam ID:", examId);
        console.log("Student ID:", studentId);

        const startResponse = await fetch(
            `http://localhost:8080/api/exam-attempts/start?examId=${examId}&studentId=${studentId}`,
            {
                method: "POST",
                headers: {
                    "Authorization":
                        "Bearer " +
                        localStorage.getItem("token")
                }
            }
        );

        const startResult = await startResponse.json();

        console.log("Start Result:", startResult);

        if (!startResult.success) {
            alert(startResult.message);
            return;
        }

        attemptId = startResult.data.id;

        await loadExamDetails();

        await loadQuestions();

    } catch (error) {

        console.error("Start Exam Error:", error);

        alert("Unable to start exam.");
    }
}

async function loadExamDetails() {

    try {

        const response = await fetch(
            `http://localhost:8080/api/exams/${examId}`
        );

        const result = await response.json();

        console.log("Exam Details:", result);

        const exam = result.data;

        document.getElementById("examTitle").innerText =
            exam.title;

        startTimer(
            exam.durationMinutes
        );

    } catch (error) {

        console.error("Exam Details Error:", error);
    }
}

async function loadQuestions() {

    try {

        const response = await fetch(
            `http://localhost:8080/api/questions/exam/${examId}`
        );

        const result = await response.json();

        console.log("Questions Response:", result);

        const questions = result.data;

        const container =
            document.getElementById("questionsContainer");

        container.innerHTML = "";

        if (!questions || questions.length === 0) {

            container.innerHTML =
                "<h3>No questions available.</h3>";

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
                    <input
                        type="radio"
                        name="q${question.id}"
                        value="A"
                        onchange="saveAnswer(${question.id},'A')">
                    ${question.optionA}
                </div>

                <div class="option">
                    <input
                        type="radio"
                        name="q${question.id}"
                        value="B"
                        onchange="saveAnswer(${question.id},'B')">
                    ${question.optionB}
                </div>

                <div class="option">
                    <input
                        type="radio"
                        name="q${question.id}"
                        value="C"
                        onchange="saveAnswer(${question.id},'C')">
                    ${question.optionC}
                </div>

                <div class="option">
                    <input
                        type="radio"
                        name="q${question.id}"
                        value="D"
                        onchange="saveAnswer(${question.id},'D')">
                    ${question.optionD}
                </div>

            </div>

            `;
        });

    } catch (error) {

        console.error("Questions Error:", error);
    }
}

function saveAnswer(questionId, answer) {

    answers[questionId] = answer;
}

async function saveAnswersToServer() {

    if (!attemptId)
        return;

    try {

        await fetch(
            `http://localhost:8080/api/exam-attempts/${attemptId}/save-answers`,
            {
                method: "PUT",

                headers: {
                    "Content-Type":
                        "application/json",

                    "Authorization":
                        "Bearer " +
                        localStorage.getItem("token")
                },

                body:
                    JSON.stringify(
                        answers
                    )
            }
        );

    } catch (error) {

        console.error(error);
    }
}

document.getElementById("submitBtn")
    .addEventListener(
        "click",
        submitExam
    );

async function submitExam() {

    try {

        await saveAnswersToServer();

        const response =
            await fetch(
                `http://localhost:8080/api/exam-attempts/${attemptId}/submit`,
                {
                    method: "PUT",

                    headers: {
                        "Authorization":
                            "Bearer " +
                            localStorage.getItem("token")
                    }
                }
            );

        const result =
            await response.json();

        console.log(result);

        alert(
            "Exam Submitted Successfully"
        );

        window.location.href =
            `result.html?attemptId=${attemptId}`;

    } catch (error) {

        console.error(error);
    }
}

function startTimer(durationMinutes) {

    let time =
        durationMinutes * 60;

    const timer =
        document.getElementById("timer");

    const interval =
        setInterval(() => {

            let minutes =
                Math.floor(
                    time / 60
                );

            let seconds =
                time % 60;

            timer.innerText =
                `${minutes}:${seconds
                    .toString()
                    .padStart(2, '0')}`;

            time--;

            if (time < 0) {

                clearInterval(interval);

                autoSubmitExam();
            }

        }, 1000);
}

async function autoSubmitExam() {

    try {

        await saveAnswersToServer();

        await fetch(
            `http://localhost:8080/api/exam-attempts/${attemptId}/auto-submit`,
            {
                method: "PUT",

                headers: {
                    "Authorization":
                        "Bearer " +
                        localStorage.getItem("token")
                }
            }
        );

        alert(
            "Time Up! Exam Auto Submitted."
        );

        window.location.href =
            `result.html?attemptId=${attemptId}`;

    } catch (error) {

        console.error(error);
    }
}

startExam();