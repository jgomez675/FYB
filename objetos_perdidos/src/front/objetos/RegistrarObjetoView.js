/* Equivale a front/objetos/RegistrarObjetoView.java */
(function (App) {
  "use strict";

  const { h, campo, mostrarMensaje, ocupado, hoyISO } = App.front.components.dom;
  const { pantalla } = App.front.components.layout;
  const { guardarAviso, irA, ponerTitulo } = App.front.components.navigation;
  const { ObjetosApi } = App.front.api;

  class RegistrarObjetoView {
    async mostrar({ usuario }) {
      ponerTitulo("Publicar objeto");

      const hoy = hoyISO();
      const mensaje = h("p", { class: "msg", role: "alert" });
      const nombre = campo({ id: "nombre", etiqueta: "Nombre del objeto", autocomplete: "off" });
      const descripcion = campo({ id: "descripcion", etiqueta: "Descripción detallada", tag: "textarea", rows: "4" });
      const lugar = campo({ id: "lugar", etiqueta: "Lugar donde se perdió", autocomplete: "off" });
      const fecha = campo({ id: "fecha", etiqueta: "Fecha", type: "date", value: hoy, max: hoy });

      let archivo = null;
      let urlVistaPrevia = null;
      const seleccionar = h("input", { id: "imagen", name: "imagen", type: "file", accept: "image/*" });
      const vistaPrevia = h("img", { class: "preview", alt: "Vista previa de la imagen seleccionada", hidden: true });

      seleccionar.addEventListener("change", () => {
        archivo = seleccionar.files && seleccionar.files[0] ? seleccionar.files[0] : null;
        if (urlVistaPrevia) {
          URL.revokeObjectURL(urlVistaPrevia);
          urlVistaPrevia = null;
        }
        if (archivo) {
          urlVistaPrevia = URL.createObjectURL(archivo);
          vistaPrevia.src = urlVistaPrevia;
          vistaPrevia.hidden = false;
        } else {
          vistaPrevia.removeAttribute("src");
          vistaPrevia.hidden = true;
        }
      });

      const publicar = h("button", { class: "btn btn-primary", type: "submit" }, "Publicar objeto");
      const formulario = h(
        "form",
        { class: "form", novalidate: true },
        nombre.wrap,
        descripcion.wrap,
        lugar.wrap,
        fecha.wrap,
        h("div", { class: "field" }, h("label", { for: "imagen" }, "Foto del objeto"), seleccionar, vistaPrevia),
        h("div", { class: "actions" }, publicar, h("a", { class: "btn btn-secondary", href: "#/objetos" }, "Volver")),
        mensaje
      );

      formulario.addEventListener("submit", async (evento) => {
        evento.preventDefault();

        ocupado(publicar, true);
        try {
          const resultado = await ObjetosApi.guardarObjeto(
            nombre.control.value,
            descripcion.control.value,
            lugar.control.value,
            fecha.control.value,
            archivo
          );

          if (resultado === ObjetosApi.OBJETO_GUARDADO) {
            guardarAviso("Objeto publicado correctamente.");
            irA("#/objetos");
          } else {
            mostrarMensaje(mensaje, resultado, "error");
          }
        } catch (error) {
          if (error.estado === 401) {
            guardarAviso(error.message, "error");
            irA("#/login");
          } else {
            mostrarMensaje(mensaje, error.message, "error");
          }
        } finally {
          ocupado(publicar, false);
        }
      });

      return pantalla(usuario, h("section", { class: "card form-card" }, h("h1", {}, "Publicar objeto perdido"), formulario));
    }
  }

  App.front.objetos.RegistrarObjetoView = RegistrarObjetoView;
})(window.App);
