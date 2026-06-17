<<<<<<< HEAD
# online-exam-system
=======
# Online Examination and Learning Management System

A complete Spring Boot-based Online Exam and Learning Management System designed for local system deployment.

## Project Overview

This system provides a comprehensive platform for educational institutions to manage:
- Student enrollments and progress tracking
- Course management with study materials
- Online examinations with MCQ questions
- Result management and reporting
- Role-based access control (Admin, Faculty, Student)

## Technology Stack

### Frontend
- HTML5
- CSS3
- Bootstrap 5
- JavaScript (Vanilla)

### Backend
- Java 17
- Spring Boot 3.2.0
- Spring MVC
- Spring Security
- Spring Data JPA
- Hibernate ORM

### Database
- MySQL 8.0

### Build Tool
- Maven 3.8+

## Prerequisites

- Java 17 or higher
- MySQL 8.0
- Maven 3.8+
- Git
- IDE (IntelliJ IDEA, Eclipse, or VS Code)

## Installation & Setup

### 1. Clone the Repository
```bash
git clone https://github.com/extrrnaluse-beep/online-exam-system.git
cd online-exam-system
```

### 2. Database Setup

Create MySQL database:
```sql
CREATE DATABASE online_exam_lms;
USE online_exam_lms;
```

The database schema will be auto-created by Hibernate (set to `create` in development).

### 3. Configure Application

Update `application.properties` with your database credentials:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/online_exam_lms
spring.datasource.username=root
spring.datasource.password=your_password
```

### 4. Build the Project
```bash
mvn clean install
```

### 5. Run the Application
```bash
mvn spring-boot:run
```

The application will start at `http://localhost:8080`

## Project Structure

```
src/
├── main/
│   ├── java/com/lms/
│   │   ├── controller/          # REST API Controllers
│   │   ├── service/             # Business Logic
│   │   ├── repository/          # Data Access Layer
│   │   ├── entity/              # JPA Entities
│   │   ├── dto/                 # Data Transfer Objects
│   │   ├── config/              # Spring Configuration
│   │   ├── security/            # Security Configuration & JWT
│   │   ├── exception/           # Custom Exceptions
│   │   ├── util/                # Utility Classes
│   │   └── OnlineExamSystemApplication.java
│   └── resources/
│       ├── static/              # CSS, JS, Images
│       │   ├── css/
│       │   ├── js/
│       │   └── images/
│       ├── templates/           # HTML Templates
│       └── application.properties
└── test/                        # Test Classes
```

## Modules

### 1. Authentication Module
- Student/Faculty Signup
- Login with JWT tokens
- Role-based access control
- Token refresh mechanism

### 2. Course Management
- Create and manage courses
- Student enrollment
- Upload study materials
- Course duration tracking

### 3. Study Materials
- Upload and manage PDF notes
- Video references
- Assignments

### 4. Progress Tracking
- Course completion percentage
- Progress visualization
- Duration management

### 5. Course Extension
- Student can request extensions (max 3)
- Faculty approval/rejection
- Extension status tracking

### 6. Examination
- MCQ-based questions
- Timer-based exams
- Auto-submit functionality
- Result calculation

### 7. Results & Reporting
- Marks and percentage
- Pass/Fail status
- Exam history
- Excel export (Apache POI)

### 8. Dashboards
- Faculty Dashboard: Course, Material, Exam Management
- Admin Dashboard: User Management, Reports
- Student Dashboard: Enrollment, Progress, Exams

## Database Schema

Key tables:
- `users` - User accounts
- `roles` - User roles (ADMIN, FACULTY, STUDENT)
- `courses` - Course information
- `enrollments` - Student enrollments
- `study_materials` - Course materials
- `progress_tracking` - Learning progress
- `extension_requests` - Extension requests
- `exams` - Exam details
- `questions` - MCQ questions
- `exam_attempts` - Exam attempts
- `results` - Exam results

## API Endpoints

### Authentication
- `POST /api/auth/signup` - Register new user
- `POST /api/auth/login` - Login user
- `POST /api/auth/refresh` - Refresh JWT token

### Courses
- `GET /api/courses` - Get all courses
- `POST /api/courses` - Create course
- `PUT /api/courses/{id}` - Update course
- `DELETE /api/courses/{id}` - Delete course

### Enrollments
- `POST /api/enrollments` - Enroll student
- `GET /api/enrollments/student/{id}` - Get student enrollments

### Exams
- `GET /api/exams/{id}` - Get exam details
- `POST /api/exams/{id}/attempt` - Submit exam attempt
- `GET /api/exams/{id}/results` - Get exam results

### Reports
- `GET /api/reports/export/students` - Export student results
- `GET /api/reports/export/progress` - Export course progress

## Features

✅ JWT Authentication with Role-Based Access Control
✅ Secure Password Management (BCrypt Encryption)
✅ Student Progress Tracking with Visualization
✅ MCQ Exam Management with Timer
✅ Auto-submit Functionality
✅ Course Extension Request System
✅ Excel Export Using Apache POI
✅ Responsive Bootstrap UI
✅ RESTful API Architecture
✅ Exception Handling & Validation
✅ Comprehensive Dashboards

## Default Credentials

### Admin
- Email: admin@lms.com
- Password: admin@123

### Faculty
- Email: faculty@lms.com
- Password: faculty@123

### Student
- Email: student@lms.com
- Password: student@123

## Testing

Run tests with:
```bash
mvn test
```

## Build & Deployment

Create a JAR file:
```bash
mvn clean package -DskipTests
```

Run the JAR:
```bash
java -jar target/online-exam-system-1.0.0.jar
```

## Security Considerations

- All endpoints are secured with JWT tokens
- Passwords are encrypted using BCrypt
- Role-based authorization on all endpoints
- CSRF protection enabled
- Input validation on all forms
- SQL injection prevention through JPA

## Troubleshooting

### Database Connection Issues
- Verify MySQL is running
- Check database credentials in application.properties
- Ensure database user has proper permissions

### Port Already in Use
- Change port in application.properties: `server.port=8081`

### Maven Build Issues
- Clear Maven cache: `mvn clean`
- Update Maven: `mvn -U clean install`

## Contributing

This is a learning project. For contributions, please follow:
- Java naming conventions
- Spring Boot best practices
- RESTful API design principles

## License

This project is for educational purposes.

## Contact & Support

For issues and questions, please create a GitHub issue.

## Author

Developed as a comprehensive Spring Boot Learning System
>>>>>>> 8785548 (Initial commit)
