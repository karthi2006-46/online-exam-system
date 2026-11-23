/* =====================================================
   ADMIN SIDEBAR
   Requires admin-common.js (window.AdminUI) and api-config.js.
   Injects the sidebar into #adminSidebar on every admin page.
===================================================== */
(function () {
    "use strict";

    var LOGIN_URL = "/login.html";
    var BASE = "/admin/";

    var MENU = [
        { page: "admindashboard.html", icon: "dashboard",   full: "Dashboard",       short: "Home" },
        { page: "courses.html",        icon: "courses",     full: "Courses",         short: "Courses" },
        { page: "faculty.html",        icon: "faculty",     full: "Faculty",         short: "Faculty" },
        { page: "students.html",       icon: "students",    full: "Students",        short: "Students" },
        { page: "approvals.html",      icon: "approvals",   full: "Exam Approvals",  short: "Approvals" },
        { page: "extensions.html",     icon: "extensions",  full: "Extensions",      short: "Extensions" },
        { page: "performance.html",    icon: "performance", full: "Performance",     short: "Perf" },
        { page: "results.html",        icon: "results",     full: "Exam Results",    short: "Results" },
        { page: "reports.html",        icon: "file",        full: "Reports",         short: "Reports" },
        { page: "settings.html",       icon: "settings",    full: "Settings",        short: "Settings" }
    ];

    function icon(name) {
        var source = window.AdminUI && window.AdminUI.icons;
        return source && source[name] ? source[name] : "";
    }

    function normalize(name) {
        return (name || "").toLowerCase().replace(/\.html?$/, "").replace(/\/$/, "");
    }

    function buildSidebar() {
        var items = MENU.map(function (item) {
            return '<a href="' + BASE + item.page + '" data-page="' + item.page + '">' +
                '<span class="as-icon" aria-hidden="true">' + icon(item.icon) + "</span>" +
                '<span class="as-label"><span class="full">' + item.full +
                '</span><span class="short">' + item.short + "</span></span>" +
                "</a>";
        }).join("");

        return '<aside class="as-sidebar" id="adminSidebarMenu">' +
            '<a href="' + BASE + 'admindashboard.html" class="as-brand">' +
                '<span class="as-brand-mark">L</span>' +
                '<span class="as-brand-text"><strong>LearnHub</strong><small>Admin Console</small></span>' +
            "</a>" +
            '<p class="as-section-label">Administration</p>' +
            '<nav class="as-nav" aria-label="Admin navigation">' + items +
                '<a href="' + LOGIN_URL + '" class="as-logout" id="asLogoutLink">' +
                    '<span class="as-icon" aria-hidden="true">' + icon("logout") + "</span>" +
                    '<span class="as-label"><span class="full">Sign Out</span><span class="short">Logout</span></span>' +
                "</a>" +
            "</nav>" +
            '<div class="as-bottom"><div class="as-profile">' +
                '<div class="as-avatar" id="adminAvatar">A</div>' +
                '<div class="as-profile-info"><strong id="adminName">Administrator</strong><small>Administrator</small></div>' +
            "</div></div>" +
        "</aside>";
    }

    function setupLogout(root) {
        var link = root.querySelector("#asLogoutLink");
        if (!link) return;

        var overlay = document.createElement("div");
        overlay.className = "as-overlay";
        overlay.innerHTML =
            '<div class="as-dialog" role="alertdialog" aria-modal="true" aria-labelledby="asLogoutTitle" aria-describedby="asLogoutDesc">' +
                '<div class="as-dialog-icon">' + icon("logout") + "</div>" +
                '<h2 id="asLogoutTitle">Sign out of the admin console?</h2>' +
                '<p id="asLogoutDesc">You will need to sign in again to manage faculty, students and performance reports.</p>' +
                '<div class="as-actions">' +
                    '<button type="button" class="as-btn no" id="asNo">No, stay</button>' +
                    '<button type="button" class="as-btn yes" id="asYes">Yes, sign out</button>' +
                "</div>" +
            "</div>";
        document.body.appendChild(overlay);

        var dialog = overlay.querySelector(".as-dialog");
        var noBtn = overlay.querySelector("#asNo");
        var yesBtn = overlay.querySelector("#asYes");
        var lastFocus = null;
        var busy = false;

        function open() {
            lastFocus = document.activeElement;
            overlay.classList.add("is-open");
            document.body.classList.add("as-lock");
            setTimeout(function () { noBtn.focus(); }, 60);
        }

        function close() {
            if (busy) return;
            overlay.classList.remove("is-open");
            document.body.classList.remove("as-lock");
            if (lastFocus && lastFocus.focus) lastFocus.focus();
        }

        link.addEventListener("click", function (event) {
            event.preventDefault();
            open();
        });

        noBtn.addEventListener("click", close);
        overlay.addEventListener("click", function (event) {
            if (event.target === overlay) close();
        });

        yesBtn.addEventListener("click", function () {
            if (busy) return;
            busy = true;
            dialog.classList.add("is-loading");
            yesBtn.textContent = "Signing out";

            if (window.AdminUI) window.AdminUI.clearAuth();
            else {
                localStorage.removeItem("token");
                localStorage.removeItem("userId");
                localStorage.removeItem("role");
            }

            setTimeout(function () { window.location.href = LOGIN_URL; }, 650);
        });

        document.addEventListener("keydown", function (event) {
            if (!overlay.classList.contains("is-open")) return;

            if (event.key === "Escape") close();
            else if (event.key === "Tab") {
                if (event.shiftKey && document.activeElement === noBtn) {
                    event.preventDefault();
                    yesBtn.focus();
                } else if (!event.shiftKey && document.activeElement === yesBtn) {
                    event.preventDefault();
                    noBtn.focus();
                }
            }
        });
    }

    function initializeAdminSidebar() {
        var host = document.getElementById("adminSidebar");
        if (!host || host.dataset.ready === "true") return;

        // Never render the console to a non-admin session.
        if (window.AdminUI) {
            var userId = window.AdminUI.guard();
            if (!userId) return;
        }

        host.dataset.ready = "true";

        host.innerHTML = buildSidebar();

        var sidebar = host.querySelector("#adminSidebarMenu");
        var current = normalize(window.location.pathname.split("/").pop());

        sidebar.querySelectorAll(".as-nav a[data-page]").forEach(function (anchor) {
            var active = normalize(anchor.dataset.page) === current;
            anchor.classList.toggle("active", active);
            if (active) anchor.setAttribute("aria-current", "page");
        });

        setupLogout(sidebar);

        var storedName = localStorage.getItem("userName");
        if (storedName) {
            var nameNode = sidebar.querySelector("#adminName");
            var avatarNode = sidebar.querySelector("#adminAvatar");
            if (nameNode) nameNode.textContent = storedName;
            if (avatarNode) avatarNode.textContent = storedName.charAt(0).toUpperCase();
        }

        if (window.AdminUI) {
            window.AdminUI.loadProfile();
        }
    }

    window.initializeAdminSidebar = initializeAdminSidebar;
    initializeAdminSidebar();
})();
