/* Equivale a front/objetos/DetalleObjetoView.java */
(function (App) {
  "use strict";

  const { h, formatearFecha, mostrarMensaje, ocupado } =
    App.front.components.dom;
  const { pantalla, imagenObjeto } = App.front.components.layout;
  const { ponerTitulo } = App.front.components.navigation;
  const { ObjetosApi } = App.front.api;

  class DetalleObjetoView {
    async mostrar({ usuario, params }) {
      const objeto = await ObjetosApi.obtenerObjeto(params[0]);

      if (!objeto) {
        ponerTitulo("Objeto no encontrado");

        return pantalla(
          usuario,
          h(
            "section",
            { class: "card notice" },
            h("h1", {}, "No encontramos este objeto"),
            h(
              "p",
              {},
              "Puede que ya no esté publicado."
            ),
            h(
              "a",
              {
                class: "btn btn-primary",
                href: "#/objetos"
              },
              "Volver a objetos"
            )
          )
        );
      }

      ponerTitulo(objeto.nombre);

      const asunto = encodeURIComponent(
        App.config.name + ": " + objeto.nombre
      );

      const mensaje = h(
        "p",
        {
          class: "msg",
          role: "alert"
        }
      );

      const caracteristica = h(
        "textarea",
        {
          class: "control",
          rows: "3",
          placeholder: "Escribe una característica que solo el dueño debería conocer..."
        }
      );

      const verificar = h(
        "button",
        {
          class: "btn btn-primary",
          type: "button"
        },
        "Verificar propiedad"
      );

      const formularioVerificacion = h(
        "div",
        {
          class: "field"
        },
        h(
          "label",
          {
            for: "caracteristica-verificacion"
          },
          "Característica del objeto"
        ),
        caracteristica
      );

      const zonaContacto = h(
        "div",
        {},
        formularioVerificacion,
        verificar,
        mensaje
      );

      verificar.addEventListener(
        "click",
        async () => {

          if (!caracteristica.value.trim()) {
            mostrarMensaje(
              mensaje,
              "Debes ingresar una característica del objeto.",
              "error"
            );
            return;
          }

          ocupado(verificar, true);

          try {
            const resultado =
              await ObjetosApi.verificarCaracteristica(
                objeto.id,
                caracteristica.value
              );

            if (resultado.verificado) {

              mostrarMensaje(
                mensaje,
                "La característica coincide. Puedes continuar con la reclamación.",
                "success"
              );

              verificar.remove();

              caracteristica.disabled = true;

              zonaContacto.appendChild(
                h(
                  "a",
                  {
                    class: "btn btn-primary",
                    href:
                      "mailto:" +
                      objeto.correoUsuario +
                      "?subject=" +
                      asunto
                  },
                  "Contactar a quien lo publicó"
                )
              );

            } else {

              mostrarMensaje(
                mensaje,
                resultado.mensaje,
                "error"
              );
            }

          } catch (error) {

            if (error.estado === 401) {
              mostrarMensaje(
                mensaje,
                error.message,
                "error"
              );
            } else {
              mostrarMensaje(
                mensaje,
                error.message,
                "error"
              );
            }

          } finally {
            ocupado(verificar, false);
          }
        }
      );

      const reporteMensaje = h("p", { class: "msg", role: "alert" });
      const motivo = h("select", { class: "control" },
        h("option", { value: "" }, "Selecciona un motivo"),
        h("option", { value: "Información falsa" }, "Información falsa"),
        h("option", { value: "Contenido inapropiado" }, "Contenido inapropiado"),
        h("option", { value: "Objeto duplicado" }, "Objeto duplicado"),
        h("option", { value: "Otro" }, "Otro")
      );
      const detalleReporte = h("textarea", {
        class: "control",
        rows: "3",
        placeholder: "Cuéntanos brevemente qué está mal en la publicación..."
      });
      const enviarReporte = h("button", { class: "btn btn-secondary", type: "button" }, "Enviar reporte");

      enviarReporte.addEventListener("click", async () => {
        if (!motivo.value) {
          mostrarMensaje(reporteMensaje, "Selecciona un motivo para el reporte.", "error");
          return;
        }

        ocupado(enviarReporte, true);
        try {
          const resultado = await ObjetosApi.reportarPublicacion(
            objeto.id,
            motivo.value,
            detalleReporte.value
          );

          if (resultado.ok) {
            mostrarMensaje(reporteMensaje, "El reporte fue enviado correctamente.", "success");
            enviarReporte.remove();
            motivo.disabled = true;
            detalleReporte.disabled = true;
          } else {
            mostrarMensaje(reporteMensaje, resultado.mensaje, "error");
          }
        } catch (error) {
          mostrarMensaje(reporteMensaje, error.message, "error");
        } finally {
          ocupado(enviarReporte, false);
        }
      });

      const zonaReporte = h("div", { class: "report-box" },
        h("h2", { class: "sub" }, "¿Hay un problema con esta publicación?"),
        h("p", { class: "report-text" }, "Puedes reportarla si encuentras información falsa o contenido inapropiado."),
        motivo,
        detalleReporte,
        enviarReporte,
        reporteMensaje
      );

      return pantalla(
        usuario,

        h(
          "a",
          {
            class: "back",
            href: "#/objetos"
          },
          "Volver a objetos"
        ),

        h(
          "article",
          {
            class: "card detail"
          },

          imagenObjeto(
            objeto,
            "detail-media"
          ),

          h(
            "div",
            {
              class: "detail-body"
            },

            h(
              "h1",
              {},
              objeto.nombre
            ),

            h(
              "div",
              {},
              h(
                "h2",
                {
                  class: "sub"
                },
                "Descripción"
              ),
              h(
                "p",
                {
                  class: "prewrap"
                },
                objeto.descripcion
              )
            ),

            h(
              "dl",
              {
                class: "facts"
              },

              h(
                "div",
                {},
                h("dt", {}, "Lugar"),
                h("dd", {}, objeto.lugar)
              ),

              h(
                "div",
                {},
                h("dt", {}, "Fecha"),
                h(
                  "dd",
                  {},
                  formatearFecha(objeto.fecha)
                )
              )
            ),

            zonaContacto,
            zonaReporte
          )
        )
      );
    }
  }

  App.front.objetos.DetalleObjetoView =
    DetalleObjetoView;

})(window.App);
