/* Equivale a front/objetos/DetalleObjetoView.java */
(function (App) {
  "use strict";

  const { h, formatearFecha } = App.front.components.dom;
  const { pantalla, imagenObjeto } = App.front.components.layout;
  const { ponerTitulo } = App.front.components.navigation;
  const { ObjetosApi } = App.front.api;
  const { ApiClient } = App.front.api;

  class DetalleObjetoView {
    async mostrar({ usuario, params }) {
      const objeto = await ObjetosApi.obtenerObjeto(params[0]);

      if (!objeto) {
        ponerTitulo("Objeto no encontrado");
        return pantalla(
          usuario,
          h("section", { class: "card notice" }, h("h1", {}, "No encontramos este objeto"), h("p", {}, "Puede que ya no esté publicado."), h("a", { class: "btn btn-primary", href: "#/objetos" }, "Volver a objetos"))
        );
      }

      ponerTitulo(objeto.nombre);
      const asunto = encodeURIComponent(App.config.name + ": " + objeto.nombre);
      const acciones = [];
      if (objeto.correoUsuario === usuario.correo || usuario.rol === "admin") acciones.push(h("a", { class: "btn btn-secondary", href: "#/editar/" + objeto.id }, "Editar publicación"));
      const puntosMensaje = h("p", { class: "msg", role: "status" });
      const ganarPuntos = h("button", { class: "btn btn-primary", type: "button" }, "Confirmar devolución y ganar puntos");
      ganarPuntos.addEventListener("click", async () => {
        ganarPuntos.disabled = true;
        try {
          const r = await ApiClient.solicitar("POST", "/api/devoluciones/confirmar", { objetoId: objeto.id });
          if (r.estado === 200) puntosMensaje.textContent = "¡Ganaste " + r.datos.puntosGanados + " puntos! Tu saldo es de " + r.datos.saldo + " puntos.";
          else puntosMensaje.textContent = (r.datos && r.datos.mensaje) || "No se pudieron asignar los puntos.";
        } catch (e) { puntosMensaje.textContent = e.message; }
        finally { ganarPuntos.disabled = false; }
      });

      return pantalla(
        usuario,
        h("a", { class: "back", href: "#/objetos" }, "Volver a objetos"),
        h(
          "article",
          { class: "card detail" },
          imagenObjeto(objeto, "detail-media"),
          h(
            "div",
            { class: "detail-body" },
            h("h1", {}, objeto.nombre),
            h("div", {}, h("h2", { class: "sub" }, "Descripción"), h("p", { class: "prewrap" }, objeto.descripcion)),
            h(
              "dl",
              { class: "facts" },
              h("div", {}, h("dt", {}, "Lugar"), h("dd", {}, objeto.lugar)),
              h("div", {}, h("dt", {}, "Fecha"), h("dd", {}, formatearFecha(objeto.fecha)))
            ),
            h("p", { class: "obj-meta" }, h("span", { class: "k" }, "Categoría: "), objeto.categoria || "Otros"),
            objeto.correoUsuario ? h("a", { class: "btn btn-primary", href: "mailto:" + objeto.correoUsuario + "?subject=" + asunto }, "Contactar a quien lo publicó") : null,
            ...acciones,
            objeto.correoUsuario !== usuario.correo ? h("div", { class: "points-box" }, h("p", {}, "Si ayudaste a devolver este objeto, puedes confirmar la devolución y recibir automáticamente entre 5 y 25 puntos."), ganarPuntos, puntosMensaje) : null
          )
        )
      );
    }
  }

  App.front.objetos.DetalleObjetoView = DetalleObjetoView;
})(window.App);
