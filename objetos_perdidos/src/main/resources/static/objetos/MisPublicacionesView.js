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
      const reclamaciones = await ObjetosApi.obtenerReclamacionesRecibidas();
      const panelReclamaciones = h("section", { class: "card claim-inbox-panel", style: "margin-bottom:1.5rem" },
        h("div", { class: "section-heading" }, h("h2", {}, "Solicitudes de reclamación"), h("p", { class: "muted" }, "Revisa las explicaciones de quienes dicen reconocer tus objetos."))
      );
      if (!reclamaciones.length) panelReclamaciones.append(h("p", { class: "muted" }, "Todavía no has recibido solicitudes."));
      reclamaciones.forEach(r => {
        const bloque = h("article", { class: "card claim-inbox-card", style: "margin-top:1rem" },
          h("h3", {}, r.objetoNombre),
          h("p", {}, "Solicitante: " + r.solicitante),
          h("p", {}, r.explicacion),
          h("p", { class: "tag " + (r.estado === "APROBADA" ? "tag-ok" : (r.estado === "RECHAZADA" ? "tag-bad" : "tag-warn")) }, r.estado)
        );
        if (r.estado === "PENDIENTE") {
          const respuesta = h("textarea", { class: "control", rows: "2", placeholder: "Mensaje opcional para el estudiante" });
          const aprobar = h("button", { class: "btn btn-primary", type: "button" }, "Aprobar");
          const rechazar = h("button", { class: "btn btn-secondary", type: "button" }, "Rechazar");
          const aviso = h("p", { class: "msg", role: "alert" });
          const decidir = async (decision, boton) => {
            boton.disabled = true;
            const original = boton.textContent;
            boton.textContent = "Guardando...";
            try {
              const out = await ObjetosApi.decidirReclamacion(r.id, decision, respuesta.value);
              if (out.ok) {
                aviso.className = "msg success";
                aviso.textContent = out.mensaje;
                aprobar.remove(); rechazar.remove();
              } else {
                aviso.className = "msg error";
                aviso.textContent = out.mensaje || "No se pudo guardar la decisión.";
                boton.disabled = false; boton.textContent = original;
              }
            } catch (e) {
              aviso.className = "msg error";
              aviso.textContent = e.message || "Ocurrió un error.";
              boton.disabled = false; boton.textContent = original;
            }
          };
          aprobar.addEventListener("click",()=>decidir("APROBADA",aprobar));
          rechazar.addEventListener("click",()=>decidir("RECHAZADA",rechazar));
          bloque.append(respuesta,h("div",{class:"actions"},aprobar,rechazar),aviso);
        }
        panelReclamaciones.append(bloque);
      });

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
        h("option", { value: "EN_CONTACTO" }, "En contacto"),
        h("option", { value: "PENDIENTE_ENTREGA" }, "Pendiente de entrega"),
        h("option", { value: "DEVUELTO" }, "Devueltos")
      );

      const pintar = () => {
        lista.replaceChildren();

        if (!objetos.length) {
          resumen.textContent = "";
          lista.append(h("div", { class: "empty fyb-empty-state" },
            h("div", { class: "fyb-empty-icon", "aria-hidden": "true" }, "⌕"),
            h("span", { class: "fyb-eyebrow fyb-eyebrow-muted" }, "Tu espacio FYB"),
            h("h2", {}, "Aquí comenzará tu historial."),
            h("p", {}, "Cuando publiques un objeto perdido o encontrado, podrás seguir su estado desde esta sección."),
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
          items.forEach((o) => lista.append(this.crearTarjeta(o, pintar, reclamaciones)));
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
        h("section", { class: "fyb-subhero" },
          h("div", {},
            h("span", { class: "fyb-eyebrow" }, "Panel personal"),
            h("h1", {}, "Mis publicaciones"),
            h("p", { class: "lede" }, "Todos los objetos que has reportado, con su estado actual.")
          ),
          h("a", { class: "btn btn-primary", href: "#/publicar" }, "＋  Publicar objeto")
        ),
        panelReclamaciones,
        objetos.length ? h("div", { class: "toolbar fyb-toolbar" }, buscar, filtroEstado, resumen) : null,
        lista
      );
    }

    async cambiarEstado(objeto, estado, boton, tarjeta, pintar) {
      boton.disabled = true;
      const textoOriginal = boton.textContent;
      boton.textContent = "Guardando...";
      try {
        const resultado = await ObjetosApi.actualizarEstado(objeto.id, estado);
        if (!resultado.ok) throw new Error(resultado.mensaje || "No se pudo actualizar el estado.");
        objeto.estado = resultado.estado || estado;
        pintar();
      } catch (error) {
        boton.disabled = false;
        boton.textContent = textoOriginal;
        const aviso = tarjeta.querySelector(".status-line") || h("p", { class: "status-line", role: "alert" });
        aviso.textContent = error.message || "No se pudo actualizar el estado.";
        if (!aviso.parentElement) tarjeta.append(aviso);
      }
    }

    crearTarjeta(objeto, pintar, reclamaciones = []) {
      const descripcion = objeto.descripcion || "";
      const texto = descripcion.length > 100
        ? descripcion.slice(0, 100).trimEnd() + "..."
        : descripcion;

      const cerrado = objeto.estado === "Encontrado" || objeto.estado === "DEVUELTO" || objeto.estado === "PENDIENTE_ENTREGA";
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
          h("p", { class: "obj-desc" }, texto),
          h("p", { class: "obj-meta" }, h("span", { class: "k" }, "Lugar: "), objeto.lugar),
          h("p", { class: "obj-meta" }, h("span", { class: "k" }, "Fecha: "), formatearFecha(objeto.fecha)),
          h("a", { class: "btn btn-secondary", href: "#/objetos/" + objeto.id }, "Ver detalles"),
          this.controlesEstado(objeto, pintar, reclamaciones)
        )
      );
    }

    controlesEstado(objeto, pintar, reclamaciones = []) {
      if (objeto.estado === "DEVUELTO") {
        return h("p", { class: "claim-status claim-status-approved" }, "Devolución confirmada. Esta publicación ya está cerrada.");
      }
      const acciones = h("div", { class: "publication-actions" });
      const tarjeta = acciones;
      const opciones = [
        ["EN_CONTACTO", "Marcar: En contacto"],
        ["PENDIENTE_ENTREGA", "Marcar: Pendiente de entrega"],
        ["Encontrado", "Volver a disponible"]
      ];
      opciones.forEach(([estado, etiqueta]) => {
        if (objeto.estado === estado) return;
        const boton = h("button", { class: "btn btn-secondary", type: "button" }, etiqueta);
        boton.addEventListener("click", () => this.cambiarEstado(objeto, estado, boton, tarjeta, pintar));
        acciones.append(boton);
      });

      const aprobada = reclamaciones.some(r => Number(r.objetoId) === Number(objeto.id) && r.estado === "APROBADA");
      if (aprobada) {
        const confirmar = h("button", { class: "btn btn-success", type: "button" }, "✓ Confirmar devolución");
        const aviso = h("p", { class: "status-line", role: "status" });
        confirmar.addEventListener("click", async () => {
          if (!confirm("¿Confirmas que el objeto ya fue entregado a su dueño? Esta acción otorgará los puntos y no se puede deshacer.")) return;
          confirmar.disabled = true;
          confirmar.textContent = "Confirmando...";
          try {
            const resultado = await ObjetosApi.confirmarDevolucion(objeto.id);
            if (!resultado.ok || !resultado.confirmado) throw new Error(resultado.mensaje || "No se pudo confirmar la devolución.");
            objeto.estado = "DEVUELTO";
            aviso.className = "claim-status claim-status-approved";
            aviso.textContent = "¡Devolución confirmada! Ganaste " + resultado.puntosGanados + " puntos.";
            pintar();
          } catch (e) {
            aviso.className = "msg error";
            aviso.textContent = e.message || "No se pudo confirmar la devolución.";
            confirmar.disabled = false;
            confirmar.textContent = "✓ Confirmar devolución";
          }
        });
        acciones.append(confirmar, aviso);
      }
      return acciones;
    }
  }

  App.front.objetos.MisPublicacionesView = MisPublicacionesView;
})(window.App);
