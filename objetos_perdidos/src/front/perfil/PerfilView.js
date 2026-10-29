/* Perfil: la persona mantiene al día su nombre y su teléfono de contacto. */
(function (App) {
  "use strict";

  const { h, campo, mostrarMensaje, ocupado } = App.front.components.dom;
  const { pantalla } = App.front.components.layout;
  const { guardarAviso, tomarAviso, irA, ponerTitulo } = App.front.components.navigation;
  const { PerfilApi } = App.front.api;

  class PerfilView {
    async mostrar({ usuario }) {
      ponerTitulo("Mi perfil");
      const perfil = await PerfilApi.obtenerPerfil();

      const mensaje = h("p", { class: "msg", role: "alert" });
      const nombre = campo({ id: "nombre", etiqueta: "Nombre completo", autocomplete: "name", maxlength: "120", value: perfil.nombre });
      const correo = campo({ id: "correo", etiqueta: "Correo institucional", type: "email", readonly: true, "aria-describedby": "correo-ayuda", value: perfil.correo });
      const telefono = campo({
        id: "telefono",
        etiqueta: "Teléfono de contacto (opcional)",
        type: "tel",
        inputmode: "tel",
        autocomplete: "tel",
        maxlength: "20",
        placeholder: "300 123 4567",
        "aria-describedby": "telefono-ayuda",
        value: perfil.telefono,
      });

      correo.wrap.append(h("p", { class: "hint", id: "correo-ayuda" }, "Es tu usuario para iniciar sesión, por eso no se puede cambiar."));
      telefono.wrap.append(
        h("p", { class: "hint", id: "telefono-ayuda" }, "Se muestra en tus publicaciones para que puedan contactarte. Déjalo vacío si prefieres no compartirlo.")
      );

      const guardar = h("button", { class: "btn btn-primary", type: "submit" }, "Guardar cambios");
      const formulario = h("form", { class: "form", novalidate: true }, nombre.wrap, correo.wrap, telefono.wrap, guardar, mensaje);

      formulario.addEventListener("submit", async (evento) => {
        evento.preventDefault();

        ocupado(guardar, true);
        try {
          const resultado = await PerfilApi.actualizarPerfil(nombre.control.value, telefono.control.value);

          if (resultado === PerfilApi.PERFIL_ACTUALIZADO) {
            // Se vuelve a cargar la pantalla para que la barra superior ya muestre el nombre nuevo.
            guardarAviso("Perfil actualizado correctamente.");
            irA("#/perfil");
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
          ocupado(guardar, false);
        }
      });

      return pantalla(
        usuario,
        h(
          "section",
          { class: "card form-card" },
          tomarAviso(),
          h("h1", {}, "Mi perfil"),
          h("p", { class: "form-lede" }, "Mantén tus datos de contacto al día para que puedan comunicarse contigo."),
          formulario
        )
      );
    }
  }

  App.front.perfil.PerfilView = PerfilView;
})(window.App);
