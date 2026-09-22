/* Cliente de AuthService.java: mismos nombres y mismos resultados, pero hablando con el servidor. */
(function (App) {
  "use strict";

  const { ApiClient, ErrorApi } = App.front.api;

  class AuthApi {
    // Devuelve REGISTRO_EXITOSO o el mensaje de error.
    static async registrarUsuario(nombre, correo, contrasena, confirmarContrasena) {
      const r = await ApiClient.solicitar("POST", "/api/registro", { nombre, correo, contrasena, confirmar: confirmarContrasena });
      if (r.estado === 200 && r.datos && r.datos.ok) return AuthApi.REGISTRO_EXITOSO;
      return ApiClient.mensajeDe(r);
    }

    // Devuelve el usuario, o null si el correo o la contraseña no coinciden.
    static async iniciarSesion(correo, contrasena) {
      const r = await ApiClient.solicitar("POST", "/api/login", { correo, contrasena });
      if (r.estado === 200 && r.datos && r.datos.usuario) return r.datos.usuario;
      if (r.estado === 401) return null;
      throw new ErrorApi(ApiClient.mensajeDe(r), r.estado);
    }

    static async usuarioActual() {
      const r = await ApiClient.solicitar("GET", "/api/sesion");
      if (r.estado === 200 && r.datos) return r.datos.usuario || null;
      throw new ErrorApi(ApiClient.mensajeDe(r), r.estado);
    }

    static async cerrarSesion() {
      try {
        await ApiClient.solicitar("POST", "/api/logout", {});
      } catch (e) {
        /* sin conexión: la cookie vence sola */
      }
    }
  }

  AuthApi.REGISTRO_EXITOSO = "REGISTRO_EXITOSO";

  App.front.api.AuthApi = AuthApi;
})(window.App);
