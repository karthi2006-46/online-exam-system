// =====================================================
// STUDENT PROFILE - LMS
// =====================================================

const API_BASE = `${window.API_BASE_URL}/api`;

const PROFILE_ENDPOINT = (userId) =>
    `${API_BASE}/auth/profile/${encodeURIComponent(userId)}/complete`;

const CHANGE_PASSWORD_ENDPOINT =
    `${API_BASE}/auth/change-password`;

const userId = localStorage.getItem("userId");
const token = localStorage.getItem("token");

const PROFILE_PICTURE_KEY = (id) => `profilePicture_${id}`;


// =====================================================
// INITIALIZE PROFILE PAGE
// =====================================================

document.addEventListener("DOMContentLoaded", () => {

    if (!userId || !token) {
        alert("Please log in to access your profile.");
        window.location.href = "../../login.html";
        return;
    }

    loadProfile();

    setupPasswordForm();

    setupPasswordVisibility();

    setupProfilePicture();

    setupLogout();

});


// =====================================================
// API HEADERS
// =====================================================

function getHeaders() {

    return {
        "Accept": "application/json",
        "Content-Type": "application/json",
        "Authorization": `Bearer ${token}`
    };

}


// =====================================================
// LOAD STUDENT PROFILE
// =====================================================

async function loadProfile() {

    const courseContainer =
        document.getElementById("courseContainer");

    if (courseContainer) {
        courseContainer.textContent =
            "Loading course information...";
    }

    try {

        const response = await fetch(
            PROFILE_ENDPOINT(userId),
            {
                method: "GET",
                headers: getHeaders()
            }
        );

        const result = await readResponse(response);

        if (!response.ok) {
            throw new Error(
                result.message ||
                result.error ||
                "Unable to load profile."
            );
        }

        /*
         * Supports responses such as:
         *
         * {
         *   "data": {
         *      "user": {...},
         *      "courses": [...]
         *   }
         * }
         *
         * or a direct profile object.
         */

        const profile = result.data || result;

        const user = profile.user || profile;

        const courses = Array.isArray(profile.courses)
            ? profile.courses
            : [];

        displayPersonalInformation(user);

        displayCourses(courses);

    } catch (error) {

        console.error(
            "Profile loading error:",
            error
        );

        if (courseContainer) {
            courseContainer.textContent =
                "Course information could not be loaded.";
        }

        showMessage(
            "passwordMessage",
            error.message ||
                "Unable to load profile information.",
            "error"
        );

    }

}


// =====================================================
// DISPLAY PERSONAL INFORMATION
// =====================================================

function displayPersonalInformation(user) {

    // Full name
    const fullName =
        `${user.firstName || ""} ${user.lastName || ""}`.trim()
        || user.fullName
        || user.name
        || "Student";

    setText("profileHeroName", fullName);

    setText("fullNameValue", fullName);


    // Email
    setText(
        "email",
        user.email || "Not provided"
    );


    // Phone
    setText(
        "phone",
        user.phone || "Not provided"
    );


    // Address
    setText(
        "address",
        user.address || "Not provided"
    );


    // Role
    let role =
        user.roleName ||
        user.role ||
        localStorage.getItem("role") ||
        "Student";

    if (typeof role === "object" && role !== null) {
        role = role.name || role.roleName || "Student";
    }

    role = String(role).replace(/^ROLE_/, "");

    setText("role", role);

    setText("heroRole", role);


    // Account status
    const activeValue =
        user.active ??
        user.isActive ??
        user.status ??
        true;

    const isActive =
        typeof activeValue === "string"
            ? (
                activeValue.toLowerCase() === "active" ||
                activeValue.toLowerCase() === "true"
            )
            : Boolean(activeValue);

    const statusText = isActive
        ? "Active"
        : "Inactive";

    setText("status", statusText);

    setText("heroStatus", statusText);


    document
        .getElementById("status")
        ?.classList.toggle("inactive", !isActive);

    document
        .getElementById("heroStatus")
        ?.classList.toggle("inactive", !isActive);


    // Registration date
    setText(
        "createdAt",
        formatDate(user.createdAt || user.registrationDate)
    );


    // Default avatar initials
    const initials = fullName
        .split(/\s+/)
        .map(part => part.charAt(0))
        .slice(0, 2)
        .join("")
        .toUpperCase() || "S";

    setText("profileAvatar", initials);


    // Load saved profile picture
    loadSavedProfilePicture();

}


// =====================================================
// DISPLAY ENROLLED COURSES
// Course completion and progress bar are removed.
// =====================================================

