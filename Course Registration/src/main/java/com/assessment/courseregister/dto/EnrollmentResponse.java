package com.assessment.courseregister.dto;


import com.assessment.courseregister.model.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentResponse {

    private String courseId;
    private String studentId;
    private EnrollmentStatus status;
    private String message;
}