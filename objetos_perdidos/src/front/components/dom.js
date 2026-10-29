/*
 * Utilidades de DOM compartidas por todas las vistas (en JavaFX esto lo daban los
 * controles Label, TextField, Button...). Nada del contenido del usuario pasa por
 * innerHTML: siempre se inserta como texto.
 */
(function (App) {
  "use strict";

  const SVG_NS = "http://www.w3.org/2000/svg";

  // h("a", { href: "#/x", class: "btn" }, "Texto", otroNodo)
  function h(tag, props, ...children) {
    const el = document.createElement(tag);
    for (const [key, value] of Object.entries(props || {})) {
      if (value == null || value === false) continue;
      if (key === "class") el.className = value;
      else if (key === "text") el.textContent = value;
      else if (key.startsWith("on") && typeof value === "function") el.addEventListener(key.slice(2), value);
      else el.setAttribute(key, value === true ? "" : value);
    }
    for (const child of children.flat()) {
      if (child == null || child === false) continue;
      el.append(child.nodeType ? child : document.createTextNode(String(child)));
    }
    return el;
  }

  function iconoMarca() {
    const svg = document.createElementNS(SVG_NS, "svg");
    svg.setAttribute("viewBox", "0 0 32 32");
    svg.setAttribute("class", "brand-icon");
    svg.setAttribute("aria-hidden", "true");
    const lente = document.createElementNS(SVG_NS, "circle");
    lente.setAttribute("cx", "13");
    lente.setAttribute("cy", "13");
    lente.setAttribute("r", "8");
    const mango = document.createElementNS(SVG_NS, "path");
    mango.setAttribute("d", "M19.5 19.5 27 27");
    svg.append(lente, mango);
    return svg;
  }

  // Corazón de favoritos: vacío por defecto, relleno cuando el botón tiene la clase is-active.
  function iconoCorazon() {
    const svg = document.createElementNS(SVG_NS, "svg");
    svg.setAttribute("viewBox", "0 0 24 24");
    svg.setAttribute("class", "heart-icon");
    svg.setAttribute("aria-hidden", "true");
    const forma = document.createElementNS(SVG_NS, "path");
    forma.setAttribute("d", "M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z");
    svg.append(forma);
    return svg;
  }

  // Zona para avisos que aparecen en la pantalla (por ejemplo, "No se pudo guardar el favorito").
  // Devuelve { nodo, avisar(texto, tipo) }; avisar() sin texto la vacía.
  function crearAvisos() {
    const nodo = h("div", { class: "avisos", "aria-live": "polite" });
    const avisar = (texto, tipo) => {
      nodo.replaceChildren();
      if (texto) nodo.append(h("p", { class: "flash " + (tipo || "ok"), role: "status" }, texto));
    };
    return { nodo, avisar };
  }

  // Campo de formulario con su etiqueta visible. Devuelve { control, wrap }.
  function campo({ id, etiqueta, tag = "input", ...attrs }) {
    const props = { id, name: id, ...attrs };
    if (tag === "input" && !props.type) props.type = "text";
    const control = h(tag, props);
    return { control, wrap: h("div", { class: "field" }, h("label", { for: id }, etiqueta), control) };
  }

  function mostrarMensaje(el, texto, tipo) {
    el.textContent = texto;
    el.className = "msg" + (tipo ? " " + tipo : "");
  }

  function ocupado(boton, estaOcupado) {
    boton.disabled = estaOcupado;
    boton.setAttribute("aria-busy", String(estaOcupado));
  }

  const normalizar = (s) => s.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "");

  function hoyISO() {
    const d = new Date();
    return d.getFullYear() + "-" + String(d.getMonth() + 1).padStart(2, "0") + "-" + String(d.getDate()).padStart(2, "0");
  }

  function formatearFecha(iso) {
    const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso || "");
    if (!m) return iso || "";
    // Se construye con partes locales para que la zona horaria no corra el día.
    return new Date(+m[1], +m[2] - 1, +m[3]).toLocaleDateString(App.config.locale, {
      day: "numeric",
      month: "long",
      year: "numeric",
    });
  }

  App.front.components.dom = { h, iconoMarca, iconoCorazon, crearAvisos, campo, mostrarMensaje, ocupado, normalizar, hoyISO, formatearFecha };
})(window.App);
