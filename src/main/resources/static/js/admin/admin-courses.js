/* =====================================================
   ADMIN - COURSE MANAGEMENT
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
        facultyId: "",
        editingId: null,
        editingFacultyId: null
    };

    var searchTimer = null;
    var facultyLoaded = false;
    var cache = {};

    var money = new Intl.NumberFormat("en-IN", {
        style: "currency",
        currency: "INR",
        maximumFractionDigits: 0
    });

    /* ---------------- icons ---------------- */

    document.querySelectorAll("[data-icon]").forEach(function (node) {
        var key = node.getAttribute("data-icon");
        if (icons[key]) node.innerHTML = icons[key];
    });

    /* ---------------- modal ---------------- */

    function openModal() {
        var node = UI.el("courseModal");
        if (!node) return;
        node.classList.add("is-open");
        node.setAttribute("aria-hidden", "false");
        document.body.classList.add("as-lock");

        var focusable = node.querySelector("input, select, textarea, button");
        if (focusable) setTimeout(function () { focusable.focus(); }, 80);
    }

    function closeModal() {
        var node = UI.el("courseModal");
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
        if (event.key !== "Escape") return;
        var open = document.querySelector(".modal-overlay.is-open");
        if (open) closeModal();
    });

    /* ---------------- options + stats ---------------- */

    async function loadFacultyOptions() {
        if (facultyLoaded) return;

        var filter = UI.el("courseFaculty");
        var select = UI.el("courseFacultySelect");

        try {
            var faculty = await UI.request("/admin/approvals/options/faculty");

            var options = (faculty || []).map(function (item) {
                return '<option value="' + item.id + '">' +
                    UI.escapeHtml(item.name || item.email || ("Faculty " + item.id)) +
                    "</option>";
            }).join("");

            if (filter) {
                filter.innerHTML = '<option value="">All faculty</option>' + options;
                if (state.facultyId) filter.value = String(state.facultyId);
            }

            if (select) {
                select.innerHTML = '<option value="">Select faculty</option>' + options;
            }

            facultyLoaded = true;

        } catch (error) {
            if (filter) filter.innerHTML = '<option value="">All faculty</option>';
        }
    }

    async function loadStats() {
        try {
            var report = await UI.request("/admin/reports/overview");

            setText("courseTotal", UI.number(report.totalCourses));
            setText("courseActive", UI.number(report.activeCourses));
            setText("courseStudents", UI.number(report.totalEnrollments));
            setText("coursePass", UI.percent(report.passPercentage, 1));

        } catch (error) {
            /* KPI cards are decorative; the table carries the message. */
        }
    }

    function setText(id, value) {
        var node = UI.el(id);
        if (node) node.textContent = value;
    }

    /* ---------------- table ---------------- */

    function renderRow(row) {
        var students = '<strong>' + UI.number(row.totalStudents) + "</strong>" +
            '<div class="subtle">' + UI.number(row.activeStudents) + " active</div>";

        var pass = Number(row.totalResults || 0)
            ? UI.progressBar(row.passPercentage) +
              '<div class="subtle">' + UI.number(row.passedResults) + " of " +
              UI.number(row.totalResults) + " passed</div>"
            : '<span class="subtle">No results yet</span>';

        var faculty = row.facultyName
            ? "<strong>" + UI.escapeHtml(row.facultyName) + "</strong>" +
              (row.facultyEmail
                  ? '<div class="subtle">' + UI.escapeHtml(row.facultyEmail) + "</div>"
                  : "")
            : '<span class="subtle">Not assigned</span>';

        return "<tr>" +
            '<td data-label="Course"><span class="person-cell">' +
                '<span class="person-avatar">' + UI.escapeHtml(UI.initials(row.title)) + "</span>" +
                "<span><strong>" + UI.escapeHtml(row.title) + "</strong>" +
                "<small>" + UI.escapeHtml(row.courseCode || "-") +
                " &middot; ID " + UI.escapeHtml(row.id) + "</small></span>" +
            "</span></td>" +
            '<td data-label="Faculty" class="cell-wrap">' + faculty + "</td>" +
            '<td data-label="Duration" class="num">' + UI.number(row.durationDays) + " d</td>" +
            '<td data-label="Fees" class="num">' + money.format(Number(row.totalFees || 0)) + "</td>" +
            '<td data-label="Students" class="num">' + students + "</td>" +
            '<td data-label="Exams" class="num">' + UI.number(row.totalExams) + "</td>" +
            '<td data-label="Materials" class="num">' + UI.number(row.totalMaterials) + "</td>" +
            '<td data-label="Pass rate" class="cell-wrap">' + pass + "</td>" +
            '<td data-label="Status">' + UI.statusPill(row.active) + "</td>" +
            '<td data-label="Actions" class="cell-actions">' +
                '<button class="button button-light button-sm" type="button" data-act="edit" data-id="' +
                    row.id + '">Edit</button>' +
                '<button class="button button-light button-sm" type="button" data-act="toggle" data-id="' +
                    row.id + '" data-active="' + (row.active ? "1" : "0") + '">' +
                    (row.active ? "Deactivate" : "Activate") + "</button>" +
                '<button class="icon-button is-danger" type="button" data-act="delete" data-id="' +
                    row.id + '" title="Delete course" aria-label="Delete course">' +
                    icons.x + "</button>" +
            "</td>" +
            "</tr>";
    }

    async function loadCourses() {

        var body = UI.el("courseTableBody");
        if (body) body.innerHTML = UI.loadingRow(10);

        cache = {};

        var params = UI.query({
            search: state.search,
            active: state.active,
            facultyId: state.facultyId,
            page: state.page,
            size: state.size
        });

        try {
            var page = await UI.request("/admin/courses" + params);
            var rows = page.content || [];

            rows.forEach(function (row) { cache[row.id] = row; });

            if (!rows.length) {
                body.innerHTML = UI.tableState(
                    10,
                    "No courses found",
                    state.search || state.active || state.facultyId
                        ? "Try a different search term or clear the filters."
                        : "Create the first course with the Add course button."
                );
            } else {
                body.innerHTML = rows.map(renderRow).join("");
            }

            UI.renderPager(UI.el("coursePager"), page, function (nextPage) {
                state.page = nextPage;
                loadCourses();
            });

            UI.showPageMessage("pageMessage", "");

        } catch (error) {
            body.innerHTML = UI.tableState(10, "Unable to load courses", error.message, true);
            UI.toast(error.message, "error");
        }
    }

    /* ---------------- form ---------------- */

    function resetForm() {
        state.editingId = null;
        state.editingFacultyId = null;

        UI.el("courseForm").reset();
        UI.el("courseActive").checked = true;
        UI.el("courseDuration").value = 30;
        UI.el("courseFees").value = 0;

        UI.el("courseModalTitle").textContent = "Add course";
        UI.el("courseModalSubtitle").textContent =
            "Create a course. Faculty can be assigned later.";
        UI.el("saveCourse").textContent = "Create course";
    }

    function startEdit(id) {

        var row = cache[id];

        if (!row) {
            UI.toast("Reload the list and try again.", "error");
            return;
        }

        state.editingId = Number(id);
        state.editingFacultyId = row.facultyId == null ? null : Number(row.facultyId);

        UI.el("courseTitle").value = row.title || "";
        UI.el("courseCode").value = row.courseCode || "";
        UI.el("courseDuration").value = row.durationDays == null ? 30 : row.durationDays;
        UI.el("courseFees").value = row.totalFees == null ? 0 : row.totalFees;
        UI.el("courseDescription").value = row.description || "";
        UI.el("courseActive").checked = row.active !== false;

        UI.el("courseModalTitle").textContent = "Edit course";
        UI.el("courseModalSubtitle").textContent = "Update the course details and ownership.";
        UI.el("saveCourse").textContent = "Save changes";

        openModal();
    }

    async function submitCourse(event) {

        event.preventDefault();

        var title = UI.el("courseTitle").value.trim();
        var code = UI.el("courseCode").value.trim();
        var duration = Number(UI.el("courseDuration").value);
        var fees = Number(UI.el("courseFees").value);

        if (!title) {
            UI.toast("Enter a course title.", "error");
            UI.el("courseTitle").focus();
            return;
        }

        if (!code) {
            UI.toast("Enter a course code.", "error");
            UI.el("courseCode").focus();
            return;
        }

        if (!duration || duration < 1) {
            UI.toast("Duration must be at least 1 day.", "error");
            UI.el("courseDuration").focus();
            return;
        }

        if (isNaN(fees) || fees < 0) {
            UI.toast("Fees cannot be negative.", "error");
            UI.el("courseFees").focus();
            return;
        }

        var payload = {
            title: title,
            courseCode: code.toUpperCase(),
            facultyId: state.editingId ? state.editingFacultyId : null,
            durationDays: duration,
            totalFees: fees,
            description: UI.el("courseDescription").value.trim(),
            active: UI.el("courseActive").checked
        };

        var button = UI.el("saveCourse");
        var editing = state.editingId;

        try {
            UI.setBusy(button, true, editing ? "Saving..." : "Creating...");

            await UI.request(
                editing
                    ? "/admin/courses/" + editing
                    : "/admin/courses",
                {
                    method: editing ? "PUT" : "POST",
                    body: payload
                }
            );

            closeModal();
            UI.toast(
                editing ? "Course details updated." : "Course created successfully.",
                "success",
                editing ? "Updated" : "Created"
            );

            state.page = 0;
            loadCourses();
            loadStats();

        } catch (error) {
            UI.toast(error.message, "error");
        } finally {
            UI.setBusy(button, false);
        }
    }

    /* ---------------- actions ---------------- */

    async function toggleCourse(id, active) {

        var next = !active;

        var confirmed = await UI.confirm({
            title: (next ? "Activate" : "Deactivate") + " this course?",
            message: next
                ? "Students will be able to enrol in the course again."
                : "The course is hidden from enrolment. Existing enrollments, exams and results are preserved.",
            confirmLabel: next ? "Activate course" : "Deactivate course",
            danger: !next
        });

        if (!confirmed) return;

        try {
            await UI.request("/admin/courses/" + id + "/active", {
                method: "PATCH",
                body: { active: next }
            });

            UI.toast("Course " + (next ? "activated" : "deactivated") + ".",
                "success", "Updated");
            loadCourses();
            loadStats();

        } catch (error) {
            UI.toast(error.message, "error");
        }
    }

    async function deleteCourse(id) {

        var row = cache[id];
        var label = row ? row.title : "this course";

        var confirmed = await UI.confirm({
            title: "Delete " + label + "?",
            message: "If the course has enrollments, exams, materials or results, it is deactivated instead of removed.",
            confirmLabel: "Delete course"
        });

        if (!confirmed) return;

        try {
            var result = await UI.request("/admin/courses/" + id, { method: "DELETE" });

            UI.toast(
                result && result.message ? result.message : "Course deleted.",
                "success",
                result && result.mode === "DEACTIVATED" ? "Course deactivated" : "Deleted"
            );

            loadCourses();
            loadStats();

        } catch (error) {
            UI.toast(error.message, "error");
        }
    }

    /* ---------------- events ---------------- */

    UI.el("addCourse").addEventListener("click", function () {
        resetForm();
        openModal();
    });

    UI.el("courseForm").addEventListener("submit", submitCourse);

    UI.el("courseSearch").addEventListener("input", function (event) {
        clearTimeout(searchTimer);
        var value = event.target.value.trim();

        searchTimer = setTimeout(function () {
            state.search = value;
            state.page = 0;
            loadCourses();
        }, 350);
    });

    UI.el("courseStatus").addEventListener("change", function (event) {
        state.active = event.target.value;
        state.page = 0;
        loadCourses();
    });

    UI.el("courseFaculty").addEventListener("change", function (event) {
        state.facultyId = event.target.value;
        state.page = 0;
        loadCourses();
    });

    UI.el("clearCourseFilters").addEventListener("click", function () {
        UI.el("courseSearch").value = "";
        UI.el("courseStatus").value = "";
        UI.el("courseFaculty").value = "";
        state.search = "";
        state.active = "";
        state.facultyId = "";
        state.page = 0;
        loadCourses();
    });

    UI.el("refreshCourses").addEventListener("click", function () {
        loadCourses();
        loadStats();
    });

    UI.el("exportCourses").addEventListener("click", async function () {
        var button = UI.el("exportCourses");

        try {
            UI.setBusy(button, true, "Preparing...");
            await UI.download(
                "/admin/export/courses.xlsx" + UI.query({
                    search: state.search,
                    active: state.active,
                    facultyId: state.facultyId
                }),
                "courses.xlsx"
            );
        } catch (error) {
            UI.toast(error.message, "error");
        } finally {
            UI.setBusy(button, false);
        }
    });

    UI.el("courseTableBody").addEventListener("click", function (event) {
        var button = event.target.closest("button[data-act]");
        if (!button) return;

        var id = Number(button.dataset.id);

        if (button.dataset.act === "edit") {
            startEdit(id);
        } else if (button.dataset.act === "toggle") {
            toggleCourse(id, button.dataset.active === "1");
        } else if (button.dataset.act === "delete") {
            deleteCourse(id);
        }
    });

    /* ---------------- init ---------------- */

    UI.bindChrome();
    loadFacultyOptions();
    loadStats();
    loadCourses();
})();
