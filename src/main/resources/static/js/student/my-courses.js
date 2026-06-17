async function loadCourses() {

    try {

        const response =
            await fetch(
                "http://localhost:8080/api/student/courses/1"
            );

        const courses =
            await response.json();

        const container =
            document.getElementById(
                "coursesContainer"
            );

        container.innerHTML = "";

        courses.forEach(course => {

            container.innerHTML += `

            <div class="course-card">

                <div class="course-title">
                    ${course.courseTitle}
                </div>

                <div class="course-info">
                    Duration :
                    ${course.durationDays} Days
                </div>

                <div class="course-info">
                    Progress :
                    ${course.progress}%
                </div>

                <div class="progress-bar">
                    <div class="progress"
                         style="width:${course.progress}%">
                    </div>
                </div>

                <div class="status">
                    ${course.status}
                </div>

                <div class="buttons">

                    <button
                        class="btn material-btn">
                        Study Materials
                    </button>

                    <button
                        class="btn exam-btn">
                        Start Exam
                    </button>

                    <button
                        class="btn extension-btn">
                        Request Extension
                    </button>

                </div>

            </div>

            `;
        });

    } catch(error) {

        console.error(error);

    }
}

loadCourses();