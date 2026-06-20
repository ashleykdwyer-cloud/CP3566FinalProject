/**
 * TODO (student) — THE WEB LAYER.   PROJECT_BRIEF.html §4.5–§4.6
 *
 * Add your @RestController classes in this package:
 *   - POST /api/login                              (200 / 401)
 *   - GET  /api/cases [?status=]  ·  GET /api/cases/{id}
 *   - POST /api/cases/{id}/{pickup|escalate|send-back|close-false|close-fraud}
 *   - POST /api/cases/{id}/notes  ·  GET /api/rules  ·  PUT /api/rules/{code} (ADMIN)
 * Return JSON and the correct status codes (200/201/401/403/404/409).
 *
 * Add the two security checks from §4.6 here (or in a filter/interceptor):
 *   login first (401), then role (403). Verify passwords with a PasswordEncoder.
 *
 * Keep controllers THIN: take the request, call a @Service, return the answer —
 * no business logic in this layer.
 */
package com.example.fraud.web;
