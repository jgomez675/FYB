/* Cliente de los datos de perfil (AuthService.actualizarPerfil). */
(function (App) {
  "use strict";

  const { ApiClient, ErrorApi } = App.front.api;

  class PerfilApi {
    // Devuelve { nombre, correo, telefono }.
    static async obtenerPerfil() {
      const r = await ApiClient.solicitar("GET", "/api/perfil");
      if (r.estado === 200 && r.datos && r.datos.perfil) return r.datos.perfil;
      throw new ErrorApi(ApiClient.mensajeDe(r), r.estado);
    }

    // Devuelve PERFIL_ACTUALIZADO o el mensaje de error. El correo no se puede cambiar.
    static async actualizarPerfil(nombre, telefono) {
      const r = await ApiClient.solicitar("PUT", "/api/perfil", { nombre, telefono });
      if (r.estado === 200 && r.datos && r.datos.perfil) return PerfilApi.PERFIL_ACTUALIZADO;
      if (r.estado === 401) throw new ErrorApi(ApiClient.mensajeDe(r), 401);
      return ApiClient.mensajeDe(r);
    }
  }

  PerfilApi.PERFIL_ACTUALIZADO = "PERFIL_ACTUALIZADO";

  App.front.api.PerfilApi = PerfilApi;
})(window.App);
