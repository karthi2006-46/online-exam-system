// =====================================================
// STUDY MATERIALS - STUDENT MODULE
// =====================================================

const MATERIALS_API = `${window.API_BASE_URL}/api/materials`;
const MATERIAL_PROGRESS_API =
    `${window.API_BASE_URL}/api/material-progress`;

// ---------- Viewer state ----------
let activeMaterial = null;
let completionUnlocked = false;
let textProgressSent = false;

let videoBlobUrl = null;
let videoProgressTimer = null;
let lastVideoProgressSent = 0;

let pdfDocument = null;
let pdfCurrentPage = 1;
let pdfZoom = 1;                 // 1 = fit whole page in the viewer
let pdfBusy = false;
let pdfQueued = null;

let viewerKeyHandler = null;
let viewerResizeObserver = null;
let viewerResizeTimer = null;

const PDF_ZOOM_MIN = 0.5;
const PDF_ZOOM_MAX = 3;
const PDF_ZOOM_STEP = 0.25;
const PDF_ANIMATION_MS = 380;

// ---------- Small inline icons ----------
const ICON_DOWNLOAD =
    '<svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 3v12"/><path d="m7 11 5 5 5-5"/><path d="M5 20h14"/></svg>';

const ICON_CHECK =
    '<svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m5 12.5 4.5 4.5L19 7.5"/></svg>';

const ICON_OPEN =
    '<svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M5 12h14"/><path d="m13 6 6 6-6 6"/></svg>';

const ICON_CLOSE =
    '<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M6 6l12 12"/><path d="M18 6 6 18"/></svg>';

const ICON_PREV =
    '<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m15 5-7 7 7 7"/></svg>';

const ICON_NEXT =
    '<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m9 5 7 7-7 7"/></svg>';

// =====================================================
// API RESPONSE HELPER
// =====================================================

async function readApiResponse(response) {
    const contentType = response.headers.get("content-type") || "";

    if (contentType.includes("application/json")) {
        return await response.json();
    }

    return await response.text();
}

// =====================================================
// DOWNLOAD COMPLETED MATERIAL
// =====================================================

async function downloadMaterial(material) {

    const token = localStorage.getItem("token");

    if (!token) {
        alert("Please log in again.");
        return;
    }

    try {

        const downloadUrl = getMaterialUrl(material.fileUrl);

        const response = await fetch(downloadUrl, {
            method: "GET",
            headers: {
                Authorization: `Bearer ${token}`
            }
        });

        if (!response.ok) {
            const errorText = await response.text();

            throw new Error(
                `Download failed (${response.status}): ${errorText}`
            );
        }

        const blob = await response.blob();
        const blobUrl = URL.createObjectURL(blob);

        const link = document.createElement("a");
        link.href = blobUrl;
        link.download = material.fileName || "study-material";

        document.body.appendChild(link);
        link.click();
        link.remove();

        setTimeout(() => URL.revokeObjectURL(blobUrl), 1000);

    } catch (error) {
        console.error("Download Material Error:", error);
        alert(error.message || "Unable to download material.");
    }
}

// =====================================================
// NORMALIZE MATERIAL URL
// =====================================================

function getMaterialUrl(fileUrl) {
    if (!fileUrl || typeof fileUrl !== "string") {
        throw new Error("Material file URL is missing.");
    }

    const url = fileUrl.trim();

    if (!url) {
        throw new Error("Material file URL is empty.");
    }

    if (/^https?:\/\//i.test(url)) {
        return url;
    }

    const baseUrl = String(window.API_BASE_URL || "")
        .replace(/\/+$/, "");

    if (!baseUrl) {
        throw new Error("Backend API URL is not configured.");
    }

    return url.startsWith("/")
        ? `${baseUrl}${url}`
        : `${baseUrl}/${url}`;
}

// =====================================================
// LOAD PDF.JS
// =====================================================

