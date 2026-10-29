/* Equivale a front/objetos/RegistrarObjetoView.java */
(function (App) {
  "use strict";

  const { h, campo, mostrarMensaje, ocupado, hoyISO } = App.front.components.dom;
  const { pantalla } = App.front.components.layout;
  const { guardarAviso, irA, ponerTitulo } = App.front.components.navigation;
  const { ObjetosApi } = App.front.api;

  class RegistrarObjetoView {
    async mostrar({ usuario, params = [] }) {
      const idEditar = params[0] || null;
      const objeto = idEditar ? await ObjetosApi.obtenerObjeto(idEditar) : null;
      if (idEditar && (!objeto || (objeto.correoUsuario !== usuario.correo && usuario.rol !== "admin"))) {
        ponerTitulo("Sin permiso");
        return pantalla(usuario, h("section", { class: "card notice" }, h("h1", {}, "No puedes editar esta publicación"), h("a", { class: "btn btn-primary", href: "#/objetos" }, "Volver")));
      }
      ponerTitulo(idEditar ? "Editar objeto" : "Publicar objeto");
      const categorias = await ObjetosApi.obtenerCategorias();
      const hoy = hoyISO();
      const mensaje = h("p", { class: "msg", role: "alert" });
      const nombre = campo({ id: "nombre", etiqueta: "Nombre del objeto", autocomplete: "off", value: objeto ? objeto.nombre : "" });
      const descripcion = campo({ id: "descripcion", etiqueta: "Descripción detallada", tag: "textarea", rows: "4" });
      if (objeto) descripcion.control.value = objeto.descripcion;
      const lugar = campo({ id: "lugar", etiqueta: "Lugar donde se perdió", autocomplete: "off", value: objeto ? objeto.lugar : "" });
      const fecha = campo({ id: "fecha", etiqueta: "Fecha", type: "date", value: objeto ? objeto.fecha : hoy, max: hoy });
      const categoria = h("select", { id: "categoria", name: "categoria" }, ...categorias.map((c) => h("option", { value: c, selected: objeto ? objeto.categoria === c : c === "Otros" }, c)));
      const campoCategoria = h("div", { class: "field" }, h("label", { for: "categoria" }, "Categoría"), categoria);

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

      const publicar = h("button", { class: "btn btn-primary", type: "submit" }, objeto ? "Guardar cambios" : "Publicar objeto");
      const formulario = h(
        "form",
        { class: "form", novalidate: true },
        nombre.wrap,
        descripcion.wrap,
        lugar.wrap,
        fecha.wrap,
        campoCategoria,
        objeto ? null : h("div", { class: "field" }, h("label", { for: "imagen" }, "Foto del objeto"), seleccionar, vistaPrevia),
        objeto && objeto.imagenUrl ? h("img", { class: "preview", src: objeto.imagenUrl, alt: "Foto actual del objeto" }) : null,
        h("div", { class: "actions" }, publicar, h("a", { class: "btn btn-secondary", href: "#/objetos" }, "Volver")),
        mensaje
      );

      formulario.addEventListener("submit", async (evento) => {
        evento.preventDefault();

        ocupado(publicar, true);
        try {
          if (objeto) {
            await ObjetosApi.editarObjeto(idEditar, nombre.control.value, descripcion.control.value, lugar.control.value, fecha.control.value, categoria.value);
            guardarAviso("Cambios guardados correctamente.");
            irA("#/objetos/" + idEditar);
          } else {
            const resultado = await ObjetosApi.guardarObjeto(nombre.control.value, descripcion.control.value, lugar.control.value, fecha.control.value, archivo, categoria.value);
            if (resultado === ObjetosApi.OBJETO_GUARDADO) { guardarAviso("Objeto publicado correctamente."); irA("#/objetos"); }
            else mostrarMensaje(mensaje, resultado, "error");
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

      return pantalla(usuario, h("section", { class: "card form-card" }, h("h1", {}, objeto ? "Editar objeto publicado" : "Publicar objeto perdido"), formulario));
    }
  }

  App.front.objetos.RegistrarObjetoView = RegistrarObjetoView;
})(window.App);
