package back;

import back.model.*;
import back.service.AuthService;
import back.service.ObjetoPerdidoService;
import back.service.ServicioEstudiantes;
import back.service.ServicioPuntos;
import back.service.ServicioRecompensas;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

/**
 * Demo interactiva del sistema de puntos: TODO lo pide por consola
 * (correo, contraseña, objetos, valor, recompensas, etc.). No hay datos
 * de ejemplo quemados en el código.
 *
 * Trabaja sobre ObjetoPerdido (la clase real del módulo de reportes),
 * usando ObjetoPerdidoService para publicar objetos igual que lo haría
 * la pantalla RegistrarObjetoView.
 *
 * OJO: esta clase NO es el Main de la aplicación JavaFX (ese vive en
 * el paquete por defecto y llama a front.login.LoginView).
 *
 * Ejecutar con: java -cp out back.DemoPuntos
 */
public class DemoPuntos {

    private static final Scanner TECLADO = new Scanner(System.in);
    private static final ServicioPuntos SERVICIO_PUNTOS = new ServicioPuntos();
    private static final ServicioRecompensas SERVICIO_RECOMPENSAS = new ServicioRecompensas();

    private static int contadorRecompensas = 0;

    public static void main(String[] args) {
        Estudiante estudiante = iniciarSesionOFallar();
        if (estudiante == null) {
            return;
        }

        boolean seguir = true;
        while (seguir) {
            mostrarMenu(estudiante);
            int opcion = leerEntero("Elige una opción: ", 1, 6);

            switch (opcion) {
                case 1 -> publicarObjetoPerdido(estudiante);
                case 2 -> verObjetoExistente(estudiante);
                case 3 -> devolverObjetoPerdido(estudiante);
                case 4 -> canjearRecompensa(estudiante);
                case 5 -> mostrarHistorial(estudiante);
                case 6 -> seguir = false;
                default -> System.out.println("Opción no válida.");
            }
        }

        System.out.println("\nSesión terminada. Los puntos quedaron guardados en data/puntos.txt");
    }

    // ---------------------------------------------------------------
    // Login / registro
    // ---------------------------------------------------------------

    private static Estudiante iniciarSesionOFallar() {
        System.out.print("Correo institucional (@usa.edu.co): ");
        String correo = TECLADO.nextLine().trim();

        System.out.print("Contraseña (déjala vacía si aún no tienes cuenta): ");
        String contraseña = TECLADO.nextLine();

        Usuario usuario = contraseña.isBlank() ? null : AuthService.iniciarSesion(correo, contraseña);

        if (usuario == null) {
            System.out.println("\nNo existe una cuenta con ese correo (o la contraseña fue incorrecta).");
            System.out.print("¿Quieres registrarte con ese correo? (s/n): ");
            if (!TECLADO.nextLine().trim().equalsIgnoreCase("s")) {
                System.out.println("Saliendo sin iniciar sesión.");
                return null;
            }
            usuario = registrarUsuarioPorConsola(correo);
            if (usuario == null) {
                return null;
            }
        }

        Estudiante estudiante = ServicioEstudiantes.obtenerOCrearEstudiante(usuario);
        System.out.println("\n== Sesión iniciada ==");
        System.out.println(estudiante);
        return estudiante;
    }

    private static Usuario registrarUsuarioPorConsola(String correo) {
        System.out.print("Nombre completo: ");
        String nombre = TECLADO.nextLine().trim();

        System.out.print("Elige una contraseña (mínimo 6 caracteres): ");
        String contraseña = TECLADO.nextLine();

        System.out.print("Confirma la contraseña: ");
        String confirmarContraseña = TECLADO.nextLine();

        String resultado = AuthService.registrarUsuario(nombre, correo, contraseña, confirmarContraseña);

        if (!"REGISTRO_EXITOSO".equals(resultado)) {
            System.out.println("No se pudo registrar: " + resultado);
            return null;
        }

        System.out.println("Registro exitoso. Sesión iniciada automáticamente.");
        return AuthService.iniciarSesion(correo, contraseña);
    }

    // ---------------------------------------------------------------
    // Menú
    // ---------------------------------------------------------------

    private static void mostrarMenu(Estudiante estudiante) {
        System.out.println("\n===================================");
        System.out.println(estudiante);
        System.out.println("1. Publicar un objeto perdido");
        System.out.println("2. Ver un objeto ya publicado");
        System.out.println("3. Devolver un objeto perdido");
        System.out.println("4. Canjear una recompensa");
        System.out.println("5. Ver mi historial de puntos");
        System.out.println("6. Salir");
    }

    // ---------------------------------------------------------------
    // Opciones del menú
    // ---------------------------------------------------------------

