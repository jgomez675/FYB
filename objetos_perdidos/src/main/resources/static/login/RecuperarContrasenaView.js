/* HU: recuperar la contraseña con un código enviado al correo institucional. */
(function (App) {
  "use strict";

  const { h, campo, mostrarMensaje, ocupado } = App.front.components.dom;
  const { logo, tarjetaAcceso } = App.front.components.layout;
  const { guardarAviso, irA, ponerTitulo } = App.front.components.navigation;
  const { AuthApi } = App.front.api;

  class RecuperarContrasenaView {
    async mostrar() {
      ponerTitulo("Recuperar contraseña");

      const cuerpo = h("div", {});
      let correoSolicitado = "";

      // Paso 1: pedir el código.
      const paso1 = () => {
        const mensaje = h("p", { class: "msg", role: "alert" });
        const correo = campo({
          id: "correo",
          etiqueta: "Correo institucional",
          type: "email",
          autocomplete: "username",
          placeholder: "usuario@" + App.config.emailDomain,
          value: correoSolicitado
        });
        const enviar = h("button", { class: "btn btn-primary", type: "submit" }, "Enviar código");
        const formulario = h("form", { class: "form", novalidate: true }, correo.wrap, enviar, mensaje);

        formulario.addEventListener("submit", async (evento) => {
          evento.preventDefault();

          const valor = correo.control.value.trim();
          if (!valor) {
            return mostrarMensaje(mensaje, "Escribe tu correo institucional.", "error");
          }

          ocupado(enviar, true);
          try {
            const r = await AuthApi.solicitarCodigo(valor);
            if (r.ok) {
              correoSolicitado = valor;
              paso2(r.mensaje);
            } else {
              mostrarMensaje(mensaje, r.mensaje, "error");
            }
          } catch (error) {
            mostrarMensaje(mensaje, error.message, "error");
          } finally {
            ocupado(enviar, false);
          }
        });

        const yaTengoCodigo = h("a", {
          href: "#",
          onclick: (evento) => {
            evento.preventDefault();
            const valor = correo.control.value.trim();
            if (!valor) {
              return mostrarMensaje(mensaje, "Escribe tu correo institucional.", "error");
            }
            correoSolicitado = valor;
            paso2("");
          }
        }, "Ya tengo un código");

        cuerpo.replaceChildren(
          h("p", { class: "tagline" }, "Escribe tu correo institucional y te enviaremos un código para crear una contraseña nueva."),
          formulario,
          h("p", { class: "alt" }, yaTengoCodigo)
        );
        correo.control.focus();
      };

      // Paso 2: escribir el código y la contraseña nueva.
      const paso2 = (aviso) => {
        const mensaje = h("p", { class: "msg", role: "alert" });
        const codigo = campo({
          id: "codigo",
          etiqueta: "Código de 6 dígitos",
          inputmode: "numeric",
          autocomplete: "one-time-code",
          maxlength: "6",
          placeholder: "123456"
        });
        const contrasena = campo({ id: "clave", etiqueta: "Contraseña nueva", type: "password", autocomplete: "new-password" });
        const confirmar = campo({ id: "confirmar", etiqueta: "Confirmar contraseña nueva", type: "password", autocomplete: "new-password" });
        const cambiar = h("button", { class: "btn btn-primary", type: "submit" }, "Cambiar contraseña");
        const formulario = h("form", { class: "form", novalidate: true }, codigo.wrap, contrasena.wrap, confirmar.wrap, cambiar, mensaje);

        formulario.addEventListener("submit", async (evento) => {
          evento.preventDefault();

          if (!codigo.control.value.trim() || !contrasena.control.value || !confirmar.control.value) {
            return mostrarMensaje(mensaje, "Completa todos los campos.", "error");
          }

          ocupado(cambiar, true);
          try {
            const r = await AuthApi.restablecerContrasena(
              correoSolicitado,
              codigo.control.value,
              contrasena.control.value,
              confirmar.control.value
            );

            if (r.ok) {
              guardarAviso("Contraseña actualizada. Ya puedes iniciar sesión con la nueva.");
              irA("#/login");
            } else {
              mostrarMensaje(mensaje, r.mensaje, "error");
            }
          } catch (error) {
            mostrarMensaje(mensaje, error.message, "error");
          } finally {
            ocupado(cambiar, false);
          }
        });

        const reenviar = h("a", {
          href: "#",
          onclick: async (evento) => {
            evento.preventDefault();
            try {
              const r = await AuthApi.solicitarCodigo(correoSolicitado);
              mostrarMensaje(mensaje, r.mensaje, r.ok ? "ok" : "error");
            } catch (error) {
              mostrarMensaje(mensaje, error.message, "error");
            }
          }
        }, "Reenviar código");

        const otroCorreo = h("a", {
          href: "#",
          onclick: (evento) => {
            evento.preventDefault();
            paso1();
          }
        }, "Usar otro correo");

        cuerpo.replaceChildren(
          h("p", { class: "tagline" }, "Escribe el código que llegó a " + correoSolicitado + " y elige tu contraseña nueva."),
          aviso ? h("p", { class: "msg ok" }, aviso) : null,
          formulario,
          h("p", { class: "alt" }, reenviar, " · ", otroCorreo)
        );
        codigo.control.focus();
      };

      paso1();

      return tarjetaAcceso(
        logo("div"),
        h("h1", { class: "auth-title" }, "Recuperar contraseña"),
        cuerpo,
        h("p", { class: "alt" }, h("a", { href: "#/login" }, "Volver a iniciar sesión"))
      );
    }
  }

  App.front.login.RecuperarContrasenaView = RecuperarContrasenaView;
})(window.App);
