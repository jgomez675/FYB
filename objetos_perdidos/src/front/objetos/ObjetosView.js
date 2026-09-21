/* Equivale a front/objetos/ObjetosView.java */
(function (App) {
  "use strict";

  const { h, normalizar, formatearFecha } = App.front.components.dom;
  const { pantalla, imagenObjeto } = App.front.components.layout;
  const { tomarAviso, ponerTitulo } = App.front.components.navigation;
  const { ObjetosApi } = App.front.api;

  class ObjetosView {
    async mostrar({ usuario }) {
      ponerTitulo("Objetos perdidos");
      const objetos = await ObjetosApi.obtenerObjetos();

      const lista = h("div", { class: "grid" });
      const estado = h("p", { class: "status-line", "aria-live": "polite" });
      const buscar = h("input", { id: "buscar", type: "search", placeholder: "Buscar por nombre, lugar o descripción", "aria-label": "Buscar objetos" });

      const pintar = () => {
        lista.replaceChildren();

        if (!objetos.length) {
          estado.textContent = "";
          lista.append(
            h("div", { class: "empty" }, h("p", {}, "Todavía no hay objetos publicados."), h("a", { class: "btn btn-primary", href: "#/publicar" }, "Publicar el primero"))
          );
          return;
        }

        const q = normalizar(buscar.value.trim());
        const items = q ? objetos.filter((o) => normalizar([o.nombre, o.descripcion, o.lugar].join(" ")).includes(q)) : objetos;

        if (!items.length) {
          lista.append(h("div", { class: "empty" }, h("p", {}, "No hay resultados para “" + buscar.value.trim() + "”.")));
        } else {
          items.forEach((o) => lista.append(this.crearTarjeta(o)));
        }
        estado.textContent = items.length === 1 ? "1 objeto" : items.length + " objetos";
      };

      buscar.addEventListener("input", pintar);
      pintar();

      return pantalla(
        usuario,
        tomarAviso(),
        h(
          "div",
          { class: "page-head" },
          h("div", {}, h("h1", {}, "Objetos perdidos"), h("p", { class: "lede" }, "Revisa las fotos y descripciones de los objetos publicados")),
          h("a", { class: "btn btn-primary", href: "#/publicar" }, "Publicar objeto")
        ),
        objetos.length ? h("div", { class: "toolbar" }, buscar, estado) : null,
        lista
      );
    }

    crearTarjeta(objeto) {
      const resumen = objeto.descripcion.length > 100 ? objeto.descripcion.slice(0, 100).trimEnd() + "..." : objeto.descripcion;

      return h(
        "article",
        { class: "card obj" },
        imagenObjeto(objeto, "obj-media"),
        h(
          "div",
          { class: "obj-body" },
          h("h2", { class: "obj-name" }, objeto.nombre),
          h("p", { class: "obj-desc" }, resumen),
          h("p", { class: "obj-meta" }, h("span", { class: "k" }, "Lugar: "), objeto.lugar),
          h("p", { class: "obj-meta" }, h("span", { class: "k" }, "Fecha: "), formatearFecha(objeto.fecha)),
          h("a", { class: "btn btn-secondary", href: "#/objetos/" + objeto.id }, "Ver detalles")
        )
      );
    }
  }

  App.front.objetos.ObjetosView = ObjetosView;
})(window.App);
