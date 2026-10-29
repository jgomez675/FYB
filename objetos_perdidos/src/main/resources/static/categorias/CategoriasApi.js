/* Cliente de CategoriasController.java. */
(function (App) {
  "use strict";

  const { ApiClient, ErrorApi } = App.front.api;

  const POR_DEFECTO = ["Tecnología", "Documentos", "Accesorios", "Ropa", "Llaves", "Otros"];

  class CategoriasApi {
    static async obtener() {
      const r = await ApiClient.solicitar("GET", "/api/categorias");
      if (r.estado === 200 && r.datos) return r.datos.categorias;
      throw new ErrorApi(ApiClient.mensajeDe(r), r.estado);
    }

    // Para los formularios y filtros: si el servidor no responde, se usa la lista básica.
    static async obtenerConRespaldo() {
      try {
        return await CategoriasApi.obtener();
      } catch (e) {
        if (e.estado === 401) throw e;
        return POR_DEFECTO.slice();
      }
    }

    static async crear(nombre) {
      const r = await ApiClient.solicitar("POST", "/api/categorias", { nombre });
      if (r.estado === 201 && r.datos) return r.datos.categorias;
      throw new ErrorApi(ApiClient.mensajeDe(r), r.estado);
    }

    static async editar(anterior, nombre) {
      const r = await ApiClient.solicitar("PUT", "/api/categorias", { anterior, nombre });
      if (r.estado === 200 && r.datos) return r.datos.categorias;
      throw new ErrorApi(ApiClient.mensajeDe(r), r.estado);
    }

    static async eliminar(nombre) {
      const r = await ApiClient.solicitar("DELETE", "/api/categorias?nombre=" + encodeURIComponent(nombre));
      if (r.estado === 200 && r.datos) return r.datos.categorias;
      throw new ErrorApi(ApiClient.mensajeDe(r), r.estado);
    }
  }

  App.front.api.CategoriasApi = CategoriasApi;
})(window.App);
