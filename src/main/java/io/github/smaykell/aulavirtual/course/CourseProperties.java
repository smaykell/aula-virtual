package io.github.smaykell.aulavirtual.course;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.courses")
public record CourseProperties(@NotBlank String invitationBaseUrl) {

    public CourseProperties {
        invitationBaseUrl = withoutTrailingSlash(invitationBaseUrl);
    }

    private static String withoutTrailingSlash(String value) {
        return value == null ? null : value.replaceAll("/+$", "");
    }
}
