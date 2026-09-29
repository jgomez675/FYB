/* Equivale a front/register/RegisterView.java */
(function (App) {
  "use strict";

  const { h, campo, mostrarMensaje, ocupado } = App.front.components.dom;
  const { logo, tarjetaAcceso } = App.front.components.layout;
  const { guardarAviso, irA, ponerTitulo } = App.front.components.navigation;
  const { AuthApi } = App.front.api;

  class RegisterView {
    async mostrar() {
      ponerTitulo("Crear cuenta");

      const mensaje = h("p", { class: "msg", role: "alert" });
      const nombre = campo({ id: "nombre", etiqueta: "Nombre completo", autocomplete: "name" });
      const correo = campo({ id: "correo", etiqueta: "Correo institucional", type: "email", autocomplete: "username", placeholder: "usuario@" + App.config.emailDomain });
      const contrasena = campo({ id: "clave", etiqueta: "Contraseña", type: "password", autocomplete: "new-password" });
      const confirmar = campo({ id: "confirmar", etiqueta: "Confirmar contraseña", type: "password", autocomplete: "new-password" });
      const registrar = h("button", { class: "btn btn-primary", type: "submit" }, "Crear cuenta");
      const formulario = h("form", { class: "form", novalidate: true }, nombre.wrap, correo.wrap, contrasena.wrap, confirmar.wrap, registrar, mensaje);

      formulario.addEventListener("submit", async (evento) => {
        evento.preventDefault();

        ocupado(registrar, true);
        try {
          const resultado = await AuthApi.registrarUsuario(
            nombre.control.value,
            correo.control.value,
            contrasena.control.value,
            confirmar.control.value
          );

          if (resultado === AuthApi.REGISTRO_EXITOSO) {
            guardarAviso("Cuenta creada correctamente. Ya puedes iniciar sesión.");
            irA("#/login");
          } else {
            mostrarMensaje(mensaje, resultado, "error");
          }
        } catch (error) {
          mostrarMensaje(mensaje, error.message, "error");
        } finally {
          ocupado(registrar, false);
        }
      });

      return tarjetaAcceso(
        logo("div"),
        h("h1", { class: "auth-title" }, "Crear cuenta"),
        h("p", { class: "tagline" }, "Regístrate en " + App.config.name + " con tu correo institucional"),
        formulario,
        h("p", { class: "alt" }, "¿Ya tienes una cuenta? ", h("a", { href: "#/login" }, "Inicia sesión"))
      );
    }
  }

  App.front.register.RegisterView = RegisterView;
})(window.App);
