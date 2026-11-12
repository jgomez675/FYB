/*
 * Mis puntos y recompensas. Ruta: #/puntos.
 * Datos: GET /api/recompensas (saldo, catálogo y canjes). Canje: POST /api/recompensas/{id}/canjear.
 * El botón de cada recompensa se activa cuando el saldo alcanza su costo.
 */
(function (App) {
  "use strict";

  const { h } = App.front.components.dom;
  const { pantalla } = App.front.components.layout;
  const { ponerTitulo, guardarAviso, tomarAviso, irA } = App.front.components.navigation;
  const { ApiClient } = App.front.api;

  function formatoFecha(texto) {
    const partes = /^(\d{4})-(\d{2})-(\d{2})$/.exec(texto || "");
    return partes ? partes[3] + "/" + partes[2] + "/" + partes[1] : (texto || "—");
  }

  class PuntosView {

    async mostrar({ usuario }) {

      ponerTitulo("Mis puntos");

      let saldo = 0;
      let recompensas = [];
      let canjes = [];
      let mensaje = "";

      try {
        const r = await ApiClient.solicitar("GET", "/api/recompensas");

        if (r.estado === 401) {
          const e = new Error(ApiClient.mensajeDe(r));
          e.estado = 401;
          throw e;
        }

        if (r.estado !== 200 || !r.datos) {
          mensaje = ApiClient.mensajeDe(r);
        } else {
          saldo = Number(r.datos.saldo) || 0;
          recompensas = r.datos.recompensas || [];
          canjes = r.datos.canjes || [];
        }
      } catch (error) {
        if (error.estado === 401) throw error;
        mensaje = error.message || "No se pudo consultar el saldo.";
      }

      // Mensaje de error del último intento de canje (si el servidor lo rechazó).
      const errorCanje = h("p", { class: "flash error", role: "alert", style: "display: none;" });

      const canjear = async (recompensa, boton) => {
        if (!window.confirm(
          "¿Canjear \"" + recompensa.nombre + "\" por " + recompensa.costo + " puntos?")) {
          return;
        }

        boton.disabled = true;
        errorCanje.style.display = "none";

        try {
          const r = await ApiClient.solicitar(
            "POST", "/api/recompensas/" + recompensa.id + "/canjear");

          if (r.estado === 401) {
            const e = new Error(ApiClient.mensajeDe(r));
            e.estado = 401;
            throw e;
          }

          if (r.estado !== 200) {
            throw new Error(ApiClient.mensajeDe(r));
          }

          // Se recarga la pantalla para ver el saldo nuevo, y se deja el aviso con el código.
          guardarAviso(r.datos.mensaje, "ok");
          irA("#/puntos");

        } catch (error) {
          if (error.estado === 401) {
            guardarAviso(error.message, "error");
            irA("#/login");
            return;
          }

          errorCanje.textContent = error.message || "No se pudo canjear la recompensa.";
          errorCanje.style.display = "";
          boton.disabled = false;
        }
      };

      const lista = h("div", { class: "grid", style: "margin-top: 20px;" });

      if (!recompensas.length && !mensaje) {
        lista.append(h("div", { class: "empty" }, h("p", {}, "Por ahora no hay recompensas disponibles.")));
      }

      recompensas.forEach((recompensa) => {

        const puedeCanjear = saldo >= recompensa.costo;

        const boton = h(
          "button",
          {
            class: "btn btn-primary",
            type: "button",
            disabled: !puedeCanjear,
            style: "width: 100%;"
          },
          puedeCanjear
            ? "Canjear recompensa"
            : "Te faltan " + (recompensa.costo - saldo) + " puntos"
        );

        boton.addEventListener("click", () => canjear(recompensa, boton));

        lista.append(
          h(
            "article",
            { class: "card obj", style: "height: 100%;" },

            h(
              "div",
              {
                class: "obj-body",
                style: "display: flex; flex-direction: column; height: 100%;"
              },

              h("h2", { class: "obj-name" }, recompensa.nombre),

              h("p", { class: "obj-desc", style: "min-height: 48px;" }, recompensa.descripcion),

              h(
                "p",
                { class: "obj-meta" },
                h("span", { class: "k" }, "Costo: "),
                recompensa.costo + " puntos"
              ),

              h("div", { style: "margin-top: auto; padding-top: 15px;" }, boton)
            )
          )
        );
      });

      const bloqueCanjes = canjes.length
        ? h(
            "section",
            { class: "card", style: "max-width: 900px; margin: 35px auto; padding: 20px; overflow-x: auto;" },
            h("h2", { style: "margin: 0 0 4px;" }, "Mis canjes"),
            h("p", { class: "lede", style: "margin: 0 0 12px;" },
              "Presenta el código para reclamar tu recompensa."),
            h(
              "table",
              { style: "width: 100%; border-collapse: collapse;" },
              h(
                "thead",
                {},
                h(
                  "tr",
                  {},
                  ...["Fecha", "Recompensa", "Código", "Puntos"].map((t, i) =>
                    h(
                      "th",
                      {
                        style:
                          "padding: 10px; text-align: " + (i === 3 ? "right" : "left") +
                          "; font-size: .8rem; color: var(--slate-500); border-bottom: 2px solid var(--slate-200);"
                      },
                      t
                    )
                  )
                )
              ),
              h(
                "tbody",
                {},
                canjes.map((c) => {
                  const celda = "padding: 12px 10px; border-bottom: 1px solid var(--slate-200);";
                  return h(
                    "tr",
                    {},
                    h("td", { style: celda + " white-space: nowrap;" }, formatoFecha(c.fecha)),
                    h("td", { style: celda }, c.nombre),
                    h("td", { style: celda + " font-weight: 700; white-space: nowrap;" }, c.codigo),
                    h(
                      "td",
                      { style: celda + " text-align: right; white-space: nowrap; color: var(--red-600); font-weight: 700;" },
                      "−" + c.costo
                    )
                  );
                })
              )
            )
          )
        : null;

      return pantalla(

        usuario,

        h(
          "div",
          { class: "page-head" },

          h(
            "div",
            {},
            h("h1", {}, "Mis puntos"),
            h("p", { class: "lede" }, "Consulta tu saldo y canjea tus puntos por recompensas")
          ),

          h("a", { class: "btn btn-secondary", href: "#/puntos/historial" }, "Ver historial de puntos")
        ),

        tomarAviso(),

        h(
          "section",
          {
            class: "card",
            style: "max-width: 900px; margin: 0 auto 35px; padding: 35px;"
          },

          h(
            "p",
            { style: "margin: 0 0 8px; font-size: 1rem; font-weight: 600; color: var(--slate-500);" },
            "SALDO DISPONIBLE"
          ),

          h(
            "p",
            { style: "margin: 0; font-size: 3.2rem; font-weight: 800; color: var(--blue-600);" },
            saldo + " puntos"
          ),

          mensaje
            ? h("p", { style: "margin: 12px 0 0; color: var(--red-600);" }, mensaje)
            : h(
                "p",
                { style: "margin: 12px 0 0; color: var(--slate-500);" },
                "Cuando tu saldo alcance el costo de una recompensa, podrás canjearla."
              )
        ),

        errorCanje,

        h(
          "div",
          { style: "margin-top: 10px;" },

          h("h2", { style: "color: white; font-size: 2rem; margin: 0 0 8px;" }, "Recompensas"),

          h("p", { class: "lede" }, "Revisa los premios disponibles y los puntos necesarios para obtenerlos.")
        ),

        lista,

        bloqueCanjes
      );
    }
  }

  App.front.puntos = App.front.puntos || {};
  App.front.puntos.PuntosView = PuntosView;

})(window.App);
