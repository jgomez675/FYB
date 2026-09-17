package back;

import back.model.*;
import back.service.AuthService;
import back.service.ServicioEstudiantes;
import back.service.ServicioPuntos;
import back.service.ServicioRecompensas;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Demo interactiva del sistema de puntos: TODO lo pide por consola
 * (correo, contraseña, objetos, categoría, valor, recompensas, etc.).
 * No hay datos de ejemplo quemados en el código; cada dato lo escribe
 * quien ejecuta el programa.
 *
 * OJO: esta clase NO es el Main de la aplicación JavaFX (ese vive en
 * el paquete por defecto y llama a front.login.LoginView, según el
 * pom.xml). Es solo un punto de entrada de prueba, útil para verificar
 * que login + puntos funcionan juntos antes de conectarlos a la UI.
 *
 * Ejecutar con: java -cp out back.DemoPuntos
 */
public class DemoPuntos {

    private static final Scanner TECLADO = new Scanner(System.in);
    private static final ServicioPuntos SERVICIO_PUNTOS = new ServicioPuntos();
    private static final ServicioRecompensas SERVICIO_RECOMPENSAS = new ServicioRecompensas();

    private static final List<Objeto> OBJETOS = new ArrayList<>();
    private static int contadorObjetos = 0;
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
                case 1 -> registrarObjetoNuevo(estudiante);
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
        System.out.println("1. Registrar un objeto (perdido/encontrado)");
        System.out.println("2. Ver un objeto ya registrado");
        System.out.println("3. Devolver un objeto perdido");
        System.out.println("4. Canjear una recompensa");
        System.out.println("5. Ver mi historial de puntos");
        System.out.println("6. Salir");
    }

    // ---------------------------------------------------------------
    // Opciones del menú
    // ---------------------------------------------------------------

    private static void registrarObjetoNuevo(Estudiante estudiante) {
        System.out.print("\nNombre del objeto: ");
        String nombre = TECLADO.nextLine().trim();

        System.out.print("Descripción: ");
        String descripcion = TECLADO.nextLine().trim();

        CategoriaObjeto categoria = leerCategoria();
        ValorObjeto valor = leerValor();
        EstadoObjeto estado = leerEstadoInicial();

        contadorObjetos++;
        Objeto objeto = new Objeto(
                "OBJ" + String.format("%03d", contadorObjetos),
                nombre,
                descripcion,
                categoria,
                valor
        );
        objeto.setEstado(estado);

        System.out.print("¿Quieres agregar una foto (ruta o URL)? Deja vacío para omitir: ");
        String foto = TECLADO.nextLine().trim();
        if (!foto.isBlank()) {
            objeto.agregarFoto(foto);
        }

        OBJETOS.add(objeto);
        SERVICIO_PUNTOS.registrarObjeto(estudiante, objeto);

        System.out.println("\nObjeto registrado: " + objeto);
        System.out.println(estudiante);
    }

    private static void verObjetoExistente(Estudiante estudiante) {
        Objeto objeto = elegirObjeto("ver");
        if (objeto == null) {
            return;
        }

        SERVICIO_PUNTOS.verObjeto(estudiante, objeto);

        System.out.println("\n" + objeto);
        if (!objeto.getFotos().isEmpty()) {
            System.out.println("Fotos: " + objeto.getFotos());
        }
        System.out.println(estudiante);
    }

    private static void devolverObjetoPerdido(Estudiante estudiante) {
        Objeto objeto = elegirObjeto("devolver");
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

    private static Objeto elegirObjeto(String accion) {
        if (OBJETOS.isEmpty()) {
            System.out.println("\nTodavía no hay objetos registrados. Registra uno primero.");
            return null;
        }

        System.out.println("\nObjetos disponibles para " + accion + ":");
        for (int i = 0; i < OBJETOS.size(); i++) {
            System.out.println((i + 1) + ". " + OBJETOS.get(i));
        }

        int indice = leerEntero("Elige un objeto (número): ", 1, OBJETOS.size());
        return OBJETOS.get(indice - 1);
    }

    private static CategoriaObjeto leerCategoria() {
        CategoriaObjeto[] categorias = CategoriaObjeto.values();
        System.out.println("Categoría:");
        for (int i = 0; i < categorias.length; i++) {
            System.out.println((i + 1) + ". " + categorias[i]);
        }
        int indice = leerEntero("Elige una categoría (número): ", 1, categorias.length);
        return categorias[indice - 1];
    }

    private static ValorObjeto leerValor() {
        ValorObjeto[] valores = ValorObjeto.values();
        System.out.println("Valor estimado del objeto:");
        for (int i = 0; i < valores.length; i++) {
            System.out.println((i + 1) + ". " + valores[i]);
        }
        int indice = leerEntero("Elige un valor (número): ", 1, valores.length);
        return valores[indice - 1];
    }

    private static EstadoObjeto leerEstadoInicial() {
        System.out.println("¿El objeto está perdido o ya lo encontraron?");
        System.out.println("1. " + EstadoObjeto.PERDIDO);
        System.out.println("2. " + EstadoObjeto.ENCONTRADO);
        int indice = leerEntero("Elige una opción: ", 1, 2);
        return indice == 1 ? EstadoObjeto.PERDIDO : EstadoObjeto.ENCONTRADO;
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
