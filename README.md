# Servicio de Notificaciones Distribuido

Prueba Técnica Java Senior — Sagant

Servicio REST que recibe solicitudes de notificación, las persiste, y las despacha de
forma asincrónica por dos canales (LOG y SERVICE), con reintentos, registro de fallos
y logs estructurados.

---

## 1. Cómo levantar el proyecto

```bash
docker compose up --build
```

Levanta 3 contenedores: `postgres` (5432), `mock-receiver` (8081, un `httpbin` para
probar el canal SERVICE) y `notification-service` (8080). No requiere
ningún paso manual adicional.

Los datos de Postgres persisten en un volumen (`pg-data`) entre reinicios. Para arrancar
de cero: `docker compose down -v`.

**Probar el endpoint:**

```bash
curl -X POST http://localhost:8080/api/v1/notifications \
  -H "Content-Type: application/json" \
  -H "X-API-Key: local-dev-api-key-change-me" \
  -d '{
    "recipient": "http://mock-receiver/post",
    "channel": "SERVICE",
    "subject": "Prueba",
    "body": "Hola",
    "priority": "HIGH",
    "metadata": {"origen": "curl"}
  }'
```

Respuesta esperada: `202 Accepted` con `id` y `status: PENDING`. A los pocos segundos el
dispatcher la toma y despacha. Se puede consultar el estado con
`GET /api/v1/notifications/{id}` (mismo header de API Key), y ver el POST recibido con
`docker compose logs mock-receiver --tail 20`. Para el canal LOG, cambiar
`"channel": "SERVICE"` por `"LOG"` y revisar `docker compose logs notification-service`.

**Tests** (requiere Docker corriendo, usan Postgres real):

```bash
mvn clean test
```

---

## 2. Precondiciones

| Herramienta | Versión mínima |
|---|---|
| Docker + Docker Compose v2 | 24.x |
| Java (solo si se corre fuera de Docker) | 17 |
| Maven (solo si se corre fuera de Docker) | 3.9+ |

---

## 3. Credenciales de prueba

| Variable | Valor por defecto |
|---|---|
| API Key (header `X-API-Key`) | `local-dev-api-key-change-me` |
| Usuario / password Postgres | `notifications` / `notifications` |

Configurables por variable de entorno (`API_KEY`, `DB_USER`, `DB_PASSWORD`). En un entorno
real saldrían de un secret manager, no hardcodeadas — así quedan por simplicidad del ejercicio.

---

## 4. Decisiones de diseño

**Cola interna (tabla `notifications` + `@Scheduled`/`@Async`), no un broker externo.**
El enunciado dice que la notificación se encola "internamente", lo que se tomó como señal
de que no hace falta un broker. La tabla es a la vez historial de estado y cola de trabajo:
un scheduler toma lotes `PENDING` con `SELECT ... FOR UPDATE SKIP LOCKED`, lo que permite
múltiples instancias en paralelo sin pisarse (aunque hoy corra una sola). Se consideró
RabbitMQ y se descartó por ser un problema de cola de trabajo simple, no de streaming de
eventos — sumaba infraestructura sin necesidad real para el alcance del ejercicio.

**Autenticación por API Key**, no JWT/OAuth2. El escenario es service-to-service (un
cliente externo hace la llamada), sin usuarios humanos ni scopes diferenciados.

**Canal SERVICE (HTTP POST) como segundo canal**, no EMAIL. Es testeable sin
infraestructura externa real (se usa `httpbin` como mock).

**Retry con backoff exponencial** (`@Retryable`, 3 intentos, 1s/2s de espera) para evitar
un "retry storm" contra un destino ya degradado. Si se agotan los intentos, queda en
`FAILED` con el motivo del error.

**Desacople REST/despacho:** el endpoint solo valida y persiste (`202 Accepted`), nunca
llama a un canal directamente — así el servicio sigue respondiendo aunque el despacho esté
caído.

---

## 5. Trade-offs y limitaciones

Dejado afuera conscientemente por el acotamiento de tiempo:

- Se usa `ddl-auto: update`.
- **Sin canal EMAIL** — justificado arriba.
- **Sin idempotencia del cliente** (`Idempotency-Key`) — si un cliente reintenta el mismo
  POST por timeout, hoy generaría una notificación duplicada.
- **Notificaciones huérfanas en `PROCESSING`** si una instancia cae a mitad de despacho —
  no vuelven solas a `PENDING`. Se resolvería con un job que las recupere por timeout.
- **Sin rate limiting** en el endpoint público.
- **Sin Circuit Breaker** — el retry cubre fallos puntuales, pero no evita que muchas
  notificaciones sigan reintentando contra un destino caído por tiempo prolongado.

**Evolución a mayor escala:** migrar la cola a RabbitMQ, si el polling se vuelve cuello de botella manteniendo la tabla como
fuente de verdad del estado. Agregar Circuit Breaker (Resilience4j) si algún destino externo
empieza a fallar de forma sostenida.

---

## 6. Consideración sobre Jakarta EE

Si este servicio se deployara en un servidor Jakarta EE (ej. WildFly):

- **Empaquetado:** WAR en vez de JAR ejecutable.
- **Inyección de dependencias:** CDI (`@Inject`, `@ApplicationScoped`) en vez de Spring DI
  — la migración sería mecánica porque ya se usa inyección por constructor en todo el código.
- **REST:** JAX-RS (`@Path`, `@POST`) en vez de Spring MVC.
- **Asincronía:** `@Async` depende de un thread pool
  gestionado por Spring, el reemplazo correcto es `@Asynchronous` de Jakarta EE Concurrency sobre un
  `ManagedExecutorService` inyectado, gestionado por el servidor.
- **Consumo de cola (si se migrara a un broker):** un Message-Driven Bean (MDB) sobre JMS
  en vez de `@RabbitListener`, con JTA para transacciones distribuidas si el despacho
  tocara múltiples recursos transaccionales.
- **Configuración:** `persistence.xml` + DataSource por JNDI en vez de `application.yml`.

---