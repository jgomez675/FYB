/* Cliente de FavoritosService.java. */
(function (App) {
  "use strict";

  const { ApiClient, ErrorApi } = App.front.api;

  class FavoritosApi {
    // Objetos favoritos de la persona, del último guardado al primero.
    static async obtenerFavoritos() {
      const r = await ApiClient.solicitar("GET", "/api/favoritos");
      if (r.estado === 200 && r.datos) return r.datos.favoritos;
      throw new ErrorApi(ApiClient.mensajeDe(r), r.estado);
    }

    // Guardar o quitar dos veces lo mismo no da error. Lanzan ErrorApi si no se pudo.
    static async guardar(objetoId) {
      const r = await ApiClient.solicitar("PUT", "/api/favoritos/" + encodeURIComponent(objetoId));
      if (r.estado === 200 && r.datos && r.datos.ok) return true;
      throw new ErrorApi(ApiClient.mensajeDe(r), r.estado);
    }

    static async quitar(objetoId) {
      const r = await ApiClient.solicitar("DELETE", "/api/favoritos/" + encodeURIComponent(objetoId));
      if (r.estado === 200 && r.datos && r.datos.ok) return true;
      throw new ErrorApi(ApiClient.mensajeDe(r), r.estado);
    }
  }

  App.front.api.FavoritosApi = FavoritosApi;
})(window.App);
