const params = new URLSearchParams(window.location.search);

const attemptId = params.get("attemptId");

console.log("Attempt ID =", attemptId);

async function loadResult() {

    try {

        if (!attemptId) {
            alert("Attempt ID not found in URL");
            return;
        }

        const response = await fetch(
            `http://localhost:8080/api/results/attempt/${attemptId}`
        );

        const result = await response.json();

        console.log("Result API Response:", result);

        if (!result.success) {
            alert(result.message);
            return;
        }

        const data = result.data;

        document.getElementById("examTitle").innerText =
            data.examTitle;

        document.getElementById("marks").innerText =
            `${data.obtainedMarks} / ${data.totalMarks}`;

        document.getElementById("percentage").innerText =
            data.percentage + "%";

        document.getElementById("status").innerText =
            data.status;

        document.getElementById("correct").innerText =
            data.correctAnswers;

        document.getElementById("wrong").innerText =
            data.wrongAnswers;

        document.getElementById("skipped").innerText =
            data.skippedQuestions;

    } catch (error) {

        console.error("Error loading result:", error);

        alert("Failed to load result");

    }
}

loadResult();