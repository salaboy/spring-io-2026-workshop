package com.example.store;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Forwards all unmatched GET requests to the React SPA's index.html,
 * enabling client-side routing. The regex pattern [^\\.] excludes paths
 * containing a dot (static assets like .js, .css, .svg), which are served
 * directly by Spring's ResourceHttpRequestHandler before this controller is reached.
 */
@Controller
public class SpaController {

    @RequestMapping("/{path:[^\\.]*}")
    public String forward() {
        return "forward:/index.html";
    }
}
