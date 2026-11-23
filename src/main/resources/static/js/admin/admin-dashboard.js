/* =====================================================
   ADMIN DASHBOARD
===================================================== */
(function () {
    "use strict";

    var UI = window.AdminUI;

    if (!UI.guard()) return;

    var icons = UI.icons;

    /* ---------------- icons ---------------- */

    var iconTargets = {
        facultyIcon: icons.faculty,
        studentsIcon: icons.students,
        enrollmentIcon: icons.check,
        passIcon: icons.performance,
        courseIcon: icons.courses,
        approvalIcon: icons.clock,
        extensionIcon: icons.clock,
        codingIcon: icons.file,
        qaFaculty: icons.faculty,
        qaStudents: icons.students,
        qaPerformance: icons.performance,
        qaResults: icons.results
    };

    Object.keys(iconTargets).forEach(function (id) {
        var node = UI.el(id);
        if (node && iconTargets[id]) node.innerHTML = iconTargets[id];
    });

    /* ---------------- helpers ---------------- */

    function setText(id, value) {
        var node = UI.el(id);
        if (node) node.textContent = value;
    }

    function donut(node, segments) {
        if (!node) return;

        var total = segments.reduce(function (sum, item) {
            return sum + Number(item.value || 0);
        }, 0);

        if (!total) {
            node.style.background = "#e7ebf3";
            return;
        }

        var cursor = 0;
        var parts = segments.map(function (item) {
            var start = (cursor / total) * 360;
            cursor += Number(item.value || 0);
            var end = (cursor / total) * 360;
            return item.color + " " + start.toFixed(2) + "deg " + end.toFixed(2) + "deg";
        });

        node.style.background = "conic-gradient(" + parts.join(", ") + ")";
    }

    function legend(node, items, total) {
        if (!node) return;

        if (!total) {
            node.innerHTML = '<p class="subtle">No records to chart yet.</p>';
            return;
        }

        node.innerHTML = items.map(function (item) {
            var share = total ? Math.round((item.value / total) * 100) : 0;
            return '<div class="legend-item">' +
                '<span class="legend-dot" style="background:' + item.color + '"></span>' +
                "<span>" + UI.escapeHtml(item.label) + "</span>" +
                "<strong>" + UI.number(item.value) + " (" + share + "%)</strong>" +
                "</div>";
        }).join("");
    }

    function pageError(error) {
        var node = UI.el("pageMessage");
        if (!node) return;

        node.className = "page-message is-error";
        node.innerHTML = UI.escapeHtml(error.message || "Unable to load the dashboard.") +
            ' <button type="button" class="text-button" id="retryDashboard">Retry</button>';

        var retry = UI.el("retryDashboard");
        if (retry) retry.addEventListener("click", load);
    }

    /* ---------------- render ---------------- */

    function renderStats(stats) {
        setText("totalFaculty", UI.number(stats.totalFaculty));
        setText("facultyNote",
            UI.number(stats.activeFaculty) + " active - " +
            UI.number(stats.inactiveFaculty) + " inactive");

        setText("totalStudents", UI.number(stats.totalStudents));
        setText("studentsNote",
            UI.number(stats.activeStudents) + " active - " +
            UI.number(stats.inactiveStudents) + " inactive");

        setText("activeEnrollments", UI.number(stats.totalActiveEnrollments));
        setText("enrollmentNote",
            UI.number(stats.totalCompletedEnrollments) + " completed - " +
            UI.number(stats.totalInactiveEnrollments) + " inactive");

        setText("passRate", UI.percent(stats.passPercentage));
        setText("passNote",
            UI.number(stats.passedResults) + " passed of " +
            UI.number(stats.totalResults) + " results");

        setText("totalCourses", UI.number(stats.totalCourses));
        setText("courseNote", UI.number(stats.activeCourses) + " active");

        setText("pendingApprovals", UI.number(stats.pendingExamApprovals));
        setText("approvalNote",
            UI.number(stats.approvedExamRequests) + " approved - " +
            UI.number(stats.rejectedExamRequests) + " rejected");

        setText("pendingExtensions", UI.number(stats.pendingExtensionRequests));
        setText("extensionNote",
            UI.number(stats.approvedExtensionRequests) + " approved - " +
            UI.number(stats.rejectedExtensionRequests) + " rejected");

        setText("pendingCoding", UI.number(stats.pendingCodingSubmissions));
        setText("codingNote", "Awaiting faculty review");

        /* enrollment chart */
        var enrollmentSegments = [
            { label: "Active", value: stats.totalActiveEnrollments, color: "#4f46e5" },
            { label: "Inactive", value: stats.totalInactiveEnrollments, color: "#f59e0b" },
            { label: "Completed", value: stats.totalCompletedEnrollments, color: "#22c55e" },
            { label: "Dropped", value: stats.totalDroppedEnrollments, color: "#ef4444" }
        ];

        var enrollmentTotal = enrollmentSegments.reduce(function (sum, item) {
            return sum + Number(item.value || 0);
        }, 0);

        setText("enrollmentTotal", UI.number(enrollmentTotal));
        donut(UI.el("enrollmentDonut"), enrollmentSegments);
        legend(UI.el("enrollmentLegend"), enrollmentSegments, enrollmentTotal);

        /* results chart */
        var resultSegments = [
            { label: "Passed", value: stats.passedResults, color: "#22c55e" },
            { label: "Failed", value: stats.failedResults, color: "#ef4444" }
        ];

        var resultTotal = resultSegments.reduce(function (sum, item) {
            return sum + Number(item.value || 0);
        }, 0);

        setText("resultTotal", UI.number(resultTotal));
        donut(UI.el("resultDonut"), resultSegments);
        legend(UI.el("resultLegend"), resultSegments, resultTotal);

        /* queue */
        setText("queueExamPending", UI.number(stats.pendingExamApprovals));
        setText("queueExamApproved", UI.number(stats.approvedExamRequests));
        setText("queueExtensionPending", UI.number(stats.pendingExtensionRequests));
        setText("queueExtensionRejected", UI.number(stats.rejectedExtensionRequests));
        setText("queueCodingPending", UI.number(stats.pendingCodingSubmissions));
    }

    function renderFaculty(page) {
        var body = UI.el("facultyPreviewBody");
        if (!body) return;

        var rows = page.content || [];

        if (!rows.length) {
            body.innerHTML = UI.tableState(5, "No faculty accounts yet",
                "Create the first faculty account from Faculty Management.");
            return;
        }

        body.innerHTML = rows.map(function (row) {
            return "<tr>" +
                '<td data-label="Faculty"><span class="person-cell">' +
                    '<span class="person-avatar">' + UI.escapeHtml(UI.initials(row.name)) + "</span>" +
                    "<span><strong>" + UI.escapeHtml(row.name) + "</strong>" +
                    "<small>" + UI.escapeHtml(row.email) + "</small></span>" +
                "</span></td>" +
                '<td data-label="Courses">' +
                    (row.assignedCourses && row.assignedCourses.length
                        ? UI.escapeHtml(row.assignedCourses.join(", "))
                        : '<span class="subtle">Not assigned</span>') +
                "</td>" +
                '<td data-label="Students" class="num">' + UI.number(row.totalStudents) + "</td>" +
                '<td data-label="Pass rate">' + UI.progressBar(row.passPercentage) + "</td>" +
                '<td data-label="Status">' + UI.statusPill(row.active) + "</td>" +
                "</tr>";
        }).join("");
    }

    /* ---------------- load ---------------- */

    var loading = false;

    async function load() {
        if (loading) return;
        loading = true;

        var message = UI.el("pageMessage");
        if (message) {
            message.className = "page-message";
            message.textContent = "Loading dashboard statistics...";
        }

        try {
            var results = await Promise.all([
                UI.request("/admin/dashboard/stats"),
                UI.request("/admin/faculty?page=0&size=5")
            ]);

            renderStats(results[0]);
            renderFaculty(results[1]);

            if (message) {
                message.textContent = "";
                message.className = "page-message";
            }
        } catch (error) {
            pageError(error);
            UI.toast(error.message, "error");
        } finally {
            loading = false;
        }
    }

    UI.bindChrome();
    load();

    document.addEventListener("visibilitychange", function () {
        if (!document.hidden) load();
    });
})();
