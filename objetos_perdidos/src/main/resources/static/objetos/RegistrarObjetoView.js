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
      const caracteristicaPrivada = campo({
        id: "caracteristicaPrivada",
        etiqueta: "Característica privada del objeto",
        tag: "textarea",
        rows: "3",
        autocomplete: "off"
      });

      const categoria = h("select", { id: "categoria", name: "categoria" },
        h("option", { value: "Tecnología" }, "Tecnología"),
        h("option", { value: "Documentos" }, "Documentos"),
        h("option", { value: "Accesorios" }, "Accesorios"),
        h("option", { value: "Ropa" }, "Ropa"),
        h("option", { value: "Llaves" }, "Llaves"),
        h("option", { value: "Otros", selected: true }, "Otros")
      );

      const estado = h("select", { id: "estado", name: "estado" },
        h("option", { value: "Perdido", selected: true }, "Perdido"),
        h("option", { value: "Encontrado" }, "Encontrado")
      );

      let archivo = null;
      let urlVistaPrevia = null;

      const seleccionar = h("input", {
        id: "imagen",
        name: "imagen",
        type: "file",
        accept: "image/jpeg,image/png,image/gif,image/webp"
      });

      const vistaPrevia = h("img", {
        class: "preview",
        alt: "Vista previa de la imagen seleccionada",
        hidden: true
      });

      seleccionar.addEventListener("change", () => {
        archivo = seleccionar.files && seleccionar.files[0] ? seleccionar.files[0] : null;

        if (urlVistaPrevia) {
          URL.revokeObjectURL(urlVistaPrevia);
          urlVistaPrevia = null;
        }

        if (!archivo) {
          vistaPrevia.removeAttribute("src");
          vistaPrevia.hidden = true;
          return;
        }

        const tiposPermitidos = ["image/jpeg", "image/png", "image/gif", "image/webp"];
        if (!tiposPermitidos.includes(archivo.type)) {
          archivo = null;
          seleccionar.value = "";
          mostrarMensaje(mensaje, "La foto debe ser JPG, PNG, GIF o WEBP.", "error");
          vistaPrevia.hidden = true;
          return;
        }

        if (archivo.size > 8 * 1024 * 1024) {
          archivo = null;
          seleccionar.value = "";
          mostrarMensaje(mensaje, "La foto no puede superar los 8 MB.", "error");
          vistaPrevia.hidden = true;
          return;
        }

        urlVistaPrevia = URL.createObjectURL(archivo);
        vistaPrevia.src = urlVistaPrevia;
        vistaPrevia.hidden = false;
        mostrarMensaje(mensaje, "Foto seleccionada correctamente.", "ok");
      });

      const publicar = h("button", { class: "btn btn-primary", type: "submit" }, "Publicar objeto");

      const formulario = h("form", { class: "form", novalidate: true },
        nombre.wrap,
        descripcion.wrap,
        lugar.wrap,
        fecha.wrap,
        h("div", { class: "field" }, h("label", { for: "categoria" }, "Categoría"), categoria),
        h("div", { class: "field" }, h("label", { for: "estado" }, "Estado"), estado),
        caracteristicaPrivada.wrap,
        h("div", { class: "field" },
          h("label", { for: "imagen" }, "Foto del objeto"),
          seleccionar,
          h("small", { class: "help-text" }, "Sube una foto clara para ayudar a identificar el objeto."),
          vistaPrevia
        ),
        h("div", { class: "actions" },
          publicar,
          h("a", { class: "btn btn-secondary", href: "#/objetos" }, "Volver")
        ),
        mensaje
      );

      formulario.addEventListener("submit", async (evento) => {
        evento.preventDefault();

        if (!archivo) {
          mostrarMensaje(mensaje, "Debes seleccionar una foto del objeto.", "error");
          return;
        }

        ocupado(publicar, true);

        try {
          const resultado = await ObjetosApi.guardarObjeto(
            nombre.control.value,
            descripcion.control.value,
            lugar.control.value,
            fecha.control.value,
            archivo,
            caracteristicaPrivada.control.value,
            categoria.value,
            estado.value
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

      return pantalla(
        usuario,
        h("section", { class: "card form-card" },
          h("h1", {}, "Publicar objeto perdido"),
          formulario
        )
      );
    }
  }

  App.front.objetos.RegistrarObjetoView = RegistrarObjetoView;
})(window.App);
