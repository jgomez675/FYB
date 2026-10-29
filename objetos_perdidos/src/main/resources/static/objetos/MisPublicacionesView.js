/* HU: consultar todas las publicaciones que el estudiante ha realizado. */
(function (App) {
  "use strict";

  const { h, normalizar, formatearFecha } = App.front.components.dom;
  const { pantalla, imagenObjeto } = App.front.components.layout;
  const { tomarAviso, ponerTitulo } = App.front.components.navigation;
  const { ObjetosApi } = App.front.api;

  class MisPublicacionesView {
    async mostrar({ usuario }) {
      ponerTitulo("Mis publicaciones");
      const objetos = await ObjetosApi.obtenerMisPublicaciones();

      const lista = h("div", { class: "grid" });
      const resumen = h("p", { class: "status-line", "aria-live": "polite" });

      const buscar = h("input", {
        type: "search",
        placeholder: "Buscar en mis publicaciones",
        "aria-label": "Buscar en mis publicaciones"
      });

      const filtroEstado = h("select", { class: "filter-select", "aria-label": "Filtrar por estado" },
        h("option", { value: "" }, "Todos los estados"),
        h("option", { value: "Perdido" }, "Perdidos"),
        h("option", { value: "Encontrado" }, "Encontrados"),
        h("option", { value: "DEVUELTO" }, "Devueltos")
      );

      const pintar = () => {
        lista.replaceChildren();

        if (!objetos.length) {
          resumen.textContent = "";
          lista.append(h("div", { class: "empty" },
            h("p", {}, "Aún no has publicado ningún objeto."),
            h("a", { class: "btn btn-primary", href: "#/publicar" }, "Publicar mi primer objeto")
          ));
          return;
        }

        const q = normalizar(buscar.value.trim());
        const estado = filtroEstado.value;

        const items = objetos.filter((o) => {
          const texto = normalizar([o.nombre, o.descripcion, o.lugar].join(" "));
          return (!q || texto.includes(q)) && (!estado || o.estado === estado);
        });

        if (!items.length) {
          lista.append(h("div", { class: "empty" },
            h("p", {}, "Ninguna de tus publicaciones coincide con los filtros.")
          ));
        } else {
          items.forEach((o) => lista.append(this.crearTarjeta(o)));
        }

        resumen.textContent =
          items.length === 1 ? "1 publicación" : items.length + " publicaciones";
      };

      buscar.addEventListener("input", pintar);
      filtroEstado.addEventListener("change", pintar);
      pintar();

      return pantalla(
        usuario,
        tomarAviso(),
        h("div", { class: "page-head" },
          h("div", {},
            h("h1", {}, "Mis publicaciones"),
            h("p", { class: "lede" }, "Todos los objetos que has reportado, con su estado actual.")
          ),
          h("a", { class: "btn btn-primary", href: "#/publicar" }, "Publicar objeto")
        ),
        objetos.length ? h("div", { class: "toolbar" }, buscar, filtroEstado, resumen) : null,
        lista
      );
    }

    crearTarjeta(objeto) {
      const descripcion = objeto.descripcion || "";
      const texto = descripcion.length > 100
        ? descripcion.slice(0, 100).trimEnd() + "..."
        : descripcion;

      const cerrado = objeto.estado === "Encontrado" || objeto.estado === "DEVUELTO";
      const estadoClase = cerrado ? "tag-found" : "tag-lost";
      const estadoTexto = objeto.estado === "DEVUELTO" ? "Devuelto" : (objeto.estado || "Perdido");

      return h("article", { class: "card obj" },
        imagenObjeto(objeto, "obj-media"),
        h("div", { class: "obj-body" },
          h("div", { class: "obj-tags" },
            h("span", { class: "tag tag-category" }, objeto.categoria || "Otros"),
            h("span", { class: "tag " + estadoClase }, estadoTexto)
          ),
          h("h2", { class: "obj-name" }, objeto.nombre),
          h("p", { class: "obj-desc" }, texto),
          h("p", { class: "obj-meta" }, h("span", { class: "k" }, "Lugar: "), objeto.lugar),
          h("p", { class: "obj-meta" }, h("span", { class: "k" }, "Fecha: "), formatearFecha(objeto.fecha)),
          h("a", { class: "btn btn-secondary", href: "#/objetos/" + objeto.id }, "Ver detalles")
        )
      );
    }
  }

  App.front.objetos.MisPublicacionesView = MisPublicacionesView;
})(window.App);
