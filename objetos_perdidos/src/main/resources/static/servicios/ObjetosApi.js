/* Cliente de ObjetoPerdidoService.java. */
(function (App) {
  "use strict";

  const { ApiClient, ErrorApi } = App.front.api;

  class ObjetosApi {

    static async obtenerObjetos() {
      const r = await ApiClient.solicitar(
        "GET",
        "/api/objetos"
      );

      if (r.estado === 200 && r.datos) {
        return r.datos.objetos;
      }

      throw new ErrorApi(
        ApiClient.mensajeDe(r),
        r.estado
      );
    }

    // Devuelve el objeto, o null si no existe.
    static async obtenerObjeto(id) {
      const r = await ApiClient.solicitar(
        "GET",
        "/api/objetos/" + encodeURIComponent(id)
      );

      if (r.estado === 200 && r.datos) {
        return r.datos.objeto;
      }

      if (r.estado === 404) {
        return null;
      }

      throw new ErrorApi(
        ApiClient.mensajeDe(r),
        r.estado
      );
    }

    // Guarda el objeto incluyendo la característica privada.
    static async guardarObjeto(
      nombre,
      descripcion,
      lugar,
      fecha,
      archivoImagen,
      caracteristicaPrivada
    ) {
      let imagen = "";

      if (archivoImagen) {
        try {
          imagen =
            await App.front.components.imagen.procesarImagen(
              archivoImagen
            );
        } catch (e) {
          return e.message;
        }
      }

      const r = await ApiClient.solicitar(
        "POST",
        "/api/objetos",
        {
          nombre,
          descripcion,
          lugar,
          fecha,
          imagen,
          caracteristicaPrivada
        }
      );

      if (
        r.estado === 201 &&
        r.datos &&
        r.datos.ok
      ) {
        return ObjetosApi.OBJETO_GUARDADO;
      }

      if (r.estado === 401) {
        throw new ErrorApi(
          ApiClient.mensajeDe(r),
          401
        );
      }

      return ApiClient.mensajeDe(r);
    }

    // Verifica la característica privada del objeto.
    static async verificarCaracteristica(
      id,
      caracteristica
    ) {
      const r = await ApiClient.solicitar(
        "POST",
        "/api/objetos/" +
          encodeURIComponent(id) +
          "/verificar",
        {
          caracteristica
        }
      );

      if (r.estado === 200 && r.datos) {
        return r.datos;
      }

      if (r.estado === 401) {
        throw new ErrorApi(
          ApiClient.mensajeDe(r),
          401
        );
      }

      return {
        ok: false,
        verificado: false,
        mensaje: ApiClient.mensajeDe(r)
      };
    }
  }

  ObjetosApi.OBJETO_GUARDADO =
    "OBJETO_GUARDADO";

  App.front.api.ObjetosApi =
    ObjetosApi;

})(window.App);