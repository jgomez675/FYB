/* Centro de notificaciones: novedades de reclamaciones y devoluciones. */
(function (App) {
  "use strict";
  const { h } = App.front.components.dom;
  const { pantalla } = App.front.components.layout;
  const { ponerTitulo } = App.front.components.navigation;
  const { ApiClient } = App.front.api;

  class NotificacionesView {
    async mostrar({ usuario }) {
      ponerTitulo("Notificaciones");
      const estado = h("p", { class: "status-line", role: "status" }, "Cargando notificaciones...");
      const lista = h("div", { class: "notification-list" });
      try {
        const r = await ApiClient.solicitar("GET", "/api/notificaciones");
        if (r.estado < 200 || r.estado >= 300) throw new Error(ApiClient.mensajeDe(r));
        const items = (r.datos && r.datos.notificaciones) || [];
        estado.textContent = "";
        estado.className = "status-line notification-loading";
        if (!items.length) {
          lista.append(h("section", { class: "card notification-empty" },
            h("div", { class: "notification-empty-icon", "aria-hidden": "true" }, "✓"),
            h("h2", {}, "Estás al día"),
            h("p", {}, "Aquí aparecerán las nuevas reclamaciones, las decisiones del propietario y las devoluciones confirmadas.")
          ));
        } else {
          items.forEach(n => {
            const fecha = n.fecha ? new Date(n.fecha).toLocaleString("es-CO", { dateStyle: "medium", timeStyle: "short" }) : "Actualización";
            lista.append(h("a", { class: "card notification-card", href: n.ruta || "#/mis-reclamaciones" },
              h("span", { class: "notification-dot", "data-tipo": n.tipo || "", "aria-hidden": "true" }),
              h("div", { class: "notification-copy" },
                h("span", { class: "notification-type" }, ({ RECLAMACION_RECIBIDA: "Nueva reclamación", RECLAMACION_APROBADA: "Reclamación aprobada", RECLAMACION_RECHAZADA: "Reclamación rechazada", DEVOLUCION_CONFIRMADA: "Devolución confirmada" })[n.tipo] || "Actualización"),
                h("h2", {}, n.titulo || "Actualización"),
                h("p", {}, n.mensaje || ""),
                h("time", {}, fecha)
              ),
              h("span", { class: "notification-arrow", "aria-hidden": "true" }, "›")
            ));
          });
        }
      } catch (e) {
        estado.className = "msg error";
        estado.textContent = e.message || "No se pudieron cargar las notificaciones.";
      }
      return pantalla(usuario,
        h("section", { class: "fyb-subhero" },
          h("div", {},
            h("span", { class: "fyb-eyebrow" }, "Centro de actividad"),
            h("h1", {}, "Notificaciones"),
            h("p", { class: "lede" }, "Mantente al tanto de lo que sucede con tus publicaciones y reclamaciones.")
          )
        ),
        estado, lista
      );
    }
  }
  App.front.notificaciones = App.front.notificaciones || {};
  App.front.notificaciones.NotificacionesView = NotificacionesView;
})(window.App);
