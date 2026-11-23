/* =====================================================
   ADMIN - EXTENSION REQUESTS
===================================================== */
(function () {
    "use strict";

    var UI = window.AdminUI;

    if (!UI.guard()) return;

    var icons = UI.icons;

    var state = {
        page: 0,
        size: 10,
        search: "",
        status: "",
        courseId: "",
        facultyId: "",
        rejectingId: null
    };

    var cache = {};
    var searchTimer = null;
    var optionsLoaded = false;

    /* ---------------- icons ---------------- */

    document.querySelectorAll("[data-icon]").forEach(function (node) {
        var key = node.getAttribute("data-icon");
        if (icons[key]) node.innerHTML = icons[key];
    });

    /* ---------------- modal ---------------- */

    function openModal() {
        var node = UI.el("extensionModal");
        if (!node) return;
        node.classList.add("is-open");
        node.setAttribute("aria-hidden", "false");
        document.body.classList.add("as-lock");

        var focusable = node.querySelector("textarea, button");
        if (focusable) setTimeout(function () { focusable.focus(); }, 80);
    }

    function closeModal() {
        var node = UI.el("extensionModal");
        if (!node) return;
        node.classList.remove("is-open");
        node.setAttribute("aria-hidden", "true");

        if (!document.querySelector(".modal-overlay.is-open")) {
            document.body.classList.remove("as-lock");
        }
    }

    document.querySelectorAll("[data-close]").forEach(function (button) {
        button.addEventListener("click", closeModal);
    });

    document.querySelectorAll(".modal-overlay").forEach(function (overlay) {
        overlay.addEventListener("click", function (event) {
            if (event.target === overlay) closeModal();
        });
    });

    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape") {
            if (document.querySelector(".modal-overlay.is-open")) closeModal();
        }
    });

    /* ---------------- options + stats ---------------- */

    async function loadOptions() {
        if (optionsLoaded) return;

        var results = await Promise.allSettled([
            UI.request("/admin/approvals/options/courses"),
            UI.request("/admin/approvals/options/faculty")
        ]);

        var courseSelect = UI.el("extensionCourse");
        var facultySelect = UI.el("extensionFaculty");

        if (results[0].status === "fulfilled") {
            var courses = results[0].value || [];

            courseSelect.innerHTML = '<option value="">All courses</option>' +
                courses.map(function (course) {
                    var label = (course.title || "Course " + course.id) +
                        (course.courseCode ? " (" + course.courseCode + ")" : "");
                    return '<option value="' + course.id + '">' +
                        UI.escapeHtml(label) + "</option>";
                }).join("");
        }

        if (results[1].status === "fulfilled") {
            var faculty = results[1].value || [];

            facultySelect.innerHTML = '<option value="">All faculty</option>' +
                faculty.map(function (item) {
                    return '<option value="' + item.id + '">' +
                        UI.escapeHtml(item.name || item.email) + "</option>";
                }).join("");
        }

        optionsLoaded = true;
    }

    async function loadStats() {
        try {
            var stats = await UI.request("/admin/extensions/stats");

            setText("extensionPending", UI.number(stats.pending));
            setText("extensionApproved", UI.number(stats.approved));
            setText("extensionRejected", UI.number(stats.rejected));
            setText("extensionTotal", UI.number(stats.total));

        } catch (error) {
            /* KPI cards are decorative. */
        }
    }

    function setText(id, value) {
        var node = UI.el(id);
        if (node) node.textContent = value;
    }

    /* ---------------- table ---------------- */

    function reasonCell(row) {
        var parts = [];

        if (row.reason) {
            parts.push("<span>" + UI.escapeHtml(row.reason) + "</span>");
        }

        if (row.rejectionReason) {
            parts.push('<div class="subtle">Rejected: ' +
                UI.escapeHtml(row.rejectionReason) + "</div>");
        }

        if (row.reviewedByName) {
            parts.push('<div class="subtle">by ' +
                UI.escapeHtml(row.reviewedByName) + "</div>");
        }

        return parts.length ? parts.join("") : '<span class="subtle">-</span>';
    }

    function renderRow(row) {

        cache[row.id] = row;

        var actions = String(row.status || "").toUpperCase() === "PENDING"
            ? '<button class="button button-light button-sm" type="button" data-act="approve" data-id="' +
                row.id + '">Approve</button>' +
              '<button class="button button-danger-light button-sm" type="button" data-act="reject" data-id="' +
                row.id + '">Reject</button>'
            : '<span class="subtle">Reviewed</span>';

        return "<tr>" +
            '<td data-label="Student"><span class="person-cell">' +
                '<span class="person-avatar">' + UI.escapeHtml(UI.initials(row.studentName)) + "</span>" +
                "<span><strong>" +
                    (row.studentId
                        ? '<a class="row-link" href="/admin/student-detail.html?id=' + row.studentId + '">' +
                          UI.escapeHtml(row.studentName) + "</a>"
                        : UI.escapeHtml(row.studentName)) +
                    "</strong><small>" + UI.escapeHtml(row.email || "-") + "</small></span>" +
            "</span></td>" +
            '<td data-label="Course" class="cell-wrap">' +
                "<strong>" + UI.escapeHtml(row.courseTitle || "-") + "</strong>" +
                '<div class="subtle">' + UI.escapeHtml(row.facultyName || "Not assigned") + "</div>" +
            "</td>" +
            '<td data-label="Due date">' + UI.dateTime(row.dueDate) +
                '<div class="subtle">' + UI.escapeHtml(row.enrollmentStatus || "-") + "</div></td>" +
            '<td data-label="Days" class="num">' + UI.number(row.daysRequested) + "</td>" +
            '<td data-label="Requested">' + UI.dateTime(row.createdAt) + "</td>" +
            '<td data-label="Status">' + UI.approvalPill(row.status) + "</td>" +
            '<td data-label="Reason" class="cell-wrap">' + reasonCell(row) + "</td>" +
            '<td data-label="Actions" class="cell-actions">' + actions + "</td>" +
            "</tr>";
    }

    async function loadRequests() {

        var body = UI.el("extensionTableBody");
        if (body) body.innerHTML = UI.loadingRow(8);

        cache = {};

        var params = UI.query({
            search: state.search,
            status: state.status,
            courseId: state.courseId,
            facultyId: state.facultyId,
            page: state.page,
            size: state.size
        });

        try {
            var page = await UI.request("/admin/extensions" + params);
            var rows = page.content || [];

            if (!rows.length) {
                body.innerHTML = UI.tableState(
                    8,
                    "No extension requests found",
                    state.search || state.status || state.courseId || state.facultyId
                        ? "Try a different filter or clear the filters."
                        : "Requests appear here when a learner asks for more time."
                );
            } else {
                body.innerHTML = rows.map(renderRow).join("");
            }

            UI.renderPager(UI.el("extensionPager"), page, function (nextPage) {
                state.page = nextPage;
                loadRequests();
            });

            UI.showPageMessage("pageMessage", "");

        } catch (error) {
            body.innerHTML = UI.tableState(8, "Unable to load requests", error.message, true);
            UI.toast(error.message, "error");
        }
    }

    /* ---------------- actions ---------------- */

    async function approve(id) {

        var row = cache[id];
        if (!row) return;

        var confirmed = await UI.confirm({
            title: "Approve " + row.daysRequested + " extra day(s)?",
            message: (row.studentName || "The student") + "'s deadline moves from " +
                UI.dateTime(row.dueDate) + " to " +
                UI.dateTime(new Date(new Date(row.dueDate).getTime() +
                    Number(row.daysRequested) * 86400000)) +
                ". The enrollment is reactivated if the new date is in the future.",
            confirmLabel: "Approve extension",
            danger: false
        });

        if (!confirmed) return;

        try {
            await UI.request("/admin/extensions/" + id + "/approve", {
                method: "POST"
            });

            UI.toast("The deadline was extended and the enrollment updated.",
                "success", "Approved");

            loadRequests();
            loadStats();

        } catch (error) {
            UI.toast(error.message, "error");
        }
    }

    function startReject(id) {

        var row = cache[id];
        if (!row) return;

        state.rejectingId = id;

        UI.el("extensionModalTitle").textContent = "Reject extension";
        UI.el("extensionModalSubtitle").textContent =
            (row.studentName || "This student") + " asked for " +
            row.daysRequested + " extra day(s) for " +
            (row.courseTitle || "this course") + ".";

        UI.el("extensionReason").value = "";

        openModal();
    }

    async function submitReject(event) {

        event.preventDefault();

        var reason = UI.el("extensionReason").value.trim();

        if (!reason) {
            UI.toast("A rejection reason is required.", "error");
            UI.el("extensionReason").focus();
            return;
        }

        var button = UI.el("extensionRejectSubmit");

        try {
            UI.setBusy(button, true, "Rejecting...");

            await UI.request("/admin/extensions/" + state.rejectingId + "/reject", {
                method: "POST",
                body: { reason: reason }
            });

            closeModal();
            UI.toast("The extension request was rejected.", "success", "Rejected");

            loadRequests();
            loadStats();

        } catch (error) {
            UI.toast(error.message, "error");
        } finally {
            UI.setBusy(button, false);
        }
    }

    /* ---------------- events ---------------- */

    UI.el("extensionRejectForm").addEventListener("submit", submitReject);

    UI.el("extensionSearch").addEventListener("input", function (event) {
        clearTimeout(searchTimer);
        var value = event.target.value.trim();

        searchTimer = setTimeout(function () {
            state.search = value;
            state.page = 0;
            loadRequests();
        }, 350);
    });

    UI.el("extensionStatus").addEventListener("change", function (event) {
        state.status = event.target.value;
        state.page = 0;
        loadRequests();
    });

    UI.el("extensionCourse").addEventListener("change", function (event) {
        state.courseId = event.target.value;
        state.page = 0;
        loadRequests();
    });

    UI.el("extensionFaculty").addEventListener("change", function (event) {
        state.facultyId = event.target.value;
        state.page = 0;
        loadRequests();
    });

    UI.el("clearExtensionFilters").addEventListener("click", function () {
        UI.el("extensionSearch").value = "";
        UI.el("extensionStatus").value = "";
        UI.el("extensionCourse").value = "";
        UI.el("extensionFaculty").value = "";
        state.search = "";
        state.status = "";
        state.courseId = "";
        state.facultyId = "";
        state.page = 0;
        loadRequests();
    });

    UI.el("refreshExtensions").addEventListener("click", function () {
        loadRequests();
        loadStats();
    });

    UI.el("exportExtensions").addEventListener("click", async function () {
        var button = UI.el("exportExtensions");

        try {
            UI.setBusy(button, true, "Preparing...");
            await UI.download(
                "/admin/export/extensions.xlsx" + UI.query({
                    search: state.search,
                    status: state.status,
                    courseId: state.courseId,
                    facultyId: state.facultyId
                }),
                "extension-requests.xlsx"
            );
        } catch (error) {
            UI.toast(error.message, "error");
        } finally {
            UI.setBusy(button, false);
        }
    });

    UI.el("extensionTableBody").addEventListener("click", function (event) {
        var button = event.target.closest("button[data-act]");
        if (!button) return;

        var id = Number(button.dataset.id);

        if (button.dataset.act === "approve") {
            approve(id);
        } else if (button.dataset.act === "reject") {
            startReject(id);
        }
    });

    /* ---------------- init ---------------- */

    UI.bindChrome();
    loadOptions();
    loadStats();
    loadRequests();
})();
