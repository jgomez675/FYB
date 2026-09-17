package front.login;

import back.model.Usuario;
import back.service.AuthService;
import front.register.RegisterView;
import front.objetos.ObjetosView;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class LoginView extends Application {

    @Override
    public void start(Stage stage) {

        Label titulo = new Label("OBJETOS PERDIDOS");
        titulo.getStyleClass().add("titulo");

        Label subtitulo = new Label(
                "Encuentra lo que creías perdido"
        );

        subtitulo.getStyleClass().add("subtitulo");

        TextField correo = new TextField();
        correo.setPromptText("Correo institucional");

        PasswordField contraseña = new PasswordField();
        contraseña.setPromptText("Contraseña");

        Button iniciarSesion = new Button("INICIAR SESIÓN");
        iniciarSesion.getStyleClass().add("boton-principal");

        Label mensaje = new Label();
        mensaje.getStyleClass().add("mensaje");

        Hyperlink registrarse = new Hyperlink(
                "¿No tienes una cuenta? Regístrate"
        );

        VBox formulario = new VBox(
                15,
                titulo,
                subtitulo,
                correo,
                contraseña,
                iniciarSesion,
                mensaje,
                registrarse
        );

        formulario.setAlignment(Pos.CENTER);
        formulario.setPadding(new Insets(40));
        formulario.setMaxWidth(420);
        formulario.getStyleClass().add("tarjeta");

        StackPane root = new StackPane(formulario);
        root.getStyleClass().add("fondo");

        Scene scene = new Scene(root, 900, 600);

        scene.getStylesheets().add(
                getClass()
                        .getResource("/front/styles/styles.css")
                        .toExternalForm()
        );

        iniciarSesion.setOnAction(event -> {

            String correoIngresado = correo.getText().trim();
            String contraseñaIngresada = contraseña.getText();

            if (correoIngresado.isBlank()
                    || contraseñaIngresada.isBlank()) {

                mensaje.setText(
                        "Completa todos los campos."
                );

                mensaje.getStyleClass().removeAll("exito");
                mensaje.getStyleClass().add("error");

                return;
            }

            Usuario usuario = AuthService.iniciarSesion(
                    correoIngresado,
                    contraseñaIngresada
            );

            if (usuario != null) {

                mensaje.setText(
                        "Bienvenido, " + usuario.getNombre()
                );

                mensaje.getStyleClass().removeAll("error");
                mensaje.getStyleClass().add("exito");

                ObjetosView objetosView = new ObjetosView();
                objetosView.mostrar(stage, usuario);

            } else {

                mensaje.setText(
                        "Correo o contraseña incorrectos."
                );

                mensaje.getStyleClass().removeAll("exito");
                mensaje.getStyleClass().add("error");
            }
        });

        registrarse.setOnAction(event -> {

            RegisterView registerView = new RegisterView();

            try {
                registerView.start(stage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        stage.setTitle("Objetos Perdidos");
        stage.setScene(scene);
        stage.show();
    }
}
