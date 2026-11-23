/* =====================================================
   ADMIN - STUDENT MANAGEMENT
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
        active: "",
        courseId: ""
    };

    var searchTimer = null;
    var coursesLoaded = false;

    /* ---------------- data ---------------- */

    async function loadCourseOptions() {
        if (coursesLoaded) return;

        var select = UI.el("studentCourse");
        if (!select) return;

        try {
            var response = await UI.request("/courses");
            var courses = (response && response.data) ? response.data : (response || []);

            select.innerHTML = '<option value="">All courses</option>' +
                courses.map(function (course) {
                    var label = (course.title || "Course " + course.id) +
                        (course.courseCode ? " (" + course.courseCode + ")" : "");
                    return '<option value="' + course.id + '">' +
                        UI.escapeHtml(label) + "</option>";
                }).join("");

            coursesLoaded = true;

            if (state.courseId) select.value = String(state.courseId);

        } catch (error) {
            select.innerHTML = '<option value="">All courses</option>';
        }
    }

    /* ---------------- table ---------------- */

    function passRate(row) {
        var total = Number(row.totalResults || 0);
        if (!total) return null;
        return (Number(row.passedResults || 0) / total) * 100;
    }

    function renderRow(row) {
        var total = Number(row.totalResults || 0);
        var passed = Number(row.passedResults || 0);
        var rate = passRate(row);

        var outcome = rate === null
            ? '<span class="subtle">No results</span>'
            : UI.progressBar(rate) +
              '<small class="subtle">' + UI.number(passed) + " of " +
              UI.number(total) + " passed</small>";

        return "<tr>" +
            '<td data-label="Student"><span class="person-cell">' +
                '<span class="person-avatar">' + UI.escapeHtml(UI.initials(row.name)) + "</span>" +
                "<span><strong>" +
                    '<a class="row-link" href="/admin/student-detail.html?id=' + row.studentId + '">' +
                    UI.escapeHtml(row.name) + "</a></strong>" +
                "<small>ID " + UI.escapeHtml(row.studentId) + "</small></span>" +
            "</span></td>" +
            '<td data-label="Contact"><span class="person-cell"><span>' +
                "<strong>" + UI.escapeHtml(row.email || "-") + "</strong>" +
                "<small>" + UI.escapeHtml(row.phone || "No phone") + "</small>" +
            "</span></span></td>" +
            '<td data-label="Courses" class="num">' + UI.number(row.courseCount) + "</td>" +
            '<td data-label="Completed" class="num">' + UI.number(row.completedCourses) + "</td>" +
            '<td data-label="Results" class="num">' + UI.number(row.totalResults) + "</td>" +
            '<td data-label="Outcome" class="cell-wrap">' + outcome + "</td>" +
            '<td data-label="Status">' + UI.statusPill(row.active) + "</td>" +
            '<td data-label="Joined">' + UI.date(row.createdAt) + "</td>" +
            '<td data-label="Actions" class="cell-actions">' +
                '<a class="button button-light button-sm" href="/admin/student-detail.html?id=' +
                    row.studentId + '">View</a>' +
                '<button class="button button-light button-sm" type="button" data-act="toggle" data-id="' +
                    row.studentId + '" data-active="' + (row.active ? "1" : "0") +
                    '" data-name="' + UI.escapeHtml(row.name) + '">' +
                    (row.active ? "Deactivate" : "Activate") + "</button>" +
                '<button class="icon-button is-danger" type="button" data-act="delete" data-id="' +
                    row.studentId + '" data-name="' + UI.escapeHtml(row.name) +
                    '" title="Delete student" aria-label="Delete student">' +
                    icons.x + "</button>" +
            "</td>" +
            "</tr>";
    }

    async function loadStudents() {

        var body = UI.el("studentTableBody");
        if (body) body.innerHTML = UI.loadingRow(9);

        var params = UI.query({
            search: state.search,
            active: state.active,
            courseId: state.courseId,
            page: state.page,
            size: state.size
        });

        try {
            var page = await UI.request("/admin/students" + params);
            var rows = page.content || [];

            if (!rows.length) {
                body.innerHTML = UI.tableState(
                    9,
                    "No students found",
                    state.search || state.active || state.courseId
                        ? "Try a different search term or clear the filters."
                        : "Student accounts appear here after signup.");
            } else {
                body.innerHTML = rows.map(renderRow).join("");
            }

            UI.renderPager(UI.el("studentPager"), page, function (nextPage) {
                state.page = nextPage;
                loadStudents();
            });

            var message = UI.el("pageMessage");
            if (message) {
                message.textContent = "";
                message.className = "page-message";
            }

        } catch (error) {
            body.innerHTML = UI.tableState(9, "Unable to load students", error.message, true);
            UI.toast(error.message, "error");
        }
    }

    /* ---------------- actions ---------------- */

    async function toggleStatus(id, name, currentlyActive) {
        var next = !currentlyActive;

        var confirmed = await UI.confirm({
            title: (next ? "Activate " : "Deactivate ") + name + "?",
            message: next
                ? "The student will be able to sign in and continue their courses."
                : "The account is blocked immediately. Academic records stay intact and the student can be reactivated later.",
            confirmLabel: next ? "Activate account" : "Deactivate account",
            danger: !next
        });

        if (!confirmed) return;

        try {
            await UI.request("/admin/students/" + id + "/status", {
                method: "PATCH",
                body: { active: next }
            });

            UI.toast("Student account " + (next ? "activated" : "deactivated") + ".",
                "success", "Updated");
            loadStudents();

        } catch (error) {
            UI.toast(error.message, "error");
        }
    }

    async function deleteStudent(id, name) {
        var confirmed = await UI.confirm({
            title: "Delete " + name + "?",
            message: "If the student has enrollments, results or other academic records, the account is deactivated instead of removed.",
            confirmLabel: "Delete student"
        });

        if (!confirmed) return;

        try {
            var result = await UI.request("/admin/students/" + id, { method: "DELETE" });

            UI.toast(
                result && result.message ? result.message : "Student account deleted.",
                "success",
                result && result.mode === "DEACTIVATED" ? "Account deactivated" : "Deleted"
            );

            loadStudents();

        } catch (error) {
            UI.toast(error.message, "error");
        }
    }

    /* ---------------- events ---------------- */

    UI.el("studentSearch").addEventListener("input", function (event) {
        clearTimeout(searchTimer);
        var value = event.target.value.trim();

        searchTimer = setTimeout(function () {
            state.search = value;
            state.page = 0;
            loadStudents();
        }, 350);
    });

    UI.el("studentStatus").addEventListener("change", function (event) {
        state.active = event.target.value;
        state.page = 0;
        loadStudents();
    });

    UI.el("studentCourse").addEventListener("change", function (event) {
        state.courseId = event.target.value;
        state.page = 0;
        loadStudents();
    });

    UI.el("clearStudentFilters").addEventListener("click", function () {
        UI.el("studentSearch").value = "";
        UI.el("studentStatus").value = "";
        UI.el("studentCourse").value = "";
        state.search = "";
        state.active = "";
        state.courseId = "";
        state.page = 0;
        loadStudents();
    });

    UI.el("refreshStudents").addEventListener("click", loadStudents);

    UI.el("exportStudents").addEventListener("click", async function () {
        var button = UI.el("exportStudents");

        try {
            UI.setBusy(button, true, "Preparing...");
            await UI.download(
                "/admin/export/students.xlsx" + UI.query({
                    search: state.search,
                    active: state.active,
                    courseId: state.courseId
                }),
                "students.xlsx"
            );
        } catch (error) {
            UI.toast(error.message, "error");
        } finally {
            UI.setBusy(button, false);
        }
    });

    UI.el("studentTableBody").addEventListener("click", function (event) {
        var button = event.target.closest("button[data-act]");
        if (!button) return;

        var id = Number(button.dataset.id);

        if (button.dataset.act === "toggle") {
            toggleStatus(id, button.dataset.name, button.dataset.active === "1");
        } else if (button.dataset.act === "delete") {
            deleteStudent(id, button.dataset.name);
        }
    });

    /* ---------------- init ---------------- */

    UI.bindChrome();
    loadCourseOptions();
    loadStudents();
})();