async function loadPdfLibrary() {
    if (window.pdfjsLib) {
        return window.pdfjsLib;
    }

    await new Promise((resolve, reject) => {
        const script = document.createElement("script");

        script.src =
            "https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.min.js";

        script.onload = resolve;
        script.onerror = () =>
            reject(new Error("Unable to load PDF viewer library."));

        document.head.appendChild(script);
    });

    window.pdfjsLib.GlobalWorkerOptions.workerSrc =
        "https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.worker.min.js";

    return window.pdfjsLib;
}

// =====================================================
// CREATE VIEWER  (styles live in study-material.css)
// =====================================================

function createViewer(material) {
    closeStudyViewer();

    activeMaterial = material;
    completionUnlocked = false;
    textProgressSent = false;
    lastVideoProgressSent = 0;

    const overlay = document.createElement("div");
    overlay.className = "study-viewer-overlay";
    overlay.id = "studyViewerOverlay";

    overlay.innerHTML = `
        <section class="study-viewer"
                 role="dialog"
                 aria-modal="true"
                 aria-labelledby="studyViewerTitle">

            <header class="study-viewer-header">
                <h3 id="studyViewerTitle"></h3>

                <button type="button"
                        class="study-viewer-close"
                        id="studyViewerClose"
                        aria-label="Close">
                    ${ICON_CLOSE}
                </button>
            </header>

            <div id="pdfToolbar"
                 class="study-viewer-toolbar study-viewer-hidden">

                <div class="study-tool-group">
                    <button type="button" id="pdfPrevious"
                            class="study-tool-btn"
                            aria-label="Previous page">${ICON_PREV}</button>

                    <span id="pdfPageInfo"
                          class="study-page-info"
                          aria-live="polite">1 / 1</span>

                    <button type="button" id="pdfNext"
                            class="study-tool-btn"
                            aria-label="Next page">${ICON_NEXT}</button>
                </div>

                <div class="study-tool-group">
                    <button type="button" id="pdfZoomOut"
                            class="study-tool-btn"
                            aria-label="Zoom out">−</button>

                    <span id="pdfZoomInfo"
                          class="study-zoom-info">Fit</span>

                    <button type="button" id="pdfZoomIn"
                            class="study-tool-btn"
                            aria-label="Zoom in">+</button>

                    <button type="button" id="pdfFit"
                            class="study-tool-btn study-tool-text"
                            aria-label="Fit whole page">Fit</button>
                </div>
            </div>

            <div id="studyProgress"
                 class="study-progress study-viewer-hidden"
                 aria-hidden="true">
                <span id="studyProgressBar"></span>
            </div>

            <div class="study-viewer-content"
                 id="studyViewerContent">

                <div class="study-viewer-loading">
                    <div class="loading-spinner"></div>
                    <p>Loading material...</p>
                </div>

            </div>

            <footer class="study-viewer-footer">

                <span id="studyUnlockMessage"
                      class="study-unlock-message">
                    Finish the material to unlock completion.
                </span>

                <button type="button"
                        id="studyCompleteButton"
                        class="study-complete-action study-viewer-hidden">
                    Mark as Completed
                </button>

            </footer>

        </section>
    `;

    document.body.appendChild(overlay);
    document.body.classList.add("study-viewer-open");

    document.getElementById("studyViewerTitle").textContent =
        material.title || "Study Material";

    document.getElementById("studyViewerClose")
        .addEventListener("click", closeStudyViewer);

    overlay.addEventListener("click", event => {
        if (event.target === overlay) closeStudyViewer();
    });

    document.getElementById("studyCompleteButton")
        .addEventListener("click", () => {
            completeMaterial(
                material.id,
                document.getElementById("studyCompleteButton")
            );
        });

    // Keyboard: Esc closes, arrows change PDF page
    viewerKeyHandler = event => {
        if (event.key === "Escape") {
            closeStudyViewer();
            return;
        }

        if (!pdfDocument) return;

        if (event.key === "ArrowRight" || event.key === "PageDown") {
            event.preventDefault();
            changePdfPage(1);
        } else if (event.key === "ArrowLeft" || event.key === "PageUp") {
            event.preventDefault();
            changePdfPage(-1);
        }
    };

    document.addEventListener("keydown", viewerKeyHandler);

    return overlay;
}

