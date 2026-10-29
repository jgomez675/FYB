/*
 * Botón para guardar un objeto en favoritos o quitarlo. Dos formas:
 *   "icono" (corazón sobre la foto de la tarjeta) y "texto" (botón con texto en el detalle).
 * Actualiza objeto.favorito, así que la lista y el detalle siempre muestran el estado real.
 */
(function (App) {
  "use strict";

  const { h, iconoCorazon } = App.front.components.dom;
  const { guardarAviso, irA } = App.front.components.navigation;
  const { FavoritosApi } = App.front.api;

  // opciones: { variante: "icono" | "texto", alCambiar(favorito), alError(mensaje) }
  function botonFavorito(objeto, opciones) {
    const { variante = "icono", alCambiar, alError } = opciones || {};
    const conTexto = variante === "texto";

    const boton = h("button", { type: "button", class: conTexto ? "btn btn-secondary fav-text" : "fav-btn" });
    const etiqueta = conTexto ? h("span", {}) : null;
    boton.append(iconoCorazon());
    if (etiqueta) boton.append(etiqueta);

    // Con icono el nombre es fijo y aria-pressed dice si está guardado; con texto cambia el texto visible.
    if (!conTexto) boton.setAttribute("aria-label", "Favorito: " + objeto.nombre);

    const pintar = () => {
      const activo = !!objeto.favorito;
      boton.classList.toggle("is-active", activo);
      if (conTexto) {
        etiqueta.textContent = activo ? "Quitar de favoritos" : "Guardar en favoritos";
      } else {
        boton.setAttribute("aria-pressed", String(activo));
        boton.title = activo ? "Quitar de favoritos" : "Guardar en favoritos";
      }
    };

    // No se usa disabled mientras se guarda: el botón perdería el foco del teclado.
    let pendiente = false;
    boton.addEventListener("click", async () => {
      if (pendiente) return;
      pendiente = true;
      boton.setAttribute("aria-busy", "true");

      const quiere = !objeto.favorito;
      try {
        if (quiere) await FavoritosApi.guardar(objeto.id);
        else await FavoritosApi.quitar(objeto.id);

        objeto.favorito = quiere;
        pintar();
        if (alCambiar) alCambiar(quiere);
      } catch (error) {
        if (error.estado === 401) {
          guardarAviso(error.message, "error");
          irA("#/login");
        } else if (alError) {
          alError(error.message);
        }
      } finally {
        pendiente = false;
        boton.removeAttribute("aria-busy");
      }
    });

    pintar();
    return boton;
  }

  App.front.components.favorito = { botonFavorito };
})(window.App);
