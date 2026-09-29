/*
 * Arranque del front. Pide al servidor el nombre de la app, decide qué pantalla mostrar según
 * el hash de la URL (#/login, #/registro, #/objetos, #/objetos/3, #/publicar) y protege las
 * pantallas que requieren sesión.
 */
(function (App) {
  "use strict";

  const { h } = App.front.components.dom;
  const { tarjetaAcceso } = App.front.components.layout;
  const { guardarAviso, irA, ponerTitulo, aplicarMarca } = App.front.components.navigation;
  const { ApiClient, AuthApi, ErrorApi } = App.front.api;
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

  // El nombre de la app vive en el servidor (AppConfig.java). Si no responde, se usan los de config.js.
  async function cargarConfiguracion() {
    try {
      const r = await ApiClient.solicitar("GET", "/api/config");
      if (r.estado === 200 && r.datos) {
        if (r.datos.nombre) App.config.name = r.datos.nombre;
        if (r.datos.eslogan) App.config.tagline = r.datos.eslogan;
        if (r.datos.dominioCorreo) App.config.emailDomain = r.datos.dominioCorreo;
      }
    } catch (e) {
      /* se quedan los valores por defecto */
    }
  }

  async function mostrarRuta() {
    const idPintado = ++pintadoActual;
    const hash = location.hash || "#/";

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

    let nodo;
    try {
      const usuario = await AuthApi.usuarioActual();
      if (idPintado !== pintadoActual) return; // el usuario ya navegó a otra pantalla

      if (!ruta) return irA(usuario ? "#/objetos" : "#/login");
      if (ruta.requiereSesion && !usuario) return irA("#/login");
      if (!ruta.requiereSesion && usuario) return irA("#/objetos");

      nodo = await new ruta.vista().mostrar({ usuario, params: coincidencia.slice(1) });
    } catch (error) {
      if (error.estado === 401) {
        guardarAviso(error.message, "error");
        return irA("#/login");
      }
      console.error(error);
      ponerTitulo("Error");
      const detalle = error instanceof ErrorApi ? error.message : "No se pudo cargar esta pantalla. Recarga la página e inténtalo de nuevo.";
      nodo = tarjetaAcceso(h("h1", { class: "auth-title" }, "Algo salió mal"), h("p", { class: "tagline" }, detalle));
    }
    if (idPintado !== pintadoActual) return;

    raiz.replaceChildren(nodo);
    window.scrollTo(0, 0);

    const titulo = raiz.querySelector("h1");
    if (titulo) {
      titulo.setAttribute("tabindex", "-1");
      titulo.focus({ preventScroll: true });
    }
  }

  async function arrancar() {
    await cargarConfiguracion();
    aplicarMarca();
    window.addEventListener("hashchange", mostrarRuta);
    mostrarRuta();
  }

  arrancar();
})(window.App);
