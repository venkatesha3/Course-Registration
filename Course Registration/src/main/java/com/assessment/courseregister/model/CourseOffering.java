package com.assessment.courseregister.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseOffering {

    private String courseId;
    private String courseName;
    private int capacity;
@Builder.Default
    private Set<String> prerequisiteCourseIds = new HashSet<>();
    @Builder.Default
    private Set<String> enrolledStudentIds = new HashSet<>();

    @Builder.Default
    private Queue<String> waitlistedStudentIds = new LinkedList<>();
}