// =====================================================
// UNLOCK COMPLETION
// =====================================================

function unlockCompletion() {
    if (completionUnlocked) return;

    completionUnlocked = true;

    const button = document.getElementById("studyCompleteButton");
    const message = document.getElementById("studyUnlockMessage");

    if (button) {
        button.classList.remove("study-viewer-hidden");
    }

    if (message) {
        message.textContent =
            "You reached the required completion point.";
    }
}

// =====================================================
// CLOSE VIEWER
// =====================================================

function closeStudyViewer() {
    const overlay = document.getElementById("studyViewerOverlay");

    if (overlay) {
        const video = overlay.querySelector("video");

        if (video) {
            video.pause();
            video.removeAttribute("src");
            video.load();
        }

        overlay.remove();
    }

    document.body.classList.remove("study-viewer-open");

    if (viewerKeyHandler) {
        document.removeEventListener("keydown", viewerKeyHandler);
        viewerKeyHandler = null;
    }

    if (viewerResizeObserver) {
        viewerResizeObserver.disconnect();
        viewerResizeObserver = null;
    }

    clearTimeout(viewerResizeTimer);

    if (videoProgressTimer) {
        clearInterval(videoProgressTimer);
        videoProgressTimer = null;
    }

    if (pdfDocument) {
        pdfDocument.destroy().catch(() => {});
    }

    if (videoBlobUrl) {
        URL.revokeObjectURL(videoBlobUrl);
        videoBlobUrl = null;
    }

    pdfDocument = null;
    activeMaterial = null;
    pdfCurrentPage = 1;
    pdfZoom = 1;
    pdfBusy = false;
    pdfQueued = null;
    completionUnlocked = false;
}

// =====================================================
// FETCH MATERIAL FILE
// =====================================================

async function fetchMaterialBlob(material) {
    const fileUrl = getMaterialUrl(material.fileUrl);
    const token = localStorage.getItem("token");

    const response = await fetch(fileUrl, {
        method: "GET",
        headers: token
            ? { Authorization: `Bearer ${token}` }
            : {}
    });

    if (!response.ok) {
        throw new Error(
            `Unable to open material. HTTP ${response.status}`
        );
    }

    return await response.blob();
}

// =====================================================
// SAVE MATERIAL VIEWING PROGRESS
// =====================================================

async function saveMaterialProgress(endpoint, payload) {

    const token = localStorage.getItem("token");

    if (!token || !activeMaterial) {
        throw new Error("Authentication required");
    }

    const response = await fetch(
        `${MATERIAL_PROGRESS_API}/${endpoint}/${activeMaterial.id}`,
        {
            method: "POST",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
                Accept: "application/json"
            },
            body: JSON.stringify(payload)
        }
    );

    if (!response.ok) {
        throw new Error(
            `Progress update failed: HTTP ${response.status}`
        );
    }

    return readApiResponse(response);
}

// =====================================================
// OPEN STUDY MATERIAL
// =====================================================

async function openStudyMaterial(material) {
    try {
        createViewer(material);

        const type = String(material.fileType || "").toUpperCase();

        const blob = await fetchMaterialBlob(material);

        // Viewer was closed while the file was downloading
        if (!document.getElementById("studyViewerOverlay")) return;

        if (type.includes("PDF") || blob.type.includes("pdf")) {

            await openPdfMaterial(blob);

        } else if (
            type.includes("VIDEO") ||
            type.includes("MP4") ||
            blob.type.startsWith("video/")
        ) {

            openVideoMaterial(blob);

        } else {

            openOtherMaterial(blob, material);
        }

    } catch (error) {
        console.error("Error opening material:", error);

        const content = document.getElementById("studyViewerContent");

        if (content) {
            content.innerHTML =
                `<div class="study-viewer-loading"><p>${escapeHtml(error.message)}</p></div>`;
        }
    }
}

