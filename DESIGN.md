# Course Registration System --- Design Specification

## 1. Scope

This implementation supports registration for one sample course
offering.

### Supported operations

-   Enroll an eligible student when a seat is available.
-   Add an eligible student to the waitlist when the course is full.
-   Reject enrollment when prerequisites are missing.
-   Prevent duplicate enrollment and duplicate waitlist entries.
-   Drop an enrolled student.
-   Cancel a student's waitlist entry.
-   Automatically promote the earliest eligible waitlisted student when
    an enrolled student drops.
-   Skip and remove waitlisted students who are no longer eligible, then
    continue checking the queue.
-   Retrieve the current enrolled and waitlisted students with their
    statuses and waitlist positions.

### Out of scope

-   Multiple course offerings or course creation APIs.
-   Persistent database storage.
-   Authentication and authorization.
-   Payments.
-   Frontend application.
-   Multi-term or historical registration management.
-   Distributed coordination across multiple application instances.

## 2. Architecture

The application follows a layered Spring Boot architecture:

``` text
HTTP Client (for example, Postman)
             |
             v
CourseRegistrationController
             |
             v
CourseRegistrationService
             |
             v
InMemoryCourseRepository
             |
             v
In-memory course and student Maps
```

`GlobalExceptionHandler` handles REST-level request and unexpected
exception responses. Jakarta Bean Validation validates required request
fields before the controller processes the request.

### Package responsibilities

  -----------------------------------------------------------------------
  Package                             Responsibility
  ----------------------------------- -----------------------------------
  `controller`                        Exposes REST endpoints and maps
                                      service results to HTTP responses

  `service`                           Implements enrollment, prerequisite
                                      checks, drop, waitlist and
                                      promotion rules

  `repository`                        Stores and retrieves course and
                                      student objects in memory

  `model`                             Defines `CourseOffering`, `Student`
                                      and `EnrollmentStatus`

  `dto`                               Defines request,
                                      success/business-result and error
                                      response objects

  `exception`                         Defines the global REST exception
                                      handler and API error response
  -----------------------------------------------------------------------

## 3. Course State and Data Structures

The sample `CourseOffering` contains:

-   `courseId`
-   `courseName`
-   `capacity`
-   `prerequisiteCourseIds`
-   `enrolledStudentIds`
-   `waitlistedStudentIds`

The implementation uses the following data structures:

  -------------------------------------------------------------------------
  Field                     Data structure          Reason
  ------------------------- ----------------------- -----------------------
  `prerequisiteCourseIds`   `Set<String>`           Stores required course
                                                    IDs without duplicates

  `enrolledStudentIds`      `Set<String>`           Prevents duplicate
                                                    student IDs in the
                                                    enrolled collection

  `waitlistedStudentIds`    `Queue<String>`         Preserves FIFO order
                            (initialized with       and supports polling
                            `LinkedList`)           the next student
  -------------------------------------------------------------------------

The in-memory repository maintains maps for courses and students and
provides lookup and student-save operations.

### Sample course

The repository initializes this sample course:

  Property       Value
  -------------- -----------------
  Course ID      `JAVA201`
  Course name    `Advanced Java`
  Capacity       `2`
  Prerequisite   `JAVA101`

A student is eligible when their `completedCourseIds` contains every ID
in the course's `prerequisiteCourseIds`.

## 4. Enrollment Rules

1.  The student ID and name must not be blank.
2.  The requested course must exist.
3.  The student must have completed all required prerequisites.
4.  An enrolled student cannot be enrolled again.
5.  A student must not be added to the waitlist more than once.
6.  If a seat is available, an eligible student is enrolled.
7.  If the course is full, an eligible student is added to the end of
    the FIFO waitlist.
8.  An ineligible student is rejected and is not enrolled or waitlisted.

### Enrollment flow

