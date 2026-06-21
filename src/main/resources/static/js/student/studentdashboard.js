async function loadDashboard() {

    const token = localStorage.getItem("token");

    try {

        const response = await fetch(
            "http://localhost:8080/api/student/dashboard",
            {
                headers: {
                    "Authorization": "Bearer " + token
                }
            }
        );

        const data = await response.json();

        document.getElementById("totalCourses").innerText =
            data.totalCourses;

        document.getElementById("completedCourses").innerText =
            data.completedCourses;

        document.getElementById("pendingExams").innerText =
            data.pendingExams;

        document.getElementById("overallProgress").innerText =
            data.overallProgress + "%";

    } catch (error) {

        console.error("Dashboard Error:", error);

    }
}

async function loadResults() {

    const studentId =
        localStorage.getItem("userId");

    const token =
        localStorage.getItem("token");

    try {

        const response = await fetch(
            `http://localhost:8080/api/results/student/${studentId}`,
            {
                headers: {
                    "Authorization":
                        "Bearer " + token
                }
            }
        );

        const result =
            await response.json();

        const results =
            result.data;

        const tableBody =
            document.getElementById(
                "resultTableBody"
            );

        tableBody.innerHTML = "";

        if (!results || results.length === 0) {

            tableBody.innerHTML = `
                <tr>
                    <td colspan="4">
                        No Results Available
                    </td>
                </tr>
            `;

            return;
        }

        results.forEach(r => {

            tableBody.innerHTML += `
                <tr>
                    <td>${r.examTitle}</td>
                    <td>${r.obtainedMarks}/${r.totalMarks}</td>
                    <td>${r.percentage}%</td>
                    <td>${r.status}</td>
                </tr>
            `;

        });

    } catch (error) {

        console.error("Result Error:", error);

    }
}

loadDashboard();
loadResults();