function displayCourses(courses) {

    const container =
        document.getElementById("courseContainer");

    if (!container) {
        return;
    }

    container.replaceChildren();


    // No enrolled courses
    if (!courses.length) {

        const emptyMessage =
            document.createElement("p");

        emptyMessage.className = "empty-text";

        emptyMessage.textContent =
            "You are not currently enrolled in any courses.";

        container.appendChild(emptyMessage);

        return;
    }


    courses.forEach(course => {

        const card =
            document.createElement("article");

        card.className = "course-card";


        // Course title
        const title =
            document.createElement("h3");

        title.textContent =
            course.courseTitle ||
            course.title ||
            "Untitled Course";

        card.appendChild(title);


        // Course description
        const description =
            document.createElement("p");

        description.className =
            "course-description";

        description.textContent =
            course.description ||
            "No course description available.";

        card.appendChild(description);


        // Course details
        const details =
            document.createElement("div");

        details.className = "course-details";


        addCourseDetail(
            details,
            "Course Code",
            course.courseCode
        );

        addCourseDetail(
            details,
            "Duration",
            course.durationDays != null
                ? `${course.durationDays} days`
                : "Not specified"
        );

        addCourseDetail(
            details,
            "Course Fees",
            formatCurrency(course.totalFees)
        );

        addCourseDetail(
            details,
            "Enrollment Date",
            formatDate(course.enrollmentDate)
        );

        addCourseDetail(
            details,
            "Due Date",
            formatDate(course.dueDate)
        );

        addCourseDetail(
            details,
            "Enrollment Status",
            course.status || "Not specified"
        );


        card.appendChild(details);

        container.appendChild(card);

    });

}


// =====================================================
// CREATE COURSE DETAIL ELEMENT
// =====================================================

function addCourseDetail(container, label, value) {

    const item =
        document.createElement("div");

    item.className = "course-detail";


    const labelElement =
        document.createElement("span");

    labelElement.textContent = label;


    const valueElement =
        document.createElement("span");

    valueElement.textContent =
        value === null ||
        value === undefined ||
        value === ""
            ? "Not specified"
            : String(value);


    item.append(
        labelElement,
        valueElement
    );

    container.appendChild(item);

}


// =====================================================
// FORMAT COURSE FEES
// =====================================================

function formatCurrency(amount) {

    if (
        amount === null ||
        amount === undefined ||
        amount === ""
    ) {
        return "Not specified";
    }

    const number = Number(amount);

    if (!Number.isFinite(number)) {
        return "Not specified";
    }

    return new Intl.NumberFormat("en-IN", {
        style: "currency",
        currency: "INR",
        maximumFractionDigits: 2
    }).format(number);

}


// =====================================================
// FORMAT DATE
// =====================================================

function formatDate(value) {

    if (!value) {
        return "Not provided";
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return "Not provided";
    }

    return date.toLocaleDateString("en-IN", {
        day: "2-digit",
        month: "short",
        year: "numeric"
    });

}


// =====================================================
// PASSWORD FORM
// =====================================================

function setupPasswordForm() {

    const form =
        document.getElementById("passwordForm");

    if (!form) {
        return;
    }

    form.addEventListener("submit", async event => {

        event.preventDefault();


        const currentPassword =
            getValue("currentPassword");

        const newPassword =
            getValue("newPassword");

        const confirmPassword =
            getValue("confirmPassword");


        // Required fields
        if (
            !currentPassword ||
            !newPassword ||
            !confirmPassword
        ) {

            showMessage(
                "passwordMessage",
                "Please fill in all password fields.",
                "error"
            );

            return;
        }


        // Minimum password length
        if (newPassword.length < 8) {

            showMessage(
                "passwordMessage",
                "Your new password must contain at least 8 characters.",
                "error"
            );

            return;
        }


        // New password must differ from current password
        if (newPassword === currentPassword) {

            showMessage(
                "passwordMessage",
                "Your new password must differ from your current password.",
                "error"
            );

            return;
        }


        // Confirm password
        if (newPassword !== confirmPassword) {

            showMessage(
                "passwordMessage",
                "The new password and confirmation do not match.",
                "error"
            );

            return;
        }


        await submitPasswordChange({
            currentPassword,
            newPassword,
            confirmPassword
        });

    });

}


// =====================================================
// SUBMIT PASSWORD CHANGE
// =====================================================

async function submitPasswordChange(passwordData) {

    const button =
        document.getElementById("changePasswordBtn");

    if (button) {

        button.disabled = true;

        button.textContent = "Updating...";

    }

    try {

        const response = await fetch(
            CHANGE_PASSWORD_ENDPOINT,
            {
                method: "POST",
                headers: getHeaders(),
                body: JSON.stringify(passwordData)
            }
        );


        const result =
            await readResponse(response);


        if (!response.ok) {

            throw new Error(
                result.message ||
                result.error ||
                "Unable to change password."
            );

        }


        showMessage(
            "passwordMessage",
            result.message ||
                "Password changed successfully.",
            "success"
        );


        // Clear password fields after success
        document
            .getElementById("passwordForm")
            .reset();


        // Reset visibility buttons
        document
            .querySelectorAll(".toggle-password")
            .forEach(button => {

                button.textContent = "Show";

                const input =
                    document.getElementById(
                        button.dataset.target
                    );

                if (input) {
                    input.type = "password";
                }

            });


    } catch (error) {

        console.error(
            "Password change error:",
            error
        );

        showMessage(
            "passwordMessage",
            error.message ||
                "Unable to change password.",
            "error"
        );

    } finally {

        if (button) {

            button.disabled = false;

            button.textContent = "Change Password";

        }

    }

}


