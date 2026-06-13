package com.swordforge.web.api;

import com.swordforge.common.api.ApiResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/security")
public class SecurityApiController {

    public static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";

    @GetMapping("/csrf")
    public ApiResponse<CsrfTokenResponse> csrf(CsrfToken csrfToken) {
        return ApiResponse.ok(new CsrfTokenResponse(
                csrfToken.getHeaderName(),
                csrfToken.getParameterName(),
                csrfToken.getToken(),
                CSRF_COOKIE_NAME
        ));
    }

    public record CsrfTokenResponse(
            String headerName,
            String parameterName,
            String token,
            String cookieName
    ) {
    }
}
