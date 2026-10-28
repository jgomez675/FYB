package back.validation;

import back.config.AppConfig;

import java.util.regex.Pattern;

public class EmailValidator {

    private static final Pattern PATRON = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@" + Pattern.quote(AppConfig.DOMINIO_CORREO) + "$",
            Pattern.CASE_INSENSITIVE
    );

    public static boolean esCorreoInstitucional(String correo) {

        if (correo == null || correo.isBlank()) {
            return false;
        }

        return PATRON.matcher(correo).matches();
    }
}
