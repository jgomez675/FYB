/* Solicitudes que el estudiante ha enviado y su estado de revisión. */
(function (App) {
  "use strict";

  const { h } = App.front.components.dom;
  const { pantalla } = App.front.components.layout;
  const { ponerTitulo } = App.front.components.navigation;
  const { ObjetosApi } = App.front.api;

  class MisReclamacionesView {
    async mostrar({ usuario }) {
      ponerTitulo("Mis reclamaciones");

      const contenido = h("section", { class: "card" },
        h("div", { class: "section-heading" },
          h("h2", {}, "Solicitudes enviadas"),
          h("p", { class: "muted" }, "Consulta las solicitudes que has enviado y las respuestas de los propietarios.")
        )
      );
      const estadoCarga = h("p", { class: "status-line", role: "status" }, "Cargando tus solicitudes...");
      const lista = h("div", { class: "claim-list" });
      contenido.append(estadoCarga, lista);

      try {
        const reclamaciones = await ObjetosApi.obtenerReclamacionesEnviadas();
        estadoCarga.remove();

        if (!reclamaciones.length) {
          lista.append(h("div", { class: "empty" },
            h("h2", {}, "Todavía no has enviado reclamaciones"),
            h("p", {}, "Cuando solicites reclamar un objeto, podrás consultar aquí si el propietario la aprobó o rechazó."),
            h("a", { class: "btn btn-primary", href: "#/objetos" }, "Explorar objetos")
          ));
        } else {
          reclamaciones.forEach((r) => {
            const aprobada = r.estado === "APROBADA";
            const rechazada = r.estado === "RECHAZADA";
            const clase = aprobada ? "tag-ok" : (rechazada ? "tag-bad" : "tag-warn");
            const estado = aprobada ? "APROBADA" : (rechazada ? "RECHAZADA" : "PENDIENTE");
            const tarjeta = h("article", { class: "card claim-card" },
              h("div", { class: "obj-tags" }, h("span", { class: "tag " + clase }, estado)),
              h("h2", {}, r.objetoNombre || ("Objeto #" + r.objetoId)),
              h("p", { class: "muted" }, "Enviada: " + this.formatearFecha(r.creada)),
              h("p", {}, h("strong", {}, "Tu explicación: "), r.explicacion || ""),
              h("p", { class: "claim-owner" }, h("strong", {}, "Correo del propietario: "), r.propietario || "No disponible")
            );

            if (aprobada) {
              tarjeta.append(
                h("p", { class: "claim-status claim-status-approved", role: "status" },
                  "¡Tu reclamación fue aprobada! Ahora puedes contactar al propietario para acordar dónde y cuándo realizar la entrega."
                )
              );
              if (r.propietario && r.propietario.includes("@")) {
                const asunto = encodeURIComponent("Coordinación de entrega - " + (r.objetoNombre || "objeto reclamado"));
                const cuerpo = encodeURIComponent(
                  "Hola, vi que aprobaste mi reclamación en FYB. ¿Podemos coordinar el lugar y la hora para la entrega?\n\nGracias."
                );
                tarjeta.append(h("a", {
                  class: "btn btn-primary claim-contact",
                  href: "mailto:" + r.propietario + "?subject=" + asunto + "&body=" + cuerpo
                }, "Contactar al propietario por correo"));
              }
            } else if (rechazada) {
              tarjeta.append(h("p", { class: "claim-status claim-status-rejected", role: "status" },
                "El propietario rechazó esta reclamación."
              ));
            } else {
              tarjeta.append(h("p", { class: "claim-status", role: "status" },
                "Tu solicitud está pendiente de revisión por parte del propietario."
              ));
            }

            if (r.respuesta && r.respuesta.trim()) {
              tarjeta.append(h("div", { class: "claim-owner-message" },
                h("strong", {}, "Mensaje del propietario"),
                h("p", {}, r.respuesta)
              ));
            }
            lista.append(tarjeta);
          });
        }
      } catch (error) {
        estadoCarga.textContent = error.message || "No se pudieron cargar tus reclamaciones.";
        estadoCarga.className = "msg error";
      }

      return pantalla(
        usuario,
        h("section", { class: "fyb-subhero" },
          h("div", {},
            h("span", { class: "fyb-eyebrow" }, "Seguimiento"),
            h("h1", {}, "Mis reclamaciones"),
            h("p", { class: "lede" }, "Aquí verás cuándo el propietario revise tus solicitudes.")
          )
        ),
        contenido
      );
    }

    formatearFecha(valor) {
      if (!valor) return "Fecha no disponible";
      const fecha = new Date(valor);
      return Number.isNaN(fecha.getTime()) ? valor : fecha.toLocaleString("es-CO", {
        dateStyle: "medium",
        timeStyle: "short"
      });
    }
  }

  App.front.objetos.MisReclamacionesView = MisReclamacionesView;
})(window.App);
