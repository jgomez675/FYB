(function (App) {
  "use strict";
  const { h, campo, mostrarMensaje, ocupado } = App.front.components.dom;
  const { pantalla } = App.front.components.layout;
  const { ponerTitulo } = App.front.components.navigation;
  const { ObjetosApi } = App.front.api;

  class CategoriasAdminView {
    async mostrar({ usuario }) {
      ponerTitulo("Gestionar categorías");
      if (usuario.rol !== "admin") return pantalla(usuario, h("section", { class: "card notice" }, h("h1", {}, "Acceso restringido"), h("p", {}, "Esta sección es solo para el administrador."), h("a", { class: "btn btn-primary", href: "#/objetos" }, "Volver")));
      let categorias = await ObjetosApi.obtenerCategorias();
      const nombre = campo({ id: "nueva-categoria", etiqueta: "Nueva categoría", placeholder: "Ej. Instrumentos musicales" });
      const mensaje = h("p", { class: "msg", role: "status" });
      const lista = h("div", { class: "category-admin-list" });
      const agregar = h("button", { class: "btn btn-primary", type: "submit" }, "Agregar categoría");
      const formulario = h("form", { class: "form category-add" }, nombre.wrap, agregar, mensaje);

      const pintar = () => {
        lista.replaceChildren();
        categorias.forEach((categoria) => {
          const nombreEditado = h("input", { type: "text", value: categoria, "aria-label": "Nombre de " + categoria });
          const guardar = h("button", { class: "btn btn-secondary", type: "button" }, "Guardar");
          const eliminar = h("button", { class: "btn btn-danger", type: "button" }, "Eliminar");
          guardar.addEventListener("click", async () => {
            guardar.disabled = true;
            try { categorias = await ObjetosApi.editarCategoria(categoria, nombreEditado.value); mostrarMensaje(mensaje, "Categoría actualizada.", "ok"); pintar(); }
            catch (e) { mostrarMensaje(mensaje, e.message, "error"); guardar.disabled = false; }
          });
          eliminar.addEventListener("click", async () => {
            if (!confirm("¿Quieres eliminar la categoría «" + categoria + "»?")) return;
            try { categorias = await ObjetosApi.eliminarCategoria(categoria); mostrarMensaje(mensaje, "Categoría eliminada.", "ok"); pintar(); }
            catch (e) { mostrarMensaje(mensaje, e.message, "error"); }
          });
          lista.append(h("div", { class: "category-admin-row" }, nombreEditado, h("div", { class: "actions" }, guardar, eliminar)));
        });
      };
      formulario.addEventListener("submit", async (evento) => {
        evento.preventDefault(); ocupado(agregar, true);
        try { categorias = await ObjetosApi.crearCategoria(nombre.control.value); nombre.control.value = ""; mostrarMensaje(mensaje, "Categoría creada.", "ok"); pintar(); }
        catch (e) { mostrarMensaje(mensaje, e.message, "error"); }
        finally { ocupado(agregar, false); }
      });
      pintar();
      return pantalla(usuario,
        h("div", { class: "page-head" }, h("div", {}, h("h1", {}, "Gestionar categorías"), h("p", { class: "lede" }, "Crea, cambia o elimina las categorías disponibles para las publicaciones.")), h("a", { class: "btn btn-secondary", href: "#/objetos" }, "Volver a objetos")),
        h("section", { class: "card form-card" }, formulario),
        h("section", { class: "card form-card" }, h("h2", {}, "Categorías existentes"), lista)
      );
    }
  }
  App.front.objetos.CategoriasAdminView = CategoriasAdminView;
})(window.App);
