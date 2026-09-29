/* Pantalla de saldo, reclamación de devolución y ruleta. */
(function (App) {
  "use strict";
  const { h } = App.front.components.dom;
  const { pantalla } = App.front.components.layout;
  const { ponerTitulo } = App.front.components.navigation;
  const { ObjetosApi, RecompensasApi } = App.front.api;

  const puntosPorNombre = (nombre) => {
    const n = String(nombre || "").toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "");
    if (n.includes("portatil") || n.includes("laptop")) return 500;
    if (n.includes("audifono") || n.includes("headphone")) return 200;
    if (n.includes("cargador")) return 50;
    return 100;
  };
  const formato = (n) => Number(n || 0).toLocaleString("es-CO");

  class RecompensasView {
    async mostrar({ usuario }) {
      ponerTitulo("Mis puntos y ruleta");
      const [objetos, resumenInicial] = await Promise.all([ObjetosApi.obtenerObjetos(), RecompensasApi.consultar()]);
      let resumen = resumenInicial;
      const historial = new Map((resumen.recompensas || []).map((r) => [Number(r.objetoId), r]));
      const saldo = h("strong", { class: "points-total" }, formato(resumen.saldo));
      const mensaje = h("p", { class: "rewards-message", "aria-live": "polite" });
      const lista = h("div", { class: "rewards-list" });
      let ruedaDisco = null;

      const actualizarSaldo = async () => {
        resumen = await RecompensasApi.consultar();
        saldo.textContent = formato(resumen.saldo);
        historial.clear();
        (resumen.recompensas || []).forEach((r) => historial.set(Number(r.objetoId), r));
      };
      const pintar = () => {
        lista.replaceChildren();
        if (!objetos.length) {
          lista.append(h("div", { class: "rewards-empty" }, "Todavía no hay objetos publicados para gestionar recompensas."));
          return;
        }
        objetos.forEach((objeto) => {
          const puntos = puntosPorNombre(objeto.nombre);
          const premio = historial.get(Number(objeto.id));
          const card = h("article", { class: "reward-object" });
          const info = h("div", { class: "reward-object-info" },
            h("span", { class: "reward-object-name" }, objeto.nombre),
            h("span", { class: "reward-object-sub" }, "Recompensa base: " + formato(premio ? premio.puntosBase : puntos) + " puntos"));
          const acciones = h("div", { class: "reward-actions" });
          if (!premio) {
            const reclamar = h("button", { class: "btn btn-primary", type: "button" }, "Confirmar devolución y reclamar");
            reclamar.addEventListener("click", async () => {
              reclamar.disabled = true;
              mensaje.className = "rewards-message";
              mensaje.textContent = "Procesando la devolución…";
              try {
                const resultado = await RecompensasApi.reclamar(objeto.id);
                await actualizarSaldo();
                mensaje.className = "rewards-message success";
                mensaje.textContent = "¡Devolución confirmada! Sumaste " + formato(resultado.puntosGanados) + " puntos. Ahora puedes girar la ruleta.";
                pintar();
              } catch (e) {
                mensaje.className = "rewards-message error";
                mensaje.textContent = e.message;
                reclamar.disabled = false;
              }
            });
            acciones.append(reclamar);
          } else {
            const estado = h("span", { class: "reward-status" }, "Recompensa reclamada · +" + formato(premio.puntosBase) + " pts");
            acciones.append(estado);
            if (!premio.ruletaUsada) {
              const girar = h("button", { class: "btn btn-secondary", type: "button" }, "Girar ruleta");
              girar.addEventListener("click", async () => {
                girar.disabled = true;
                mensaje.className = "rewards-message";
                mensaje.textContent = "La ruleta está girando…";
                if (ruedaDisco) {
                  ruedaDisco.classList.remove("is-spinning");
                  void ruedaDisco.offsetWidth;
                  ruedaDisco.classList.add("is-spinning");
                }
                try {
                  const premioRuleta = await RecompensasApi.girar(objeto.id);
                  await actualizarSaldo();
                  mensaje.className = "rewards-message success";
                  mensaje.textContent = "¡Salió x" + premioRuleta.multiplicador + "! Ganaste " + formato(premioRuleta.puntosExtra) + " puntos extra. Tu saldo es " + formato(premioRuleta.saldo) + ".";
                  pintar();
                } catch (e) {
                  mensaje.className = "rewards-message error";
                  mensaje.textContent = e.message;
                  if (ruedaDisco) ruedaDisco.classList.remove("is-spinning");
                  girar.disabled = false;
                }
              });
              acciones.append(girar);
            } else {
              acciones.append(h("span", { class: "reward-status done" }, "Ruleta utilizada"));
            }
          }
          card.append(info, acciones);
          lista.append(card);
        });
      };
      pintar();
      const cabecera = h("section", { class: "rewards-hero" },
        h("div", { class: "rewards-kicker" }, "TU RECOMPENSA FYB"),
        h("h1", {}, "Mis puntos"),
        h("p", {}, "Acumula puntos al devolver objetos y prueba tu suerte en la ruleta."),
        h("div", { class: "balance-card" }, h("span", {}, "Saldo disponible"), h("div", { class: "balance-number" }, saldo, h("span", {}, "pts")))
      );
      const valores = h("section", { class: "rewards-panel" },
        h("h2", {}, "¿Cuánto vale cada objeto?"),
        h("p", { class: "panel-caption" }, "Puntos base por categoría. La bonificación de la ruleta se suma después."),
        h("div", { class: "points-table" },
          h("div", { class: "points-row points-heading" }, h("span", {}, "Categoría"), h("span", {}, "Puntos")),
          ...[["Portátil",500],["Audífonos",200],["Cargador",50],["Otros objetos",100]].map(([nombre,valor]) => h("div", { class: "points-row" }, h("span", {}, nombre), h("strong", {}, "+"+formato(valor))))
        )
      );
      const rueda = h("section", { class: "rewards-panel wheel-panel" },
        h("div", { class: "wheel-title" }, h("div", {}, h("h2", {}, "Ruleta de bonificación"), h("p", { class: "panel-caption" }, "Un giro por cada devolución confirmada.")), h("span", { class: "wheel-icon", "aria-hidden": "true" }, "✦")),
        h("div", { class: "wheel-visual", "aria-label": "Premios de la ruleta: 10, 25, 50 por ciento extra o doble de puntos" },
          h("div", { class: "wheel-pointer" }, "▼"),
          h("div", { class: "wheel-disc" }, ...["+10%","+25%","+50%","x2"].map((x,i)=>h("span", { class: "wheel-segment segment-"+(i+1) },x)), h("span", { class: "wheel-center" }, "FYB"))
        ),
        h("p", { class: "wheel-footnote" }, "La recompensa se sortea en el servidor y se acredita automáticamente al girar.")
      );
      ruedaDisco = rueda.querySelector(".wheel-disc");
      return pantalla(usuario,
        h("div", { class: "page-head rewards-page-head" }, h("div", {}, h("p", { class: "back-link" }, h("a", { href: "#/objetos" }, "← Volver a objetos")))) ,
        cabecera,
        h("div", { class: "rewards-layout" }, valores, rueda),
        h("section", { class: "rewards-panel claims-panel" }, h("h2", {}, "Devoluciones y reclamación de puntos"), h("p", { class: "panel-caption" }, "Cuando entregues un objeto y su devolución esté confirmada, selecciónalo aquí para reclamar la recompensa y habilitar su giro."), mensaje, lista)
      );
    }
  }
  App.front.recompensas = App.front.recompensas || {};
  App.front.recompensas.RecompensasView = RecompensasView;
})(window.App);
