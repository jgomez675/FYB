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

      const perdidos = objetos.filter((o) => o.estado === "Perdido").length;
      const encontrados = objetos.filter((o) => o.estado === "Encontrado").length;

      const portada = h("section", { class: "fyb-hero" },
        h("div", { class: "fyb-hero-copy" },
          h("span", { class: "fyb-eyebrow" }, "Conectamos personas con lo que importa"),
          h("h1", {}, "Un objeto puede volver a encontrar su dueño."),
          h("p", { class: "fyb-hero-lede" }, "Explora los reportes de la comunidad universitaria o publica lo que perdiste o encontraste. Entre todos hacemos que vuelva a casa."),
          h("div", { class: "fyb-hero-actions" },
            h("a", { class: "btn btn-primary", href: "#/publicar" }, "＋  Reportar un objeto"),
            h("a", { class: "btn fyb-btn-outline", href: "#/mis-publicaciones" }, "Mis publicaciones")
          )
        ),
        h("div", { class: "fyb-hero-orbit", "aria-hidden": "true" },
          h("div", { class: "fyb-orbit-ring fyb-ring-one" }),
          h("div", { class: "fyb-orbit-ring fyb-ring-two" }),
          h("div", { class: "fyb-orbit-core" },
            h("span", { class: "fyb-orbit-symbol" }, "⌕"),
            h("span", { class: "fyb-orbit-label" }, "FYB")
          ),
          h("span", { class: "fyb-orbit-dot fyb-dot-one" }),
          h("span", { class: "fyb-orbit-dot fyb-dot-two" }),
          h("span", { class: "fyb-orbit-dot fyb-dot-three" })
        ),
        h("div", { class: "fyb-hero-stats" },
          h("div", { class: "fyb-stat" }, h("strong", {}, String(objetos.length)), h("span", {}, "Publicaciones")),
          h("div", { class: "fyb-stat" }, h("strong", {}, String(perdidos)), h("span", {}, "Reportados como perdidos")),
          h("div", { class: "fyb-stat" }, h("strong", {}, String(encontrados)), h("span", {}, "Encontrados"))
        )
      );

      const encabezadoListado = h("div", { class: "fyb-section-heading" },
        h("div", {},
          h("span", { class: "fyb-eyebrow fyb-eyebrow-muted" }, "Comunidad FYB"),
          h("h2", {}, "Explorar publicaciones"),
          h("p", {}, "Encuentra coincidencias usando el buscador y los filtros.")
        ),
        estado
      );

      return pantalla(
        usuario,
        tomarAviso(),
        portada,
        encabezadoListado,
        objetos.length ? h("div", { class: "toolbar fyb-toolbar" }, buscar, categoria, filtroEstado) : null,
        lista
      );
    }

    crearTarjeta(objeto) {
      const resumen = objeto.descripcion.length > 100
        ? objeto.descripcion.slice(0, 100).trimEnd() + "..."
        : objeto.descripcion;

      const ESTADOS = { Perdido: ["tag-bad", "Perdido"], Encontrado: ["tag-ok", "Encontrado"], EN_CONTACTO: ["tag-info", "En contacto"], PENDIENTE_ENTREGA: ["tag-warn", "Pendiente de entrega"], DEVUELTO: ["tag-neutral", "Devuelto"] };
      const [estadoClase, estadoTexto] = ESTADOS[objeto.estado] || ESTADOS.Perdido;

      return h("article", { class: "card obj" },
        imagenObjeto(objeto, "obj-media"),
        h("div", { class: "obj-body" },
          h("div", { class: "obj-tags" },
            h("span", { class: "tag tag-category" }, objeto.categoria || "Otros"),
            h("span", { class: "tag " + estadoClase }, estadoTexto)
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
