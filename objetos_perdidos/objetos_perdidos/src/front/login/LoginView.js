/* Equivale a front/login/LoginView.java */
(function (App) {
  "use strict";

  const { h, campo, mostrarMensaje, ocupado } = App.front.components.dom;
  const { logo, tarjetaAcceso } = App.front.components.layout;
  const { guardarAviso, tomarAviso, irA, ponerTitulo } = App.front.components.navigation;
  const { AuthApi } = App.front.api;

  class LoginView {
    async mostrar() {
      ponerTitulo("Iniciar sesión");

      const mensaje = h("p", { class: "msg", role: "alert" });
      const correo = campo({ id: "correo", etiqueta: "Correo institucional", type: "email", autocomplete: "username", placeholder: "usuario@" + App.config.emailDomain });
      const contrasena = campo({ id: "clave", etiqueta: "Contraseña", type: "password", autocomplete: "current-password" });
      const iniciarSesion = h("button", { class: "btn btn-primary", type: "submit" }, "Iniciar sesión");
      const formulario = h("form", { class: "form", novalidate: true }, correo.wrap, contrasena.wrap, iniciarSesion, mensaje);

      formulario.addEventListener("submit", async (evento) => {
        evento.preventDefault();

        const correoIngresado = correo.control.value.trim();
        const contrasenaIngresada = contrasena.control.value;

        if (!correoIngresado || !contrasenaIngresada) {
          return mostrarMensaje(mensaje, "Completa todos los campos.", "error");
        }

        ocupado(iniciarSesion, true);
        try {
          const usuario = await AuthApi.iniciarSesion(correoIngresado, contrasenaIngresada);
          if (usuario) {
            guardarAviso("Bienvenido, " + usuario.nombre);
            irA("#/objetos");
          } else {
            mostrarMensaje(mensaje, "Correo o contraseña incorrectos.", "error");
          }
        } catch (error) {
          mostrarMensaje(mensaje, error.message, "error");
        } finally {
          ocupado(iniciarSesion, false);
        }
      });

      return tarjetaAcceso(
        logo("h1"),
        h("p", { class: "tagline" }, App.config.tagline),
        tomarAviso(),
        formulario,
        h("p", { class: "alt" }, "¿No tienes una cuenta? ", h("a", { href: "#/registro" }, "Regístrate"))
      );
    }
  }

  App.front.login.LoginView = LoginView;
})(window.App);
