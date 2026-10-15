const API_BASE =
    "http://localhost:8080/api/exam-approval";


// ==========================================
// LOAD PENDING REQUESTS
// ==========================================

async function loadApprovalRequests() {

    const container =
        document.getElementById("requestsContainer");

    try {

        const token =
            localStorage.getItem("token");

        console.log("Loading approval requests...");

        const response =
            await fetch(
                `${API_BASE}/pending`,
                {
                    method: "GET",

                    headers: {
                        "Authorization":
                            `Bearer ${token}`
                    }
                }
            );

        console.log(
            "Approval API Status:",
            response.status
        );

        if (!response.ok) {

            const errorText =
                await response.text();

            console.error(
                "Approval API Error:",
                errorText
            );

            throw new Error(
                "Failed to load approval requests"
            );
        }

        const requests =
            await response.json();

        console.log(
            "Pending Requests:",
            requests
        );

        container.innerHTML = "";

        if (
            !requests ||
            requests.length === 0
        ) {

            container.innerHTML = `
                <div class="no-requests">
                    No pending exam approval requests.
                </div>
            `;

            return;
        }


        // ==========================================
        // DISPLAY REQUESTS
        // ==========================================

        requests.forEach(request => {

            const student =
                request.student;

            const course =
                request.course;


            const studentName =
                student
                    ? `${student.firstName || ""} ${student.lastName || ""}`
                    : `Student ID: ${request.studentId || "-"}`;


            const studentId =
                student
                    ? student.id
                    : request.studentId;


            const courseName =
                course
                    ? course.title
                    : `Course ID: ${request.courseId || "-"}`;


            const courseId =
                course
                    ? course.id
                    : request.courseId;


            const requestedAt =
                request.requestedAt
                    ? new Date(
                        request.requestedAt
                    ).toLocaleString()
                    : "-";


            container.innerHTML += `

                <div class="request-card">

                    <h3>
                        Exam Approval Request
                    </h3>

                    <p class="request-info">
                        <strong>Student:</strong>
                        ${studentName}
                    </p>

                    <p class="request-info">
                        <strong>Student ID:</strong>
                        ${studentId || "-"}
                    </p>

                    <p class="request-info">
                        <strong>Course:</strong>
                        ${courseName}
                    </p>

                    <p class="request-info">
                        <strong>Course ID:</strong>
                        ${courseId || "-"}
                    </p>

                    <p class="request-info">
                        <strong>Requested At:</strong>
                        ${requestedAt}
                    </p>

                    <p>
                        <span class="status">
                            ${request.status}
                        </span>
                    </p>

                    <div class="actions">

                        <button
                            class="approve-btn"
                            onclick="
                                approveRequest(
                                    ${request.id}
                                )
                            ">
                            Approve
                        </button>

                        <button
                            class="reject-btn"
                            onclick="
                                rejectRequest(
                                    ${request.id}
                                )
                            ">
                            Reject
                        </button>

                    </div>

                </div>

            `;

        });

    } catch (error) {

        console.error(
            "Load Approval Requests Error:",
            error
        );

        container.innerHTML = `
            <div class="error">
                Unable to load exam approval requests.
                Please check the backend.
            </div>
        `;
    }
}



// ==========================================
// APPROVE REQUEST
// ==========================================

async function approveRequest(requestId) {

    const comment =
        prompt(
            "Enter approval comment (optional):"
        );


    try {

        const token =
            localStorage.getItem("token");


        let url =
            `${API_BASE}/${requestId}/approve`;


        if (comment) {

            url +=
                `?comment=${encodeURIComponent(comment)}`;

        }


        const response =
            await fetch(
                url,
                {
                    method: "PUT",

                    headers: {
                        "Authorization":
                            `Bearer ${token}`
                    }
                }
            );


        if (!response.ok) {

            const errorText =
                await response.text();

            console.error(
                "Approve Error:",
                errorText
            );

            alert(
                "Failed to approve request"
            );

            return;
        }


        alert(
            "Exam approval request approved successfully."
        );


        // Reload pending requests
        loadApprovalRequests();


    } catch (error) {

        console.error(
            "Approve Request Error:",
            error
        );

        alert(
            "Unable to approve request."
        );
    }
}



// ==========================================
// REJECT REQUEST
// ==========================================

async function rejectRequest(requestId) {

    const comment =
        prompt(
            "Enter rejection reason:"
        );


    if (
        comment === null
    ) {
        return;
    }


    try {

        const token =
            localStorage.getItem("token");


        let url =
            `${API_BASE}/${requestId}/reject`;


        if (comment) {

            url +=
                `?comment=${encodeURIComponent(comment)}`;

        }


        const response =
            await fetch(
                url,
                {
                    method: "PUT",

                    headers: {
                        "Authorization":
                            `Bearer ${token}`
                    }
                }
            );


        if (!response.ok) {

            const errorText =
                await response.text();

            console.error(
                "Reject Error:",
                errorText
            );

            alert(
                "Failed to reject request"
            );

            return;
        }


        alert(
            "Exam approval request rejected."
        );


        // Reload pending requests
        loadApprovalRequests();


    } catch (error) {

        console.error(
            "Reject Request Error:",
            error
        );

        alert(
            "Unable to reject request."
        );
    }
}



// ==========================================
// START
// ==========================================

loadApprovalRequests();