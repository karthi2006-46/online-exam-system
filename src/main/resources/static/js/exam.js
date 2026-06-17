// Exam JS
let attemptId = null;
let currentAnswers = {};

// Get exam ID from URL
const urlParams = new URLSearchParams(window.location.search);
const examId = urlParams.get('examId');
const studentId = localStorage.getItem('userId');

// Initialize exam
async function initializeExam() {
    try {
        // Get exam details
        const examResponse = await api.getExamById(examId);
        if (examResponse.data) {
            const exam = examResponse.data;
            document.getElementById('examTitle').textContent = exam.title;
            document.getElementById('examDescription').textContent = exam.description;
            document.getElementById('totalQuestions').textContent = exam.totalQuestions;
            document.getElementById('totalMarks').textContent = exam.totalMarks;
            document.getElementById('passingMarks').textContent = exam.passingMarks;
        }

        // Start exam attempt
        const attemptResponse = await api.startExam(examId, studentId);
        if (attemptResponse.data) {
            attemptId = attemptResponse.data.id;
            loadQuestions();
            startTimer(examResponse.data.durationMinutes * 60);
        }
    } catch (error) {
        console.error('Error initializing exam:', error);
    }
}

// Load questions
async function loadQuestions() {
    try {
        const response = await api.getExamQuestions(examId);
        if (response.data) {
            displayQuestions(response.data);
        }
    } catch (error) {
        console.error('Error loading questions:', error);
    }
}

function displayQuestions(questions) {
    const questionsList = document.getElementById('questionsList');
    questionsList.innerHTML = questions.map(question => `
        <div class="question-item">
            <div class="question-text">Q${question.questionNumber}. ${question.questionText}</div>
            <div class="question-options">
                <label class="option-label">
                    <input type="radio" name="question_${question.id}" value="A" onchange="saveAnswer(${question.id}, 'A')">
                    A. ${question.optionA}
                </label>
                <label class="option-label">
                    <input type="radio" name="question_${question.id}" value="B" onchange="saveAnswer(${question.id}, 'B')">
                    B. ${question.optionB}
                </label>
                <label class="option-label">
                    <input type="radio" name="question_${question.id}" value="C" onchange="saveAnswer(${question.id}, 'C')">
                    C. ${question.optionC}
                </label>
                <label class="option-label">
                    <input type="radio" name="question_${question.id}" value="D" onchange="saveAnswer(${question.id}, 'D')">
                    D. ${question.optionD}
                </label>
            </div>
        </div>
    `).join('');
}

function saveAnswer(questionId, answer) {
    currentAnswers[questionId] = answer;
}

// Timer
function startTimer(duration) {
    let timeLeft = duration;
    const timerDisplay = document.getElementById('timer');

    const interval = setInterval(() => {
        timeLeft--;
        const hours = Math.floor(timeLeft / 3600);
        const minutes = Math.floor((timeLeft % 3600) / 60);
        const seconds = timeLeft % 60;
        timerDisplay.textContent = `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`;

        if (timeLeft <= 0) {
            clearInterval(interval);
            autoSubmitExam();
        }
    }, 1000);
}

function pad(num) {
    return num < 10 ? '0' + num : num;
}

// Submit exam
document.getElementById('submitBtn').addEventListener('click', async () => {
    if (confirm('Are you sure you want to submit the exam?')) {
        await submitExam();
    }
});

// Save answers
document.getElementById('saveBtn').addEventListener('click', async () => {
    try {
        await api.saveAnswers(attemptId, JSON.stringify(currentAnswers));
        alert('Answers saved successfully!');
    } catch (error) {
        console.error('Error saving answers:', error);
    }
});

async function submitExam() {
    try {
        await api.saveAnswers(attemptId, JSON.stringify(currentAnswers));
        const response = await api.submitExam(attemptId);
        if (response.data) {
            alert('Exam submitted successfully!');
            window.location.href = `/result.html?attemptId=${attemptId}`;
        }
    } catch (error) {
        console.error('Error submitting exam:', error);
    }
}

async function autoSubmitExam() {
    try {
        await submitExam();
    } catch (error) {
        console.error('Error in auto-submit:', error);
    }
}

// Initialize on page load
if (examId && studentId) {
    initializeExam();
} else {
    window.location.href = '/dashboard.html';
}
