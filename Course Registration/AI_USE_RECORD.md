# AI Prompt and Use Record — Course Registration System

## 1. Purpose

AI was used as an engineering assistant during this bounded course-registration feature. The prompts below summarize the engineering activities discussed during development; they are concise summaries, not claimed verbatim transcripts of every chat message. Suggestions were reviewed and adapted to the starter project's scope and the assessment requirements.

## 2. Prompt and decision log

| # | Prompt / engineering activity | Relevant AI assistance | Decision | Files or decisions informed | Verification evidence |
|---|---|---|---|---|---|
| 1 | Analyze the assessment requirements and break the course-registration workflow into manageable implementation tasks. | Helped identify enrollment, prerequisite checks, capacity, duplicate prevention, waitlisting, dropping, promotion, validation, and testing as separate concerns. | Accepted and adapted to the single-course scope. | Service boundary, project structure, `CourseRegistrationService`, design specification. | The service exposes enrollment, drop, and course-enrollment retrieval operations; the design document describes the bounded scope. |
| 2 | Implement enrollment so a learner is accepted only when the prerequisite rule is satisfied and a seat is available. | Suggested checking required completed-course IDs before changing course state and returning a clear rejection when prerequisites are missing. | Accepted. | `CourseRegistrationService.java`, enrollment tests. | `shouldEnrollStudentWhenSeatIsAvailable` and `shouldRejectStudentWhenPrerequisiteIsMissing`. |
| 3 | Prevent duplicate enrollment and duplicate waitlist requests. | Suggested checking both enrolled and waitlisted student IDs before adding a new entry. | Accepted. | `CourseRegistrationService.java`, enrollment tests. | `shouldRejectDuplicateEnrollment` and `shouldNotAddStudentToWaitlistTwice`. |
| 4 | Use a FIFO structure for learners waiting when the course is full. | Suggested a queue so requests retain arrival order and the earliest eligible learner can be considered first. | Accepted. | `CourseOffering` waitlist data structure, `CourseRegistrationService.java`. | `shouldAddStudentToWaitlistWhenCourseIsFull` and `shouldPromoteFirstStudentFromWaitlist`. |
| 5 | When an enrolled learner drops, promote the earliest eligible learner from the waitlist. | Suggested polling waitlisted learners in queue order and enrolling the first eligible candidate. | Accepted and adapted. | `dropEnrollment()` and `promoteFromWaitlist()` in `CourseRegistrationService.java`. | `shouldPromoteFirstStudentFromWaitlist`. |
| 6 | Define what happens if a learner at the front of the waitlist is no longer eligible. | Suggested skipping an ineligible candidate and checking the next candidate. The implemented policy removes the ineligible candidate from the queue. | Accepted and adapted; this policy is documented explicitly. | `promoteFromWaitlist()`, `DESIGN.md`. | `shouldSkipIneligibleStudentAndPromoteNextEligible` checks that the ineligible learner is not promoted and is removed from the waitlist. |
| 7 | Support both dropping an active enrollment and cancelling a waitlist request, with clear responses for invalid drops. | Suggested handling waitlist cancellation separately from enrolled-student removal before attempting promotion. | Accepted. | `dropEnrollment()` in `CourseRegistrationService.java`, drop tests. | `shouldDropEnrolledStudent`, `shouldRejectDropWhenStudentIsNotEnrolled`, `shouldRejectDropWhenCourseDoesNotExist`, `shouldRejectDropWithBlankStudentId`, and `shouldRemoveStudentFromWaitlist`. |
| 8 | Return a view of course enrollment state, including enrolled and waitlisted learners and waitlist positions. | Suggested building a response DTO from the course and student data while keeping the service boundary focused. | Accepted and adapted. | `getCourseEnrollments()`, `CourseEnrollmentView`, `StudentEnrollmentView`. | `shouldReturnCourseEnrollmentDetails` and `shouldReturnEmptyWhenCourseDoesNotExist`. |
| 9 | Protect course capacity and waitlist state when enrollment requests arrive concurrently. | Explained synchronization on the shared course object as a way to serialize state changes within one application instance. | Accepted; implemented with `synchronized (course)`, not `ReentrantLock`. | `CourseRegistrationService.java`, concurrency test, `DESIGN.md`. | `shouldNotExceedCourseCapacityDuringConcurrentEnrollment` submits 10 concurrent requests and checks 2 enrolled, 8 waitlisted, and 10 accounted for. |
| 10 | Identify focused service tests for normal, boundary, duplicate, prerequisite, waitlist, and invalid-request scenarios. | Helped organize test cases around the assessment acceptance criteria and use JUnit assertions. | Accepted and adapted. | `CourseRegistrationServiceTest.java`. | The uploaded service test file contains the named scenarios above. Controller-level tests and their final results should be confirmed from the complete project/test report. |
| 11 | Review the implementation and prepare proportionate setup and design documentation. | Helped review the feature against the bounded scope and draft explanations of assumptions, invariants, concurrency, trade-offs, and run instructions. | Accepted and edited to match the implementation. | `README.md`, `DESIGN.md`, this record. | Documentation describes one in-memory course offering, prerequisite `JAVA101`, capacity 2, FIFO promotion, removal of ineligible waitlisted learners, and synchronization within one application instance. |

## 3. Assumptions and implementation decisions

- The sample offering is `JAVA201` (Advanced Java), with capacity 2 and prerequisite `JAVA101`.
- A learner must have completed every prerequisite listed for the offering.
- Only eligible learners may enter the waitlist.
- The waitlist follows FIFO order.
- When a seat opens, ineligible learners encountered during promotion are removed from the queue; the next eligible learner is promoted.
- A learner may cancel their waitlist request through the drop operation.
- Course state is held in memory and is reset when the application restarts.
- Synchronization is local to one application instance; it does not provide distributed locking across multiple instances.

## 4. Independent review and verification

The implementation was reviewed against the acceptance criteria. The uploaded service test file provides evidence for service-level enrollment, prerequisite, duplicate, capacity, waitlist, drop, promotion, retrieval, and concurrent-capacity scenarios.

The developer also reported exercising the REST API manually with Postman, including prerequisite rejection, duplicate enrollment, waitlist cancellation, and the final waitlist-promotion state. This is recorded as developer-reported manual verification, not as an independently inspected Postman execution log.

Before submission, run the following from the project root and retain the actual output as verification evidence:

```bash
mvn clean test
```

The service test file reviewed for this record includes a concurrent test for 10 distinct learners competing for capacity. It does not include a separate concurrent test in which multiple requests attempt to enroll the same learner. Do not claim that scenario has been tested unless such a test is added and passes.

Controller tests, the complete Maven test count, and the final build result should be confirmed from the complete project and actual test output before submission.

## 5. AI-use boundaries

AI suggestions were used as engineering guidance. The implementation choices were reviewed and adapted to the assessment's limited scope. The developer is responsible for understanding the code, validating the behavior, and ensuring this record accurately reflects the assistance used.

No credentials, secrets, or unnecessary sensitive information are included in this record.
