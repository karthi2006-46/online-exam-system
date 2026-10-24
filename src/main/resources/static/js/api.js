// ==========================================
// LMS API CONFIGURATION
// Works on localhost and mobile devices
// ==========================================

const API_BASE_URL = "/api";

class APIClient {

    constructor() {
        this.refreshToken();
    }

    // Always read the latest token from localStorage
    refreshToken() {
        this.token = localStorage.getItem("token");
    }

    async request(endpoint, method = "GET", data = null) {

        this.refreshToken();

        const url = `${API_BASE_URL}${endpoint}`;

        const headers = {
            "Accept": "application/json"
        };

        if (data !== null) {
            headers["Content-Type"] = "application/json";
        }

        if (this.token) {
            headers["Authorization"] = `Bearer ${this.token}`;
        }

        const options = {
            method,
            headers
        };

        if (data !== null) {
            options.body = JSON.stringify(data);
        }

        console.log("API Request:", method, url);
        console.log("Token available:", Boolean(this.token));

        try {

            const response = await fetch(url, options);

            const contentType =
                response.headers.get("content-type") || "";

            let result;

            if (contentType.includes("application/json")) {
                result = await response.json();
            } else {
                result = await response.text();
            }

            console.log("API Response:", response.status, result);

            if (!response.ok) {
                throw new Error(
                    `API request failed (${response.status}): ` +
                    (typeof result === "string"
                        ? result
                        : JSON.stringify(result))
                );
            }

            return result;

        } catch (error) {

            console.error("API Error:", url, error);

            throw error;
        }
    }

    // ==========================================
    // AUTHENTICATION
    // ==========================================

    async login(email, password) {

        const result = await this.request(
            "/auth/login",
            "POST",
            { email, password }
        );

        // Adapt these property names to your actual AuthResponse.
        const token =
            result?.token ??
            result?.accessToken ??
            result?.data?.token ??
            result?.data?.accessToken;

        if (token) {
            localStorage.setItem("token", token);
            this.refreshToken();
        }

        return result;
    }

    async signup(userData) {
        return this.request("/auth/signup", "POST", userData);
    }

    async getUserProfile(userId) {
        return this.request(`/auth/profile/${userId}`);
    }

    // ==========================================
    // COURSES
    // ==========================================

    async getCourses() {
        return this.request("/courses/active");
    }

    async getCourseById(courseId) {
        return this.request(`/courses/${courseId}`);
    }

    async createCourse(courseData) {
        return this.request("/courses", "POST", courseData);
    }

    // ==========================================
    // ENROLLMENTS
    // ==========================================

    async getStudentEnrollments(studentId) {
        return this.request(`/enrollments/student/${studentId}`);
    }

    async enrollStudent(enrollData) {
        return this.request("/enrollments", "POST", enrollData);
    }

    // ==========================================
    // EXAMS
    // ==========================================

    async getPublishedExams(courseId) {
        return this.request(`/exams/course/${courseId}/published`);
    }

    async getExamById(examId) {
        return this.request(`/exams/${examId}`);
    }

    // ==========================================
    // QUESTIONS
    // ==========================================

    async getExamQuestions(examId) {
        return this.request(`/questions/exam/${examId}`);
    }

    // ==========================================
    // EXAM ATTEMPTS
    // ==========================================

    async startExam(examId, studentId) {
        return this.request(
            `/exam-attempts/start?examId=${examId}&studentId=${studentId}`,
            "POST"
        );
    }

    async saveAnswers(attemptId, answers) {
        return this.request(
            `/exam-attempts/${attemptId}/save-answers`,
            "PUT",
            answers
        );
    }

    async submitExam(attemptId) {
        return this.request(
            `/exam-attempts/${attemptId}/submit`,
            "PUT"
        );
    }

    // ==========================================
    // RESULTS
    // ==========================================

    async getStudentResults(studentId) {
        return this.request(`/results/student/${studentId}`);
    }

    async getResultByAttempt(attemptId) {
        return this.request(`/results/attempt/${attemptId}`);
    }
}

const api = new APIClient();