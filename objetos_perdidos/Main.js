/*
 * Equivale a Main.java: arranca la app. Decide qué pantalla mostrar según el hash de la URL
 * (#/login, #/registro, #/objetos, #/objetos/3, #/publicar) y protege las que requieren sesión.
 */
(function (App) {
  "use strict";

  const { h } = App.front.components.dom;
  const { tarjetaAcceso } = App.front.components.layout;
  const { irA, ponerTitulo, aplicarMarca } = App.front.components.navigation;
  const { AuthService } = App.back.service;
  const { LoginView } = App.front.login;
  const { RegisterView } = App.front.register;
  const { ObjetosView, DetalleObjetoView, RegistrarObjetoView } = App.front.objetos;

  const rutas = [
    { patron: /^#\/login$/, requiereSesion: false, vista: LoginView },
    { patron: /^#\/registro$/, requiereSesion: false, vista: RegisterView },
    { patron: /^#\/objetos$/, requiereSesion: true, vista: ObjetosView },
    { patron: /^#\/objetos\/(\d+)$/, requiereSesion: true, vista: DetalleObjetoView },
    { patron: /^#\/publicar$/, requiereSesion: true, vista: RegistrarObjetoView },
  ];

  const raiz = document.getElementById("app");
  let pintadoActual = 0;

  async function mostrarRuta() {
    const idPintado = ++pintadoActual;
    const hash = location.hash || "#/";
    const usuario = AuthService.usuarioActual();

    let ruta = null;
    let coincidencia = null;
    for (const r of rutas) {
      const m = r.patron.exec(hash);
      if (m) {
        ruta = r;
        coincidencia = m;
        break;
      }
    }

    if (!ruta) return irA(usuario ? "#/objetos" : "#/login");
    if (ruta.requiereSesion && !usuario) return irA("#/login");
    if (!ruta.requiereSesion && usuario) return irA("#/objetos");

    let nodo;
    try {
      nodo = await new ruta.vista().mostrar({ usuario, params: coincidencia.slice(1) });
    } catch (error) {
      console.error(error);
      ponerTitulo("Error");
      nodo = tarjetaAcceso(h("h1", { class: "auth-title" }, "Algo salió mal"), h("p", { class: "tagline" }, "No se pudo cargar esta pantalla. Recarga la página e inténtalo de nuevo."));
    }
    if (idPintado !== pintadoActual) return; // el usuario ya navegó a otra pantalla

    raiz.replaceChildren(nodo);
    window.scrollTo(0, 0);

    const titulo = raiz.querySelector("h1");
    if (titulo) {
      titulo.setAttribute("tabindex", "-1");
      titulo.focus({ preventScroll: true });
    }
  }

  aplicarMarca();
  window.addEventListener("hashchange", mostrarRuta);
  mostrarRuta();
})(window.App);
