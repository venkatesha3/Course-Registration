
package com.assessment.courseregister.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Set;

@Data
public class EnrollmentRequest {

    @NotBlank(message = "Student ID is required.")
    private String studentId;

    @NotBlank(message = "Student name is required.")
    private String name;

    private Set<String> completedCourseIds;
}
