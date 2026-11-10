package com.assessment.courseregister.service;
import com.assessment.courseregister.dto.CourseEnrollmentView;
import com.assessment.courseregister.dto.EnrollmentResponse;
import com.assessment.courseregister.model.CourseOffering;
import com.assessment.courseregister.model.EnrollmentStatus;
import com.assessment.courseregister.model.Student;
import com.assessment.courseregister.repository.InMemoryCourseRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class CourseRegistrationServiceTest {

    private InMemoryCourseRepository repository;
    private CourseRegistrationService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCourseRepository();
        service = new CourseRegistrationService(repository);
    }

    // Helper method to create eligible students
    private Student createEligibleStudent(String id) {
        return Student.builder()
                .studentId(id)
                .name("Student " + id)
                .completedCourseIds(Set.of("JAVA101"))
                .build();
    }

    // 1. Successful enrollment when seat is available
    @Test
    void shouldEnrollStudentWhenSeatIsAvailable() {

        Student student = createEligibleStudent("S201");

        EnrollmentResponse response =
                service.enrollStudent("JAVA201", student);

        assertEquals(EnrollmentStatus.ENROLLED, response.getStatus());

        CourseOffering course =
                repository.findCourseById("JAVA201").orElseThrow();

        assertTrue(course.getEnrolledStudentIds().contains("S201"));
        assertEquals(1, course.getEnrolledStudentIds().size());
        assertTrue(course.getWaitlistedStudentIds().isEmpty());
    }

    // 2. Reject enrollment when prerequisite is missing
    @Test
    void shouldRejectStudentWhenPrerequisiteIsMissing() {

        Student student = Student.builder()
                .studentId("S202")
                .name("Student S202")
                .completedCourseIds(Set.of("JAVA102"))
                .build();

        EnrollmentResponse response =
                service.enrollStudent("JAVA201", student);

        assertEquals(EnrollmentStatus.REJECTED, response.getStatus());

        CourseOffering course =
                repository.findCourseById("JAVA201").orElseThrow();

        assertFalse(course.getEnrolledStudentIds().contains("S202"));
        assertFalse(course.getWaitlistedStudentIds().contains("S202"));
    }

    // 3. Reject duplicate enrollment
    @Test
    void shouldRejectDuplicateEnrollment() {

        Student student = createEligibleStudent("S203");

        service.enrollStudent("JAVA201", student);

        EnrollmentResponse response =
                service.enrollStudent("JAVA201", student);

        assertEquals(EnrollmentStatus.REJECTED, response.getStatus());

        CourseOffering course =
                repository.findCourseById("JAVA201").orElseThrow();

        assertEquals(1, course.getEnrolledStudentIds().size());
    }

    // 4. Add student to waitlist when course is full
    @Test
    void shouldAddStudentToWaitlistWhenCourseIsFull() {

        service.enrollStudent("JAVA201", createEligibleStudent("S204"));
        service.enrollStudent("JAVA201", createEligibleStudent("S205"));

        EnrollmentResponse response =
                service.enrollStudent("JAVA201", createEligibleStudent("S206"));

        assertEquals(EnrollmentStatus.WAITLISTED, response.getStatus());

        CourseOffering course =
                repository.findCourseById("JAVA201").orElseThrow();

        assertEquals(2, course.getEnrolledStudentIds().size());
        assertEquals(1, course.getWaitlistedStudentIds().size());
        assertTrue(course.getWaitlistedStudentIds().contains("S206"));
    }

    // 5. Avoid duplicate waitlist entries
    @Test
    void shouldNotAddStudentToWaitlistTwice() {

        service.enrollStudent("JAVA201", createEligibleStudent("S207"));
        service.enrollStudent("JAVA201", createEligibleStudent("S208"));

        Student student = createEligibleStudent("S209");

        service.enrollStudent("JAVA201", student);

        EnrollmentResponse response =
                service.enrollStudent("JAVA201", student);

        assertEquals(EnrollmentStatus.WAITLISTED, response.getStatus());

        CourseOffering course =
                repository.findCourseById("JAVA201").orElseThrow();

        assertEquals(1, course.getWaitlistedStudentIds().size());
    }

    // 6. Reject enrollment for an unknown course
    @Test
    void shouldRejectEnrollmentWhenCourseDoesNotExist() {

        EnrollmentResponse response =
                service.enrollStudent(
                        "INVALID",
                        createEligibleStudent("S210")
                );

        assertEquals(EnrollmentStatus.REJECTED, response.getStatus());
    }

    // 7. Reject null student details
    @Test
    void shouldRejectNullStudent() {

        EnrollmentResponse response =
                service.enrollStudent("JAVA201", null);

        assertEquals(EnrollmentStatus.REJECTED, response.getStatus());
    }

    // 8. Reject blank student ID
    @Test
    void shouldRejectBlankStudentId() {

        Student student = createEligibleStudent(" ");

        EnrollmentResponse response =
                service.enrollStudent("JAVA201", student);

        assertEquals(EnrollmentStatus.REJECTED, response.getStatus());
    }

    // 9. Successfully drop an enrolled student
    @Test
    void shouldDropEnrolledStudent() {

        service.enrollStudent("JAVA201", createEligibleStudent("S211"));

        EnrollmentResponse response =
                service.dropEnrollment("JAVA201", "S211");

        assertEquals(EnrollmentStatus.DROPPED, response.getStatus());

        CourseOffering course =
                repository.findCourseById("JAVA201").orElseThrow();

        assertFalse(course.getEnrolledStudentIds().contains("S211"));
    }

    // 10. Reject drop when student is not enrolled
    @Test
    void shouldRejectDropWhenStudentIsNotEnrolled() {

        EnrollmentResponse response =
                service.dropEnrollment("JAVA201", "S212");

        assertEquals(EnrollmentStatus.REJECTED, response.getStatus());
    }

    // 11. Reject drop for an unknown course
    @Test
    void shouldRejectDropWhenCourseDoesNotExist() {

        EnrollmentResponse response =
                service.dropEnrollment("INVALID", "S213");

        assertEquals(EnrollmentStatus.REJECTED, response.getStatus());
    }

    // 12. Reject drop with blank student ID
    @Test
    void shouldRejectDropWithBlankStudentId() {

        EnrollmentResponse response =
                service.dropEnrollment("JAVA201", " ");

        assertEquals(EnrollmentStatus.REJECTED, response.getStatus());
    }

    // 13. Promote the first eligible student from waitlist (FIFO)
    @Test
    void shouldPromoteFirstStudentFromWaitlist() {

        service.enrollStudent("JAVA201", createEligibleStudent("S214"));
        service.enrollStudent("JAVA201", createEligibleStudent("S215"));

        service.enrollStudent("JAVA201", createEligibleStudent("S216"));
        service.enrollStudent("JAVA201", createEligibleStudent("S217"));

        service.dropEnrollment("JAVA201", "S214");

        CourseOffering course =
                repository.findCourseById("JAVA201").orElseThrow();

        assertTrue(course.getEnrolledStudentIds().contains("S216"));
        assertFalse(course.getWaitlistedStudentIds().contains("S216"));

        assertTrue(course.getWaitlistedStudentIds().contains("S217"));
    }

    // 14. Skip an ineligible student and promote the next eligible student
    @Test
    void shouldSkipIneligibleStudentAndPromoteNextEligibleStudent() {

        service.enrollStudent("JAVA201", createEligibleStudent("S218"));
        service.enrollStudent("JAVA201", createEligibleStudent("S219"));

        service.enrollStudent("JAVA201", createEligibleStudent("S220"));
        service.enrollStudent("JAVA201", createEligibleStudent("S221"));

        // Simulate prerequisite eligibility changing after waitlisting
        Student firstWaitlisted =
                repository.findStudentById("S220").orElseThrow();

        firstWaitlisted.setCompletedCourseIds(new HashSet<>());

        service.dropEnrollment("JAVA201", "S218");

        CourseOffering course =
                repository.findCourseById("JAVA201").orElseThrow();

        assertFalse(course.getEnrolledStudentIds().contains("S220"));
        assertTrue(course.getEnrolledStudentIds().contains("S221"));

        assertFalse(course.getWaitlistedStudentIds().contains("S220"));
        assertTrue(course.getWaitlistedStudentIds().isEmpty());
    }

    // 15. Drop when there are no waitlisted students
    @Test
    void shouldDropEnrollmentWhenWaitlistIsEmpty() {

        service.enrollStudent("JAVA201", createEligibleStudent("S222"));

        EnrollmentResponse response =
                service.dropEnrollment("JAVA201", "S222");

        assertEquals(EnrollmentStatus.DROPPED, response.getStatus());

        CourseOffering course =
                repository.findCourseById("JAVA201").orElseThrow();

        assertTrue(course.getEnrolledStudentIds().isEmpty());
        assertTrue(course.getWaitlistedStudentIds().isEmpty());
    }

    // 16. Get course enrollment details
    @Test
    void shouldReturnCourseEnrollmentDetails() {

        service.enrollStudent("JAVA201", createEligibleStudent("S223"));
        service.enrollStudent("JAVA201", createEligibleStudent("S224"));
        service.enrollStudent("JAVA201", createEligibleStudent("S225"));

        CourseEnrollmentView view =
                service.getCourseEnrollments("JAVA201").orElseThrow();

        assertEquals("JAVA201", view.getCourseId());
        assertEquals(2, view.getEnrolledCount());
        assertEquals(1, view.getWaitlistedCount());
        assertEquals(3, view.getStudents().size());
    }

    // 17. Return empty when course does not exist
    @Test
    void shouldReturnEmptyWhenCourseDoesNotExist() {

        assertTrue(service.getCourseEnrollments("INVALID").isEmpty());
    }
    @Test
    void shouldRemoveStudentFromWaitlist() {

        service.enrollStudent("JAVA201", createEligibleStudent("S301"));
        service.enrollStudent("JAVA201", createEligibleStudent("S302"));

        service.enrollStudent("JAVA201", createEligibleStudent("S303"));

        EnrollmentResponse response =
                service.dropEnrollment("JAVA201", "S303");

        assertEquals(EnrollmentStatus.DROPPED, response.getStatus());

        CourseOffering course =
                repository.findCourseById("JAVA201").orElseThrow();

        assertFalse(course.getWaitlistedStudentIds().contains("S303"));
        assertEquals(2, course.getEnrolledStudentIds().size());
        assertTrue(course.getWaitlistedStudentIds().isEmpty());
    }




    @Test
    void shouldNotExceedCourseCapacityDuringConcurrentEnrollment()
            throws Exception {

        // Number of students trying to enroll simultaneously
        int numberOfStudents = 10;

        // Create a thread pool to execute enrollment requests concurrently
        ExecutorService executor =
                Executors.newFixedThreadPool(numberOfStudents);

        // Wait until all threads are ready before starting enrollment
        CountDownLatch ready = new CountDownLatch(numberOfStudents);

        // Common starting point to release all threads simultaneously
        CountDownLatch start = new CountDownLatch(1);

        try {
            // Stores the results of all submitted enrollment tasks
            List<Future<EnrollmentResponse>> futures = new ArrayList<>();

            // Submit enrollment requests for all students
            for (int i = 1; i <= numberOfStudents; i++) {

                String studentId = "CONCURRENT" + i;

                Future<EnrollmentResponse> future = executor.submit(() -> {

                    // Notify that this thread is ready
                    ready.countDown();

                    // Wait until all threads are ready to start together
                    start.await();

                    // Attempt to enroll the student in the course
                    return service.enrollStudent(
                            "JAVA201",
                            createEligibleStudent(studentId)
                    );
                });

                futures.add(future);
            }

            // Wait until all 10 threads are ready
            assertTrue(ready.await(5, TimeUnit.SECONDS));

            // Release all threads to execute enrollment concurrently
            start.countDown();

            // Wait for all enrollment requests to complete
            for (Future<EnrollmentResponse> future : futures) {
                future.get(5, TimeUnit.SECONDS);
            }

            // Retrieve the final course state after all requests complete
            CourseOffering course =
                    repository.findCourseById("JAVA201").orElseThrow();

            // Verify that enrolled students never exceed course capacity
            assertEquals(2, course.getEnrolledStudentIds().size());

            // Remaining students should be added to the waitlist
            assertEquals(8, course.getWaitlistedStudentIds().size());

            // Verify that all 10 students are accounted for
            assertEquals(10,
                    course.getEnrolledStudentIds().size()
                            + course.getWaitlistedStudentIds().size());

            /*
             * Expected Result:
             *
             * Total enrollment requests : 10
             * Course capacity           : 2
             * Enrolled students         : 2
             * Waitlisted students       : 8
             *
             * All 10 students should be accounted for.
             * Course capacity must never be exceeded.
             */

        } finally {
            // Shut down the thread pool and release resources
            executor.shutdownNow();
        }
    }

}






