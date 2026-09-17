package front.objetos;

import back.model.Usuario;
import back.service.ReportePerdidaService;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDate;

public class ReportarObjetoView {

    public void mostrar(Stage stage, Usuario usuario) {

        Label titulo = new Label("REPORTAR OBJETO PERDIDO");
        titulo.getStyleClass().add("titulo");

        Label subtitulo = new Label(
                "Realiza un reporte rápido para ayudar a encontrar tu objeto."
        );
        subtitulo.getStyleClass().add("subtitulo");

        TextField nombre = new TextField();
        nombre.setPromptText("¿Qué objeto perdiste?");

        TextArea descripcion = new TextArea();
        descripcion.setPromptText(
                "Describe brevemente el objeto"
        );
        descripcion.setPrefRowCount(4);
        descripcion.setWrapText(true);

        TextField lugar = new TextField();
        lugar.setPromptText(
                "¿Dónde se perdió?"
        );

        DatePicker fecha =
                new DatePicker(LocalDate.now());

        fecha.setMaxWidth(Double.MAX_VALUE);

        Label mensaje = new Label();
        mensaje.getStyleClass().add("mensaje");

        Button reportar =
                new Button("ENVIAR REPORTE");

        reportar.getStyleClass().add(
                "boton-principal"
        );

        Button volver =
                new Button("VOLVER");

        volver.getStyleClass().add(
                "boton-secundario"
        );

        reportar.setOnAction(event -> {

            String resultado =
                    ReportePerdidaService.guardarReporte(
                            nombre.getText(),
                            descripcion.getText(),
                            lugar.getText(),
                            fecha.getValue() == null
                                    ? ""
                                    : fecha.getValue().toString(),
                            usuario.getCorreo()
                    );

            if ("REPORTE_GUARDADO".equals(resultado)) {

                mensaje.setText(
                        "Reporte enviado correctamente."
                );

                mensaje.getStyleClass()
                        .removeAll("error");

                mensaje.getStyleClass()
                        .add("exito");

                nombre.clear();
                descripcion.clear();
                lugar.clear();

                fecha.setValue(LocalDate.now());

            } else {

                mensaje.setText(resultado);

                mensaje.getStyleClass()
                        .removeAll("exito");

                mensaje.getStyleClass()
                        .add("error");
            }
        });

        volver.setOnAction(event ->
                new ObjetosView()
                        .mostrar(stage, usuario)
        );

        VBox formulario = new VBox(
                12,
                titulo,
                subtitulo,
                nombre,
                descripcion,
                lugar,
                fecha,
                reportar,
                mensaje,
                volver
        );

        formulario.setAlignment(
                Pos.CENTER
        );

        formulario.setPadding(
                new Insets(35)
        );

        formulario.setMaxWidth(520);

        formulario.getStyleClass()
                .add("tarjeta");

        VBox root =
                new VBox(formulario);

        root.setAlignment(
                Pos.CENTER
        );

        root.getStyleClass()
                .add("fondo");

        Scene scene =
                new Scene(root, 900, 700);

        scene.getStylesheets().add(
                getClass()
                        .getResource(
                                "/front/styles/styles.css"
                        )
                        .toExternalForm()
        );

        stage.setTitle(
                "Reportar objeto perdido"
        );

        stage.setScene(scene);
        stage.show();
    }
}