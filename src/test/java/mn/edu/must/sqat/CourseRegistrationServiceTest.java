
package mn.edu.must.sqat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseRegistrationServiceTest {

    @Mock StudentRepository repository;
    @Mock EmailSender emailSender;
    CourseRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new CourseRegistrationService(repository, emailSender);
    }

    // A1 - Successful registration
    @Test
    void activeStudent_registrationReturnsOK() {
        Student student = new Student(
            "S001", "student@example.com", true, List.of()
        );
        when(repository.findStudent("S001")).thenReturn(student);
        when(repository.courseExists("CS101")).thenReturn(true);
        when(repository.prerequisites("CS101")).thenReturn(List.of());

        assertEquals("OK", service.register("S001", "CS101"));

        verify(repository).saveRegistration("S001", "CS101");
        verify(emailSender).send(
            eq("student@example.com"),
            anyString(),
            contains("CS101")
        );
    }

    // A2 - Student not found
    @Test
    void missingStudent_returnsErrorWithoutSaving() {
        when(repository.findStudent("S999")).thenReturn(null);

        assertEquals("ERROR_NO_STUDENT",
            service.register("S999", "CS101"));

        verify(repository, never())
            .saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }

    // A3 - Inactive student
    @Test
    void inactiveStudent_returnsErrorWithoutEmail() {
        Student student = new Student(
            "S002", "inactive@example.com", false, List.of()
        );
        when(repository.findStudent("S002")).thenReturn(student);

        assertEquals("ERROR_INACTIVE_STUDENT",
            service.register("S002", "CS101"));

        verify(repository, never())
            .saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }

    // A4 - Missing prerequisites
    @Test
    void missingPrerequisites_preventsRegistration() {
        Student student = new Student(
            "S004", "student4@example.com", true, List.of("CS100")
        );
        when(repository.findStudent("S004")).thenReturn(student);
        when(repository.courseExists("CS201")).thenReturn(true);
        when(repository.prerequisites("CS201"))
            .thenReturn(List.of("CS100", "CS101"));

        assertEquals("ERROR_PREREQUISITES",
            service.register("S004", "CS201"));

        verify(repository, never())
            .saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }

    // A5 - Email failure must not cancel registration
    @Test
    void emailFailure_registrationRemainsSuccessful() {
        Student student = new Student(
            "S005", "student5@example.com", true, List.of()
        );
        when(repository.findStudent("S005")).thenReturn(student);
        when(repository.courseExists("CS101")).thenReturn(true);
        when(repository.prerequisites("CS101")).thenReturn(List.of());

        doThrow(new RuntimeException("Email unavailable"))
            .when(emailSender)
            .send(anyString(), anyString(), anyString());

        assertEquals("OK", service.register("S005", "CS101"));

        verify(repository).saveRegistration("S005", "CS101");
    }

    // A6 - Both null arguments
    @Test
    void nullInputs_throwExceptionWithoutInteractions() {
        assertThrows(IllegalArgumentException.class,
            () -> service.register(null, "CS101"));

        assertThrows(IllegalArgumentException.class,
            () -> service.register("S001", null));

        verifyNoInteractions(repository, emailSender);
    }

    // A7 - Capture the actual email body
    @Test
    void successfulRegistration_emailContainsCourseCode() {
        Student student = new Student(
            "S007", "student7@example.com", true, List.of()
        );
        when(repository.findStudent("S007")).thenReturn(student);
        when(repository.courseExists("CS101")).thenReturn(true);
        when(repository.prerequisites("CS101")).thenReturn(List.of());

        assertEquals("OK", service.register("S007", "CS101"));

        ArgumentCaptor<String> bodyCaptor =
            ArgumentCaptor.forClass(String.class);

        verify(emailSender).send(
            eq("student7@example.com"),
            anyString(),
            bodyCaptor.capture()
        );

        assertTrue(bodyCaptor.getValue().contains("CS101"));
    }

    // A8 - Three error categories in one parameterized test
    @ParameterizedTest
    @CsvSource({
        "MISSING, ERROR_NO_STUDENT",
        "INACTIVE, ERROR_INACTIVE_STUDENT",
        "NO_COURSE, ERROR_NO_COURSE"
    })
    void invalidRegistration_returnsExpectedError(
            String scenario, String expected) {

        Student activeStudent = new Student(
            "S008", "student8@example.com", true, List.of()
        );
        Student inactiveStudent = new Student(
            "S008", "student8@example.com", false, List.of()
        );

        if (scenario.equals("MISSING")) {
            when(repository.findStudent("S008")).thenReturn(null);
        } else if (scenario.equals("INACTIVE")) {
            when(repository.findStudent("S008"))
                .thenReturn(inactiveStudent);
        } else {
            when(repository.findStudent("S008"))
                .thenReturn(activeStudent);
            when(repository.courseExists("CS999")).thenReturn(false);
        }

        assertEquals(expected, service.register("S008", "CS999"));

        verify(repository, never())
            .saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }
}
