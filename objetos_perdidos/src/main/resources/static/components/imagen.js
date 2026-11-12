/* Reduce la foto en el navegador antes de subirla (el servidor solo recibe hasta 5 MB). */
(function (App) {
  "use strict";

  const MAX_BYTES = 10 * 1024 * 1024;

  function procesarImagen(archivo) {
    return new Promise((resolve, reject) => {
      if (!archivo || !/^image\//.test(archivo.type)) {
        return reject(new Error("El archivo seleccionado no es una imagen."));
      }
      if (archivo.size > MAX_BYTES) {
        return reject(new Error("La imagen pesa más de 10 MB. Elige una más liviana."));
      }

      const url = URL.createObjectURL(archivo);
      const img = new Image();

      img.onload = () => {
        const lado = Math.max(img.naturalWidth, img.naturalHeight) || 1;
        const escala = Math.min(1, App.config.maxImageSide / lado);
        const ancho = Math.max(1, Math.round(img.naturalWidth * escala));
        const alto = Math.max(1, Math.round(img.naturalHeight * escala));

        const canvas = document.createElement("canvas");
        canvas.width = ancho;
        canvas.height = alto;
        const ctx = canvas.getContext("2d");
        ctx.fillStyle = "#ffffff"; // los PNG transparentes quedan sobre blanco al pasar a JPEG
        ctx.fillRect(0, 0, ancho, alto);
        ctx.drawImage(img, 0, 0, ancho, alto);

        URL.revokeObjectURL(url);
        resolve(canvas.toDataURL("image/jpeg", 0.82));
      };
      img.onerror = () => {
        URL.revokeObjectURL(url);
        reject(new Error("No se pudo leer la imagen. Prueba con un archivo PNG o JPG."));
      };
      img.src = url;
    });
  }

  App.front.components.imagen = { procesarImagen };
})(window.App);
