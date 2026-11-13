/*
 * Piezas de pantalla que se repiten: el logo, la tarjeta de acceso, la barra superior
 * con la sesiÃ³n y la foto de un objeto.
 */
(function (App) {
  "use strict";

  const { h, iconoMarca } = App.front.components.dom;
  const { irA } = App.front.components.navigation;
  const { AuthApi } = App.front.api;

  // Logo con el nombre de la app.
  function logo(tag) {
    return h(
      "div",
      { class: "wordmark" },
      iconoMarca(),
      h(tag, { class: "wm" }, App.config.name)
    );
  }

  // Tarjeta blanca centrada sobre el fondo azul.
  function tarjetaAcceso(...hijos) {
    return h(
      "div",
      { class: "auth" },
      h(
        "section",
        { class: "card auth-card" },
        ...hijos
      )
    );
  }

  // Pantalla con sesiÃ³n iniciada: barra superior + contenido.
  function pantalla(usuario, ...contenido) {

    const cerrarSesion = async () => {
      await AuthApi.cerrarSesion();
      irA("#/login");
    };

    const enlaces = [
      ["#/objetos", "Explorar"],
      ["#/publicar", "Publicar objeto"],
      ["#/mis-publicaciones", "Mis publicaciones"],
      ["#/mis-reclamaciones", "Reclamaciones"],
      ["#/notificaciones", "Notificaciones"],
      ["#/puntos", "Puntos"],
      ["#/perfil", "Mi perfil"]
    ];

    if (usuario.correo && usuario.correo.toLowerCase() === "admin@usa.edu.co") {
      enlaces.push(["#/admin", "Administración"]);
    }

    const navegacion = h(
      "nav",
      { class: "app-nav", "aria-label": "Navegación principal" },
      ...enlaces.map(([href, texto]) => h(
        "a",
        { class: "app-nav-link" + (location.hash === href ? " is-active" : ""), href, ...(location.hash === href ? { "aria-current": "page" } : {}) },
        texto
      ))
    );

    const barra = h(
      "header",
      { class: "topbar" },
      h(
        "div",
        { class: "topbar-inner" },
        h(
          "a",
          {
            class: "brand-link",
            href: "#/objetos",
            "aria-label": App.config.name + ", ir al inicio"
          },
          iconoMarca(),
          h("span", {}, App.config.name),
          h("small", { class: "brand-caption" }, "COMUNIDAD · OBJETOS PERDIDOS")
        ),
        h(
          "div",
          { class: "topbar-user" },
          h("span", { class: "user-avatar", "aria-hidden": "true" },
            (usuario.nombre || "U").trim().charAt(0).toUpperCase()
          ),
          h("span", { class: "user-name", text: usuario.nombre }),
          h("button", {
            class: "btn btn-ghost logout-button",
            type: "button",
            onclick: cerrarSesion
          }, "Cerrar sesión")
        )
      ),
      navegacion
    );

    return h(
      "div",
      {},
      barra,
      h(
        "main",
        { class: "container" },
        ...contenido
      )
    );
  }

  function imagenObjeto(objeto, clase) {

    const valida =
      typeof objeto.imagenUrl === "string" &&
      objeto.imagenUrl.startsWith(
        "/api/imagenes/"
      );

    return h(
      "div",
      { class: clase },

      valida
        ? h(
            "img",
            {
              src: objeto.imagenUrl,
              alt:
                "Foto de " +
                objeto.nombre,
              loading: "lazy"
            }
          )
        : h(
            "span",
            { class: "no-img" },
            "Sin imagen"
          )
    );
  }

  App.front.components.layout = {
    logo,
    tarjetaAcceso,
    pantalla,
    imagenObjeto
  };

})(window.App);
