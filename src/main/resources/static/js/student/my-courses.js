async function loadCourses() {

    try {

        const studentId =
            localStorage.getItem("userId") || 1;

        console.log("Student ID:", studentId);

        const response =
            await fetch(
                `http://localhost:8080/api/student/courses/${studentId}`
            );

        const courses =
            await response.json();

        console.log("Courses:", courses);

        const container =
            document.getElementById("coursesContainer");

        if (!container) {
            console.error("coursesContainer not found");
            return;
        }

        container.innerHTML = "";

        courses.forEach(course => {

            container.innerHTML += `

            <div class="course-card">

                <h3>${course.courseTitle}</h3>

                <p>
                    Duration :
                    ${course.durationDays} Days
                </p>

                <p>
                    Progress :
                    ${course.progress}%
                </p>

                <p>
                    Status :
                    ${course.status}
                </p>

                <button
                    onclick="startExam(${course.courseId})">
                    Start Exam
                </button>

            </div>

            `;
        });

    } catch(error) {

        console.error(
            "Load Courses Error:",
            error
        );
    }
}

async function startExam(courseId) {

    try {

        const response =
            await fetch(
                `http://localhost:8080/api/exams/course/${courseId}/published`
            );

        const result =
            await response.json();

        console.log("Exam Result:", result);

        const exams =
            result.data;

        if (!exams || exams.length === 0) {

            alert(
                "No published exam found"
            );

            return;
        }

        const examId =
            exams[0].id;

        localStorage.setItem(
            "examId",
            examId
        );

        window.location.href =
            `exam.html?examId=${examId}`;

    } catch(error) {

        console.error(
            "Exam Error:",
            error
        );
    }
}

loadCourses();