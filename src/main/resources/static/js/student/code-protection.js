(function () {
    "use strict";

    if (window.lmsStudentProtectionInitialized) return;
    window.lmsStudentProtectionInitialized = true;

    // ================= SETTINGS =================
    const DEV_MODE = true;   // true = right click + DevTools allowed. Set to false before going live.

    const CONFIG = {
        blurOnWindowBlur: true,    // blur when the window loses focus
        blurOnHiddenTab: true,     // blur when the tab is hidden
        detectDevTools: true,      // basic DevTools size check (ignored in DEV_MODE)
        blockContextMenu: true,    // block right click (ignored in DEV_MODE)
        blockDevToolsKeys: true,   // block F12, Ctrl+Shift+I/J/C (ignored in DEV_MODE)
        revealDelay: 150,          // ms before the page opens after keys are released
        failSafeMs: 4000           // auto-clear stuck key state
    };

    // ================= SESSION CHECK =================
    const token = localStorage.getItem("token");
    const studentId = localStorage.getItem("userId");
    if (!token || !studentId) return;

    // Only protect students; admin/teacher accounts are never restricted
    const role = (localStorage.getItem("role") || "").toLowerCase();
    if (role && role !== "student") return;

    document.body.classList.add("lms-student-protection");

    const state = { meta: false, shift: false, snip: false };
    let revealTimer = null;
    let failSafeTimer = null;

    // ================= UI =================
    function buildWarning() {
        if (document.getElementById("lmsProtectionWarning")) return;
        const overlay = document.createElement("div");
        overlay.id = "lmsProtectionWarning";
        overlay.setAttribute("role", "dialog");
        overlay.setAttribute("aria-modal", "true");
        overlay.innerHTML =
            '<div class="lms-protection-card">' +
            "<h2>Security Notice</h2>" +
            "<p>Your LMS learning content is protected. Please do not capture, copy, or share learning materials without permission.</p>" +
            '<button type="button" id="lmsProtectionContinue">Continue Learning</button>' +
            "</div>";
        document.body.appendChild(overlay);
        document.getElementById("lmsProtectionContinue").addEventListener("click", function () {
            hideWarning();
            resetKeys();
            revealPage(true);
        });
    }

    function showWarning() {
        const o = document.getElementById("lmsProtectionWarning");
        if (o) o.classList.add("lms-protection-visible");
    }

    function hideWarning() {
        const o = document.getElementById("lmsProtectionWarning");
        if (o) o.classList.remove("lms-protection-visible");
    }

    function warningVisible() {
        const o = document.getElementById("lmsProtectionWarning");
        return !!(o && o.classList.contains("lms-protection-visible"));
    }

    function blurPage() {
        clearTimeout(revealTimer);
        document.body.classList.add("lms-protection-blurred");
    }

    function revealPage(force) {
        clearTimeout(revealTimer);
        revealTimer = setTimeout(function () {
            if (!force) {
                if (state.snip || warningVisible()) return;
                if (document.hidden) return;
                if (!document.hasFocus()) return;
            }
            document.body.classList.remove("lms-protection-blurred");
        }, force ? 0 : CONFIG.revealDelay);
    }

    function resetKeys() {
        state.meta = false;
        state.shift = false;
        state.snip = false;
        clearTimeout(failSafeTimer);
    }

    function clearClipboard() {
        try {
            if (navigator.clipboard && navigator.clipboard.writeText) {
                navigator.clipboard
                    .writeText("Screenshots are disabled on this portal.")
                    .catch(function () {});
            }
        } catch (e) {}
    }
    function handlePrintScreen() {
    blurPage();
    showWarning();
    clearClipboard();
    setTimeout(clearClipboard, 100);
    setTimeout(clearClipboard, 500);
}

    // ================= KEY HELPERS =================
    function isMeta(e) {
        const k = (e.key || "").toLowerCase();
        return k === "meta" || k === "os" || e.code === "MetaLeft" || e.code === "MetaRight";
    }

    function isShift(e) {
        return (e.key || "").toLowerCase() === "shift";
    }

    function isPrintScreen(e) {
        return (e.key || "").toLowerCase() === "printscreen" || e.code === "PrintScreen";
    }

    // Win + Shift (first two keys of Win+Shift+S) held together
    function evaluateSnip() {
        const combo = state.meta && state.shift;
        if (combo && !state.snip) {
            state.snip = true;
            blurPage();
            clearTimeout(failSafeTimer);
            failSafeTimer = setTimeout(function () {
                resetKeys();
                revealPage(false);
            }, CONFIG.failSafeMs);
        } else if (!combo && state.snip) {
            state.snip = false;
            clearTimeout(failSafeTimer);
            revealPage(false);
        }
    }

    buildWarning();

    // ================= KEYDOWN =================
    document.addEventListener("keydown", function (e) {
        const key = (e.key || "").toLowerCase();

        if (isMeta(e)) { state.meta = true; evaluateSnip(); }
        if (isShift(e)) { state.shift = true; evaluateSnip(); }

        // Win+Shift+S reached the page, or Mac Cmd+Shift+3/4/5
        if (e.metaKey && e.shiftKey && (key === "s" || ["3", "4", "5"].indexOf(e.key) !== -1)) {
            state.meta = true;
            state.shift = true;
            evaluateSnip();
            blurPage();
        }

        // Print Screen / Fn+PrtScr
     if (isPrintScreen(e)) handlePrintScreen();

        // Print (Ctrl+P) / Save (Ctrl+S) / View source (Ctrl+U)
        if ((e.ctrlKey || e.metaKey) && !e.shiftKey && ["p", "s", "u"].indexOf(key) !== -1) {
            // Allow view-source in DEV_MODE only
            if (!(DEV_MODE && key === "u")) {
                e.preventDefault();
                e.stopImmediatePropagation();
                blurPage();
                showWarning();
            }
        }

        // DevTools shortcuts (skipped in DEV_MODE)
        if (!DEV_MODE && CONFIG.blockDevToolsKeys) {
            const devCombo =
                key === "f12" ||
                ((e.ctrlKey || e.metaKey) && e.shiftKey && ["i", "j", "c"].indexOf(key) !== -1) ||
                (e.metaKey && e.altKey && ["i", "j", "c"].indexOf(key) !== -1);
            if (devCombo) {
                e.preventDefault();
                e.stopImmediatePropagation();
                blurPage();
                showWarning();
            }
        }
    }, true);

    // ================= KEYUP =================
    document.addEventListener("keyup", function (e) {
        if (isMeta(e)) { state.meta = false; evaluateSnip(); }
        if (isShift(e)) { state.shift = false; evaluateSnip(); }

        // Windows often fires only keyup for PrintScreen
if (isPrintScreen(e)) handlePrintScreen();
    }, true);

    // ================= FOCUS / VISIBILITY =================
    window.addEventListener("blur", function () {
        if (CONFIG.blurOnWindowBlur) blurPage();
    });

    window.addEventListener("focus", function () {
        // Snipping Tool swallows keyup, so reset the key state here
        resetKeys();
        revealPage(false);
    });

    document.addEventListener("visibilitychange", function () {
        if (!CONFIG.blurOnHiddenTab) return;
        if (document.hidden) {
            blurPage();
        } else {
            resetKeys();
            revealPage(false);
        }
    });

    // ================= PRINT =================
    window.addEventListener("beforeprint", function () {
        blurPage();
        showWarning();
    });
    window.addEventListener("afterprint", function () {
        revealPage(true);
    });

    // ================= RIGHT CLICK (skipped in DEV_MODE) =================
    if (!DEV_MODE && CONFIG.blockContextMenu) {
        document.addEventListener("contextmenu", function (e) {
            if (e.target.closest("input, textarea, select, [contenteditable='true']")) return;
            e.preventDefault();
        });
    }

    // ================= DEVTOOLS SIZE CHECK (skipped in DEV_MODE) =================
    if (!DEV_MODE && CONFIG.detectDevTools) {
        setInterval(function () {
            const open =
                (window.outerWidth - window.innerWidth > 250) ||
                (window.outerHeight - window.innerHeight > 250);
            if (open && !warningVisible()) {
                blurPage();
                showWarning();
            }
        }, 1500);
    }

    console.info(
        "LMS Student Portal protection initialized" +
        (DEV_MODE ? " (DEV_MODE: right click and DevTools allowed)" : " (deterrence only)")
    );
})();