package com.assessment.courseregister.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentEnrollmentView {

    private String studentId;
    private String name;
    private String status;
    private Integer waitlistPosition;
}
