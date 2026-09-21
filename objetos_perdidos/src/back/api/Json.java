package back.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON mínimo (el JDK no trae uno). Lee objetos, listas, textos, números, booleanos y null;
 * escribe Map, List, String, Number, Boolean y null.
 */
final class Json {

    private static final int PROFUNDIDAD_MAXIMA = 32;

    private Json() {
    }

    /* ---------------- Lectura ---------------- */

    static Object leer(String texto) {
        Lector lector = new Lector(texto);
        lector.espacios();
        Object valor = lector.valor(0);
        lector.espacios();
        if (lector.pos != texto.length()) {
            throw new IllegalArgumentException("JSON inválido");
        }
        return valor;
    }

    private static final class Lector {
        private final String s;
        private int pos = 0;

        Lector(String s) {
            this.s = s;
        }

        void espacios() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
        }

        char actual() {
            if (pos >= s.length()) throw new IllegalArgumentException("JSON incompleto");
            return s.charAt(pos);
        }

        Object valor(int profundidad) {
            if (profundidad > PROFUNDIDAD_MAXIMA) throw new IllegalArgumentException("JSON demasiado anidado");

            char c = actual();
            switch (c) {
                case '{':
                    return objeto(profundidad);
                case '[':
                    return lista(profundidad);
                case '"':
                    return texto();
                case 't':
                    return palabra("true", Boolean.TRUE);
                case 'f':
                    return palabra("false", Boolean.FALSE);
                case 'n':
                    return palabra("null", null);
                default:
                    return numero();
            }
        }

        Object palabra(String palabra, Object valor) {
            if (!s.startsWith(palabra, pos)) throw new IllegalArgumentException("JSON inválido");
            pos += palabra.length();
            return valor;
        }

        Object numero() {
            int inicio = pos;
            while (pos < s.length() && "+-0123456789.eE".indexOf(s.charAt(pos)) >= 0) pos++;
            if (inicio == pos) throw new IllegalArgumentException("JSON inválido");
            return Double.valueOf(s.substring(inicio, pos));
        }

        Map<String, Object> objeto(int profundidad) {
            Map<String, Object> mapa = new LinkedHashMap<>();
            pos++; // {
            espacios();
            if (actual() == '}') {
                pos++;
                return mapa;
            }
            while (true) {
                espacios();
                if (actual() != '"') throw new IllegalArgumentException("JSON inválido");
                String clave = texto();
                espacios();
                if (actual() != ':') throw new IllegalArgumentException("JSON inválido");
                pos++;
                espacios();
                mapa.put(clave, valor(profundidad + 1));
                espacios();
                char c = actual();
                pos++;
                if (c == '}') return mapa;
                if (c != ',') throw new IllegalArgumentException("JSON inválido");
            }
        }

        List<Object> lista(int profundidad) {
            List<Object> lista = new ArrayList<>();
            pos++; // [
            espacios();
            if (actual() == ']') {
                pos++;
                return lista;
            }
            while (true) {
                espacios();
                lista.add(valor(profundidad + 1));
                espacios();
                char c = actual();
                pos++;
                if (c == ']') return lista;
                if (c != ',') throw new IllegalArgumentException("JSON inválido");
            }
        }

        String texto() {
            pos++; // comilla inicial
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = actual();
                pos++;
                if (c == '"') return sb.toString();
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                char e = actual();
                pos++;
                switch (e) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        if (pos + 4 > s.length()) throw new IllegalArgumentException("JSON inválido");
                        sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                        pos += 4;
                        break;
                    default:
                        throw new IllegalArgumentException("JSON inválido");
                }
            }
        }
    }

    /* ---------------- Escritura ---------------- */

    static String escribir(Object valor) {
        StringBuilder sb = new StringBuilder();
        escribir(sb, valor);
        return sb.toString();
    }

    private static void escribir(StringBuilder sb, Object valor) {
        if (valor == null) {
            sb.append("null");
        } else if (valor instanceof String) {
            texto(sb, (String) valor);
        } else if (valor instanceof Boolean || valor instanceof Number) {
            sb.append(valor);
        } else if (valor instanceof Map) {
            sb.append('{');
            boolean primero = true;
            for (Map.Entry<?, ?> entrada : ((Map<?, ?>) valor).entrySet()) {
                if (!primero) sb.append(',');
                primero = false;
                texto(sb, String.valueOf(entrada.getKey()));
                sb.append(':');
                escribir(sb, entrada.getValue());
            }
            sb.append('}');
        } else if (valor instanceof Iterable) {
            sb.append('[');
            boolean primero = true;
            for (Object elemento : (Iterable<?>) valor) {
                if (!primero) sb.append(',');
                primero = false;
                escribir(sb, elemento);
            }
            sb.append(']');
        } else {
            throw new IllegalArgumentException("Tipo no soportado: " + valor.getClass());
        }
    }

    private static void texto(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        sb.append('"');
    }
}
