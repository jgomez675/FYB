/* Panel administrativo: estadísticas, moderación de reportes y gestión de usuarios. */
(function (App) {
  "use strict";
  const { h } = App.front.components.dom;
  const { pantalla } = App.front.components.layout;
  const { ponerTitulo } = App.front.components.navigation;
  const { ApiClient } = App.front.api;

  class AdminView {
    async mostrar({ usuario }) {
      ponerTitulo("Administración");
      const aviso = h("p", { class: "msg", role: "status" });
      const estadisticas = h("div", { class: "admin-stats" });
      const reportes = h("section", { class: "card admin-card" });
      const usuarios = h("section", { class: "card admin-card" });

      const pedir = async (metodo, ruta, cuerpo) => {
        const r = await ApiClient.solicitar(metodo, ruta, cuerpo);
        if (r.estado < 200 || r.estado >= 300) throw new Error(ApiClient.mensajeDe(r));
        return r.datos || {};
      };

      const cargar = async () => {
        aviso.textContent = "";
        try {
          const [resumen, datosReportes, datosUsuarios] = await Promise.all([
            pedir("GET", "/api/admin/resumen"),
            pedir("GET", "/api/admin/reportes"),
            pedir("GET", "/api/admin/usuarios")
          ]);
          estadisticas.replaceChildren(
            ...[
              ["Objetos publicados", resumen.totalObjetos],
              ["Objetos encontrados", resumen.encontrados],
              ["Objetos devueltos", resumen.devueltos],
              ["Reportes pendientes", resumen.reportesPendientes]
            ].map(([titulo, valor]) => h("article", { class: "card admin-stat" },
              h("span", { class: "admin-stat-label" }, titulo),
              h("strong", {}, String(valor))
            ))
          );

          const listaReportes = (datosReportes.reportes || []).map(r => {
            const tarjeta = h("article", { class: "admin-row" },
              h("div", { class: "admin-row-main" },
                h("h3", {}, "#" + r.id + " · " + r.nombreObjeto),
                h("p", {}, "Motivo: " + r.motivo),
                h("p", {}, "Detalle: " + (r.detalle || "Sin detalle adicional")),
                h("p", { class: "admin-muted" }, "Reportado por: " + r.correoReportante + " · " + r.fecha),
                h("span", { class: "tag " + (r.estado === "PENDIENTE" ? "tag-lost" : "tag-found") }, r.estado)
              )
            );
            if (r.estado === "PENDIENTE") {
              const ocultar = h("button", { class: "btn btn-primary", type: "button" }, "Ocultar publicación");
              const descartar = h("button", { class: "btn btn-secondary", type: "button" }, "Descartar reporte");
              ocultar.addEventListener("click", () => decidir(r.id, "OCULTAR"));
              descartar.addEventListener("click", () => decidir(r.id, "DESCARTAR"));
              tarjeta.append(h("div", { class: "actions" }, ocultar, descartar));
            }
            return tarjeta;
          });
          reportes.replaceChildren(
            h("h2", {}, "Reportes de publicaciones"),
            h("p", { class: "admin-muted" }, "Revisa el motivo y el detalle antes de tomar una decisión."),
            ...(listaReportes.length ? listaReportes : [h("p", { class: "empty admin-empty" }, "Todavía no hay reportes.")])
          );

          const filas = (datosUsuarios.usuarios || []).map(u => {
            const fila = h("article", { class: "admin-row admin-user" },
              h("div", { class: "admin-row-main" },
                h("h3", {}, u.nombre),
                h("p", {}, u.correo),
                h("span", { class: "tag " + (u.bloqueado ? "tag-lost" : "tag-found") }, u.bloqueado ? "Bloqueado" : "Activo")
              )
            );
            if (u.correo.toLowerCase() !== usuario.correo.toLowerCase()) {
              const boton = h("button", { class: "btn " + (u.bloqueado ? "btn-secondary" : "btn-primary"), type: "button" },
                u.bloqueado ? "Desbloquear" : "Bloquear cuenta");
              boton.addEventListener("click", async () => {
                if (u.bloqueado || confirm("¿Seguro que deseas bloquear esta cuenta?")) {
                  try {
                    await pedir("POST", "/api/admin/usuarios/bloqueo", { correo: u.correo, bloqueado: !u.bloqueado });
                    aviso.textContent = u.bloqueado ? "Cuenta desbloqueada." : "Cuenta bloqueada correctamente.";
                    await cargar();
                  } catch (e) { aviso.textContent = e.message; }
                }
              });
              fila.append(boton);
            }
            return fila;
          });
          usuarios.replaceChildren(
            h("h2", {}, "Usuarios registrados"),
            h("p", { class: "admin-muted" }, "Puedes bloquear cuentas que estén haciendo un uso inadecuado de la plataforma."),
            ...(filas.length ? filas : [h("p", { class: "empty admin-empty" }, "No hay usuarios registrados.")])
          );
        } catch (e) {
          aviso.textContent = e.message;
        }
      };

      const decidir = async (id, decision) => {
        const pregunta = decision === "OCULTAR"
          ? "La publicación será retirada del listado. ¿Continuar?"
          : "El reporte se marcará como descartado y la publicación seguirá visible. ¿Continuar?";
        if (!confirm(pregunta)) return;
        try {
          await pedir("POST", "/api/admin/reportes/" + id + "/decision", { decision });
          aviso.textContent = decision === "OCULTAR" ? "Publicación ocultada." : "Reporte descartado.";
          await cargar();
        } catch (e) { aviso.textContent = e.message; }
      };

      const recargar = h("button", { class: "btn btn-secondary", type: "button" }, "Actualizar información");
      recargar.addEventListener("click", cargar);
      await cargar();
      return pantalla(usuario, null,
        h("div", { class: "page-head" },
          h("div", {}, h("h1", {}, "Panel de administración"),
            h("p", { class: "lede" }, "Estadísticas, reportes y control de usuarios.")),
          recargar
        ),
        aviso, estadisticas, reportes, usuarios
      );
    }
  }
  App.front.admin = App.front.admin || {};
  App.front.admin.AdminView = AdminView;
})(window.App);
