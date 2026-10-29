/* Cliente de ObjetoPerdidoService.java. */
(function (App) {
  "use strict";

  const { ApiClient, ErrorApi } = App.front.api;

  class ObjetosApi {
    static async obtenerObjetos() {
      const r = await ApiClient.solicitar("GET", "/api/objetos");
      if (r.estado === 200 && r.datos) return r.datos.objetos;
      throw new ErrorApi(ApiClient.mensajeDe(r), r.estado);
    }

    // Devuelve el objeto, o null si no existe.
    static async obtenerObjeto(id) {
      const r = await ApiClient.solicitar("GET", "/api/objetos/" + encodeURIComponent(id));
      if (r.estado === 200 && r.datos) return r.datos.objeto;
      if (r.estado === 404) return null;
      throw new ErrorApi(ApiClient.mensajeDe(r), r.estado);
    }

    // Devuelve OBJETO_GUARDADO o el mensaje de error. La foto se reduce aquí antes de subirla.
    static async guardarObjeto(nombre, descripcion, lugar, fecha, archivoImagen, categoria = "Otros") {
      let imagen = "";
      if (archivoImagen) {
        try {
          imagen = await App.front.components.imagen.procesarImagen(archivoImagen);
        } catch (e) {
          return e.message;
        }
      }

      const r = await ApiClient.solicitar("POST", "/api/objetos", { nombre, descripcion, lugar, fecha, imagen, categoria });
      if (r.estado === 201 && r.datos && r.datos.ok) return ObjetosApi.OBJETO_GUARDADO;
      if (r.estado === 401) throw new ErrorApi(ApiClient.mensajeDe(r), 401);
      return ApiClient.mensajeDe(r);
    }

    static async editarObjeto(id, nombre, descripcion, lugar, fecha, categoria) {
      const r = await ApiClient.solicitar("PUT", "/api/objetos/" + encodeURIComponent(id) + "/editar", { nombre, descripcion, lugar, fecha, categoria });
      if (r.estado === 200 && r.datos && r.datos.ok) return true;
      throw new ErrorApi(ApiClient.mensajeDe(r), r.estado);
    }

    static async obtenerCategorias() {
      const r = await ApiClient.solicitar("GET", "/api/categorias");
      if (r.estado === 200 && r.datos) return r.datos.categorias;
      throw new ErrorApi(ApiClient.mensajeDe(r), r.estado);
    }

    static async crearCategoria(nombre) {
      const r = await ApiClient.solicitar("POST", "/api/categorias", { nombre });
      if (r.estado === 201 && r.datos) return r.datos.categorias;
      throw new ErrorApi(ApiClient.mensajeDe(r), r.estado);
    }

    static async editarCategoria(anterior, nombre) {
      const r = await ApiClient.solicitar("PUT", "/api/categorias/" + encodeURIComponent(anterior) + "/editar", { nombre });
      if (r.estado === 200 && r.datos) return r.datos.categorias;
      throw new ErrorApi(ApiClient.mensajeDe(r), r.estado);
    }

    static async eliminarCategoria(nombre) {
      const r = await ApiClient.solicitar("DELETE", "/api/categorias/" + encodeURIComponent(nombre));
      if (r.estado === 200 && r.datos) return r.datos.categorias;
      throw new ErrorApi(ApiClient.mensajeDe(r), r.estado);
    }
  }

  ObjetosApi.OBJETO_GUARDADO = "OBJETO_GUARDADO";

  App.front.api.ObjetosApi = ObjetosApi;
})(window.App);
