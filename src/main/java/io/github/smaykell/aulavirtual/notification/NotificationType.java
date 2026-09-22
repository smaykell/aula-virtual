package io.github.smaykell.aulavirtual.notification;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public enum NotificationType {

    ACCOUNT_CREATED("Ya puedes entrar al aula virtual", """
            Hola {firstName}:

            Tu cuenta del aula virtual ya está lista. Entras con estos datos:

              Usuario: {username}
              Contraseña: tu número de documento

            Cámbiala en cuanto entres, desde la opción «Mi perfil».
            """),

    ENROLLMENT_REQUESTED("Recibimos tu solicitud para {courseName}", """
            Hola {firstName}:

            Recibimos tu solicitud para el curso «{courseName}». El docente la
            revisará y te avisaremos por este mismo medio en cuanto la resuelva.
            """),

    ENROLLMENT_ACTIVE("Bienvenido a {courseName}", """
            Hola {firstName}:

            Ya estás inscrito en el curso «{courseName}». Entra al aula virtual
            para ver el material y las tareas.
            """),

    PASSWORD_RESET("Cambia tu contraseña del aula virtual", """
            Hola {firstName}:

            Alguien pidió cambiar la contraseña de tu cuenta del aula virtual.
            Tu usuario es: {username}

            Para elegir una contraseña nueva, abre este enlace:

              {link}

            El enlace sirve una sola vez y caduca en {minutes} minutos. Si no
            lo pediste tú, ignora este correo: tu contraseña no cambia.
            """);

    private final String subject;
    private final String body;
    private final Set<String> placeholders;

    NotificationType(String subject, String body) {
        this.subject = subject;
        this.body = body;
        this.placeholders = placeholdersOf(subject + body);
    }

    public Set<String> placeholders() {
        return placeholders;
    }

    public String subjectFor(Map<String, String> data) {
        return render(subject, data);
    }

    public String bodyFor(Map<String, String> data) {
        return render(body, data);
    }

    private String render(String template, Map<String, String> data) {
        if (!data.keySet().containsAll(placeholders)) {
            throw new IllegalArgumentException(
                    "A la notificacion %s le faltan datos: %s".formatted(name(), placeholders));
        }
        String rendered = template;
        for (Map.Entry<String, String> value : data.entrySet()) {
            rendered = rendered.replace("{" + value.getKey() + "}", value.getValue());
        }
        return rendered;
    }

    private static Set<String> placeholdersOf(String template) {
        // El patron no puede ser una constante: los constructores de las constantes del enum
        // corren antes que los campos estaticos y lo encontrarian todavia a null.
        Matcher matcher = Pattern.compile("[{]([A-Za-z0-9]+)[}]").matcher(template);
        return matcher.results().map(result -> result.group(1))
                .collect(Collectors.toUnmodifiableSet());
    }
}
