/* Equivale a front/objetos/DetalleObjetoView.java */
(function (App) {
  "use strict";

  const { h, formatearFecha } = App.front.components.dom;
  const { pantalla, imagenObjeto } = App.front.components.layout;
  const { ponerTitulo } = App.front.components.navigation;
  const { ObjetosApi } = App.front.api;

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
            objeto.correoUsuario ? h("a", { class: "btn btn-primary", href: "mailto:" + objeto.correoUsuario + "?subject=" + asunto }, "Contactar a quien lo publicó") : null
          )
        )
      );
    }
  }

  App.front.objetos.DetalleObjetoView = DetalleObjetoView;
})(window.App);
