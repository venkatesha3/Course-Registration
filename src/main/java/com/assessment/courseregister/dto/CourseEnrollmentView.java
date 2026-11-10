package com.assessment.courseregister.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseEnrollmentView {

    private String courseId;
    private String courseName;
    private int capacity;
    private int enrolledCount;
    private int waitlistedCount;
    private List<StudentEnrollmentView> students;
}