// =====================================================
// PDF VIEWER
// =====================================================

async function openPdfMaterial(blob) {
    const pdfjs = await loadPdfLibrary();
    const buffer = await blob.arrayBuffer();

    const doc = await pdfjs.getDocument({
        data: new Uint8Array(buffer)
    }).promise;

    // Viewer closed while loading
    if (!document.getElementById("studyViewerOverlay")) {
        doc.destroy().catch(() => {});
        return;
    }

    pdfDocument = doc;
    pdfCurrentPage = 1;
    pdfZoom = 1;

    const content = document.getElementById("studyViewerContent");

    content.classList.add("is-pdf");
    content.innerHTML = `<div class="study-pdf-stage" id="studyPdfStage"></div>`;

    document.getElementById("pdfToolbar")
        .classList.remove("study-viewer-hidden");

    document.getElementById("studyProgress")
        .classList.remove("study-viewer-hidden");

    // Toolbar actions
    document.getElementById("pdfPrevious")
        .addEventListener("click", () => changePdfPage(-1));

    document.getElementById("pdfNext")
        .addEventListener("click", () => changePdfPage(1));

    document.getElementById("pdfZoomIn")
        .addEventListener("click", () => setPdfZoom(pdfZoom + PDF_ZOOM_STEP));

    document.getElementById("pdfZoomOut")
        .addEventListener("click", () => setPdfZoom(pdfZoom - PDF_ZOOM_STEP));

    document.getElementById("pdfFit")
        .addEventListener("click", () => setPdfZoom(1));

    enablePdfSwipe(content);
    watchViewerResize(content);

    await showPdfPage(1, 0, true);
}

function changePdfPage(delta) {
    if (!pdfDocument) return;

    const target = pdfCurrentPage + delta;

    if (target < 1 || target > pdfDocument.numPages) return;

    showPdfPage(target, delta > 0 ? 1 : -1);
}

function setPdfZoom(value) {
    const next = Math.min(
        PDF_ZOOM_MAX,
        Math.max(PDF_ZOOM_MIN, Math.round(value * 100) / 100)
    );

    if (next === pdfZoom) return;

    pdfZoom = next;
    showPdfPage(pdfCurrentPage, 0, false);
}

// Swipe left / right on touch screens to change page
function enablePdfSwipe(content) {
    let startX = null;
    let startY = null;

    content.addEventListener("touchstart", event => {
        if (event.touches.length !== 1 || pdfZoom > 1) {
            startX = null;
            return;
        }

        startX = event.touches[0].clientX;
        startY = event.touches[0].clientY;
    }, { passive: true });

    content.addEventListener("touchend", event => {
        if (startX === null) return;

        const dx = event.changedTouches[0].clientX - startX;
        const dy = event.changedTouches[0].clientY - startY;

        startX = null;

        if (Math.abs(dx) > 50 && Math.abs(dx) > Math.abs(dy) * 1.5) {
            changePdfPage(dx < 0 ? 1 : -1);
        }
    }, { passive: true });
}

// Re-fit the page when the window / phone orientation changes
function watchViewerResize(content) {
    if (!("ResizeObserver" in window)) return;

    let lastWidth = content.clientWidth;
    let lastHeight = content.clientHeight;

    viewerResizeObserver = new ResizeObserver(() => {
        if (
            content.clientWidth === lastWidth &&
            content.clientHeight === lastHeight
        ) {
            return;
        }

        lastWidth = content.clientWidth;
        lastHeight = content.clientHeight;

        clearTimeout(viewerResizeTimer);

        viewerResizeTimer = setTimeout(() => {
            if (pdfDocument) {
                showPdfPage(pdfCurrentPage, 0, false);
            }
        }, 150);
    });

    viewerResizeObserver.observe(content);
}

