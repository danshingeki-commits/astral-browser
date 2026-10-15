# Astral Assistant backend (Cloudflare Worker)

Este backend mantiene la clave de Gemini en el servidor y no dentro del APK.

## Configuración

1. Crea un Worker en Cloudflare y publica el contenido de `worker.js`.
2. En la configuración del Worker, añade `GEMINI_API_KEY` como **secret**. No la pongas en este repositorio ni en el código Android.
3. Usa la URL HTTPS del Worker como endpoint para Astral Browser.
4. Prueba primero con una petición POST JSON:
   `{"message":"Hola, Astral","history":[]}`
   Una respuesta correcta tiene el formato `{"reply":"..."}`.

## Seguridad y límites

- La clave de Gemini solo se lee desde `env.GEMINI_API_KEY`.
- El Worker limita el texto de entrada a 8000 caracteres y el historial enviado a 12 mensajes.
- Antes de usarlo públicamente, restringe el acceso al Worker o añade autenticación y límites de uso por usuario. CORS no es un sistema de autenticación.
- El nivel gratuito de Gemini está sujeto a disponibilidad, políticas y límites del proveedor. No configures facturación para esta prueba si el objetivo es mantener el coste en cero.
- No envíes datos privados ni contenido de páginas web sin consentimiento explícito.

## Estado

Este archivo es el proxy inicial. Astral Android todavía debe conectarse a la URL HTTPS del Worker; la integración no se considera terminada hasta probarla desde la app.
