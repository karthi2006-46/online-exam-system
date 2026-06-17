// API Configuration
const API_BASE_URL = 'http://localhost:8080/api';

class APIClient {
    constructor() {
        this.token = localStorage.getItem('token');
    }

    async request(endpoint, method = 'GET', data = null) {
        const url = `${API_BASE_URL}${endpoint}`;
        const headers = {
            'Content-Type': 'application/json',
        };

        if (this.token) {
            headers['Authorization'] = `Bearer ${this.token}`;
        }

        const options = {
            method,
            headers,
        };

        if (data) {
            options.body = JSON.stringify(data);
        }

        try {
            const response = await fetch(url, options);
            const result = await response.json();
            return result;
        } catch (error) {
            console.error('API Error:', error);
            throw error;
        }
    }

    // Auth endpoints
    async login(email, password) {
        return this.request('/auth/login', 'POST', { email, password });
    }

    async signup(userData) {
        return this.request('/auth/signup', 'POST', userData);
    }

    async getUserProfile(userId) {
        return this.request(`/auth/profile/${userId}`, 'GET');
    }

    // Courses endpoints
    async getCourses() {
        return this.request('/courses/active', 'GET');
    }

    async getCourseById(courseId) {
        return this.request(`/courses/${courseId}`, 'GET');
    }

    async createCourse(courseData) {
        return this.request('/courses', 'POST', courseData);
    }

    // Enrollments endpoints
    async getStudentEnrollments(studentId) {
        return this.request(`/enrollments/student/${studentId}`, 'GET');
    }

    async enrollStudent(enrollData) {
        return this.request('/enrollments', 'POST', enrollData);
    }

    // Exams endpoints
    async getPublishedExams(courseId) {
        return this.request(`/exams/course/${courseId}/published`, 'GET');
    }

    async getExamById(examId) {
        return this.request(`/exams/${examId}`, 'GET');
    }

    // Questions endpoints
    async getExamQuestions(examId) {
        return this.request(`/questions/exam/${examId}`, 'GET');
    }

    // Exam Attempts endpoints
    async startExam(examId, studentId) {
        return this.request(`/exam-attempts/start?examId=${examId}&studentId=${studentId}`, 'POST');
    }

    async saveAnswers(attemptId, answers) {
        return this.request(`/exam-attempts/${attemptId}/save-answers`, 'PUT', answers);
    }

    async submitExam(attemptId) {
        return this.request(`/exam-attempts/${attemptId}/submit`, 'PUT');
    }

    // Results endpoints
    async getStudentResults(studentId) {
        return this.request(`/results/student/${studentId}`, 'GET');
    }

    async getResultByAttempt(attemptId) {
        return this.request(`/results/attempt/${attemptId}`, 'GET');
    }
}

const api = new APIClient();
