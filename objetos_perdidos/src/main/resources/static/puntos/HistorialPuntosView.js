/*
 * Historial de puntos: muestra cada movimiento (ganado o gastado) y cómo cambió el saldo.
 * Ruta: #/puntos/historial. Datos: GET /api/puntos/historial.
 */
(function (App) {
  "use strict";

  const { h } = App.front.components.dom;
  const { pantalla } = App.front.components.layout;
  const { ponerTitulo } = App.front.components.navigation;
  const { ApiClient } = App.front.api;

  function formatoFecha(texto) {
    // El servidor guarda la fecha como AAAA-MM-DD.
    const partes = /^(\d{4})-(\d{2})-(\d{2})$/.exec(texto || "");
    return partes ? partes[3] + "/" + partes[2] + "/" + partes[1] : (texto || "—");
  }

  function resumen(titulo, valor, color) {
    return h(
      "div",
      { style: "flex: 1 1 180px;" },
      h("p", { style: "margin: 0 0 4px; font-size: .85rem; font-weight: 600; color: var(--slate-500);" }, titulo),
      h("p", { style: "margin: 0; font-size: 2rem; font-weight: 800; color: " + color + ";" }, String(valor))
    );
  }

  function fila(m) {
    const gano = m.tipo === "GANADO";
    const color = gano ? "var(--green-700)" : "var(--red-600)";
    const celda = "padding: 12px 10px; border-bottom: 1px solid var(--slate-200);";

    return h(
      "tr",
      {},
      h("td", { style: celda + " white-space: nowrap;" }, formatoFecha(m.fecha)),
      h("td", { style: celda }, m.motivo || "—"),
      h(
        "td",
        { style: celda + " text-align: right; font-weight: 700; white-space: nowrap; color: " + color + ";" },
        (gano ? "+" : "−") + Math.abs(m.cantidad)
      ),
      h("td", { style: celda + " text-align: right; white-space: nowrap;" }, m.saldoDespues + " pts")
    );
  }

  class HistorialPuntosView {

    async mostrar({ usuario }) {
      ponerTitulo("Historial de puntos");

      let datos = null;
      let error = "";

      try {
        const r = await ApiClient.solicitar("GET", "/api/puntos/historial");

        if (r.estado === 401) {
          const e = new Error(ApiClient.mensajeDe(r));
          e.estado = 401;
          throw e;
        }

        if (r.estado !== 200 || !r.datos) {
          error = ApiClient.mensajeDe(r);
        } else {
          datos = r.datos;
        }
      } catch (e) {
        if (e.estado === 401) throw e;
        error = e.message || "No se pudo consultar el historial.";
      }

      const cabecera = h(
        "div",
        { class: "page-head" },
        h(
          "div",
          {},
          h("h1", {}, "Historial de puntos"),
          h("p", { class: "lede" }, "Revisa los puntos que ganaste y gastaste, y cómo llegaste a tu saldo.")
        ),
        h("a", { class: "btn btn-secondary", href: "#/puntos" }, "Volver a mis puntos")
      );

      if (error) {
        return pantalla(usuario, cabecera, h("p", { class: "flash error", role: "alert" }, error));
      }

      const tarjetaResumen = h(
        "section",
        { class: "card", style: "max-width: 900px; margin: 0 auto 25px; padding: 25px; display: flex; flex-wrap: wrap; gap: 20px;" },
        resumen("SALDO ACTUAL", datos.saldo + " pts", "var(--blue-600)"),
        resumen("TOTAL GANADO", "+" + datos.totalGanado, "var(--green-700)"),
        resumen("TOTAL GASTADO", "−" + datos.totalGastado, "var(--red-600)")
      );

      if (!datos.movimientos.length) {
        return pantalla(
          usuario,
          cabecera,
          tarjetaResumen,
          h(
            "div",
            { class: "empty" },
            h("p", {}, "Aún no tienes movimientos. Publica un objeto o consulta uno para empezar a ganar puntos.")
          )
        );
      }

      const cabeza = (texto, derecha) => h(
        "th",
        { style: "padding: 10px; text-align: " + (derecha ? "right" : "left") + "; font-size: .8rem; color: var(--slate-500); border-bottom: 2px solid var(--slate-200);" },
        texto
      );

      const tabla = h(
        "table",
        { style: "width: 100%; border-collapse: collapse;" },
        h("thead", {}, h("tr", {}, cabeza("Fecha"), cabeza("Motivo"), cabeza("Puntos", true), cabeza("Saldo después", true))),
        h("tbody", {}, datos.movimientos.map(fila))
      );

      return pantalla(
        usuario,
        cabecera,
        tarjetaResumen,
        h(
          "section",
          { class: "card", style: "max-width: 900px; margin: 0 auto 35px; padding: 20px; overflow-x: auto;" },
          tabla
        )
      );
    }
  }

  App.front.puntos = App.front.puntos || {};
  App.front.puntos.HistorialPuntosView = HistorialPuntosView;

})(window.App);