// Draw one page into a fresh canvas, sized to fit the viewer
async function drawPdfPage(pageNumber) {
    const page = await pdfDocument.getPage(pageNumber);

    const content = document.getElementById("studyViewerContent");
    const styles = getComputedStyle(content);

    const availableWidth =
        content.clientWidth -
        parseFloat(styles.paddingLeft) -
        parseFloat(styles.paddingRight);

    const availableHeight =
        content.clientHeight -
        parseFloat(styles.paddingTop) -
        parseFloat(styles.paddingBottom);

    const base = page.getViewport({ scale: 1 });

    // "Fit" = the whole page is visible without scrolling
    const fitScale = Math.min(
        availableWidth / base.width,
        availableHeight / base.height
    );

    const viewport = page.getViewport({
        scale: Math.max(fitScale, 0.1) * pdfZoom
    });

    const ratio = Math.min(window.devicePixelRatio || 1, 2);

    const canvas = document.createElement("canvas");

    canvas.width = Math.floor(viewport.width * ratio);
    canvas.height = Math.floor(viewport.height * ratio);
    canvas.style.width = `${Math.floor(viewport.width)}px`;
    canvas.style.height = `${Math.floor(viewport.height)}px`;
    canvas.className = "study-pdf-canvas";

    await page.render({
        canvasContext: canvas.getContext("2d"),
        viewport: viewport,
        transform: ratio !== 1 ? [ratio, 0, 0, ratio, 0, 0] : null
    }).promise;

    return canvas;
}

function updatePdfControls() {
    if (!pdfDocument) return;

    const total = pdfDocument.numPages;

    const info = document.getElementById("pdfPageInfo");
    const zoomInfo = document.getElementById("pdfZoomInfo");
    const previous = document.getElementById("pdfPrevious");
    const next = document.getElementById("pdfNext");
    const zoomIn = document.getElementById("pdfZoomIn");
    const zoomOut = document.getElementById("pdfZoomOut");
    const bar = document.getElementById("studyProgressBar");

    if (info) info.textContent = `${pdfCurrentPage} / ${total}`;

    if (zoomInfo) {
        zoomInfo.textContent =
            pdfZoom === 1 ? "Fit" : `${Math.round(pdfZoom * 100)}%`;
    }

    if (previous) previous.disabled = pdfCurrentPage <= 1;
    if (next) next.disabled = pdfCurrentPage >= total;
    if (zoomIn) zoomIn.disabled = pdfZoom >= PDF_ZOOM_MAX;
    if (zoomOut) zoomOut.disabled = pdfZoom <= PDF_ZOOM_MIN;

    if (bar) bar.style.width = `${(pdfCurrentPage / total) * 100}%`;
}

// direction:  1 = next (slides in from right)
//            -1 = previous (slides in from left)
//             0 = no slide (zoom / resize / first page)
async function showPdfPage(pageNumber, direction, fadeIn) {
    if (!pdfDocument) return;

    pageNumber = Math.max(1, Math.min(pageNumber, pdfDocument.numPages));

    if (pdfBusy) {
        pdfQueued = { pageNumber, direction: direction || 0, fadeIn };
        return;
    }

    pdfBusy = true;

    try {
        const newCanvas = await drawPdfPage(pageNumber);

        const stage = document.getElementById("studyPdfStage");

        if (!stage || !pdfDocument) return;

        const oldCanvases = Array.from(stage.querySelectorAll("canvas"));

        const reduceMotion = window.matchMedia(
            "(prefers-reduced-motion: reduce)"
        ).matches;

        if (direction && oldCanvases.length && !reduceMotion) {

            const dir = direction > 0 ? "next" : "prev";

            newCanvas.classList.add(`enter-${dir}`);
            oldCanvases.forEach(c => c.classList.add(`leave-${dir}`));

            stage.appendChild(newCanvas);

            setTimeout(() => {
                oldCanvases.forEach(c => c.remove());
                newCanvas.classList.remove(`enter-${dir}`);
            }, PDF_ANIMATION_MS);

        } else {

            if (fadeIn && !reduceMotion) {
                newCanvas.classList.add("enter-fade");
            }

            oldCanvases.forEach(c => c.remove());
            stage.appendChild(newCanvas);
        }

        const pageChanged = pageNumber !== pdfCurrentPage || fadeIn;

        pdfCurrentPage = pageNumber;
        updatePdfControls();

        const content = document.getElementById("studyViewerContent");

        if (content && direction) {
            content.scrollTo({ top: 0, left: 0 });
        }

        // Save PDF page progress
        if (pageChanged) {
            const total = pdfDocument.numPages;

            saveMaterialProgress("pdf", {
                currentPage: pageNumber,
                totalPages: total
            }).then(() => {
                if (pageNumber === total) {
                    unlockCompletion();
                }
            }).catch(error => {
                console.error("PDF progress error:", error);
            });
        }

    } catch (error) {
        console.error("PDF rendering error:", error);
    } finally {
        pdfBusy = false;

        if (pdfQueued) {
            const queued = pdfQueued;
            pdfQueued = null;

            showPdfPage(
                queued.pageNumber,
                queued.direction,
                queued.fadeIn
            );
        }
    }
}

