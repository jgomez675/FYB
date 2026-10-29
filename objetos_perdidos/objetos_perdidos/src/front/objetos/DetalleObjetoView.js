/* Equivale a front/objetos/DetalleObjetoView.java */
(function (App) {
  "use strict";

  const { h, formatearFecha, ocupado, crearAvisos } = App.front.components.dom;
  const { pantalla, imagenObjeto } = App.front.components.layout;
  const { guardarAviso, irA, ponerTitulo } = App.front.components.navigation;
  const { botonFavorito } = App.front.components.favorito;
  const { ObjetosApi } = App.front.api;

  class DetalleObjetoView {
    async mostrar({ usuario, params }) {
      const objeto = await ObjetosApi.obtenerObjeto(params[0]);

      if (!objeto) {
        ponerTitulo("Objeto no encontrado");
        return pantalla(
          usuario,
          h("section", { class: "card notice" }, h("h1", {}, "No encontramos este objeto"), h("p", {}, "Puede que ya no esté publicado."), h("a", { class: "btn btn-primary", href: "#/objetos" }, "Volver a objetos"))
        );
      }

      ponerTitulo(objeto.nombre);
      const asunto = encodeURIComponent(App.config.name + ": " + objeto.nombre);
      const avisos = crearAvisos();

      const datos = [
        h("div", {}, h("dt", {}, "Lugar"), h("dd", {}, objeto.lugar)),
        h("div", {}, h("dt", {}, "Fecha"), h("dd", {}, formatearFecha(objeto.fecha))),
      ];
      if (objeto.publicadoPor) {
        datos.push(h("div", {}, h("dt", {}, "Publicado por"), h("dd", {}, objeto.publicadoPor)));
      }
      if (objeto.telefonoContacto) {
        const numero = objeto.telefonoContacto.replace(/[^\d+]/g, "");
        datos.push(h("div", {}, h("dt", {}, "Teléfono de contacto"), h("dd", {}, h("a", { href: "tel:" + numero }, objeto.telefonoContacto))));
      }

      return pantalla(
        usuario,
        h("a", { class: "back", href: "#/objetos" }, "Volver a objetos"),
        h(
          "article",
          { class: "card detail" },
          imagenObjeto(objeto, "detail-media"),
          h(
            "div",
            { class: "detail-body" },
            h("h1", {}, objeto.nombre),
            objeto.propio ? h("span", { class: "badge" }, "Tu publicación") : null,
            h("div", {}, h("h2", { class: "sub" }, "Descripción"), h("p", { class: "prewrap" }, objeto.descripcion)),
            h("dl", { class: "facts" }, ...datos),
            h(
              "div",
              { class: "detail-actions" },
              botonFavorito(objeto, { variante: "texto", alError: (mensaje) => avisos.avisar(mensaje, "error") }),
              !objeto.propio && objeto.correoUsuario ? h("a", { class: "btn btn-primary", href: "mailto:" + objeto.correoUsuario + "?subject=" + asunto }, "Contactar a quien lo publicó") : null
            ),
            objeto.propio ? this.zonaRetiro(objeto, avisos) : null,
            avisos.nodo
          )
        )
      );
    }

    // Solo la ve quien publicó el objeto. Pide confirmar antes de retirarlo, porque no se puede deshacer.
    zonaRetiro(objeto, avisos) {
      const zona = h("div", { class: "retirar" });

      const mostrarBoton = () => {
        const boton = h("button", { class: "btn btn-danger", type: "button" }, "Retirar publicación");
        boton.addEventListener("click", pedirConfirmacion);
        zona.replaceChildren(boton);
        return boton;
      };

      const retirar = async (confirmar) => {
        ocupado(confirmar, true);
        try {
          const resultado = await ObjetosApi.retirarObjeto(objeto.id);
          if (resultado === ObjetosApi.OBJETO_ELIMINADO) {
            guardarAviso("Publicación retirada correctamente.");
            irA("#/objetos");
            return;
          }
          avisos.avisar(resultado, "error");
          mostrarBoton().focus();
        } catch (error) {
          if (error.estado === 401) {
            guardarAviso(error.message, "error");
            irA("#/login");
            return;
          }
          avisos.avisar(error.message, "error");
          mostrarBoton().focus();
        } finally {
          ocupado(confirmar, false);
        }
      };

      const pedirConfirmacion = () => {
        avisos.avisar();
        const confirmar = h("button", { class: "btn btn-danger", type: "button" }, "Sí, retirar");
        const cancelar = h("button", { class: "btn btn-secondary", type: "button", "aria-describedby": "retirar-texto" }, "Cancelar");
        confirmar.addEventListener("click", () => retirar(confirmar));
        cancelar.addEventListener("click", () => mostrarBoton().focus());

        zona.replaceChildren(
          h("p", { class: "confirm-text", id: "retirar-texto" }, "¿Retirar esta publicación? Dejará de verse en la aplicación y en los favoritos de quienes la guardaron. No se puede deshacer."),
          h("div", { class: "actions" }, confirmar, cancelar)
        );
        cancelar.focus(); // el foco empieza en la opción segura
      };

      mostrarBoton();
      return zona;
    }
  }

  App.front.objetos.DetalleObjetoView = DetalleObjetoView;
})(window.App);