``` text
Enrollment request
       |
       v
Validate student and course
       |
       v
Already enrolled or waitlisted?
       | Yes
       +------> Reject duplicate / avoid duplicate waitlist entry
       |
       | No
       v
Check prerequisites
       |
       +------> Missing prerequisite: REJECTED
       |
       v
Is a seat available?
       | Yes                 | No
       v                     v
   ENROLLED               WAITLISTED
```

The service returns an `EnrollmentResponse` with a status and message.
Normal business outcomes are represented by statuses such as `ENROLLED`,
`WAITLISTED`, and `REJECTED`.

## 5. Prerequisite Assumption

Prerequisite eligibility is determined from the student's
`completedCourseIds` and the course's `prerequisiteCourseIds`.

The service checks eligibility:

-   When the student initially requests enrollment.
-   Again when the student is considered for promotion from the
    waitlist.

The second check matters because a student's eligibility may have
changed while they were waiting.

## 6. Waitlist Promotion Policy

The waitlist follows FIFO ordering: students are considered in the order
they joined the queue.

When an enrolled student drops:

1.  The service removes that student from the enrolled set.
2.  It checks the waitlist from the front of the queue.
3.  It removes the next candidate from the queue.
4.  It looks up the candidate and checks their prerequisites again.
5.  If the candidate is missing or no longer eligible, that candidate is
    skipped and removed from the queue.
6.  The service continues until it finds an eligible candidate or the
    queue is exhausted.
7.  The first eligible candidate is added to the enrolled set and
    promotion stops.

### Example

Assume the waitlist is:

``` text
Front -> S103 (eligible) -> S104 (ineligible) -> S105 (eligible) -> Rear
```

When a seat becomes available:

-   `S103` is checked first and promoted.
-   Promotion stops because the first candidate is eligible.
-   `S104` and `S105` remain in their original queue order.

If instead the queue is:

``` text
Front -> S104 (ineligible) -> S105 (eligible) -> S106 (eligible) -> Rear
```

Then:

-   `S104` is polled and skipped because they are no longer eligible.
-   `S105` is the first eligible candidate and is promoted.
-   `S106` remains waiting.

The implementation removes skipped ineligible candidates from the
waitlist; it does not leave them in the queue.

### Waitlist cancellation

A student can cancel their waitlist entry using the DELETE endpoint.
Removing a waitlisted student does not free an enrolled seat and does
not trigger promotion.

## 7. Invariants

The service is designed to maintain these conditions:

1.  `enrolledStudentIds.size() <= capacity`.
2.  A student ID must not occur more than once in the enrolled set.
3.  A student ID must not be added to the waitlist more than once.
4.  A student who does not satisfy prerequisites must not be newly
    enrolled.
5.  A waitlisted student must pass the prerequisite check before
    promotion.
6.  A dropped or cancelled student must not remain in the corresponding
    active collection.
7.  An invalid operation must not partially update course registration
    state.
8.  The waitlist order must remain FIFO for students who are still
    waiting.

## 8. Concurrency

Enrollment and drop operations read and modify shared course state. For
example, two requests arriving at the same time must not both claim the
final available seat.

The service uses the course object as a monitor:

``` java
synchronized (course) {
    // Check and update the course's enrollment and waitlist state
}
```

The same course lock is used around the GET method's reads of the
enrolled and waitlisted collections. This avoids iterating those
collections while a service operation is modifying them.

### Why `synchronized`?

-   It makes the critical section mutually exclusive for requests using
    the same course object.
-   It keeps the capacity check and the corresponding enrollment or
    waitlist update together.
-   It is simple and appropriate for this single-instance, in-memory
    assessment implementation.

The concurrency tests submit multiple requests together and verify that
capacity is not exceeded and that simultaneous requests for the same
student do not create duplicate enrollment.

### Concurrency limitations

Java synchronization coordinates threads only inside one JVM. It does
not coordinate multiple application instances. If the application later
uses a database or is deployed across multiple instances, use database
transactions, suitable locking or atomic updates, and uniqueness
constraints as appropriate.

