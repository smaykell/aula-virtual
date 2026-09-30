package io.github.smaykell.aulavirtual.course.enrollment.dto;

import io.github.smaykell.aulavirtual.course.dto.CourseConstraints;
import io.github.smaykell.aulavirtual.person.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record DirectEnrollmentRequest(
        @NotNull(message = CourseConstraints.DOCUMENT_TYPE_REQUIRED)
        DocumentType documentType,

        @NotEmpty(message = CourseConstraints.DOCUMENT_NUMBERS_REQUIRED)
        @Size(max = CourseConstraints.DIRECT_ENROLLMENT_MAX,
                message = CourseConstraints.DOCUMENT_NUMBERS_TOO_MANY)
        List<@NotBlank(message = CourseConstraints.DOCUMENT_NUMBERS_REQUIRED) String>
                documentNumbers) {
}
