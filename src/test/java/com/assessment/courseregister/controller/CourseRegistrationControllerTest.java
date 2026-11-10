package com.assessment.courseregister.controller;


import com.assessment.courseregister.dto.CourseEnrollmentView;
import com.assessment.courseregister.dto.EnrollmentResponse;
import com.assessment.courseregister.dto.StudentEnrollmentView;
import com.assessment.courseregister.model.EnrollmentStatus;
import com.assessment.courseregister.service.CourseRegistrationService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@WebMvcTest(CourseRegistrationController.class)
class CourseRegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseRegistrationService registrationService;


    @Test
    void shouldReturnCourseEnrollmentDetails() throws Exception {

        List<StudentEnrollmentView> students = List.of(
                StudentEnrollmentView.builder()
                        .studentId("S301")
                        .name("John")
                        .status("ENROLLED")
                        .build(),

                StudentEnrollmentView.builder()
                        .studentId("S302")
                        .name("David")
                        .status("WAITLISTED")
                        .waitlistPosition(1)
                        .build()
        );

        CourseEnrollmentView view = CourseEnrollmentView.builder()
                .courseId("JAVA201")
                .courseName("Advanced Java")
                .capacity(2)
                .enrolledCount(1)
                .waitlistedCount(1)
                .students(students)
                .build();

        when(registrationService.getCourseEnrollments("JAVA201"))
                .thenReturn(java.util.Optional.of(view));

        mockMvc.perform(get("/api/courses/JAVA201/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value("JAVA201"))
                .andExpect(jsonPath("$.courseName").value("Advanced Java"))
                .andExpect(jsonPath("$.capacity").value(2))
                .andExpect(jsonPath("$.enrolledCount").value(1))
                .andExpect(jsonPath("$.waitlistedCount").value(1))
                .andExpect(jsonPath("$.students[0].status").value("ENROLLED"))
                .andExpect(jsonPath("$.students[1].status").value("WAITLISTED"))
                .andExpect(jsonPath("$.students[1].waitlistPosition").value(1));
    }

    @Test
    void shouldReturnNotFoundWithErrorResponseWhenCourseDoesNotExist()
            throws Exception {

        when(registrationService.getCourseEnrollments("JAVA202"))
                .thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/api/courses/JAVA202/students"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("Course not found: JAVA202"));
    }
    @Test
    void shouldEnrollStudentSuccessfully() throws Exception {

        EnrollmentResponse response = EnrollmentResponse.builder()
                .courseId("JAVA201")
                .studentId("S301")
                .status(EnrollmentStatus.ENROLLED)
                .message("Enrollment successful.")
                .build();

        when(registrationService.enrollStudent(
                org.mockito.ArgumentMatchers.eq("JAVA201"),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(response);

        mockMvc.perform(post("/api/courses/JAVA201/enrollments")
                        .contentType("application/json")
                        .content("""
                                {
                                  "studentId": "S301",
                                  "name": "John",
                                  "completedCourseIds": ["JAVA101"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value("JAVA201"))
                .andExpect(jsonPath("$.studentId").value("S301"))
                .andExpect(jsonPath("$.status").value("ENROLLED"))
                .andExpect(jsonPath("$.message").value("Enrollment successful."));
    }


    @Test
    void shouldReturnRejectedResponseWhenEnrollmentFails() throws Exception {

        EnrollmentResponse response = EnrollmentResponse.builder()
                .courseId("JAVA201")
                .studentId("S302")
                .status(EnrollmentStatus.REJECTED)
                .message("Required prerequisite has not been completed.")
                .build();

        when(registrationService.enrollStudent(
                org.mockito.ArgumentMatchers.eq("JAVA201"),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(response);

        mockMvc.perform(post("/api/courses/JAVA201/enrollments")
                        .contentType("application/json")
                        .content("""
                            {
                              "studentId": "S302",
                              "name": "David",
                              "completedCourseIds": []
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.message")
                        .value("Required prerequisite has not been completed."));
    }

    @Test
    void shouldDropEnrolledStudentSuccessfully() throws Exception {

        EnrollmentResponse response = EnrollmentResponse.builder()
                .courseId("JAVA201")
                .studentId("S301")
                .status(EnrollmentStatus.DROPPED)
                .message("Enrollment dropped successfully.")
                .build();

        when(registrationService.dropEnrollment("JAVA201", "S301"))
                .thenReturn(response);

        mockMvc.perform(delete("/api/courses/JAVA201/enrollments/S301"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DROPPED"))
                .andExpect(jsonPath("$.studentId").value("S301"));
    }
    @Test
    void shouldRemoveWaitlistedStudentSuccessfully() throws Exception {

        EnrollmentResponse response = EnrollmentResponse.builder()
                .courseId("JAVA201")
                .studentId("S302")
                .status(EnrollmentStatus.DROPPED)
                .message("Student successfully removed from waitlist.")
                .build();

        when(registrationService.dropEnrollment("JAVA201", "S302"))
                .thenReturn(response);

        mockMvc.perform(delete("/api/courses/JAVA201/enrollments/S302"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DROPPED"))
                .andExpect(jsonPath("$.message")
                        .value("Student successfully removed from waitlist."));
    }

    @Test
    void shouldRejectDropForNonRegisteredStudent() throws Exception {

        EnrollmentResponse response = EnrollmentResponse.builder()
                .courseId("JAVA201")
                .studentId("S303")
                .status(EnrollmentStatus.REJECTED)
                .message("No active enrollment or waitlist entry found.")
                .build();

        when(registrationService.dropEnrollment("JAVA201", "S303"))
                .thenReturn(response);

        mockMvc.perform(delete("/api/courses/JAVA201/enrollments/S303"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void shouldReturnBadRequestForMalformedJson() throws Exception {

        mockMvc.perform(post("/api/courses/JAVA201/enrollments")
                        .contentType("application/json")
                        .content("""
                            {
                              "studentId": "S301",
                              "name": "John",
                              "completedCourseIds": [
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message")
                        .value("Invalid or malformed request body."));
    }


    @Test
    void shouldRejectEnrollmentWhenStudentIdIsBlank() throws Exception {

        mockMvc.perform(post("/api/courses/JAVA201/enrollments")
                        .contentType("application/json")
                        .content("""
                            {
                              "studentId": "",
                              "name": "John",
                              "completedCourseIds": ["JAVA101"]
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Student ID is required."));
    }

    @Test
    void shouldRejectEnrollmentWhenStudentNameIsBlank() throws Exception {

        mockMvc.perform(post("/api/courses/JAVA201/enrollments")
                        .contentType("application/json")
                        .content("""
                            {
                              "studentId": "S304",
                              "name": "",
                              "completedCourseIds": ["JAVA101"]
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Student name is required."));
    }
}
