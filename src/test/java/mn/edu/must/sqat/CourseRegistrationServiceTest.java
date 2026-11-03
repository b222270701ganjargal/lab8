
package mn.edu.must.sqat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.List;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseRegistrationServiceTest {

    @Mock
    StudentRepository repository;

    @Mock
    EmailSender emailSender;

    @Test
    void testSuccessfulRegistration() {
        Student student = new Student(
            "S001",
            "student@example.com",
            true,
            List.of()
        );

        when(repository.findStudent("S001")).thenReturn(student);
        when(repository.courseExists("CS101")).thenReturn(true);
        when(repository.prerequisites("CS101")).thenReturn(List.of());

        CourseRegistrationService service =
            new CourseRegistrationService(repository, emailSender);

        String result = service.register("S001", "CS101");

        assertEquals("OK", result);
        verify(repository).saveRegistration("S001", "CS101");
        verify(emailSender).send(
            eq("student@example.com"),
            anyString(),
            contains("CS101")
        );
    }
    
    @Test
    void testStudentNotFound() {
        when(repository.findStudent("S999")).thenReturn(null);

        CourseRegistrationService service =
                new CourseRegistrationService(repository, emailSender);

        String result = service.register("S999", "CS101");

        assertEquals("ERROR_NO_STUDENT", result);

        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }

    @Test
    void testInactiveStudent() {
        Student student = new Student(
            "S002",
            "inactive@example.com",
            false,
            List.of()
        );

        when(repository.findStudent("S002")).thenReturn(student);

        CourseRegistrationService service =
            new CourseRegistrationService(repository, emailSender);

        String result = service.register("S002", "CS101");

        assertEquals("ERROR_INACTIVE_STUDENT", result);

        verify(repository, never()).courseExists(anyString());
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }

    @Test
    void testCourseNotFound() {
        Student student = new Student(
            "S003",
            "student3@example.com",
            true,
            List.of()
        );

        when(repository.findStudent("S003")).thenReturn(student);
        when(repository.courseExists("CS999")).thenReturn(false);

        CourseRegistrationService service =
            new CourseRegistrationService(repository, emailSender);

        String result = service.register("S003", "CS999");

        assertEquals("ERROR_NO_COURSE", result);

        verify(repository, never()).prerequisites(anyString());
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }

    @Test
    void testMissingPrerequisites() {
        Student student = new Student(
            "S004",
            "student4@example.com",
            true,
            List.of("CS100")
        );

        when(repository.findStudent("S004")).thenReturn(student);
        when(repository.courseExists("CS201")).thenReturn(true);
        when(repository.prerequisites("CS201"))
                .thenReturn(List.of("CS100", "CS101"));

        CourseRegistrationService service =
                new CourseRegistrationService(repository, emailSender);

        String result = service.register("S004", "CS201");

        assertEquals("ERROR_PREREQUISITES", result);

        verify(repository, never())
                .saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }

    @Test
    void testEmailFailureDoesNotCancelRegistration() {
        Student student = new Student(
            "S005",
            "student5@example.com",
            true,
            List.of()
        );

        when(repository.findStudent("S005")).thenReturn(student);
        when(repository.courseExists("CS101")).thenReturn(true);
        when(repository.prerequisites("CS101")).thenReturn(List.of());

        doThrow(new RuntimeException("Email service unavailable"))
            .when(emailSender)
            .send(anyString(), anyString(), anyString());

        CourseRegistrationService service =
            new CourseRegistrationService(repository, emailSender);

        String result = service.register("S005", "CS101");

        assertEquals("OK", result);

        verify(repository).saveRegistration("S005", "CS101");
        verify(emailSender).send(
            eq("student5@example.com"),
            anyString(),
            contains("CS101")
        );
    }

    @Test
    void testNullStudentId() {
        CourseRegistrationService service =
            new CourseRegistrationService(repository, emailSender);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.register(null, "CS101")
        );

        assertTrue(exception.getMessage().contains("studentId"));
        verifyNoInteractions(repository, emailSender);
    }

    static Stream<Arguments> invalidRegistrationInputs() {
        return Stream.of(
            Arguments.of(null, "CS101"),
            Arguments.of("S001", null),
            Arguments.of(null, null)
        );
    }

    @ParameterizedTest
    @MethodSource("invalidRegistrationInputs")
    void testInvalidRegistrationInputs(
            String studentId, String courseId) {

        CourseRegistrationService service =
                new CourseRegistrationService(repository, emailSender);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.register(studentId, courseId)
        );

        verifyNoInteractions(repository, emailSender);
    }

}
