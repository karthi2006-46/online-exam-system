(function () {
    "use strict";

    var LOGOUT_ICON =
        '<svg viewBox="0 0 24 24"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>' +
        '<path d="m16 17 5-5-5-5"/><path d="M21 12H9"/></svg>';

    function normalize(name) {
        return (name || "").toLowerCase().replace(/\.html?$/, "").replace(/\/$/, "");
    }

    function clearAuth() {
        ["token", "user", "userId"].forEach(function (key) {
            try { localStorage.removeItem(key); } catch (e) {}
            try { sessionStorage.removeItem(key); } catch (e) {}
        });
    }

    /* ---------- Logout confirmation dialog ---------- */
    function buildLogoutDialog() {
        var existing = document.getElementById("logoutOverlay");
        if (existing) return existing;

        var overlay = document.createElement("div");
        overlay.className = "logout-overlay";
        overlay.id = "logoutOverlay";
        overlay.innerHTML =
            '<div class="logout-dialog" role="alertdialog" aria-modal="true" ' +
            'aria-labelledby="logoutTitle" aria-describedby="logoutDesc">' +
                '<div class="logout-icon">' + LOGOUT_ICON + "</div>" +
                '<h2 id="logoutTitle">Log out of your account?</h2>' +
                '<p id="logoutDesc">You will need to sign in again to continue your courses, exams and challenges.</p>' +
                '<div class="logout-actions">' +
                    '<button type="button" class="logout-btn no" id="logoutNo">No, stay</button>' +
                    '<button type="button" class="logout-btn yes" id="logoutYes">Yes, log out</button>' +
                "</div>" +
            "</div>";
        document.body.appendChild(overlay);
        return overlay;
    }

    function setupLogout(sidebar) {
        var logoutLink = sidebar.querySelector("#logoutLink");
        if (!logoutLink) return;

        var overlay = buildLogoutDialog();
        var dialog = overlay.querySelector(".logout-dialog");
        var noBtn = overlay.querySelector("#logoutNo");
        var yesBtn = overlay.querySelector("#logoutYes");
        var lastFocus = null;
        var busy = false;

        function open() {
            lastFocus = document.activeElement;
            overlay.classList.add("is-open");
            document.body.classList.add("logout-open");
            setTimeout(function () { noBtn.focus(); }, 60);
        }

        function close() {
            if (busy) return;
            overlay.classList.remove("is-open");
            document.body.classList.remove("logout-open");
            if (lastFocus && lastFocus.focus) lastFocus.focus();
        }

        logoutLink.addEventListener("click", function (e) {
            e.preventDefault();
            open();
        });

        noBtn.addEventListener("click", close);

        overlay.addEventListener("click", function (e) {
            if (e.target === overlay) close();
        });

        yesBtn.addEventListener("click", function () {
            if (busy) return;
            busy = true;
            dialog.classList.add("is-loading");
            yesBtn.textContent = "Logging out";
            clearAuth();
            setTimeout(function () {
                window.location.href = logoutLink.getAttribute("href");
            }, 700);
        });

        document.addEventListener("keydown", function (e) {
            if (!overlay.classList.contains("is-open")) return;

            if (e.key === "Escape") {
                close();
            } else if (e.key === "Tab") {
                // Keep focus inside the dialog
                var first = noBtn, last = yesBtn;
                if (e.shiftKey && document.activeElement === first) {
                    e.preventDefault(); last.focus();
                } else if (!e.shiftKey && document.activeElement === last) {
                    e.preventDefault(); first.focus();
                }
            }
        });
    }

    /* ---------- Sidebar ---------- */
    function initializeStudentSidebar() {
        var sidebar = document.getElementById("studentSidebarMenu");
        if (!sidebar) {
            console.warn("Student sidebar menu was not found.");
            return;
        }
        if (sidebar.dataset.ready === "true") return; // avoid double init
        sidebar.dataset.ready = "true";

        var currentPage = normalize(window.location.pathname.split("/").pop());

        sidebar.querySelectorAll(".sidebar-nav a[data-page]").forEach(function (link) {
            var isActive = normalize(link.dataset.page) === currentPage;
            link.classList.toggle("active", isActive);
            if (isActive) {
                link.setAttribute("aria-current", "page");
            } else {
                link.removeAttribute("aria-current");
            }
        });

        setupLogout(sidebar);
    }

    window.initializeStudentSidebar = initializeStudentSidebar;

    if (document.readyState !== "loading") {
        initializeStudentSidebar();
    } else {
        document.addEventListener("DOMContentLoaded", initializeStudentSidebar);
    }
})();