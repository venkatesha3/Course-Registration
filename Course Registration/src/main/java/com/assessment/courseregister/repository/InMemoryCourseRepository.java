package com.assessment.courseregister.repository;

import com.assessment.courseregister.model.CourseOffering;
import com.assessment.courseregister.model.Student;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class InMemoryCourseRepository {

    private final Map<String, Student> students = new HashMap<>();

    private final Map<String, CourseOffering> courses = new HashMap<>();

    public InMemoryCourseRepository() {

        // Create sample course
        CourseOffering course = CourseOffering.builder()
                .courseId("JAVA201")
                .courseName("Advanced Java")
                .capacity(2)
                .build();

        course.getPrerequisiteCourseIds().add("JAVA101");

        courses.put(course.getCourseId(), course);
    }

    public void saveStudent(Student student) {
        students.put(student.getStudentId(), student);
    }

    public Optional<Student> findStudentById(String studentId) {
        return Optional.ofNullable(students.get(studentId));
    }

    public Optional<CourseOffering> findCourseById(String courseId) {
        return Optional.ofNullable(courses.get(courseId));
    }
}