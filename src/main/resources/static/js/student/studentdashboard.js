async function loadDashboard() {

    const token =
        localStorage.getItem("token");

    try {

        const response =
            await fetch(
                "http://localhost:8080/api/student/dashboard",
                {
                    headers: {
                        "Authorization":
                            "Bearer " + token
                    }
                }
            );

        const data =
            await response.json();

        document.getElementById(
            "totalCourses"
        ).innerText =
            data.totalCourses;

        document.getElementById(
            "completedCourses"
        ).innerText =
            data.completedCourses;

        document.getElementById(
            "pendingExams"
        ).innerText =
            data.pendingExams;

        document.getElementById(
            "overallProgress"
        ).innerText =
            data.overallProgress + "%";

    } catch(error){

        console.log(error);

    }
}

loadDashboard();