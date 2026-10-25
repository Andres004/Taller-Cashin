# Configurar Supabase

> ✅ **Ya está hecho** (Andy, 24/10/2026). Esta guía queda como referencia por si hay que volver a crear el proyecto.

Tiempo estimado: 15 minutos. Al terminar, la app permite crear cuenta, ingresar, recuperar la contraseña y cerrar sesión.

## 1. Crear el proyecto

1. Entrar a https://supabase.com con la cuenta del equipo → **New project**.
2. Nombre: `cashin` · Región: **South America (São Paulo)** · guardar la contraseña de la base de datos en un lugar seguro.
3. Esperar a que termine de crearse (1–2 minutos).

## 2. Crear las tablas

1. Menú izquierdo → **SQL Editor** → **New query**.
2. Pegar todo `supabase/migrations/001_esquema.sql` → **Run**. Debe decir *Success. No rows returned*.
3. Nueva consulta → pegar `supabase/migrations/002_rls.sql` → **Run**.
4. Verificar en **Table Editor**: deben aparecer 9 tablas (`perfil`, `categoria`, `registro`, `registro_planificado`, `meta`, `aporte_meta`, `deuda`, `abono_deuda`, `desafio`) y `categoria` debe tener 11 filas.

> Regla del equipo: cualquier cambio a la base de datos se hace con un archivo nuevo `supabase/migrations/00X_descripcion.sql` en un PR, y se ejecuta en el SQL Editor después de aprobarse. Nunca editar tablas a mano desde el panel.

## 3. Configurar el login con correo

**Authentication → Sign In / Providers → Email**
- *Enable Email provider*: activado.
- *Confirm email*:
  - **Durante el desarrollo: desactivado** (así se prueba rápido; al crear la cuenta se entra directo).
  - Para la demo final se puede activar: la app ya muestra "revisa tu correo".
- *Minimum password length*: 8.

**Authentication → URL Configuration**
- *Site URL*: `https://cashin.app` (no se usa en la app, pero no puede quedar vacío).

## 4. Plantilla del correo de recuperación (HU-03)

La app recupera la contraseña con un **código numérico** (sin enlaces), así no hace falta configurar deep links. La app acepta códigos de 6 a 10 dígitos; el largo se define en **Authentication → Sign In / Providers → Email → Email OTP Length** (proyectos nuevos: 8).

**Primero, SMTP propio:** Supabase exige un servidor de correo propio para editar plantillas.
1. En la cuenta de Gmail del proyecto: activar la **verificación en 2 pasos** y crear una **contraseña de aplicación** en https://myaccount.google.com/apppasswords.
2. **Authentication → Emails → SMTP Settings → Enable custom SMTP**:
   Sender email y Username = el Gmail · Sender name = `Cashin` · Host `smtp.gmail.com` · Port `465` · Password = la contraseña de aplicación (16 letras, sin espacios).
3. **Authentication → Rate Limits**: subir el límite de correos a `30` por hora.

**Después, Authentication → Emails → Templates → Reset Password**: reemplazar el cuerpo por:

```html
<h2>Recupera tu contraseña de Cashin</h2>
<p>Tu código es:</p>
<p style="font-size:28px;font-weight:bold;letter-spacing:6px">{{ .Token }}</p>
<p>Escríbelo en la app junto con tu nueva contraseña. Vence en 1 hora.</p>
<p>Si no lo pediste, ignora este correo.</p>
```

Opcional (si se activa *Confirm email*): en **Confirm signup** dejar el enlace `{{ .ConfirmationURL }}`; al tocarlo la cuenta queda confirmada y el usuario vuelve a la app a iniciar sesión.


## 5. Copiar las claves a la app

**Project Settings → API Keys** (o botón **Connect** arriba):
- `Project URL` → `SUPABASE_URL`
- clave **anon / publishable** → `SUPABASE_KEY`

Pegarlas en `local.properties` (ver `local.properties.example`) y compartirlas con el equipo **por mensaje privado**.

⚠️ Nunca usar ni compartir la clave `service_role` / `secret`: salta la seguridad RLS.

## 6. Prueba rápida

1. Correr la app → **Crear cuenta** con un correo real.
2. En Supabase → **Table Editor → perfil** debe aparecer una fila con el nombre escrito.
3. Cerrar sesión desde **Perfil** e ingresar de nuevo.
4. Probar **¿Olvidaste tu contraseña?** → llega el código (6 a 10 dígitos) → cambiar contraseña → la app entra sola.

## 7. Probar el SQL en una PC (opcional)

`supabase/tests/` tiene un simulador del esquema `auth` de Supabase y pruebas de RLS para un PostgreSQL 15+ local:

```bash
createdb cashin_test
psql -d cashin_test -f supabase/tests/00_mock_supabase_local.sql \
  -f supabase/migrations/001_esquema.sql -f supabase/migrations/002_rls.sql
psql -d cashin_test -f supabase/tests/pruebas_rls.sql
```

Las líneas marcadas "ERROR esperado" deben fallar (así se demuestra que la seguridad funciona); el resto debe pasar.
