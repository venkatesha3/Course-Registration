package com.assessment.courseregister.controller;

import com.assessment.courseregister.dto.ApiErrorResponse;
import com.assessment.courseregister.dto.CourseEnrollmentView;
import com.assessment.courseregister.dto.EnrollmentRequest;
import com.assessment.courseregister.dto.EnrollmentResponse;
import com.assessment.courseregister.model.EnrollmentStatus;
import com.assessment.courseregister.model.Student;
import com.assessment.courseregister.service.CourseRegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashSet;

@RestController
@RequestMapping("/api/courses")
public class CourseRegistrationController {

    private final CourseRegistrationService registrationService;

    public CourseRegistrationController(
            CourseRegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping("/{courseId}/students")
    public ResponseEntity<?> getCourseStudents(
            @PathVariable String courseId) {

        return registrationService.getCourseEnrollments(courseId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> {
                    ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                            .timestamp(LocalDateTime.now())
                            .status(HttpStatus.NOT_FOUND.value())
                            .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                            .message("Course not found: " + courseId)
                            .build();

                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(errorResponse);
                });
    }
    // Enroll a student or add them to the waitlist
    @PostMapping("/{courseId}/enrollments")
    public ResponseEntity<EnrollmentResponse> enrollStudent(
            @PathVariable String courseId,
            @Valid @RequestBody EnrollmentRequest request) {

        Student student = Student.builder()
                .studentId(request.getStudentId())
                .name(request.getName())
                .completedCourseIds(
                        request.getCompletedCourseIds() == null
                                ? new HashSet<>()
                                : request.getCompletedCourseIds())
                .build();

        EnrollmentResponse response =
                registrationService.enrollStudent(courseId, student);
        if (response.getStatus() == EnrollmentStatus.REJECTED) {
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    // Drop an existing enrollment
    @DeleteMapping("/{courseId}/enrollments/{studentId}")
    public ResponseEntity<EnrollmentResponse> dropEnrollment(
            @PathVariable String courseId,
            @PathVariable String studentId) {

        EnrollmentResponse response =
                registrationService.dropEnrollment(courseId, studentId);

        if (response.getStatus() == EnrollmentStatus.REJECTED) {
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }
}