// =====================================================
// SHOW / HIDE PASSWORD
// =====================================================

function setupPasswordVisibility() {

    document
        .querySelectorAll(".toggle-password")
        .forEach(button => {

            button.addEventListener("click", () => {

                const input =
                    document.getElementById(
                        button.dataset.target
                    );

                if (!input) {
                    return;
                }

                const shouldShow =
                    input.type === "password";

                input.type =
                    shouldShow ? "text" : "password";

                button.textContent =
                    shouldShow ? "Hide" : "Show";


                button.setAttribute(
                    "aria-label",
                    `${shouldShow ? "Hide" : "Show"} password`
                );

            });

        });

}


// =====================================================
// PROFILE PICTURE UPLOAD / PREVIEW
// =====================================================

function setupProfilePicture() {

    const input =
        document.getElementById("profilePictureInput");

    if (!input) {
        return;
    }

    input.addEventListener("change", () => {

        const file = input.files?.[0];

        if (!file) {
            return;
        }


        // Validate file type
        if (!file.type.startsWith("image/")) {

            showMessage(
                "profilePictureMessage",
                "Please select an image file.",
                "error"
            );

            input.value = "";

            return;
        }


        // Limit image size to 2 MB
        if (file.size > 2 * 1024 * 1024) {

            showMessage(
                "profilePictureMessage",
                "Please choose an image smaller than 2 MB.",
                "error"
            );

            input.value = "";

            return;
        }


        const reader = new FileReader();

        reader.onload = () => {

            try {

                const imageData = reader.result;

                localStorage.setItem(
                    PROFILE_PICTURE_KEY(userId),
                    imageData
                );

                applyProfilePicture(imageData);


                showMessage(
                    "profilePictureMessage",
                    "Profile picture saved in this browser.",
                    "success"
                );

            } catch (error) {

                console.error(
                    "Profile picture storage error:",
                    error
                );

                showMessage(
                    "profilePictureMessage",
                    "Unable to save this image. Please choose a smaller image.",
                    "error"
                );

            }

        };


        reader.onerror = () => {

            showMessage(
                "profilePictureMessage",
                "Unable to read the selected image.",
                "error"
            );

        };


        reader.readAsDataURL(file);

    });

}


// =====================================================
// LOAD SAVED PROFILE PICTURE
// =====================================================

function loadSavedProfilePicture() {

    try {

        const savedImage =
            localStorage.getItem(
                PROFILE_PICTURE_KEY(userId)
            );

        if (savedImage) {
            applyProfilePicture(savedImage);
        }

    } catch (error) {

        console.error(
            "Unable to load profile picture:",
            error
        );

    }

}


// =====================================================
// DISPLAY PROFILE PICTURE
// =====================================================

function applyProfilePicture(imageSource) {

    const image =
        document.getElementById("profilePicture");

    const avatar =
        document.getElementById("profileAvatar");

    if (!image || !avatar) {
        return;
    }

    image.src = imageSource;

    image.hidden = false;

    avatar.hidden = true;

}


// =====================================================
// GET INPUT VALUE
// =====================================================

function getValue(id) {

    return (
        document.getElementById(id)?.value?.trim() || ""
    );

}


// =====================================================
// SET TEXT SAFELY
// =====================================================

function setText(id, value) {

    const element =
        document.getElementById(id);

    if (!element) {
        return;
    }

    element.textContent =
        value === null ||
        value === undefined ||
        value === ""
            ? "Not provided"
            : value;

}


// =====================================================
// DISPLAY SUCCESS / ERROR MESSAGES
// =====================================================

function showMessage(id, message, type) {

    const element =
        document.getElementById(id);

    if (!element) {
        return;
    }

    element.textContent = message || "";


    if (id === "passwordMessage") {

        element.className = "form-message";

    } else {

        element.className = "small-message";

    }


    if (type) {
        element.classList.add(type);
    }

    element.hidden = !message;

}


// =====================================================
// READ API RESPONSE
// =====================================================

async function readResponse(response) {

    const responseText =
        await response.text();

    if (!responseText) {
        return {};
    }

    try {

        return JSON.parse(responseText);

    } catch {

        return {
            message: responseText
        };

    }

}


// =====================================================
// LOGOUT
// =====================================================

function setupLogout() {

    const logoutButton =
        document.getElementById("logoutBtn");

    if (!logoutButton) {
        return;
    }

    logoutButton.addEventListener("click", event => {

        event.preventDefault();

        const confirmed = confirm(
            "Are you sure you want to log out?"
        );

        if (!confirmed) {
            return;
        }


        localStorage.removeItem("token");
        localStorage.removeItem("refreshToken");
        localStorage.removeItem("userId");
        localStorage.removeItem("role");
        localStorage.removeItem("user");


        window.location.href = "../../login.html";

    });

}