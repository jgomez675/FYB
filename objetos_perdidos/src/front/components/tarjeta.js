/* Tarjeta de un objeto, compartida por la lista de objetos y la de favoritos. */
(function (App) {
  "use strict";

  const { h, formatearFecha } = App.front.components.dom;
  const { imagenObjeto } = App.front.components.layout;
  const { botonFavorito } = App.front.components.favorito;

  // opciones: { alCambiarFavorito(objeto, favorito), alError(mensaje) }
  function tarjetaObjeto(objeto, opciones) {
    const { alCambiarFavorito, alError } = opciones || {};
    const resumen = objeto.descripcion.length > 100 ? objeto.descripcion.slice(0, 100).trimEnd() + "..." : objeto.descripcion;

    return h(
      "article",
      { class: "card obj" },
      h(
        "div",
        { class: "obj-top" },
        imagenObjeto(objeto, "obj-media"),
        botonFavorito(objeto, { alCambiar: alCambiarFavorito ? (favorito) => alCambiarFavorito(objeto, favorito) : null, alError })
      ),
      h(
        "div",
        { class: "obj-body" },
        h("h2", { class: "obj-name" }, objeto.nombre),
        objeto.propio ? h("span", { class: "badge" }, "Tu publicación") : null,
        h("p", { class: "obj-desc" }, resumen),
        h("p", { class: "obj-meta" }, h("span", { class: "k" }, "Lugar: "), objeto.lugar),
        h("p", { class: "obj-meta" }, h("span", { class: "k" }, "Fecha: "), formatearFecha(objeto.fecha)),
        h("a", { class: "btn btn-secondary", href: "#/objetos/" + objeto.id }, "Ver detalles")
      )
    );
  }

  App.front.components.tarjeta = { tarjetaObjeto };
})(window.App);
