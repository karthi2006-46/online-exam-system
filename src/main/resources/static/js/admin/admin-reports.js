/* =====================================================
   ADMIN - PLATFORM REPORTS
===================================================== */
(function () {
    "use strict";

    var UI = window.AdminUI;

    if (!UI.guard()) return;

    var icons = UI.icons;

    document.querySelectorAll("[data-icon]").forEach(function (node) {
        var key = node.getAttribute("data-icon");
        if (icons[key]) node.innerHTML = icons[key];
    });

    /* ---------------- helpers ---------------- */

    function setText(id, value) {
        var node = UI.el(id);
        if (node) node.textContent = value;
    }

    function setHtml(id, value) {
        var node = UI.el(id);
        if (node) node.innerHTML = value;
    }

    function legendItem(color, label, value) {
        return '<span class="legend-item">' +
            '<span class="legend-dot" style="background:' + color + '"></span>' +
            UI.escapeHtml(label) +
            "<strong>" + UI.number(value) + "</strong>" +
            "</span>";
    }

    /* ---------------- render ---------------- */

    function renderSummary(report) {

        var totalCourses = Number(report.totalCourses || 0);
        var activeCourses = Number(report.activeCourses || 0);
        var totalStudents = Number(report.totalStudents || 0);
        var totalResults = Number(report.totalResults || 0);
        var passed = Number(report.passedResults || 0);

        setText("reportCourses", UI.number(totalCourses));
        setText("reportCoursesNote", UI.number(activeCourses) + " active");

        setText("reportStudents", UI.number(totalStudents));
        setText("reportStudentsNote",
            UI.number(report.activeEnrollments) + " active enrollments");

        setText("reportFaculty", UI.number(report.totalFaculty));

        setText("reportPass", UI.percent(report.passPercentage, 1));
        setText("reportPassNote",
            UI.number(passed) + " of " + UI.number(totalResults) + " results");

        var queues = Number(report.pendingExamApprovals || 0) +
            Number(report.pendingExtensionRequests || 0) +
            Number(report.pendingCodingSubmissions || 0);

        setText("reportQueues", UI.number(queues));

        setText("queueApprovals", UI.number(report.pendingExamApprovals));
        setText("queueExtensions", UI.number(report.pendingExtensionRequests));
        setText("queueCoding", UI.number(report.pendingCodingSubmissions));
        setText("queueResults", UI.number(report.totalResults));
        setText("queuePassed", UI.number(report.passedResults));
        setText("queueFailed", UI.number(report.failedResults));
        setText("queueActive", UI.number(report.activeEnrollments));
        setText("queueCompleted", UI.number(report.completedEnrollments));

        setText("courseReportMeta", "Generated " +
            UI.dateTime(report.generatedAt));
    }

    function renderDonut(report) {

        var active = Number(report.activeEnrollments || 0);
        var inactive = Number(report.inactiveEnrollments || 0);
        var completed = Number(report.completedEnrollments || 0);
        var dropped = Number(report.droppedEnrollments || 0);

        var total = active + inactive + completed + dropped;

        setText("enrollmentTotal", UI.number(total));

        var donut = UI.el("enrollmentDonut");
        if (!donut) return;

        if (!total) {
            donut.style.background = "conic-gradient(#e7ebf3 0deg 360deg)";
        } else {
            var first = active / total * 360;
            var second = first + inactive / total * 360;
            var third = second + completed / total * 360;

            donut.style.background = "conic-gradient(" +
                "#3b82f6 0deg " + first + "deg, " +
                "#94a3b8 " + first + "deg " + second + "deg, " +
                "#22c55e " + second + "deg " + third + "deg, " +
                "#ef4444 " + third + "deg 360deg)";
        }

        setHtml("enrollmentLegend",
            legendItem("#3b82f6", "Active", active) +
            legendItem("#94a3b8", "Inactive", inactive) +
            legendItem("#22c55e", "Completed", completed) +
            legendItem("#ef4444", "Dropped", dropped));
    }

    function renderCourses(rows) {

        var body = UI.el("courseReportBody");
        if (!body) return;

        if (!rows || !rows.length) {
            body.innerHTML = UI.tableState(
                8,
                "No courses yet",
                "Course performance appears once courses and enrollments exist."
            );
            return;
        }

        body.innerHTML = rows.map(function (row) {
            return "<tr>" +
                '<td data-label="Course"><strong>' + UI.escapeHtml(row.title) +
                    "</strong><small class=\"subtle\" style=\"display:block\">" +
                    UI.escapeHtml(row.courseCode || "") + "</small></td>" +
                '<td data-label="Faculty" class="cell-wrap">' +
                    UI.escapeHtml(row.facultyName || "Not assigned") + "</td>" +
                '<td data-label="Students" class="num">' +
                    UI.number(row.students) + "</td>" +
                '<td data-label="Exams" class="num">' +
                    UI.number(row.exams) + "</td>" +
                '<td data-label="Results" class="num">' +
                    UI.number(row.results) + "</td>" +
                '<td data-label="Pass rate" class="cell-wrap">' +
                    (Number(row.results)
                        ? UI.progressBar(row.passPercentage)
                        : '<span class="subtle">No results</span>') + "</td>" +
                '<td data-label="Average" class="num">' +
                    UI.percent(row.averagePercentage, 1) + "</td>" +
                '<td data-label="Status">' + UI.statusPill(row.active) + "</td>" +
                "</tr>";
        }).join("");
    }

    function renderFaculty(rows) {

        var body = UI.el("facultyReportBody");
        if (!body) return;

        if (!rows || !rows.length) {
            body.innerHTML = UI.tableState(
                8,
                "No faculty yet",
                "Faculty workload appears once faculty accounts exist."
            );
            return;
        }

        body.innerHTML = rows.map(function (row) {
            var pending = Number(row.pendingExamApprovals || 0) +
                Number(row.pendingExtensions || 0);

            return "<tr>" +
                '<td data-label="Faculty"><span class="person-cell">' +
                    '<span class="person-avatar">' +
                    UI.escapeHtml(UI.initials(row.name)) + "</span>" +
                    "<span><strong>" + UI.escapeHtml(row.name) + "</strong>" +
                    "<small>" + UI.escapeHtml(row.email || "") + "</small></span>" +
                "</span></td>" +
                '<td data-label="Courses" class="num">' +
                    UI.number(row.courses) + "</td>" +
                '<td data-label="Students" class="num">' +
                    UI.number(row.students) + "</td>" +
                '<td data-label="Exams" class="num">' +
                    UI.number(row.exams) + "</td>" +
                '<td data-label="Approvals" class="num">' +
                    UI.number(row.pendingExamApprovals) + "</td>" +
                '<td data-label="Extensions" class="num">' +
                    UI.number(row.pendingExtensions) + "</td>" +
                '<td data-label="Coding" class="num">' +
                    UI.number(row.pendingCodingReviews) + "</td>" +
                '<td data-label="Status">' +
                    (pending > 0
                        ? '<span class="pill pill-orange">' + UI.number(pending) +
                          " pending</span>"
                        : UI.statusPill(row.active)) +
                "</td>" +
                "</tr>";
        }).join("");
    }

    /* ---------------- load ---------------- */

    async function loadReport() {

        var courseBody = UI.el("courseReportBody");
        var facultyBody = UI.el("facultyReportBody");

        if (courseBody) courseBody.innerHTML = UI.loadingRow(8);
        if (facultyBody) facultyBody.innerHTML = UI.loadingRow(8);

        try {
            var report = await UI.request("/admin/reports/overview");

            renderSummary(report);
            renderDonut(report);
            renderCourses(report.courses);
            renderFaculty(report.faculty);

            UI.showPageMessage("pageMessage", "");

        } catch (error) {
            if (courseBody) {
                courseBody.innerHTML = UI.tableState(
                    8, "Unable to load the report", error.message, true);
            }
            if (facultyBody) {
                facultyBody.innerHTML = UI.tableState(
                    8, "Unable to load the report", error.message, true);
            }

            UI.showPageMessage("pageMessage", error.message, "error");
            UI.toast(error.message, "error");
        }
    }

    /* ---------------- downloads ---------------- */

    async function download(path, fallback) {
        try {
            await UI.download(path, fallback);
        } catch (error) {
            UI.toast(error.message, "error");
        }
    }

    UI.el("downloadReportPdf").addEventListener("click", async function () {
        var button = UI.el("downloadReportPdf");

        try {
            UI.setBusy(button, true, "Preparing...");
            await download(
                "/admin/export/report/platform.pdf",
                "platform-report.pdf"
            );
        } finally {
            UI.setBusy(button, false);
        }
    });

    UI.el("exportCourseReport").addEventListener("click", async function () {
        var button = UI.el("exportCourseReport");

        try {
            UI.setBusy(button, true, "Preparing...");
            await download(
                "/admin/export/report/courses.xlsx",
                "course-report.xlsx"
            );
        } finally {
            UI.setBusy(button, false);
        }
    });

    UI.el("exportFacultyReport").addEventListener("click", async function () {
        var button = UI.el("exportFacultyReport");

        try {
            UI.setBusy(button, true, "Preparing...");
            await download(
                "/admin/export/report/faculty.xlsx",
                "faculty-workload.xlsx"
            );
        } finally {
            UI.setBusy(button, false);
        }
    });

    /* ---------------- init ---------------- */

    UI.bindChrome();
    loadReport();
})();
