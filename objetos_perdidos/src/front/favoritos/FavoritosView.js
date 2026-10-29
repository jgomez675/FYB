/* Lista de favoritos: los objetos que la persona guardó para consultarlos fácilmente. */
(function (App) {
  "use strict";

  const { h, crearAvisos } = App.front.components.dom;
  const { pantalla } = App.front.components.layout;
  const { tomarAviso, ponerTitulo } = App.front.components.navigation;
  const { tarjetaObjeto } = App.front.components.tarjeta;
  const { FavoritosApi } = App.front.api;

  class FavoritosView {
    async mostrar({ usuario }) {
      ponerTitulo("Favoritos");
      const favoritos = await FavoritosApi.obtenerFavoritos();

      const avisos = crearAvisos();
      const titulo = h("h1", {}, "Mis favoritos");
      const lista = h("div", { class: "grid" });
      const estado = h("p", { class: "status-line", "aria-live": "polite" });

      const pintar = () => {
        lista.replaceChildren();

        if (!favoritos.length) {
          estado.textContent = "";
          lista.append(
            h(
              "div",
              { class: "empty" },
              h("p", {}, "Todavía no tienes favoritos. Toca el corazón de un objeto para guardarlo aquí."),
              h("a", { class: "btn btn-primary", href: "#/objetos" }, "Ver objetos")
            )
          );
          return;
        }

        favoritos.forEach((o) => lista.append(tarjetaObjeto(o, { alCambiarFavorito: quitar, alError: (m) => avisos.avisar(m, "error") })));
        estado.textContent = favoritos.length === 1 ? "1 objeto guardado" : favoritos.length + " objetos guardados";
      };

      // Al quitar un favorito desaparece de esta lista; el foco pasa a la tarjeta vecina para no perderlo.
      const quitar = (objeto, esFavorito) => {
        if (esFavorito) return;

        const posicion = favoritos.indexOf(objeto);
        if (posicion >= 0) favoritos.splice(posicion, 1);
        pintar();
        avisos.avisar("Quitaste “" + objeto.nombre + "” de tus favoritos.");

        const botones = lista.querySelectorAll(".fav-btn");
        if (botones.length) {
          botones[Math.min(posicion, botones.length - 1)].focus();
        } else {
          titulo.setAttribute("tabindex", "-1");
          titulo.focus();
        }
      };

      pintar();

      return pantalla(
        usuario,
        tomarAviso(),
        h(
          "div",
          { class: "page-head" },
          h("div", {}, titulo, h("p", { class: "lede" }, "Los objetos que guardaste para consultarlos fácilmente")),
          h("a", { class: "btn btn-secondary", href: "#/objetos" }, "Ver todos los objetos")
        ),
        h("div", { class: "toolbar" }, estado),
        avisos.nodo,
        lista
      );
    }
  }

  App.front.favoritos.FavoritosView = FavoritosView;
})(window.App);
