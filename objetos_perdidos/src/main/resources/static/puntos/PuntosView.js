(function (App) {
  "use strict";

  const { h } = App.front.components.dom;
  const { pantalla } = App.front.components.layout;
  const { ponerTitulo } = App.front.components.navigation;

  class PuntosView {

    async mostrar({ usuario }) {

      ponerTitulo("Mis puntos");

      let saldo = 0;
      let mensaje = "";

      try {

        const respuesta =
          await fetch("/api/puntos", {
            method: "GET",
            credentials: "same-origin"
          });

        const datos =
          await respuesta.json();

        if (!respuesta.ok) {
          throw new Error(
            datos.mensaje ||
            "No se pudo consultar el saldo."
          );
        }

        saldo =
          Number(datos.puntos) || 0;

      } catch (error) {

        mensaje =
          error.message ||
          "No se pudo consultar el saldo.";
      }

      const recompensas = [
        {
          nombre: "Recompensa 1",
          descripcion: "Premio disponible para canjear.",
          costo: 100
        },
        {
          nombre: "Recompensa 2",
          descripcion: "Premio disponible para canjear.",
          costo: 250
        },
        {
          nombre: "Recompensa 3",
          descripcion: "Premio disponible para canjear.",
          costo: 500
        }
      ];

      const lista = h(
        "div",
        {
          class: "grid",
          style: "margin-top: 20px;"
        }
      );

      recompensas.forEach((recompensa) => {

        const puedeCanjear =
          saldo >= recompensa.costo;

        lista.append(
          h(
            "article",
            {
              class: "card obj",
              style: "height: 100%;"
            },

            h(
              "div",
              {
                class: "obj-body",
                style:
                  "display: flex; flex-direction: column; height: 100%;"
              },

              h(
                "h2",
                {
                  class: "obj-name"
                },
                recompensa.nombre
              ),

              h(
                "p",
                {
                  class: "obj-desc",
                  style: "min-height: 48px;"
                },
                recompensa.descripcion
              ),

              h(
                "p",
                {
                  class: "obj-meta"
                },

                h(
                  "span",
                  {
                    class: "k"
                  },
                  "Costo: "
                ),

                recompensa.costo +
                " puntos"
              ),

              h(
                "div",
                {
                  style:
                    "margin-top: auto; padding-top: 15px;"
                },

                h(
                  "button",
                  {
                    class: "btn btn-primary",
                    type: "button",
                    disabled: !puedeCanjear,
                    style: "width: 100%;"
                  },

                  puedeCanjear
                    ? "Canjear recompensa"
                    : "Puntos insuficientes"
                )
              )
            )
          )
        );
      });

      return pantalla(

        usuario,

        h(
          "div",
          {
            class: "page-head"
          },

          h(
            "div",
            {},

            h(
              "h1",
              {},
              "Mis puntos"
            ),

            h(
              "p",
              {
                class: "lede"
              },
              "Consulta tu saldo y las recompensas disponibles"
            )
          )
        ),

        h(
          "section",
          {
            class: "card",
            style:
              "max-width: 900px; margin: 0 auto 35px; padding: 35px;"
          },

          h(
            "p",
            {
              style:
                "margin: 0 0 8px; font-size: 1rem; font-weight: 600; color: var(--slate-500);"
            },
            "SALDO DISPONIBLE"
          ),

          h(
            "p",
            {
              style:
                "margin: 0; font-size: 3.2rem; font-weight: 800; color: var(--blue-600);"
            },
            saldo + " puntos"
          ),

          mensaje
            ? h(
                "p",
                {
                  style:
                    "margin: 12px 0 0; color: var(--red-600);"
                },
                mensaje
              )
            : h(
                "p",
                {
                  style:
                    "margin: 12px 0 0; color: var(--slate-500);"
                },
                "Puedes utilizar tus puntos para reclamar las recompensas disponibles."
              )
        ),

        h(
          "div",
          {
            style: "margin-top: 10px;"
          },

          h(
            "h2",
            {
              style:
                "color: white; font-size: 2rem; margin: 0 0 8px;"
            },
            "Recompensas"
          ),

          h(
            "p",
            {
              class: "lede"
            },
            "Revisa los premios disponibles y los puntos necesarios para obtenerlos."
          )
        ),

        lista
      );
    }
  }

  App.front.puntos =
    App.front.puntos || {};

  App.front.puntos.PuntosView =
    PuntosView;

})(window.App);