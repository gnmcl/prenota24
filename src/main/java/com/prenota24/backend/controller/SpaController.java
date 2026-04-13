package com.prenota24.backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Fallback controller: restituisce index.html per qualsiasi route non-API,
 * in modo che Angular possa gestire il routing lato client.
 */
@Controller
public class SpaController {

    @RequestMapping(value = {
            "/",
            "/{path:[^\\.]*}",
            "/{path:^(?!api|actuator).*$}/**/{subpath:[^\\.]*}"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
