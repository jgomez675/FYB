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

  App.front.components.dom = { h, iconoMarca, campo, mostrarMensaje, ocupado, normalizar, hoyISO, formatearFecha };
})(window.App);
