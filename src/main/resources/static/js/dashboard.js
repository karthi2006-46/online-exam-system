// Dashboard JS
let userId = localStorage.getItem('userId');
let coursesList = document.getElementById('coursesList');
let enrollmentsList = document.getElementById('enrollmentsList');
let examsList = document.getElementById('examsList');
let resultsList = document.getElementById('resultsList');

// Load dashboard data
async function loadDashboard() {
    try {
        // Load courses
        const coursesResponse = await api.getCourses();
        if (coursesResponse.data) {
            displayCourses(coursesResponse.data);
        }

        // Load enrollments
        const enrollmentsResponse = await api.getStudentEnrollments(userId);
        if (enrollmentsResponse.data) {
            displayEnrollments(enrollmentsResponse.data);
        }

        // Load results
        const resultsResponse = await api.getStudentResults(userId);
        if (resultsResponse.data) {
            displayResults(resultsResponse.data);
        }
    } catch (error) {
        console.error('Error loading dashboard:', error);
    }
}

function displayCourses(courses) {
    coursesList.innerHTML = courses.map(course => `
        <div class="course-item" onclick="selectCourse(${course.id})">
            <div class="item-title">${course.title}</div>
            <div class="item-subtitle">Duration: ${course.durationDays} days</div>
        </div>
    `).join('');
}

function displayEnrollments(enrollments) {
    if (enrollments.length === 0) {
        enrollmentsList.innerHTML = '<p>No enrollments yet</p>';
        return;
    }
    enrollmentsList.innerHTML = enrollments.map(enrollment => `
        <div class="enrollment-item">
            <div class="item-title">${enrollment.courseName}</div>
            <div class="item-subtitle">Status: ${enrollment.status} | Progress: ${enrollment.completionPercentage}%</div>
        </div>
    `).join('');
}

function displayResults(results) {
    if (results.length === 0) {
        resultsList.innerHTML = '<p>No results yet</p>';
        return;
    }
    resultsList.innerHTML = results.map(result => `
        <div class="result-item">
            <div class="item-title">${result.examTitle}</div>
            <div class="item-subtitle">Score: ${result.obtainedMarks}/${result.totalMarks} (${result.percentage.toFixed(2)}%) - ${result.status}</div>
        </div>
    `).join('');
}

async function selectCourse(courseId) {
    window.location.href = `/course.html?courseId=${courseId}`;
}

// Logout
document.getElementById('logout').addEventListener('click', (e) => {
    e.preventDefault();
    localStorage.clear();
    window.location.href = '/login.html';
});

// Load dashboard on page load
if (userId) {
    loadDashboard();
} else {
    window.location.href = '/login.html';
}