## 9. Error Handling and Validation

`EnrollmentRequest` uses Jakarta Bean Validation annotations such as
`@NotBlank` for required fields. The controller uses `@Valid` to trigger
validation.

`GlobalExceptionHandler`, annotated with `@RestControllerAdvice`,
handles:

-   Malformed JSON (`HttpMessageNotReadableException`).
-   Request validation failures (`MethodArgumentNotValidException`).
-   Unexpected exceptions (`Exception`).

These exceptions are returned in a structured `ApiErrorResponse`
containing a timestamp, HTTP status, error name, and message.

Business rejections, such as missing prerequisites, duplicate
enrollment, or an unknown course, are represented by the service's
`EnrollmentResponse` with `REJECTED` status and mapped by the controller
to an HTTP error response. They are not treated as unexpected technical
exceptions.

## 10. API Endpoints

  ---------------------------------------------------------------------------------------------------
  Method                  Endpoint                                            Purpose
  ----------------------- --------------------------------------------------- -----------------------
  POST                    `/api/courses/{courseId}/enrollments`               Enroll a student or add
                                                                              them to the waitlist

  DELETE                  `/api/courses/{courseId}/enrollments/{studentId}`   Drop an enrollment or
                                                                              cancel a waitlist entry

  GET                     `/api/courses/{courseId}/students`                  Retrieve course
                                                                              enrollment and waitlist
                                                                              details
  ---------------------------------------------------------------------------------------------------

See `README.md` for request and response examples.

## 11. Testing Strategy

The automated tests cover service and controller behavior, including:

-   Successful enrollment when capacity is available.
-   Rejection when prerequisites are missing.
-   Duplicate enrollment prevention.
-   Waitlisting when the course is full.
-   Duplicate waitlist prevention.
-   Unknown course and invalid student ID handling.
-   Dropping an enrolled student.
-   Cancelling a waitlist entry.
-   FIFO waitlist promotion.
-   Skipping an ineligible waitlisted student.
-   Course enrollment details and unknown-course lookup.
-   Malformed JSON and request-field validation.
-   Concurrent enrollment without exceeding capacity.
-   Concurrent requests for the same student without duplicate
    enrollment.

The complete suite is run with:

``` bash
mvn clean test
```

Manual API verification is performed using Postman against the running
application.

## 12. Limitations, Trade-offs and Future Improvements

### Current Limitations and Trade-offs

1. **In-memory storage:** Student and course information is stored in memory to keep the implementation small and focused. Data will be lost when the application restarts.

2. **Single course offering:** The implementation currently supports the sample course offering `JAVA201`. Course creation, course management, and multiple academic terms are outside the current scope.

3. **Single application instance:** Synchronization using `synchronized(course)` protects concurrent operations within a single JVM instance. It does not provide distributed locking across multiple application instances.

4. **No authentication or authorization:** The APIs do not currently verify learner identity or restrict access based on user roles.

5. **No persistent audit history:** The implementation maintains the current enrollment and waitlist state but does not store a historical record of enrollment, cancellation, or promotion activities.

### Follow-up Improvements

If the requirements expand, the design could be extended with:

1. **Database persistence:** Introduce a database such as PostgreSQL to persist course, student, enrollment, and waitlist information.

2. **Distributed consistency:** Use database transactions and appropriate locking mechanisms to maintain consistency when multiple application instances process enrollment requests.

3. **Security:** Add authentication and authorization to secure enrollment APIs and control access based on user roles.

4. **Audit logging:** Track enrollment, cancellation, and waitlist promotion activities for historical reference and auditing.

5. **Multiple course offerings:** Extend the implementation to support multiple courses, course offerings, and academic terms.

6. **Pagination:** Add pagination to enrollment and waitlist retrieval APIs when handling larger datasets.

7. **Additional testing:** Introduce more integration and performance tests as the application grows.

These improvements are outside the scope of the current assessment implementation and can be considered for future iterations.