    private static void publicarObjetoPerdido(Estudiante estudiante) {
        System.out.print("\nNombre del objeto: ");
        String nombre = TECLADO.nextLine().trim();

        System.out.print("Descripción: ");
        String descripcion = TECLADO.nextLine().trim();

        System.out.print("Lugar donde se perdió: ");
        String lugar = TECLADO.nextLine().trim();

        System.out.print("Fecha (por ejemplo 2026-09-17): ");
        String fecha = TECLADO.nextLine().trim();

        ValorObjeto valor = leerValor();

        System.out.print("Ruta de una imagen en tu computador (deja vacío para usar una de prueba): ");
        String rutaImagen = TECLADO.nextLine().trim();
        Path imagen = obtenerRutaImagen(rutaImagen);
        if (imagen == null) {
            System.out.println("No se pudo preparar la imagen; se cancela el registro.");
            return;
        }

        String resultado = ObjetoPerdidoService.guardarObjeto(
                nombre, descripcion, lugar, fecha, imagen, estudiante.getCorreo(), valor
        );

        if (!"OBJETO_GUARDADO".equals(resultado)) {
            System.out.println("No se pudo publicar el objeto: " + resultado);
            return;
        }

        ObjetoPerdido objeto = ObjetoPerdidoService.obtenerUltimoObjeto();
        SERVICIO_PUNTOS.registrarObjeto(estudiante, objeto);

        System.out.println("\nObjeto publicado: " + objeto);
        System.out.println(estudiante);
    }

    private static void verObjetoExistente(Estudiante estudiante) {
        ObjetoPerdido objeto = elegirObjeto("ver");
        if (objeto == null) {
            return;
        }

        SERVICIO_PUNTOS.verObjeto(estudiante, objeto);

        System.out.println("\n" + objeto);
        System.out.println("Imagen: " + objeto.getImagen());
        System.out.println(estudiante);
    }

    private static void devolverObjetoPerdido(Estudiante estudiante) {
        ObjetoPerdido objeto = elegirObjeto("devolver");
        if (objeto == null) {
            return;
        }

        try {
            SERVICIO_PUNTOS.devolverObjetoPerdido(estudiante, objeto);
            System.out.println("\nObjeto devuelto: " + objeto);
            System.out.println(estudiante);
        } catch (IllegalStateException e) {
            System.out.println("No se pudo devolver el objeto: " + e.getMessage());
        }
    }

    private static void canjearRecompensa(Estudiante estudiante) {
        System.out.print("\nNombre de la recompensa: ");
        String nombre = TECLADO.nextLine().trim();

        int costoPuntos = leerEntero("Costo en puntos: ", 1, Integer.MAX_VALUE);

        contadorRecompensas++;
        Recompensa recompensa = new Recompensa(
                "REC" + String.format("%03d", contadorRecompensas),
                nombre,
                costoPuntos
        );

        boolean exitoso = SERVICIO_RECOMPENSAS.canjear(estudiante, recompensa);

        System.out.println("\n¿Canje exitoso? " + exitoso);
        if (!exitoso) {
            System.out.println("(No tienes suficientes puntos: te faltan "
                    + (costoPuntos - estudiante.getPuntosTotales()) + ")");
        }
        System.out.println(estudiante);
    }

    private static void mostrarHistorial(Estudiante estudiante) {
        System.out.println("\n== Historial de puntos de " + estudiante.getNombre() + " ==");
        if (estudiante.getHistorialPuntos().isEmpty()) {
            System.out.println("(Todavía no tienes movimientos)");
        } else {
            estudiante.getHistorialPuntos().forEach(System.out::println);
        }
        System.out.println(estudiante);
    }

    // ---------------------------------------------------------------
    // Utilidades de lectura por consola
    // ---------------------------------------------------------------

    private static ObjetoPerdido elegirObjeto(String accion) {
        List<ObjetoPerdido> objetos = ObjetoPerdidoService.obtenerObjetos();
        if (objetos.isEmpty()) {
            System.out.println("\nTodavía no hay objetos publicados. Publica uno primero.");
            return null;
        }

        System.out.println("\nObjetos disponibles para " + accion + ":");
        for (int i = 0; i < objetos.size(); i++) {
            System.out.println((i + 1) + ". " + objetos.get(i));
        }

        int indice = leerEntero("Elige un objeto (número): ", 1, objetos.size());
        return objetos.get(indice - 1);
    }

    private static ValorObjeto leerValor() {
        ValorObjeto[] valores = ValorObjeto.values();
        System.out.println("¿Qué tan valioso es el objeto?");
        for (int i = 0; i < valores.length; i++) {
            System.out.println((i + 1) + ". " + valores[i]);
        }
        int indice = leerEntero("Elige un valor (número): ", 1, valores.length);
        return valores[indice - 1];
    }

    private static Path obtenerRutaImagen(String rutaEscrita) {
        if (!rutaEscrita.isBlank()) {
            Path ruta = Path.of(rutaEscrita);
            if (Files.exists(ruta)) {
                return ruta;
            }
            System.out.println("Esa ruta no existe. Se usará una imagen de prueba en su lugar.");
        }

        try {
            Path temporal = Files.createTempFile("demo-objeto", ".jpg");
            Files.writeString(temporal, "imagen de prueba");
            return temporal;
        } catch (IOException e) {
            System.out.println("No se pudo crear una imagen de prueba: " + e.getMessage());
            return null;
        }
    }

    private static int leerEntero(String mensaje, int minimo, int maximo) {
        while (true) {
            System.out.print(mensaje);
            String linea = TECLADO.nextLine().trim();
            try {
                int valor = Integer.parseInt(linea);
                if (valor >= minimo && valor <= maximo) {
                    return valor;
                }
                System.out.println("Debe ser un número entre " + minimo + " y " + maximo + ".");
            } catch (NumberFormatException e) {
                System.out.println("Escribe un número válido.");
            }
        }
    }
}
