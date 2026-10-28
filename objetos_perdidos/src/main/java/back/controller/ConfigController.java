package back.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConfigController {

    @GetMapping("/api/config")
    public String config() {
        return "{\"ok\":true}";
    }
}