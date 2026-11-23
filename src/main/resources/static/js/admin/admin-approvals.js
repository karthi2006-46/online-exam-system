/* =====================================================
   ADMIN - EXAM APPROVALS
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
        facultyId: ""
    };

    var review = { id: null, action: null };
    var searchTimer = null;
    var optionsLoaded = false;
    var cache = {};

    /* ---------------- icons ---------------- */

    document.querySelectorAll("[data-icon]").forEach(function (node) {
        var key = node.getAttribute("data-icon");
        if (icons[key]) node.innerHTML = icons[key];
    });

    /* ---------------- modal ---------------- */

    function openModal() {
        var node = UI.el("reviewModal");
        if (!node) return;
        node.classList.add("is-open");
        node.setAttribute("aria-hidden", "false");
        document.body.classList.add("as-lock");

        var focusable = node.querySelector("textarea, button");
        if (focusable) setTimeout(function () { focusable.focus(); }, 80);
    }

    function closeModal() {
        var node = UI.el("reviewModal");
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

    function showReview(row, action) {

        review.id = row.id;
        review.action = action;

        var approve = action === "approve";

        UI.el("reviewModalTitle").textContent =
            approve ? "Approve exam access" : "Reject exam access";

        UI.el("reviewModalSubtitle").textContent =
            (row.studentName || "This student") + " for " +
            (row.courseTitle || "this course") + ".";

        UI.el("reviewComment").value = "";
        UI.el("reviewComment").placeholder = approve
            ? "Optional note for the student and faculty"
            : "Optional reason for the rejection";

        var submit = UI.el("reviewSubmit");
        submit.textContent = approve ? "Approve request" : "Reject request";
        submit.classList.toggle("button-primary", approve);
        submit.classList.toggle("button-danger", !approve);

        openModal();
    }

    /* ---------------- options + stats ---------------- */

    async function loadOptions() {
        if (optionsLoaded) return;

        var results = await Promise.allSettled([
            UI.request("/admin/approvals/options/courses"),
            UI.request("/admin/approvals/options/faculty")
        ]);

        var courseSelect = UI.el("approvalCourse");
        var facultySelect = UI.el("approvalFaculty");

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
            var stats = await UI.request("/admin/approvals/stats");

            setText("approvalPending", UI.number(stats.pending));
            setText("approvalApproved", UI.number(stats.approved));
            setText("approvalRejected", UI.number(stats.rejected));
            setText("approvalTotal", UI.number(stats.total));

        } catch (error) {
            /* KPI cards are decorative. */
        }
    }

    function setText(id, value) {
        var node = UI.el(id);
        if (node) node.textContent = value;
    }

    /* ---------------- table ---------------- */

    function commentCell(text) {
        if (!text) return '<span class="subtle">-</span>';

        var value = String(text);
        var short = value.length > 70 ? value.slice(0, 70) + "..." : value;

        return '<span title="' + UI.escapeHtml(value) + '">' +
            UI.escapeHtml(short) + "</span>";
    }

    function renderRow(row) {

        cache[row.id] = row;

        var actions = row.status === "PENDING"
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
                "<div class=\"subtle\">" + UI.escapeHtml(row.courseCode || "") + "</div>" +
            "</td>" +
            '<td data-label="Faculty" class="cell-wrap">' +
                UI.escapeHtml(row.facultyName || "Not assigned") + "</td>" +
            '<td data-label="Requested">' + UI.dateTime(row.requestedAt) + "</td>" +
            '<td data-label="Reviewed">' + (row.reviewedAt
                ? UI.dateTime(row.reviewedAt)
                : '<span class="subtle">Awaiting review</span>') + "</td>" +
            '<td data-label="Status">' + UI.approvalPill(row.status) + "</td>" +
            '<td data-label="Comment" class="cell-wrap">' + commentCell(row.comment) + "</td>" +
            '<td data-label="Actions" class="cell-actions">' + actions + "</td>" +
            "</tr>";
    }

    async function loadApprovals() {

        var body = UI.el("approvalTableBody");
        if (body) body.innerHTML = UI.loadingRow(8);

        var params = UI.query({
            search: state.search,
            status: state.status,
            courseId: state.courseId,
            facultyId: state.facultyId,
            page: state.page,
            size: state.size
        });

        try {
            var page = await UI.request("/admin/approvals" + params);
            var rows = page.content || [];

            if (!rows.length) {
                body.innerHTML = UI.tableState(
                    8,
                    "No approval requests found",
                    state.search || state.status || state.courseId || state.facultyId
                        ? "Try a different filter or clear the filters."
                        : "Students will appear here once they request exam approval."
                );
            } else {
                body.innerHTML = rows.map(renderRow).join("");
            }

            UI.renderPager(UI.el("approvalPager"), page, function (nextPage) {
                state.page = nextPage;
                loadApprovals();
            });

            UI.showPageMessage("pageMessage", "");

        } catch (error) {
            body.innerHTML = UI.tableState(8, "Unable to load requests", error.message, true);
            UI.toast(error.message, "error");
        }
    }

    /* ---------------- review ---------------- */

    async function submitReview(event) {

        event.preventDefault();

        if (!review.id || !review.action) return;

        var comment = UI.el("reviewComment").value.trim();
        var button = UI.el("reviewSubmit");
        var action = review.action;
        var id = review.id;

        if (action === "reject" && !comment) {
            UI.toast("Add a short reason so the student knows what to fix.",
                "error");
            UI.el("reviewComment").focus();
            return;
        }

        try {
            UI.setBusy(button, true, action === "approve" ? "Approving..." : "Rejecting...");

            await UI.request(
                "/admin/approvals/" + id + "/" + action,
                { method: "POST", body: { comment: comment } }
            );

            closeModal();

            UI.toast(
                action === "approve"
                    ? "The student can now access exams for this course."
                    : "The request was rejected. The student may reapply when eligible.",
                "success",
                action === "approve" ? "Approved" : "Rejected"
            );

            loadApprovals();
            loadStats();

        } catch (error) {
            UI.toast(error.message, "error");
        } finally {
            UI.setBusy(button, false);
        }
    }

    /* ---------------- events ---------------- */

    UI.el("reviewForm").addEventListener("submit", submitReview);

    UI.el("approvalSearch").addEventListener("input", function (event) {
        clearTimeout(searchTimer);
        var value = event.target.value.trim();

        searchTimer = setTimeout(function () {
            state.search = value;
            state.page = 0;
            loadApprovals();
        }, 350);
    });

    UI.el("approvalStatus").addEventListener("change", function (event) {
        state.status = event.target.value;
        state.page = 0;
        loadApprovals();
    });

    UI.el("approvalCourse").addEventListener("change", function (event) {
        state.courseId = event.target.value;
        state.page = 0;
        loadApprovals();
    });

    UI.el("approvalFaculty").addEventListener("change", function (event) {
        state.facultyId = event.target.value;
        state.page = 0;
        loadApprovals();
    });

    UI.el("clearApprovalFilters").addEventListener("click", function () {
        UI.el("approvalSearch").value = "";
        UI.el("approvalStatus").value = "";
        UI.el("approvalCourse").value = "";
        UI.el("approvalFaculty").value = "";
        state.search = "";
        state.status = "";
        state.courseId = "";
        state.facultyId = "";
        state.page = 0;
        loadApprovals();
    });

    UI.el("refreshApprovals").addEventListener("click", function () {
        loadApprovals();
        loadStats();
    });

    UI.el("exportApprovals").addEventListener("click", async function () {
        var button = UI.el("exportApprovals");

        try {
            UI.setBusy(button, true, "Preparing...");
            await UI.download(
                "/admin/export/approvals.xlsx" + UI.query({
                    search: state.search,
                    status: state.status,
                    courseId: state.courseId,
                    facultyId: state.facultyId
                }),
                "exam-approvals.xlsx"
            );
        } catch (error) {
            UI.toast(error.message, "error");
        } finally {
            UI.setBusy(button, false);
        }
    });

    UI.el("approvalTableBody").addEventListener("click", function (event) {
        var button = event.target.closest("button[data-act]");
        if (!button) return;

        var id = Number(button.dataset.id);
        var row = cache[id];

        if (!row) return;

        showReview(row, button.dataset.act);
    });

    /* ---------------- init ---------------- */

    UI.bindChrome();
    loadOptions();
    loadStats();
    loadApprovals();
})();
