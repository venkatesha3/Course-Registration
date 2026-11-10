package com.assessment.courseregister.service;

import com.assessment.courseregister.dto.CourseEnrollmentView;
import com.assessment.courseregister.dto.EnrollmentResponse;
import com.assessment.courseregister.dto.StudentEnrollmentView;
import com.assessment.courseregister.model.CourseOffering;
import com.assessment.courseregister.model.EnrollmentStatus;
import com.assessment.courseregister.model.Student;
import com.assessment.courseregister.repository.InMemoryCourseRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CourseRegistrationService {

    private final InMemoryCourseRepository repository;

    public CourseRegistrationService(InMemoryCourseRepository repository) {
        this.repository = repository;
    }


    public EnrollmentResponse enrollStudent(String courseId, Student student) {

        // Validate student details
        if (student == null ||
                student.getStudentId() == null ||
                student.getStudentId().isBlank()) {

            return buildResponse(courseId, null,
                    EnrollmentStatus.REJECTED,
                    "Valid student details are required.");
        }

        String studentId = student.getStudentId();

        CourseOffering course = repository.findCourseById(courseId)
                .orElse(null);

        if (course == null) {
            return buildResponse(courseId, studentId,
                    EnrollmentStatus.REJECTED,
                    "Course not found.");
        }

        synchronized (course) {

            // Check duplicate enrollment
            if (course.getEnrolledStudentIds().contains(studentId)) {
                return buildResponse(courseId, studentId,
                        EnrollmentStatus.REJECTED,
                        "You are already enrolled in this course.");
            }

            // Check duplicate waitlist request
            if (course.getWaitlistedStudentIds().contains(studentId)) {
                return buildResponse(courseId, studentId,
                        EnrollmentStatus.WAITLISTED,
                        "You are already on the waitlist.");
            }

            // Check prerequisites
            if (!hasCompletedPrerequisites(student, course)) {
                return buildResponse(courseId, studentId,
                        EnrollmentStatus.REJECTED,
                        "Enrollment rejected: Required prerequisite(s) "
                                + "have not been completed: "
                                + course.getPrerequisiteCourseIds());
            }

            // Store student details for future waitlist promotion
            repository.saveStudent(student);

            // Check capacity
            if (course.getEnrolledStudentIds().size() < course.getCapacity()) {

                course.getEnrolledStudentIds().add(studentId);

                return buildResponse(courseId, studentId,
                        EnrollmentStatus.ENROLLED,
                        "Enrollment successful.");
            }

            // Course is full - add to FIFO waitlist
            course.getWaitlistedStudentIds().add(studentId);

            return buildResponse(courseId, studentId,
                    EnrollmentStatus.WAITLISTED,
                    "Course is full. You have been added to the waitlist.");
        }
    }


    public EnrollmentResponse dropEnrollment(String courseId, String studentId) {

        CourseOffering course =
                repository.findCourseById(courseId).orElse(null);

        if (course == null) {
            return buildResponse(
                    courseId, studentId,
                    EnrollmentStatus.REJECTED,
                    "Course not found."
            );
        }

        synchronized (course) {

            if (studentId == null || studentId.isBlank()) {
                return buildResponse(
                        courseId, studentId,
                        EnrollmentStatus.REJECTED,
                        "Invalid student ID."
                );
            }

            // Case 1: Student is on the waitlist
            if (course.getWaitlistedStudentIds().remove(studentId)) {

                return buildResponse(
                        courseId, studentId,
                        EnrollmentStatus.DROPPED,
                        "Student successfully removed from waitlist."
                );
            }

            // Case 2: Student is enrolled
            if (!course.getEnrolledStudentIds().contains(studentId)) {

                return buildResponse(
                        courseId, studentId,
                        EnrollmentStatus.REJECTED,
                        "No active enrollment or waitlist entry found."
                );
            }

            course.getEnrolledStudentIds().remove(studentId);

            String promotedStudentId = promoteFromWaitlist(course);

            String message = promotedStudentId == null
                    ? "Enrollment dropped successfully. No eligible students in waitlist."
                    : "Enrollment dropped successfully. Student "
                    + promotedStudentId + " has been promoted.";

            return buildResponse(
                    courseId, studentId,
                    EnrollmentStatus.DROPPED,
                    message
            );
        }
    }

    private boolean hasCompletedPrerequisites(
            Student student, CourseOffering course) {

        return student.getCompletedCourseIds()
                .containsAll(course.getPrerequisiteCourseIds());
    }

    private String promoteFromWaitlist(CourseOffering course) {

        int waitlistSize = course.getWaitlistedStudentIds().size();

        for (int i = 0; i < waitlistSize; i++) {

            // Get the first student ID from the queue
            String studentId = course.getWaitlistedStudentIds().poll();

            // Fetch student details using the ID
            Student student = repository.findStudentById(studentId)
                    .orElse(null);

            // Skip invalid or ineligible students
            if (student == null || !hasCompletedPrerequisites(student, course)) {
                continue;
            }

            // Promote the earliest eligible student
            course.getEnrolledStudentIds().add(studentId);

            return studentId;
        }

        return null;
    }

    private EnrollmentResponse buildResponse(
            String courseId,
            String studentId,
            EnrollmentStatus status,
            String message) {

        return EnrollmentResponse.builder()
                .courseId(courseId)
                .studentId(studentId)
                .status(status)
                .message(message)
                .build();
    }


    public Optional<CourseEnrollmentView> getCourseEnrollments(String courseId) {

        CourseOffering course =
                repository.findCourseById(courseId).orElse(null);

        if (course == null) {
            return Optional.empty();
        }

        synchronized (course) {

            List<StudentEnrollmentView> students = new ArrayList<>();

            for (String studentId : course.getEnrolledStudentIds()) {

                Student student =
                        repository.findStudentById(studentId).orElse(null);

                students.add(StudentEnrollmentView.builder()
                        .studentId(studentId)
                        .name(student != null ? student.getName() : null)
                        .status("ENROLLED")
                        .build());
            }

            int position = 1;

            for (String studentId : course.getWaitlistedStudentIds()) {

                Student student =
                        repository.findStudentById(studentId).orElse(null);

                students.add(StudentEnrollmentView.builder()
                        .studentId(studentId)
                        .name(student != null ? student.getName() : null)
                        .status("WAITLISTED")
                        .waitlistPosition(position++)
                        .build());
            }

            return Optional.of(CourseEnrollmentView.builder()
                    .courseId(course.getCourseId())
                    .courseName(course.getCourseName())
                    .capacity(course.getCapacity())
                    .enrolledCount(course.getEnrolledStudentIds().size())
                    .waitlistedCount(course.getWaitlistedStudentIds().size())
                    .students(students)
                    .build());
        }
    }
}
