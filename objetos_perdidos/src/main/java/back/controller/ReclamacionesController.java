package back.controller;

import back.api.SessionManager;
import back.service.ReclamacionesService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/reclamaciones")
public class ReclamacionesController {
    @PostMapping
    public Map<String,Object> crear(@RequestBody Map<String,Object> body,HttpServletRequest request) {
        SessionManager.Sesion s=sesion(request);
        int objetoId; try { objetoId=Integer.parseInt(String.valueOf(body.get("objetoId"))); } catch(Exception e) { throw new IllegalArgumentException("Objeto no válido."); }
        String explicacion=String.valueOf(body.getOrDefault("explicacion", "")).trim();
        if(explicacion.length()<15) throw new IllegalArgumentException("Explica con al menos 15 caracteres por qué reconoces el objeto.");
        if(explicacion.length()>2000) throw new IllegalArgumentException("La explicación no puede superar 2000 caracteres.");
        try { var r=ReclamacionesService.crear(objetoId,s.correo(),explicacion); return Map.of("ok",true,"reclamacion",r,"mensaje","Tu solicitud fue enviada al propietario."); }
        catch(java.io.IOException e) { throw new RuntimeException("No se pudo guardar la solicitud."); }
    }
    @GetMapping("/recibidas") public Map<String,Object> recibidas(HttpServletRequest request) { return Map.of("reclamaciones",ReclamacionesService.recibidas(sesion(request).correo())); }
    @GetMapping("/enviadas") public Map<String,Object> enviadas(HttpServletRequest request) { return Map.of("reclamaciones",ReclamacionesService.enviadas(sesion(request).correo())); }
    @PostMapping("/{id}/decision") public Map<String,Object> decidir(@PathVariable String id,@RequestBody Map<String,Object> body,HttpServletRequest request) {
        String decision=String.valueOf(body.getOrDefault("decision","")); String respuesta=String.valueOf(body.getOrDefault("respuesta",""));
        try { var r=ReclamacionesService.decidir(id,sesion(request).correo(),decision,respuesta); return Map.of("ok",true,"reclamacion",r,"mensaje",decision.equals("APROBADA")?"Reclamación aprobada. Ya puedes coordinar la entrega.":"Reclamación rechazada."); }
        catch(java.io.IOException e) { throw new RuntimeException("No se pudo guardar la decisión."); }
    }
    private SessionManager.Sesion sesion(HttpServletRequest req) { Cookie[] cs=req.getCookies(); if(cs!=null) for(Cookie c:cs) if("fyb_session".equals(c.getName())) { var s=SessionManager.obtener(c.getValue()); if(s!=null)return s; } throw new RuntimeException("Tu sesión expiró. Inicia sesión de nuevo."); }
}