// =====================================================
// VIDEO VIEWER
// =====================================================

function openVideoMaterial(blob) {

    const content = document.getElementById("studyViewerContent");

    videoBlobUrl = URL.createObjectURL(blob);

    content.innerHTML = `
        <video
            id="studyVideoPlayer"
            class="study-video"
            controls
            playsinline
            controlsList="nodownload"
            preload="metadata">

            <source
                src="${videoBlobUrl}"
                type="${escapeHtml(blob.type || "video/mp4")}">

            Your browser does not support video playback.

        </video>
    `;

    const video = document.getElementById("studyVideoPlayer");

    video.addEventListener("timeupdate", () => {

        if (!Number.isFinite(video.duration) || video.duration <= 0) {
            return;
        }

        const remaining = video.duration - video.currentTime;

        if (remaining <= 60 && video.currentTime > 0) {
            unlockCompletion();
        }
    });

    // Send progress periodically while video is playing
    video.addEventListener("play", () => {

        if (videoProgressTimer) {
            clearInterval(videoProgressTimer);
        }

        videoProgressTimer = setInterval(async () => {

            if (video.paused || video.ended) return;

            const now = Date.now();

            if (now - lastVideoProgressSent < 5000) return;

            lastVideoProgressSent = now;

            try {
                await saveMaterialProgress("video", {
                    currentTime: video.currentTime,
                    duration: video.duration,
                    playing: !video.paused
                });
            } catch (error) {
                console.error("Video progress error:", error);
            }

        }, 5000);
    });

    const stopTimer = () => {
        if (videoProgressTimer) {
            clearInterval(videoProgressTimer);
            videoProgressTimer = null;
        }
    };

    video.addEventListener("pause", stopTimer);
    video.addEventListener("ended", stopTimer);
}

// =====================================================
// OTHER MATERIAL TYPES
// =====================================================

function openOtherMaterial(blob, material) {
    const content = document.getElementById("studyViewerContent");

    const type = String(material.fileType || "").toUpperCase();

    if (blob.type.startsWith("text/") ||
        type.includes("TEXT") ||
        type.includes("NOTES")) {

        blob.text().then(text => {

            content.innerHTML = `<article id="studyTextContent" class="study-text"></article>`;

            document.getElementById("studyTextContent").textContent = text;

            const checkBottom = () => {
                const reachedBottom =
                    content.scrollTop + content.clientHeight >=
                    content.scrollHeight - 10;

                if (reachedBottom && !textProgressSent) {

                    textProgressSent = true;

                    saveMaterialProgress("text", { reachedBottom: true })
                        .then(() => unlockCompletion())
                        .catch(error => {
                            textProgressSent = false;
                            console.error("Text progress error:", error);
                        });
                }
            };

            content.addEventListener("scroll", checkBottom);

            // Short text that doesn't need scrolling
            checkBottom();
        });

        return;
    }

    const url = URL.createObjectURL(blob);

    content.innerHTML = `
        <div class="study-other">
            <p>This material format does not currently support
            automatic completion tracking.</p>

            <a href="${url}" target="_blank" rel="noopener noreferrer">
                Open material in a new tab
            </a>
        </div>
    `;
}

