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
      const buscar = h("input", {
        id: "buscar",
        type: "search",
        placeholder: "Buscar por nombre, lugar o descripción",
        "aria-label": "Buscar objetos"
      });

      const categoria = h("select", { class: "filter-select", "aria-label": "Filtrar por categoría" },
        h("option", { value: "" }, "Todas las categorías"),
        h("option", { value: "Tecnología" }, "Tecnología"),
        h("option", { value: "Documentos" }, "Documentos"),
        h("option", { value: "Accesorios" }, "Accesorios"),
        h("option", { value: "Ropa" }, "Ropa"),
        h("option", { value: "Llaves" }, "Llaves"),
        h("option", { value: "Otros" }, "Otros")
      );

      const filtroEstado = h("select", { class: "filter-select", "aria-label": "Filtrar por estado" },
        h("option", { value: "" }, "Todos los estados"),
        h("option", { value: "Perdido" }, "Perdidos"),
        h("option", { value: "Encontrado" }, "Encontrados")
      );

      const pintar = () => {
        lista.replaceChildren();

        if (!objetos.length) {
          estado.textContent = "";
          lista.append(h("div", { class: "empty" },
            h("p", {}, "Todavía no hay objetos publicados."),
            h("a", { class: "btn btn-primary", href: "#/publicar" }, "Publicar el primero")
          ));
          return;
        }

        const q = normalizar(buscar.value.trim());
        const categoriaSeleccionada = categoria.value;
        const estadoSeleccionado = filtroEstado.value;

        const items = objetos.filter((o) => {
          const texto = normalizar([o.nombre, o.descripcion, o.lugar].join(" "));
          const coincideTexto = !q || texto.includes(q);
          const coincideCategoria = !categoriaSeleccionada || o.categoria === categoriaSeleccionada;
          const coincideEstado = !estadoSeleccionado || o.estado === estadoSeleccionado;
          return coincideTexto && coincideCategoria && coincideEstado;
        });

        if (!items.length) {
          lista.append(h("div", { class: "empty" },
            h("p", {}, "No hay objetos que coincidan con los filtros seleccionados.")
          ));
        } else {
          items.forEach((o) => lista.append(this.crearTarjeta(o)));
        }

        estado.textContent = items.length === 1 ? "1 objeto" : items.length + " objetos";
      };

      buscar.addEventListener("input", pintar);
      categoria.addEventListener("change", pintar);
      filtroEstado.addEventListener("change", pintar);
      pintar();

      return pantalla(
        usuario,
        tomarAviso(),
        h("div", { class: "page-head" },
          h("div", {},
            h("h1", {}, "Objetos publicados"),
            h("p", { class: "lede" }, "Busca objetos, revisa sus fotos y filtra por categoría o estado.")
          ),
          h("a", { class: "btn btn-primary", href: "#/publicar" }, "Publicar objeto")
        ),
        objetos.length ? h("div", { class: "toolbar" }, buscar, categoria, filtroEstado, estado) : null,
        lista
      );
    }

    crearTarjeta(objeto) {
      const resumen = objeto.descripcion.length > 100
        ? objeto.descripcion.slice(0, 100).trimEnd() + "..."
        : objeto.descripcion;

      const estadoClase = objeto.estado === "Encontrado" ? "tag-found" : "tag-lost";

      return h("article", { class: "card obj" },
        imagenObjeto(objeto, "obj-media"),
        h("div", { class: "obj-body" },
          h("div", { class: "obj-tags" },
            h("span", { class: "tag tag-category" }, objeto.categoria || "Otros"),
            h("span", { class: "tag " + estadoClase }, objeto.estado || "Perdido")
          ),
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
