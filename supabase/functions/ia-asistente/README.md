# Edge Function `ia-asistente` (Sprint 3)

Aquí va `index.ts`. Especificación completa: `docs/CASHIN_MAESTRO.md` §8
(acciones `estimar_meta`, `consejo`, `proponer_desafios`).

Comandos (requiere Supabase CLI):

```bash
supabase functions new ia-asistente      # crea index.ts
supabase secrets set GEMINI_API_KEY=... GEMINI_MODEL=...
supabase functions deploy ia-asistente
```

Desde la app: `supabase.functions.invoke("ia-asistente", body = ...)` (el token del usuario viaja solo).
