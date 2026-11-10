# Course Registration System

A Spring Boot REST API for managing student enrollment in a course,
including prerequisite validation, course capacity, a FIFO waitlist,
enrollment cancellation, and automatic promotion from the waitlist.

## Features

-   Enroll students in a course.
-   Validate that students have completed the required prerequisite
    courses.
-   Enforce the course capacity.
-   Add eligible students to a first-in, first-out (FIFO) waitlist when
    the course is full.
-   Prevent duplicate enrollment and duplicate waitlist entries.
-   Drop an active enrollment or cancel a waitlist entry.
-   Automatically promote the earliest eligible waitlisted student when
    an enrolled student drops.
-   Skip waitlisted students who are no longer eligible and continue
    checking the queue.
-   Return course enrollment details, including each student's status
    and waitlist position.
-   Handle invalid request bodies and validation errors with structured
    error responses.
-   Protect course state changes and reads with synchronization for
    concurrent requests within one application instance.

## Technology Stack

-   Java 17
-   Spring Boot
-   Spring Web
-   Jakarta Bean Validation
-   Maven
-   Lombok
-   JUnit and Spring Boot Test

## Prerequisites

Install the following before running the application:

-   JDK 17
-   Maven (or use the Maven Wrapper if it is included in the project)
-   An IDE such as IntelliJ IDEA (optional)
-   Postman or another HTTP client (optional, for API testing)

Check your Java and Maven installations:

``` bash
java -version
mvn -version
```

## Getting Started

1.  Clone or download the project.
2.  Open the project in IntelliJ IDEA, or navigate to the project root
    in a terminal.
3.  Ensure the project is configured to use JDK 17.
4.  Build and run the tests:

``` bash
mvn clean test
```

5.  Start the application:

``` bash
mvn spring-boot:run
```

The API is available at:

``` text
http://localhost:8080
```

You can also run the main Spring Boot application class directly from
IntelliJ IDEA.

## Sample Course

The in-memory repository initializes one sample course:

Property                Value
  ----------------------- -----------------
Course ID               `JAVA201`
Course name             `Advanced Java`
Capacity                `2`
Required prerequisite   `JAVA101`

A student must include `JAVA101` in `completedCourseIds` to be eligible
for this sample course.

## API Endpoints

### 1. Enroll a student

**POST** `/api/courses/{courseId}/enrollments`

Example request:

``` http
POST http://localhost:8080/api/courses/JAVA201/enrollments
Content-Type: application/json
```

``` json
{
  "studentId": "S101",
  "name": "Ravi",
  "completedCourseIds": ["JAVA101"]
}
```

Possible outcomes:

-   `ENROLLED` --- the student meets the prerequisites and a seat is
    available.
-   `WAITLISTED` --- the student meets the prerequisites, but the course
    is full.
-   `REJECTED` --- the request is invalid, the course does not exist,
    the student is already enrolled, or the student does not meet the
    prerequisites.

The controller returns HTTP 200 for successful enrollment or
waitlisting, and HTTP 400 for a business rejection.

Example successful response:

``` json
{
  "courseId": "JAVA201",
  "studentId": "S101",
  "status": "ENROLLED",
  "message": "Student successfully enrolled."
}
```

The exact message text is determined by the service implementation.

### 2. Drop an enrollment or cancel a waitlist entry

**DELETE** `/api/courses/{courseId}/enrollments/{studentId}`

Example:

``` http
DELETE http://localhost:8080/api/courses/JAVA201/enrollments/S101
```

Possible outcomes:

-   `DROPPED` --- the enrolled student is removed, or the waitlisted
    student cancels their waitlist entry.
-   `REJECTED` --- the course or student entry does not exist, or the
    request is invalid.

When an enrolled student drops, the system checks the waitlist in FIFO
order and promotes the earliest eligible student if a seat becomes
available.

### 3. Get course enrollment details

**GET** `/api/courses/{courseId}/students`

Example:

``` http
GET http://localhost:8080/api/courses/JAVA201/students
```

Example response:

``` json
{
  "courseId": "JAVA201",
  "courseName": "Advanced Java",
  "capacity": 2,
  "enrolledCount": 2,
  "waitlistedCount": 1,
  "students": [
    {
      "studentId": "S101",
      "name": "Ravi",
      "status": "ENROLLED",
      "waitlistPosition": null
    },
    {
      "studentId": "S102",
      "name": "Kiran",
      "status": "ENROLLED",
      "waitlistPosition": null
    },
    {
      "studentId": "S103",
      "name": "Arun",
      "status": "WAITLISTED",
      "waitlistPosition": 1
    }
  ]
}
```

The waitlist position starts at `1`. Enrolled students have a `null`
waitlist position. The GET endpoint returns HTTP 404 when the course
does not exist.

## Business Rules

1.  A student must provide a non-blank student ID and name.
2.  A student must complete all prerequisite courses before enrolling or
    joining the waitlist.
3.  A student can have only one active enrollment or waitlist entry for
    this course.
4.  The number of enrolled students must not exceed the course capacity.
5.  When the course is full, eligible students are added to the end of
    the FIFO waitlist.
6.  When an enrolled student drops, the system checks waitlisted
    students in queue order.
7.  The first eligible waitlisted student is promoted into the available
    seat.
8.  A waitlisted student who is no longer eligible is skipped and
    removed from the queue; the system continues checking the remaining
    students.
9.  A student can cancel their waitlist entry without affecting enrolled
    students.
10. Invalid requests and unsuccessful business operations return clear
    responses.

## Error Handling and Validation

The enrollment request validates required fields using Jakarta Bean
Validation.

The global exception handler returns a structured error response for
malformed JSON, request validation failures, and unexpected exceptions.

Example validation error:

``` json
{
  "timestamp": "2026-11-06T20:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Student ID is required."
}
```

The timestamp is generated at runtime, so the value in an actual
response will differ.

## Running Tests

Run the complete test suite:

``` bash
mvn clean test
```

The tests cover the service and REST controller behavior, including:

-   Successful enrollment and prerequisite rejection
-   Capacity and waitlist behavior
-   Duplicate enrollment prevention
-   Dropping an enrollment and cancelling a waitlist entry
-   FIFO promotion and skipping ineligible waitlisted students
-   Course enrollment details
-   Request validation and malformed JSON handling
-   Concurrent enrollment and duplicate concurrent requests

A successful Maven run should finish with:

``` text
BUILD SUCCESS
```

## Concurrency

Course operations synchronize on the relevant `CourseOffering` instance.
This protects the in-memory course collections from concurrent access
within a single running application instance.

This is an in-process solution. It does not coordinate access across
multiple application instances.

## Design and Limitations

-   The repository stores course and student data in memory.
-   Data is lost when the application stops or restarts.
-   The sample setup contains one course, `JAVA201`.
-   No database persistence, authentication, authorization, or frontend
    is included.
-   Synchronization protects shared in-memory state only within one
    application instance.

These limitations are intentional for this focused assessment
implementation.

## Project Structure

The application uses the following package responsibilities:

  -----------------------------------------------------------------------
Package                             Responsibility
  ----------------------------------- -----------------------------------
`controller`                        Exposes the course registration
REST endpoints

`dto`                               Request and response objects used
by the API

`exception`                         Structured API error response and
global exception handling

`model`                             Course, student, and enrollment
status models

`repository`                        In-memory storage for courses and
students

`service`                           Enrollment, prerequisite, waitlist,
drop, and promotion business logic
  -----------------------------------------------------------------------

## Example End-to-End Flow

With the sample course capacity set to `2`:

1.  Enroll `S101` with prerequisite `JAVA101` --- status `ENROLLED`.
2.  Enroll `S102` with prerequisite `JAVA101` --- status `ENROLLED`.
3.  Enroll `S103` with prerequisite `JAVA101` --- status `WAITLISTED`,
    position `1`.
4.  Drop `S101` --- status `DROPPED`.
5.  The system promotes `S103` --- its final status becomes `ENROLLED`.
6.  Call the GET endpoint to verify the final course state.

## Author
Venkatesh Anantapalli
