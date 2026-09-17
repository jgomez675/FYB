package front.objetos;

import back.model.Usuario;
import back.service.ObjetoPerdidoService;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDate;

public class RegistrarObjetoView {

    public void mostrar(Stage stage, Usuario usuario) {
        Label titulo = new Label("PUBLICAR OBJETO PERDIDO");
        titulo.getStyleClass().add("titulo");

        TextField nombre = new TextField();
        nombre.setPromptText("Nombre del objeto");

        TextArea descripcion = new TextArea();
        descripcion.setPromptText("Descripción detallada del objeto");
        descripcion.setPrefRowCount(4);
        descripcion.setWrapText(true);

        TextField lugar = new TextField();
        lugar.setPromptText("Lugar donde se perdió");

        DatePicker fecha = new DatePicker(LocalDate.now());
        fecha.setMaxWidth(Double.MAX_VALUE);

        Label imagenSeleccionada = new Label("Ninguna imagen seleccionada");
        imagenSeleccionada.getStyleClass().add("mensaje");

        final File[] archivo = {null};

        Button seleccionar = new Button("SELECCIONAR IMAGEN");
        seleccionar.getStyleClass().add("boton-secundario");
        seleccionar.setOnAction(event -> {
            FileChooser selector = new FileChooser();
            selector.setTitle("Seleccionar imagen del objeto");
            selector.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif")
            );
            File seleccionado = selector.showOpenDialog(stage);
            if (seleccionado != null) {
                archivo[0] = seleccionado;
                imagenSeleccionada.setText("Imagen: " + seleccionado.getName());
            }
        });

        Label mensaje = new Label();
        mensaje.getStyleClass().add("mensaje");

        Button publicar = new Button("PUBLICAR OBJETO");
        publicar.getStyleClass().add("boton-principal");

        Button volver = new Button("VOLVER");
        volver.getStyleClass().add("boton-secundario");

        publicar.setOnAction(event -> {
            String resultado = ObjetoPerdidoService.guardarObjeto(
                    nombre.getText(),
                    descripcion.getText(),
                    lugar.getText(),
                    fecha.getValue() == null ? "" : fecha.getValue().toString(),
                    archivo[0] == null ? null : archivo[0].toPath(),
                    usuario.getCorreo()
            );

            if ("OBJETO_GUARDADO".equals(resultado)) {
                mensaje.setText("Objeto publicado correctamente.");
                mensaje.getStyleClass().removeAll("error");
                mensaje.getStyleClass().add("exito");
                nombre.clear();
                descripcion.clear();
                lugar.clear();
                fecha.setValue(LocalDate.now());
                archivo[0] = null;
                imagenSeleccionada.setText("Ninguna imagen seleccionada");
            } else {
                mensaje.setText(resultado);
                mensaje.getStyleClass().removeAll("exito");
                mensaje.getStyleClass().add("error");
            }
        });

        volver.setOnAction(event -> new ObjetosView().mostrar(stage, usuario));

        VBox formulario = new VBox(12, titulo, nombre, descripcion, lugar, fecha,
                seleccionar, imagenSeleccionada, publicar, mensaje, volver);
        formulario.setAlignment(Pos.CENTER);
        formulario.setPadding(new Insets(35));
        formulario.setMaxWidth(520);
        formulario.getStyleClass().add("tarjeta");

        VBox root = new VBox(formulario);
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("fondo");

        Scene scene = new Scene(root, 900, 700);
        scene.getStylesheets().add(getClass().getResource("/front/styles/styles.css").toExternalForm());

        stage.setTitle("Publicar objeto perdido");
        stage.setScene(scene);
        stage.show();
    }
}
