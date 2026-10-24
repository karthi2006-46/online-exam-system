const API_BASE_URL = "/api/extensions";

let requestToReject = null;

function getToken() {
    return localStorage.getItem("token");
}

function showMessage(text, type = "success") {
    const message = document.getElementById("message");

    message.textContent = text;
    message.className = `message ${type}`;
    message.hidden = false;
}

function hideMessage() {
    document.getElementById("message").hidden = true;
}

function setLoading(loading) {
    document.getElementById("loading").hidden = !loading;
}

function getRequestArray(response) {
    if (Array.isArray(response)) return response;
    if (Array.isArray(response?.data)) return response.data;
    if (Array.isArray(response?.data?.content)) {
        return response.data.content;
    }

    return [];
}

async function apiRequest(url, method = "GET", data = null) {
    const options = {
        method,
        headers: {
            "Authorization": `Bearer ${getToken()}`,
            "Content-Type": "application/json"
        }
    };

    if (data !== null) {
        options.body = JSON.stringify(data);
    }

    const response = await fetch(url, options);

    let result = {};

    try {
        result = await response.json();
    } catch {
        // Some error responses may have no JSON body.
    }

    if (!response.ok) {
        throw new Error(
            result.message ||
            `Request failed with HTTP status ${response.status}`
        );
    }

    if (result.success === false) {
        throw new Error(result.message || "The request failed.");
    }

    return result;
}

async function loadPendingRequests() {
    hideMessage();
    setLoading(true);

    document.getElementById("tableWrapper").hidden = true;
    document.getElementById("emptyState").hidden = true;

    try {
        const response = await apiRequest(`${API_BASE_URL}/pending`);
        const requests = getRequestArray(response);

        document.getElementById("pendingTotal").textContent =
            requests.length;

        renderRequests(requests);

    } catch (error) {
        console.error("Unable to load extension requests:", error);

        showMessage(
            `${error.message}. Check your faculty login and server logs.`,
            "error"
        );

    } finally {
        setLoading(false);
    }
}

function renderRequests(requests) {
    const tbody = document.getElementById("requestTableBody");
    tbody.replaceChildren();

    if (requests.length === 0) {
        document.getElementById("emptyState").hidden = false;
        document.getElementById("tableWrapper").hidden = true;
        return;
    }

    requests.forEach(request => {
        const row = document.createElement("tr");

        const idCell = document.createElement("td");
        idCell.textContent = request.id ?? "—";

        const enrollmentCell = document.createElement("td");
        enrollmentCell.textContent = request.enrollmentId ?? "—";

        const daysCell = document.createElement("td");
        daysCell.textContent = request.daysRequested ?? "—";

        const reasonCell = document.createElement("td");
        reasonCell.className = "reason-cell";
        reasonCell.textContent = request.reason || "No reason provided";

        const statusCell = document.createElement("td");
        const pill = document.createElement("span");
        pill.className = "status-pill";
        pill.textContent = request.status || "PENDING";
        statusCell.appendChild(pill);

        const actionsCell = document.createElement("td");
        const buttons = document.createElement("div");
        buttons.className = "action-buttons";

        const approveButton = document.createElement("button");
        approveButton.className = "button button-small button-success";
        approveButton.textContent = "Approve";
        approveButton.addEventListener("click", () => {
            approveRequest(request.id);
        });

        const rejectButton = document.createElement("button");
        rejectButton.className = "button button-small button-danger";
        rejectButton.textContent = "Reject";
        rejectButton.addEventListener("click", () => {
            openRejectModal(request.id);
        });

        buttons.append(approveButton, rejectButton);
        actionsCell.appendChild(buttons);

        row.append(
            idCell,
            enrollmentCell,
            daysCell,
            reasonCell,
            statusCell,
            actionsCell
        );

        tbody.appendChild(row);
    });

    document.getElementById("tableWrapper").hidden = false;
    document.getElementById("emptyState").hidden = true;
}

async function approveRequest(requestId) {
    const confirmed = window.confirm(
        `Approve extension request ${requestId}? The requested days will be added to the enrollment due date.`
    );

    if (!confirmed) return;

    hideMessage();

    try {
        await apiRequest(
            `${API_BASE_URL}/${encodeURIComponent(requestId)}/approve`,
            "PUT"
        );

        showMessage(
            `Extension request ${requestId} approved successfully. The enrollment due date should now be extended.`,
            "success"
        );

        await loadPendingRequests();

    } catch (error) {
        console.error("Approval failed:", error);
        showMessage(error.message, "error");
    }
}

function openRejectModal(requestId) {
    requestToReject = requestId;

    document.getElementById("rejectionReason").value = "";
    document.getElementById("rejectModal").hidden = false;
}

function closeRejectModal() {
    requestToReject = null;
    document.getElementById("rejectModal").hidden = true;
}

async function rejectRequest() {
    const reason = document
        .getElementById("rejectionReason")
        .value
        .trim();

    if (!requestToReject) return;

    if (!reason) {
        alert("Please enter a rejection reason.");
        return;
    }

    const requestId = requestToReject;
    const confirmButton = document.getElementById("confirmReject");

    confirmButton.disabled = true;

    try {
        const query = new URLSearchParams({
            rejectionReason: reason
        });

        await apiRequest(
            `${API_BASE_URL}/${encodeURIComponent(requestId)}/reject?${query}`,
            "PUT"
        );

        closeRejectModal();

        showMessage(
            `Extension request ${requestId} rejected successfully.`,
            "success"
        );

        await loadPendingRequests();

    } catch (error) {
        console.error("Rejection failed:", error);
        showMessage(error.message, "error");

    } finally {
        confirmButton.disabled = false;
    }
}

document.addEventListener("DOMContentLoaded", () => {
    document.getElementById("year").textContent =
        new Date().getFullYear();

    document.getElementById("refreshButton")
        .addEventListener("click", loadPendingRequests);

    document.getElementById("confirmReject")
        .addEventListener("click", rejectRequest);

    document.getElementById("closeModal")
        .addEventListener("click", closeRejectModal);

    document.getElementById("cancelReject")
        .addEventListener("click", closeRejectModal);

    document.getElementById("rejectModal")
        .addEventListener("click", event => {
            if (event.target.id === "rejectModal") {
                closeRejectModal();
            }
        });

    loadPendingRequests();
});