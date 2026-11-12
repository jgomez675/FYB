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
      const objetos = h("section", { class: "card admin-card" });
      const busquedaObjetos = h("input", {
        class: "admin-filter-input",
        type: "search",
        placeholder: "Buscar por nombre, lugar o usuario",
        "aria-label": "Buscar publicaciones"
      });
      const categoriaObjetos = h("select", {
        class: "admin-filter-input",
        "aria-label": "Filtrar por categoría"
      },
        h("option", { value: "" }, "Todas las categorías"),
        ...["Tecnología", "Documentos", "Accesorios", "Ropa", "Llaves", "Otros"]
          .map(c => h("option", { value: c }, c))
      );
      const estadoObjetos = h("select", {
        class: "admin-filter-input",
        "aria-label": "Filtrar por estado"
      },
        h("option", { value: "" }, "Todos los estados"),
        ...["Perdido", "Encontrado", "DEVUELTO"].map(e => h("option", { value: e }, e))
      );
      const listaObjetos = h("div", { class: "admin-object-list" });

      const pedir = async (metodo, ruta, cuerpo) => {
        const r = await ApiClient.solicitar(metodo, ruta, cuerpo);
        if (r.estado < 200 || r.estado >= 300) throw new Error(ApiClient.mensajeDe(r));
        return r.datos || {};
      };

      const cargar = async () => {
        aviso.textContent = "";
        try {
          const [resumen, datosReportes, datosUsuarios, datosObjetos] = await Promise.all([
            pedir("GET", "/api/admin/resumen"),
            pedir("GET", "/api/admin/reportes"),
            pedir("GET", "/api/admin/usuarios"),
            pedir("GET", rutaObjetos())
          ]);
          pintarObjetos(datosObjetos.objetos || []);
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
          const eliminarCuentas = h("button", { class: "btn btn-danger", type: "button" }, "Eliminar todas las cuentas de estudiantes");
          eliminarCuentas.addEventListener("click", async () => {
            const confirmar = confirm(
              "ATENCIÓN: se eliminarán todas las cuentas de estudiantes y sus puntos e historiales. " +
              "La cuenta administradora " + usuario.correo + " se conservará. Esta acción no se puede deshacer. ¿Continuar?"
            );
            if (!confirmar) return;
            const frase = prompt('Para confirmar, escribe exactamente: ELIMINAR TODAS LAS CUENTAS');
            if (frase !== "ELIMINAR TODAS LAS CUENTAS") {
              aviso.textContent = "Operación cancelada. No se eliminaron cuentas.";
              return;
            }
            eliminarCuentas.disabled = true;
            try {
              const resultado = await pedir("DELETE", "/api/admin/usuarios/todos-excepto-admin");
              aviso.textContent = resultado.mensaje || "Se eliminaron las cuentas de estudiantes. La cuenta administradora sigue activa.";
              await cargar();
            } catch (e) {
              aviso.textContent = e.message;
            } finally {
              eliminarCuentas.disabled = false;
            }
          });
          usuarios.replaceChildren(
            h("h2", {}, "Usuarios registrados"),
            h("p", { class: "admin-muted" }, "Puedes bloquear cuentas o eliminar todas las cuentas estudiantiles. La cuenta administradora siempre se conserva."),
            h("div", { class: "admin-bulk-actions" }, eliminarCuentas),
            ...(filas.length ? filas : [h("p", { class: "empty admin-empty" }, "No hay usuarios registrados.")])
          );
        } catch (e) {
          aviso.textContent = e.message;
        }
      };

      const rutaObjetos = () => {
        const params = new URLSearchParams();
        if (categoriaObjetos.value) params.set("categoria", categoriaObjetos.value);
        if (estadoObjetos.value) params.set("estado", estadoObjetos.value);
        if (busquedaObjetos.value.trim()) params.set("q", busquedaObjetos.value.trim());
        const query = params.toString();
        return "/api/admin/objetos" + (query ? "?" + query : "");
      };

      const pintarObjetos = (elementos) => {
        const filas = elementos.map(o => {
          const contenido = h("div", { class: "admin-object-content" },
            h("div", { class: "admin-object-heading" },
              h("h3", {}, "#" + o.id + " · " + o.nombre),
              h("span", { class: "tag " + (o.oculto ? "tag-lost" : "tag-found") },
                o.oculto ? "Oculto" : "Visible")
            ),
            h("p", {}, o.descripcion || "Sin descripción"),
            h("p", { class: "admin-muted" },
              "Categoría: " + (o.categoria || "Otros") + " · Estado: " + (o.estado || "Sin estado")),
            h("p", { class: "admin-muted" },
              "Lugar: " + (o.lugar || "No especificado") + " · Fecha: " + (o.fecha || "Sin fecha")),
            h("p", { class: "admin-muted" }, "Publicado por: " + (o.correoUsuario || "Sin correo"))
          );
          const miniatura = o.imagenUrl
            ? h("img", { class: "admin-object-thumb", src: o.imagenUrl, alt: "Foto de " + o.nombre, loading: "lazy" })
            : null;
          const tarjeta = h("article", { class: "admin-row admin-object-row" }, miniatura, contenido);
          const eliminar = h("button", {
            class: "btn btn-danger",
            type: "button"
          }, "Eliminar publicación");
          eliminar.addEventListener("click", async () => {
            if (!confirm("¿Eliminar definitivamente la publicación #" + o.id + " (" + o.nombre + ")? Esta acción no se puede deshacer.")) return;
            eliminar.disabled = true;
            try {
              await pedir("DELETE", "/api/admin/objetos/" + o.id);
              aviso.textContent = "La publicación #" + o.id + " fue eliminada.";
              await cargar();
            } catch (e) {
              aviso.textContent = e.message;
              eliminar.disabled = false;
            }
          });
          tarjeta.append(eliminar);
          return tarjeta;
        });
        listaObjetos.replaceChildren(
          ...(filas.length ? filas : [h("p", { class: "empty admin-empty" }, "No hay publicaciones que coincidan con los filtros.")])
        );
      };

      const filtrarObjetos = async () => {
        try {
          const datos = await pedir("GET", rutaObjetos());
          pintarObjetos(datos.objetos || []);
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

      const aplicarFiltros = h("button", { class: "btn btn-primary", type: "button" }, "Buscar");
      const limpiarFiltros = h("button", { class: "btn btn-secondary", type: "button" }, "Limpiar filtros");
      aplicarFiltros.addEventListener("click", filtrarObjetos);
      busquedaObjetos.addEventListener("keydown", e => {
        if (e.key === "Enter") filtrarObjetos();
      });
      limpiarFiltros.addEventListener("click", () => {
        busquedaObjetos.value = "";
        categoriaObjetos.value = "";
        estadoObjetos.value = "";
        filtrarObjetos();
      });
      const eliminarTodas = h("button", { class: "btn btn-danger", type: "button" }, "Eliminar todas las publicaciones");
      eliminarTodas.addEventListener("click", async () => {
        const primeraConfirmacion = confirm(
          "ATENCIÓN: vas a eliminar TODAS las publicaciones de FYB, incluidas las ocultas. " +
          "Esta acción es definitiva y también retirará sus imágenes. ¿Deseas continuar?"
        );
        if (!primeraConfirmacion) return;
        const frase = prompt('Para confirmar, escribe exactamente: ELIMINAR TODAS');
        if (frase !== "ELIMINAR TODAS") {
          aviso.textContent = "Operación cancelada. No se eliminó ninguna publicación.";
          return;
        }
        eliminarTodas.disabled = true;
        try {
          const resultado = await pedir("DELETE", "/api/admin/objetos/todos");
          aviso.textContent = resultado.mensaje || ("Se eliminaron " + resultado.eliminados + " publicaciones.");
          await cargar();
        } catch (e) {
          aviso.textContent = e.message;
        } finally {
          eliminarTodas.disabled = false;
        }
      });

      objetos.replaceChildren(
        h("h2", {}, "Gestión de publicaciones"),
        h("p", { class: "admin-muted" }, "Consulta las publicaciones y elimina las que sean duplicadas, de prueba o inapropiadas. La eliminación es definitiva."),
        h("div", { class: "admin-bulk-actions" },
          h("span", { class: "admin-muted" }, "Limpieza completa: elimina todas las publicaciones, no solo las que coinciden con los filtros."),
          eliminarTodas
        ),
        h("div", { class: "admin-object-filters" },
          busquedaObjetos, categoriaObjetos, estadoObjetos, aplicarFiltros, limpiarFiltros
        ),
        listaObjetos
      );

      const recargar = h("button", { class: "btn btn-secondary", type: "button" }, "Actualizar información");
      recargar.addEventListener("click", cargar);
      await cargar();
      return pantalla(usuario, null,
        h("div", { class: "page-head" },
          h("div", {}, h("h1", {}, "Panel de administración"),
            h("p", { class: "lede" }, "Estadísticas, reportes y control de usuarios.")),
          recargar
        ),
        aviso, estadisticas, objetos, reportes, usuarios
      );
    }
  }
  App.front.admin = App.front.admin || {};
  App.front.admin.AdminView = AdminView;
})(window.App);
