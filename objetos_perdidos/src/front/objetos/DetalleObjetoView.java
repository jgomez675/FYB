package front.objetos;

import back.model.ObjetoPerdido;
import back.model.Usuario;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.File;

public class DetalleObjetoView {

    public void mostrar(Stage stage, Usuario usuario, ObjetoPerdido objeto) {
        Label titulo = new Label("DETALLE DEL OBJETO");
        titulo.getStyleClass().add("titulo");

        ImageView imagen = new ImageView();
        imagen.setFitWidth(300);
        imagen.setFitHeight(220);
        imagen.setPreserveRatio(true);

        File archivo = new File(objeto.getImagen());
        if (archivo.exists()) {
            imagen.setImage(new Image(archivo.toURI().toString(), 300, 220, true, true));
        }

        Label nombre = new Label(objeto.getNombre());
        nombre.getStyleClass().add("nombre-objeto");

        Label descripcion = new Label("Descripción:\n" + objeto.getDescripcion());
        descripcion.setWrapText(true);

        Label lugar = new Label("Lugar: " + objeto.getLugar());
        Label fecha = new Label("Fecha: " + objeto.getFecha());

        Button volver = new Button("VOLVER A OBJETOS");
        volver.getStyleClass().add("boton-principal");
        volver.setOnAction(event -> new ObjetosView().mostrar(stage, usuario));

        VBox tarjeta = new VBox(15, titulo, imagen, nombre, descripcion, lugar, fecha, volver);
        tarjeta.setAlignment(Pos.CENTER);
        tarjeta.setPadding(new Insets(30));
        tarjeta.setMaxWidth(560);
        tarjeta.getStyleClass().add("tarjeta");

        StackPane root = new StackPane(tarjeta);
        root.getStyleClass().add("fondo");

        Scene scene = new Scene(root, 900, 700);
        scene.getStylesheets().add(getClass().getResource("/front/styles/styles.css").toExternalForm());

        stage.setTitle("Detalle del objeto");
        stage.setScene(scene);
        stage.show();
    }
}
