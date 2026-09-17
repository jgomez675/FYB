package front.objetos;

import back.model.ObjetoPerdido;
import back.model.Usuario;
import back.service.ObjetoPerdidoService;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.File;

public class ObjetosView {

    public void mostrar(Stage stage, Usuario usuario) {

        Label titulo = new Label("OBJETOS PERDIDOS");
        titulo.getStyleClass().add("titulo");

        Label subtitulo = new Label(
                "Revisa las fotos y descripciones de los objetos publicados"
        );
        subtitulo.getStyleClass().add("subtitulo");

        // Botón existente para publicar un objeto
        Button publicar = new Button("+ PUBLICAR OBJETO PERDIDO");
        publicar.getStyleClass().add("boton-principal");
        publicar.setOnAction(event ->
                new RegistrarObjetoView().mostrar(stage, usuario)
        );

        // NUEVO: botón para reportar un objeto perdido
        Button reportar = new Button("REPORTAR OBJETO PERDIDO");
        reportar.getStyleClass().add("boton-principal");
        reportar.setOnAction(event ->
                new ReportarObjetoView().mostrar(stage, usuario)
        );

        FlowPane lista = new FlowPane();
        lista.setHgap(18);
        lista.setVgap(18);
        lista.setAlignment(Pos.TOP_CENTER);
        lista.setPrefWrapLength(820);

        for (ObjetoPerdido objeto : ObjetoPerdidoService.obtenerObjetos()) {
            lista.getChildren().add(
                    crearTarjeta(stage, usuario, objeto)
            );
        }

        if (lista.getChildren().isEmpty()) {
            Label vacio = new Label(
                    "Todavía no hay objetos publicados."
            );
            vacio.getStyleClass().add("mensaje");
            lista.getChildren().add(vacio);
        }

        ScrollPane scroll = new ScrollPane(lista);
        scroll.setFitToWidth(true);
        scroll.setStyle(
                "-fx-background: transparent; " +
                "-fx-background-color: transparent;"
        );
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // Se agrega el nuevo botón al contenido
        VBox contenido = new VBox(
                15,
                titulo,
                subtitulo,
                publicar,
                reportar,
                scroll
        );

        contenido.setPadding(new Insets(30));
        contenido.setAlignment(Pos.TOP_CENTER);
        contenido.getStyleClass().add("fondo");

        Scene scene = new Scene(contenido, 1000, 700);

        scene.getStylesheets().add(
                getClass()
                        .getResource("/front/styles/styles.css")
                        .toExternalForm()
        );

        stage.setTitle("Objetos Perdidos");
        stage.setScene(scene);
        stage.show();
    }

    private VBox crearTarjeta(
            Stage stage,
            Usuario usuario,
            ObjetoPerdido objeto
    ) {

        ImageView imagen = new ImageView();

        imagen.setFitWidth(180);
        imagen.setFitHeight(130);
        imagen.setPreserveRatio(true);

        File archivo = new File(objeto.getImagen());

        if (archivo.exists()) {
            imagen.setImage(
                    new Image(
                            archivo.toURI().toString(),
                            180,
                            130,
                            true,
                            true
                    )
            );
        }

        Label nombre = new Label(objeto.getNombre());
        nombre.getStyleClass().add("nombre-objeto");

        String resumen = objeto.getDescripcion();

        if (resumen.length() > 100) {
            resumen = resumen.substring(0, 100) + "...";
        }

        Label descripcion = new Label(resumen);
        descripcion.setWrapText(true);

        Label lugar = new Label(
                "Lugar: " + objeto.getLugar()
        );

        Label fecha = new Label(
                "Fecha: " + objeto.getFecha()
        );

        Button detalles = new Button("VER DETALLES");
        detalles.getStyleClass().add("boton-secundario");

        detalles.setOnAction(event ->
                new DetalleObjetoView()
                        .mostrar(stage, usuario, objeto)
        );

        VBox tarjeta = new VBox(
                10,
                imagen,
                nombre,
                descripcion,
                lugar,
                fecha,
                detalles
        );

        tarjeta.setPadding(new Insets(18));
        tarjeta.setPrefWidth(220);
        tarjeta.setMaxWidth(220);
        tarjeta.getStyleClass().add("tarjeta-objeto");

        return tarjeta;
    }
}