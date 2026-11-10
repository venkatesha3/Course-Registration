package com.assessment.courseregister.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    private String studentId;
    private String name;
    @Builder.Default
    private Set<String> completedCourseIds = new HashSet<>();
}
