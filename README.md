# 🏪 Sistema de Distribución de Alimentos con Despacho a Domicilio

## Reglas de Negocio

### RN-01: Cálculo de Tarifa de Despacho

| Monto de Compra | Condición | Tarifa de Despacho |
|---|---|---|
| ≥ $50.000 | Dentro de 20 km | **Gratis** |
| $25.000 – $49.999 | Cualquier distancia | **$150 / km recorrido** |
| < $25.000 | Cualquier distancia | **$300 / km recorrido** |

### RN-02: Control de Cadena de Frío

- Los alimentos como **carnes y mariscos congelados** requieren mantener la cadena de frío durante el transporte.
- El sistema debe monitorear la **temperatura del congelador del camión** en tiempo real.
- Si la temperatura **supera el límite permitido**, se debe emitir una **alarma en el dispositivo móvil** del administrador.

### RN-03: Autenticación

- Los usuarios se registran e inician sesión mediante sus **cuentas Gmail (Google OAuth)**.

---

## ✅ Requerimientos

### Requerimientos Funcionales

#### RF-01: Registro e Inicio de Sesión
- El sistema debe permitir que los usuarios se registren usando su cuenta de Gmail.
- El sistema debe permitir iniciar sesión mediante autenticación OAuth de Google.
- El sistema debe mantener la sesión activa entre usos de la aplicación.

#### RF-02: Cálculo Automático del Costo de Despacho
- El sistema debe solicitar o detectar la distancia entre la distribuidora y el domicilio del cliente.
- El sistema debe calcular el costo de despacho automáticamente según las reglas de negocio (RN-01).
- El costo de despacho debe mostrarse de forma clara antes de confirmar la compra.

#### RF-03: Monitoreo de Temperatura del Congelador
- El sistema debe leer la temperatura del congelador del camión en tiempo real.
- El sistema debe comparar la temperatura actual con el límite permitido.
- Si la temperatura supera el límite, el sistema debe emitir una **alarma/notificación push** en el dispositivo del administrador.

---

### Requerimientos No Funcionales

#### RNF-01: Compatibilidad de Plataforma
- La aplicación debe ser compatible con **Android Lollipop (API 21)** como versión mínima, dado que el administrador usa esta versión.
- La aplicación debe estar optimizada para **Android Oreo (API 26)** y versiones superiores, ya que es la versión más utilizada por los clientes potenciales.

#### RNF-02: Seguridad
- La autenticación debe realizarse exclusivamente mediante **Google Auth**.
- La comunicación entre la aplicación y el servidor debe estar cifrada mediante **HTTPS/TLS**.
- Los datos personales de los usuarios deben almacenarse de forma segura.

#### RNF-03: Rendimiento
- El cálculo del costo de despacho debe realizarse en menos de **2 segundos**.
- La carga del catálogo de productos no debe superar los **3 segundos** en condiciones de red normal.

#### RNF-04: Disponibilidad
- El sistema de monitoreo de temperatura debe estar disponible **24/7**.
- Las alertas de temperatura deben emitirse en **tiempo real**.

#### RNF-05: Usabilidad
- La interfaz debe ser intuitiva y accesible para usuarios no técnicos.
- El flujo de compra no debe requerir más de **5 pasos** desde el catálogo hasta la confirmación.

#### RNF-06: Escalabilidad
- El sistema debe soportar al menos **100 usuarios concurrentes** sin degradación del servicio.

---

## 📊 Casos de Uso

### CU-01: Registrarse en la Aplicación

| Campo | Detalle |
|---|---|
| **Actor principal** | Cliente |
| **Precondición** | El usuario tiene una cuenta de Gmail activa |
| **Postcondición** | El usuario queda registrado en el sistema |
| **Flujo principal** | 1. El usuario abre la aplicación. 2. Selecciona "Iniciar sesión con Google". 3. El sistema redirige a autenticación OAuth de Google. 4. El usuario acepta los permisos. 5. El sistema crea la cuenta y redirige al inicio. |
| **Flujo alternativo** | Si el usuario ya está registrado, el sistema inicia sesión directamente. |

---

### CU-02: Calcular Costo de Despacho

| Campo | Detalle |
|---|---|
| **Actor principal** | Sistema |
| **Precondición** | El usuario tiene productos en el carrito y ha ingresado su dirección |
| **Postcondición** | Se muestra el costo de despacho calculado |
| **Flujo principal** | 1. El sistema obtiene el monto total de la compra. 2. El sistema obtiene la distancia al domicilio del cliente. 3. Aplica la regla correspondiente (RN-01). 4. Muestra el costo al usuario. |

---

### CU-03: Monitorear Temperatura del Congelador

| Campo | Detalle |
|---|---|
| **Actor principal** | Sistema / Administrador |
| **Precondición** | El camión está en ruta con sensor de temperatura conectado |
| **Postcondición** | El administrador recibe una alerta si la temperatura supera el límite |
| **Flujo principal** | 1. El sistema lee la temperatura del sensor del congelador. 2. Compara la temperatura con el umbral definido. 3. Si está dentro del rango, registra el dato y espera el siguiente ciclo. 4. Si supera el límite, emite una notificación push al administrador. |
| **Excepción** | Si el sensor pierde conexión, se emite una alerta de "sensor desconectado". |

---

## 📝 Historias de Usuario

### HU-01: Registro con Gmail

> **Como** cliente nuevo,  
> **quiero** registrarme en la aplicación usando mi cuenta de Gmail,  
> **para** no tener que crear una nueva cuenta y contraseña.

**Criterios de aceptación:**
- El botón "Iniciar sesión con Google" está visible en la pantalla de bienvenida.
- Al completar la autenticación, el usuario queda registrado con su nombre, foto y correo de Google.
- Si el usuario ya existe, el sistema lo autentica directamente sin crear un duplicado.

---

### HU-02: Cálculo Automático del Despacho

> **Como** cliente,  
> **quiero** que el sistema calcule automáticamente el costo de despacho según mi compra y mi distancia,  
> **para** saber cuánto pagaré en total antes de confirmar el pedido.

**Criterios de aceptación:**
- El sistema solicita o detecta la dirección del cliente.
- Si el total de compra es ≥ $50.000 y está dentro de 20 km, el despacho se muestra como **Gratis**.
- Si el total es entre $25.000 y $49.999, se cobra $150 por km recorrido.
- Si el total es menor a $25.000, se cobra $300 por km recorrido.
- El costo de despacho aparece en el resumen antes de confirmar la compra.

---

### HU-03: Alerta de Temperatura del Congelador

> **Como** administrador,  
> **quiero** recibir una alarma en mi dispositivo móvil si la temperatura del congelador del camión supera el límite permitido,  
> **para** actuar a tiempo y proteger la cadena de frío de carnes y mariscos.

**Criterios de aceptación:**
- El sistema monitorea la temperatura del congelador en tiempo real.
- Si la temperatura supera el umbral, se envía una notificación push al dispositivo del administrador en menos de 30 segundos.
- La alerta incluye la temperatura actual, el límite y la hora del evento.
- La alerta es visible aunque la aplicación esté en segundo plano o bloqueada.

---

### HU-04: Panel de Temperatura en Tiempo Real

> **Como** administrador,  
> **quiero** ver la temperatura actual del congelador del camión desde mi aplicación,  
> **para** hacer seguimiento continuo sin esperar a que se active una alarma.

**Criterios de aceptación:**
- El panel del administrador muestra la temperatura actual del congelador.
- La temperatura se actualiza en intervalos de máximo 30 segundos.
- Se muestra un indicador visual (verde/amarillo/rojo) según el estado de la temperatura.

---