// =====================================================
// ESCAPE HTML
// =====================================================

function escapeHtml(value) {
    return String(value || "").replace(/[&<>"']/g, char => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#039;"
    })[char]);
}

// =====================================================
// LOAD STUDY MATERIALS
// =====================================================

async function loadMaterials() {
    const studentId = localStorage.getItem("userId");
   const courseId =
    new URLSearchParams(window.location.search).get("courseId") ||
    localStorage.getItem("courseId");
    const token = localStorage.getItem("token");

    const container = document.getElementById("materialsContainer");

    if (!container) return;

    if (!studentId || !token) {
        container.innerHTML = `
            <div class="empty-state">
                <h3>Session expired</h3>
                <p>Please log in again.</p>
            </div>
        `;
        return;
    }

    if (!courseId) {
        container.innerHTML = `
            <div class="empty-state">
                <h3>No course selected</h3>
                <p>Please select an enrolled course.</p>
            </div>
        `;
        return;
    }

    container.innerHTML = `
        <div class="materials-loading">
            <div class="loading-spinner"></div>
            <p>Loading your study materials...</p>
        </div>
    `;

    try {
        const materialsResponse = await fetch(
            `${MATERIALS_API}/course/${encodeURIComponent(courseId)}`,
            {
                headers: {
                    Authorization: `Bearer ${token}`,
                    Accept: "application/json"
                }
            }
        );

        if (!materialsResponse.ok) {
            throw new Error(
                `Failed to load materials: HTTP ${materialsResponse.status}`
            );
        }

        const materialsResult = await materialsResponse.json();

        const materials = Array.isArray(materialsResult)
            ? materialsResult
            : Array.isArray(materialsResult.data)
                ? materialsResult.data
                : [];

        const completedResponse = await fetch(
            `${MATERIAL_PROGRESS_API}/student/${encodeURIComponent(studentId)}`,
            {
                headers: {
                    Authorization: `Bearer ${token}`,
                    Accept: "application/json"
                }
            }
        );

        if (!completedResponse.ok) {
            throw new Error(
                `Failed to load completion status: HTTP ${completedResponse.status}`
            );
        }

        const completedResult = await completedResponse.json();

        const completedIds = Array.isArray(completedResult)
            ? completedResult
            : Array.isArray(completedResult.data)
                ? completedResult.data
                : [];

        const completedSet = new Set(
            completedIds.map(item => {
                if (typeof item === "object" && item !== null) {
                    return Number(item.materialId ?? item.id);
                }

                return Number(item);
            }).filter(Number.isFinite)
        );

        container.replaceChildren();

        if (!materials.length) {
            container.innerHTML = `
                <div class="empty-state">
                    <h3>No study materials available</h3>
                    <p>Your faculty has not added materials yet.</p>
                </div>
            `;
            return;
        }

        materials.forEach(material => {
            const isCompleted = completedSet.has(Number(material.id));

            const card = document.createElement("article");
            card.className = "material-card";

            // ----- Top row: file icon + small icon-only buttons -----
            const top = document.createElement("div");
            top.className = "material-top";

            const icon = document.createElement("div");
            icon.className = "material-icon";
            icon.textContent = getMaterialIcon(material.fileType);

            const tools = document.createElement("div");
            tools.className = "material-tools";

            if (isCompleted) {

                // Completed symbol (not clickable)
                const doneButton = document.createElement("button");
                doneButton.type = "button";
                doneButton.className = "icon-btn is-done";
                doneButton.disabled = true;
                doneButton.title = "Completed";
                doneButton.setAttribute("aria-label", "Completed");
                doneButton.innerHTML = ICON_CHECK;

                // Download symbol
                const downloadButton = document.createElement("button");
                downloadButton.type = "button";
                downloadButton.className = "icon-btn";
                downloadButton.title = "Download material";
                downloadButton.setAttribute("aria-label", "Download material");
                downloadButton.innerHTML = ICON_DOWNLOAD;

                downloadButton.addEventListener("click", () => {
                    downloadMaterial(material);
                });

                tools.append(doneButton, downloadButton);
            }

            top.append(icon, tools);

            // ----- Text -----
            const title = document.createElement("h3");
            title.className = "material-title";
            title.textContent = material.title || "Untitled Material";
            title.title = material.title || "Untitled Material";

            const type = document.createElement("span");
            type.className = "material-type";
            type.textContent = material.fileType || "Study Material";

            const description = document.createElement("p");
            description.className = "material-desc";
            description.textContent =
                material.description || "No description provided.";

            // ----- Footer: status + small Open button -----
            const footer = document.createElement("div");
            footer.className = "material-footer";

            const status = document.createElement("div");
            status.className =
                `material-status ${isCompleted ? "is-completed" : "is-pending"}`;

            const dot = document.createElement("span");
            dot.className = "status-dot";

            const statusText = document.createElement("span");
            statusText.textContent = isCompleted ? "Completed" : "Pending";

            status.append(dot, statusText);

            const openButton = document.createElement("button");
            openButton.type = "button";
            openButton.className = "open-btn";
            openButton.innerHTML = `<span>Open</span>${ICON_OPEN}`;
            openButton.setAttribute("aria-label", `Open ${material.title || "material"}`);

            openButton.addEventListener("click", () => {
                openStudyMaterial(material);
            });

            footer.append(status, openButton);

            card.append(top, title, type, description, footer);

            container.appendChild(card);
        });

    } catch (error) {
        console.error("Load Materials Error:", error);

        container.innerHTML = `
            <div class="empty-state error-state">
                <h3>Unable to load study materials</h3>
                <p>Please check your connection and try again.</p>
                <button type="button"
                    class="retry-btn"
                    id="retryMaterialsBtn">
                    Try Again
                </button>
            </div>
        `;

        document.getElementById("retryMaterialsBtn")
            ?.addEventListener("click", loadMaterials);
    }
}

