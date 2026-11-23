(function () {
    "use strict";

    var LOGIN_URL = "/login.html"; // change if your login page is elsewhere

    function icon(paths) {
        return '<svg viewBox="0 0 24 24">' + paths + "</svg>";
    }

    var ICONS = {
        dashboard:  icon('<rect x="3" y="3" width="7" height="9" rx="1.5"/><rect x="14" y="3" width="7" height="5" rx="1.5"/><rect x="14" y="12" width="7" height="9" rx="1.5"/><rect x="3" y="16" width="7" height="5" rx="1.5"/>'),
        extension:  icon('<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>'),
        approval:   icon('<rect x="5" y="4" width="14" height="17" rx="2"/><path d="M9 4h6v3H9z"/><path d="m9 14 2 2 4-4"/>'),
        courses:    icon('<path d="M2 4h6a4 4 0 0 1 4 4v12a3 3 0 0 0-3-3H2z"/><path d="M22 4h-6a4 4 0 0 0-4 4v12a3 3 0 0 1 3-3h7z"/>'),
        exams:      icon('<path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z"/><path d="M14 3v5h5"/><path d="M9 13h6M9 17h6"/>'),
        code:       icon('<path d="m9 18-6-6 6-6"/><path d="m15 6 6 6-6 6"/>'),
        materials:  icon('<path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>'),
        results:    icon('<path d="M4 20V10"/><path d="M10 20V4"/><path d="M16 20v-8"/><path d="M22 20H2"/>'),
        backup:     icon('<path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><path d="m17 8-5-5-5 5"/><path d="M12 3v12"/>'),
        logout:     icon('<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><path d="m16 17 5-5-5-5"/><path d="M21 12H9"/>')
    };

    /* ---------- Menu definition (edit here) ---------- */
    var MENU = [
        { page: "facultydashboard.html",         icon: "dashboard", full: "Dashboard",           short: "Home" },
        { page: "extension-requests.html",       icon: "extension", full: "Extension Requests",  short: "Extend",  badge: "extensionBadge" },
        { page: "exam-approval-requests.html",   icon: "approval",  full: "Exam Approvals",      short: "Approve", badge: "examApprovalBadge" },
        { page: "manage-courses.html",           icon: "courses",   full: "Manage Courses",      short: "Courses" },
        { page: "manage-exams.html",             icon: "exams",     full: "Manage Exams",        short: "Exams" },
        { page: "manage-challenges.html",        icon: "code",      full: "Coding Challenges",   short: "Code" },
        { page: "manage-materials.html",         icon: "materials", full: "Study Materials",     short: "Notes" },
        { page: "faculty-results.html",          icon: "results",   full: "Student Results",     short: "Results" },
        { page: "backup.html",                   icon: "backup",    full: "Backup",              short: "Backup" }
    ];

    var BASE = "/faculty/";

    function normalize(name) {
        return (name || "").toLowerCase().replace(/\.html?$/, "").replace(/\/$/, "");
    }

    function clearAuth() {
        ["token", "user", "userId"].forEach(function (key) {
            try { localStorage.removeItem(key); } catch (e) {}
            try { sessionStorage.removeItem(key); } catch (e) {}
        });
    }

    function storedName() {
        try {
            var u = JSON.parse(localStorage.getItem("user") || sessionStorage.getItem("user") || "null");
            return (u && (u.name || u.fullName || u.username)) || "";
        } catch (e) { return ""; }
    }

    /* ---------- Build sidebar HTML ---------- */
    function buildSidebar() {
        var items = MENU.map(function (m) {
            return '<a href="' + BASE + m.page + '" data-page="' + m.page + '">' +
                '<span class="fs-icon" aria-hidden="true">' + ICONS[m.icon] + "</span>" +
                '<span class="fs-label"><span class="full">' + m.full + '</span><span class="short">' + m.short + "</span></span>" +
                (m.badge ? '<span class="fs-badge is-empty" id="' + m.badge + '">0</span>' : "") +
                "</a>";
        }).join("");

        return '<aside class="fs-sidebar" id="facultySidebarMenu">' +
            '<a href="' + BASE + 'facultydashboard.html" class="fs-brand">' +
                '<span class="fs-brand-mark">L</span>' +
                '<span class="fs-brand-text"><strong>LearnHub</strong><small>Faculty Portal</small></span>' +
            "</a>" +
            '<p class="fs-section-label">Teaching menu</p>' +
            '<nav class="fs-nav" aria-label="Faculty navigation">' + items +
                '<a href="' + LOGIN_URL + '" class="fs-logout" id="fsLogoutLink">' +
                    '<span class="fs-icon" aria-hidden="true">' + ICONS.logout + "</span>" +
                    '<span class="fs-label"><span class="full">Sign Out</span><span class="short">Logout</span></span>' +
                "</a>" +
            "</nav>" +
            '<div class="fs-bottom"><div class="fs-profile">' +
                '<div class="fs-avatar" id="avatar">F</div>' +
                '<div class="fs-profile-info"><strong id="facultyName">Faculty</strong><small>Faculty Account</small></div>' +
            "</div></div>" +
        "</aside>";
    }

    /* ---------- Badges: hide when 0 / empty ---------- */
    function watchBadge(el) {
        function sync() {
            var t = (el.textContent || "").trim();
            el.classList.toggle("is-empty", t === "" || t === "0" || t === "—");
        }
        sync();
        new MutationObserver(sync).observe(el, { childList: true, characterData: true, subtree: true });
    }

    /* ---------- Logout dialog ---------- */
    function setupLogout(root) {
        var link = root.querySelector("#fsLogoutLink");
        if (!link) return;

        var overlay = document.createElement("div");
        overlay.className = "fs-overlay";
        overlay.innerHTML =
            '<div class="fs-dialog" role="alertdialog" aria-modal="true" aria-labelledby="fsLogoutTitle" aria-describedby="fsLogoutDesc">' +
                '<div class="fs-dialog-icon">' + ICONS.logout + "</div>" +
                '<h2 id="fsLogoutTitle">Sign out of your account?</h2>' +
                '<p id="fsLogoutDesc">You will need to sign in again to manage your courses, exams and student results.</p>' +
                '<div class="fs-actions">' +
                    '<button type="button" class="fs-btn no" id="fsNo">No, stay</button>' +
                    '<button type="button" class="fs-btn yes" id="fsYes">Yes, sign out</button>' +
                "</div>" +
            "</div>";
        document.body.appendChild(overlay);

        var dialog = overlay.querySelector(".fs-dialog");
        var noBtn = overlay.querySelector("#fsNo");
        var yesBtn = overlay.querySelector("#fsYes");
        var lastFocus = null, busy = false;

        function open() {
            lastFocus = document.activeElement;
            overlay.classList.add("is-open");
            document.body.classList.add("fs-lock");
            setTimeout(function () { noBtn.focus(); }, 60);
        }
        function close() {
            if (busy) return;
            overlay.classList.remove("is-open");
            document.body.classList.remove("fs-lock");
            if (lastFocus && lastFocus.focus) lastFocus.focus();
        }

        link.addEventListener("click", function (e) { e.preventDefault(); open(); });
        noBtn.addEventListener("click", close);
        overlay.addEventListener("click", function (e) { if (e.target === overlay) close(); });

        yesBtn.addEventListener("click", function () {
            if (busy) return;
            busy = true;
            dialog.classList.add("is-loading");
            yesBtn.textContent = "Signing out";
            clearAuth();
            setTimeout(function () { window.location.href = LOGIN_URL; }, 700);
        });

        document.addEventListener("keydown", function (e) {
            if (!overlay.classList.contains("is-open")) return;
            if (e.key === "Escape") close();
            else if (e.key === "Tab") {
                if (e.shiftKey && document.activeElement === noBtn) { e.preventDefault(); yesBtn.focus(); }
                else if (!e.shiftKey && document.activeElement === yesBtn) { e.preventDefault(); noBtn.focus(); }
            }
        });
    }

    /* ---------- Init ---------- */
    function initializeFacultySidebar() {
        var host = document.getElementById("facultySidebar");
        if (!host || host.dataset.ready === "true") return;
        host.dataset.ready = "true";

        host.innerHTML = buildSidebar();
        var sidebar = host.querySelector("#facultySidebarMenu");

        var current = normalize(window.location.pathname.split("/").pop());
        sidebar.querySelectorAll(".fs-nav a[data-page]").forEach(function (a) {
            var active = normalize(a.dataset.page) === current;
            a.classList.toggle("active", active);
            if (active) a.setAttribute("aria-current", "page");
        });

        sidebar.querySelectorAll(".fs-badge").forEach(watchBadge);

        // Fallback name/avatar; your page script can still overwrite these
        var name = storedName();
        if (name) {
            sidebar.querySelector("#facultyName").textContent = name;
            sidebar.querySelector("#avatar").textContent = name.charAt(0).toUpperCase();
        }

        setupLogout(sidebar);
    }

    window.initializeFacultySidebar = initializeFacultySidebar;
    initializeFacultySidebar(); // runs immediately so #extensionBadge etc. exist before page scripts
})();