/* HU: actualizar el perfil (nombre y contraseña) para mantener vigentes los datos. */
(function (App) {
  "use strict";

  const { h, campo, mostrarMensaje, ocupado } = App.front.components.dom;
  const { pantalla } = App.front.components.layout;
  const { guardarAviso, tomarAviso, irA, ponerTitulo } = App.front.components.navigation;
  const { AuthApi } = App.front.api;

  class PerfilView {
    async mostrar({ usuario }) {
      ponerTitulo("Mi perfil");

      const mensaje = h("p", { class: "msg", role: "alert" });

      const correo = campo({
        id: "perfil-correo",
        etiqueta: "Correo institucional (no se puede cambiar)",
        type: "email",
        value: usuario.correo,
        readonly: true,
        autocomplete: "username"
      });

      const nombre = campo({
        id: "perfil-nombre",
        etiqueta: "Nombre completo",
        value: usuario.nombre,
        maxlength: "120",
        autocomplete: "name"
      });

      const actual = campo({
        id: "perfil-actual",
        etiqueta: "Contraseña actual",
        type: "password",
        autocomplete: "current-password"
      });

      const nueva = campo({
        id: "perfil-nueva",
        etiqueta: "Contraseña nueva",
        type: "password",
        autocomplete: "new-password"
      });

      const confirmar = campo({
        id: "perfil-confirmar",
        etiqueta: "Confirmar contraseña nueva",
        type: "password",
        autocomplete: "new-password"
      });

      const guardar = h("button", { class: "btn btn-primary", type: "submit" }, "Guardar cambios");

      const formulario = h("form", { class: "form", novalidate: true },
        correo.wrap,
        nombre.wrap,
        h("h2", { class: "form-section" }, "Cambiar contraseña (opcional)"),
        h("p", { class: "form-hint" }, "Déjalo en blanco si solo quieres actualizar tu nombre."),
        actual.wrap,
        nueva.wrap,
        confirmar.wrap,
        guardar,
        mensaje
      );

      formulario.addEventListener("submit", async (evento) => {
        evento.preventDefault();

        const nombreNuevo = nombre.control.value.trim();

        if (!nombreNuevo) {
          mostrarMensaje(mensaje, "El nombre es obligatorio.", "error");
          return;
        }

        ocupado(guardar, true);
        try {
          const r = await AuthApi.actualizarPerfil(
            nombreNuevo,
            actual.control.value,
            nueva.control.value,
            confirmar.control.value
          );

          if (r.ok) {
            guardarAviso(
              r.contrasenaCambiada
                ? "Perfil y contraseña actualizados correctamente."
                : "Perfil actualizado correctamente."
            );
            irA("#/perfil");
          } else {
            mostrarMensaje(mensaje, r.mensaje, "error");
          }
        } catch (error) {
          if (error.estado === 401) {
            throw error;
          }
          mostrarMensaje(mensaje, error.message, "error");
        } finally {
          ocupado(guardar, false);
        }
      });

      return pantalla(
        usuario,
        tomarAviso(),
        h("div", { class: "page-head" },
          h("div", {},
            h("h1", {}, "Mi perfil"),
            h("p", { class: "lede" }, "Mantén vigentes tus datos de cuenta.")
          )
        ),
        h("section", { class: "card form-card" }, formulario)
      );
    }
  }

  App.front.perfil = App.front.perfil || {};
  App.front.perfil.PerfilView = PerfilView;
})(window.App);