// =====================================================
// MATERIAL ICON
// =====================================================

function getMaterialIcon(fileType) {
    const type = String(fileType || "").toLowerCase();

    if (type.includes("pdf")) return "PDF";

    if (type.includes("video") || type.includes("mp4")) {
        return "▶";
    }

    if (type.includes("ppt") || type.includes("presentation")) {
        return "PPT";
    }

    if (type.includes("doc") || type.includes("word")) {
        return "DOC";
    }

    if (type.includes("image") ||
        type.includes("jpg") ||
        type.includes("png")) {
        return "IMG";
    }

    return "FILE";
}

// =====================================================
// MARK MATERIAL AS COMPLETED
// =====================================================

async function completeMaterial(materialId, button) {
    const token = localStorage.getItem("token");

    if (!token) {
        alert("Please log in again.");
        return;
    }

    if (!completionUnlocked) {
        alert("Please complete the material before marking it completed.");
        return;
    }

    button.disabled = true;
    button.textContent = "Saving...";

    try {
        const response = await fetch(
            `${MATERIAL_PROGRESS_API}/complete/${encodeURIComponent(materialId)}`,
            {
                method: "POST",
                headers: {
                    Authorization: `Bearer ${token}`,
                    Accept: "application/json"
                }
            }
        );

        if (!response.ok) {
            throw new Error(
                `HTTP ${response.status}: ${await response.text()}`
            );
        }

        closeStudyViewer();
        await loadMaterials();

    } catch (error) {
        console.error("Error completing material:", error);

        alert("Unable to mark this material as completed.");

        button.disabled = false;
        button.textContent = "Mark as Completed";
    }
}

// =====================================================
// PAGE LOAD
// =====================================================

document.addEventListener("DOMContentLoaded", () => {
    loadMaterials();
});