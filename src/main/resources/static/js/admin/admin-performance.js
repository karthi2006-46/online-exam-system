/* =====================================================
   ADMIN - STUDENT PERFORMANCE MATRIX
===================================================== */
(function () {
    "use strict";

    var UI = window.AdminUI;

    if (!UI.guard()) return;

    var state = {
        page: 0,
        size: 10,
        search: "",
        courseId: "",
        facultyId: "",
        status: "",
        approvalStatus: ""
    };

    var searchTimer = null;
    var optionsLoaded = false;

    /* ---------------- filter options ---------------- */

    async function loadOptions() {
        if (optionsLoaded) return;

        try {
            var results = await Promise.all([
                UI.request("/courses"),
                UI.request("/admin/faculty?page=0&size=100")
            ]);

            var courses = (results[0] && results[0].data) ? results[0].data : (results[0] || []);
            var facultyPage = results[1] || {};
            var faculty = facultyPage.content || [];

            var courseSelect = UI.el("performanceCourse");
            courseSelect.innerHTML = '<option value="">All courses</option>' +
                courses.map(function (course) {
                    var label = (course.title || "Course " + course.id) +
                        (course.courseCode ? " (" + course.courseCode + ")" : "");
                    return '<option value="' + course.id + '">' +
                        UI.escapeHtml(label) + "</option>";
                }).join("");

            var facultySelect = UI.el("performanceFaculty");
            facultySelect.innerHTML = '<option value="">All faculty</option>' +
                faculty.map(function (row) {
                    return '<option value="' + row.facultyId + '">' +
                        UI.escapeHtml(row.name || ("Faculty " + row.facultyId)) + "</option>";
                }).join("");

            optionsLoaded = true;

        } catch (error) {
            /* filters stay usable without options */
        }
    }

    /* ---------------- table ---------------- */

    function enrollmentPill(status) {
        var value = String(status || "").toUpperCase();
        var map = {
            ACTIVE: "pill-green",
            COMPLETED: "pill-blue",
            INACTIVE: "pill-grey",
            DROPPED: "pill-red"
        };
        var label = value ? value.charAt(0) + value.slice(1).toLowerCase() : "-";

        return '<span class="pill ' + (map[value] || "pill-grey") + '">' +
            UI.escapeHtml(label) + "</span>";
    }

    function latestExamCell(row) {
        if (!row.latestExamTitle) {
            return '<span class="subtle">No attempt</span>';
        }

        return "<strong>" + UI.escapeHtml(row.latestExamTitle) + "</strong>" +
            '<small class="subtle">' +
                UI.number(row.latestExamMarks) + " / " +
                UI.number(row.latestExamTotalMarks) + " &middot; " +
                UI.percent(row.latestExamPercentage) +
            "</small>";
    }

    function renderRow(row) {

        var studentCell = '<span class="person-cell">' +
            '<span class="person-avatar">' + UI.escapeHtml(UI.initials(row.studentName)) + "</span>" +
            "<span><strong>" +
                '<a class="row-link" href="/admin/student-detail.html?id=' + row.studentId + '">' +
                UI.escapeHtml(row.studentName) + "</a>" +
                (row.studentActive === false ? ' <span class="pill pill-red">Inactive</span>' : "") +
            "</strong><small>" + UI.escapeHtml(row.email || "-") + "</small></span>" +
        "</span>";

        var courseCell = "<strong>" + UI.escapeHtml(row.courseTitle || "-") + "</strong>" +
            '<small class="subtle">' + UI.escapeHtml(row.courseCode || "-") + "</small>";

        return "<tr>" +
            '<td data-label="Student">' + studentCell + "</td>" +
            '<td data-label="Course" class="cell-wrap">' + courseCell + "</td>" +
            '<td data-label="Faculty">' +
                (row.facultyName
                    ? UI.escapeHtml(row.facultyName)
                    : '<span class="subtle">Unassigned</span>') +
            "</td>" +
            '<td data-label="Enrollment">' + enrollmentPill(row.enrollmentStatus) + "</td>" +
            '<td data-label="Materials">' + UI.progressBar(row.materialsProgress) + "</td>" +
            '<td data-label="Challenges">' + UI.progressBar(row.challengesProgress) + "</td>" +
            '<td data-label="Overall">' + UI.progressBar(row.overallProgress) + "</td>" +
            '<td data-label="Exam approval">' + UI.approvalPill(row.examApprovalStatus) + "</td>" +
            '<td data-label="Coding review">' + UI.approvalPill(row.codingApprovalStatus) + "</td>" +
            '<td data-label="Latest exam" class="cell-wrap">' + latestExamCell(row) + "</td>" +
            '<td data-label="Result">' + UI.resultPill(row.overallResult) + "</td>" +
            "</tr>";
    }

    async function loadPerformance() {

        var body = UI.el("performanceTableBody");
        if (body) body.innerHTML = UI.loadingRow(11);

        var params = UI.query({
            search: state.search,
            courseId: state.courseId,
            facultyId: state.facultyId,
            status: state.status,
            approvalStatus: state.approvalStatus,
            page: state.page,
            size: state.size
        });

        try {
            var page = await UI.request("/admin/performance" + params);
            var rows = page.content || [];

            if (!rows.length) {
                body.innerHTML = UI.tableState(
                    11,
                    "No performance records",
                    state.search || state.courseId || state.facultyId ||
                    state.status || state.approvalStatus
                        ? "Adjust or clear the filters to see more enrollments."
                        : "Enrollments appear here once students join a course.");
            } else {
                body.innerHTML = rows.map(renderRow).join("");
            }

            UI.renderPager(UI.el("performancePager"), page, function (nextPage) {
                state.page = nextPage;
                loadPerformance();
            });

            var message = UI.el("pageMessage");
            if (message) {
                message.textContent = "";
                message.className = "page-message";
            }

        } catch (error) {
            body.innerHTML = UI.tableState(11, "Unable to load performance", error.message, true);
            UI.toast(error.message, "error");
        }
    }

    /* ---------------- events ---------------- */

    UI.el("performanceSearch").addEventListener("input", function (event) {
        clearTimeout(searchTimer);
        var value = event.target.value.trim();

        searchTimer = setTimeout(function () {
            state.search = value;
            state.page = 0;
            loadPerformance();
        }, 350);
    });

    [
        ["performanceCourse", "courseId"],
        ["performanceFaculty", "facultyId"],
        ["performanceStatus", "status"],
        ["performanceApproval", "approvalStatus"]
    ].forEach(function (pair) {
        UI.el(pair[0]).addEventListener("change", function (event) {
            state[pair[1]] = event.target.value;
            state.page = 0;
            loadPerformance();
        });
    });

    UI.el("clearPerformanceFilters").addEventListener("click", function () {
        UI.el("performanceSearch").value = "";
        UI.el("performanceCourse").value = "";
        UI.el("performanceFaculty").value = "";
        UI.el("performanceStatus").value = "";
        UI.el("performanceApproval").value = "";

        state.search = "";
        state.courseId = "";
        state.facultyId = "";
        state.status = "";
        state.approvalStatus = "";
        state.page = 0;

        loadPerformance();
    });

    UI.el("refreshPerformance").addEventListener("click", loadPerformance);

    UI.el("exportPerformance").addEventListener("click", async function () {
        var button = UI.el("exportPerformance");

        try {
            UI.setBusy(button, true, "Preparing...");
            await UI.download(
                "/admin/export/performance.xlsx" + UI.query({
                    search: state.search,
                    courseId: state.courseId,
                    facultyId: state.facultyId,
                    status: state.status,
                    approvalStatus: state.approvalStatus
                }),
                "student-performance.xlsx"
            );
        } catch (error) {
            UI.toast(error.message, "error");
        } finally {
            UI.setBusy(button, false);
        }
    });

    /* ---------------- init ---------------- */

    UI.bindChrome();
    loadOptions();
    loadPerformance();
})();
