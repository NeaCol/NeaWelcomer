![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Paper](https://img.shields.io/badge/Paper%20%2F%20Purpur-1.21.1-1B1E2B?style=for-the-badge&logo=apachemaven&logoColor=white)
![Adventure](https://img.shields.io/badge/Adventure-MiniMessage-5B5FFF?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-brightgreen?style=for-the-badge)

[English](#english) | [Español](#español)

---

<a id="english"></a>
# NeaWelcomer

**NeaWelcomer** is a high-performance, developer-grade connection greeting and player retention plugin engineered for modern **Paper** and **Purpur 1.21.1+** Minecraft servers. Powered natively by Kyori Adventure, NeaWelcomer delivers rich MiniMessage formatting, asynchronous non-blocking player tracking, protection against tag injection vulnerabilities, multi-channel connection delivery (chat broadcasts, private welcomes, on-screen titles, hotbar action bars, and spatial audio), and granular permission-based visibility filtering.

---

## Architecture & Technical Highlights

- **Adventure MiniMessage Engine**: Native support for 24-bit TrueColor RGB (`<#RRGGBB>`, `<color:#RRGGBB>`), linear and multi-stop gradients (`<gradient:#7C4DFF:#00BCD4>`), standard font decorations (`<bold>`, `<italic>`, `<underlined>`, `<strikethrough>`, `<obfuscated>`), and interactive components (hover tooltips `<hover:show_text:...>`, command execution `<click:run_command:...>`, suggestions `<click:suggest_command:...>`, and URLs `<click:open_url:...>`).
- **Tag Injection Sanitization**: Dynamic placeholders (`{player}`, `{displayname}`, `{rank}`) are resolved via Kyori Adventure's `TagResolver` and `Placeholder.unparsed`, strictly isolating runtime strings from the parser to prevent MiniMessage injection attacks or unauthorized command execution via formatted nicknames.
- **Asynchronous Unique Player Tracking (`UniquePlayerTracker`)**: Eliminates the catastrophic main-thread disk I/O of legacy plugins scanning the `world/playerdata` directory. Performs an initial non-blocking asynchronous directory scan via Java NIO `Files.newDirectoryStream` on startup, caching and incrementing counts in real-time with a thread-safe `AtomicInteger`.
- **Screen Titles & Subtitles (`Title.Times`)**: Cinematic welcome titles rendered directly onto player viewports using Adventure's `Title.Times` API with configurable tick-based fade-in, stay, and fade-out animation timings (20 ticks = 1 second).
- **Hotbar Action Bar Delivery**: Unobtrusive connection notifications displayed directly above the player's hotbar via `player.sendActionBar(...)`, avoiding chat spam while ensuring high visibility.
- **Visibility Permission Filtering**: Independent permission checks (`neawelcomer.join.see`, `neawelcomer.quit.see`, `neawelcomer.firstjoin.see`) ensure broadcasts are only received by eligible players, enabling silent connections and staff-only announcements.
- **Spatial Audio Effects**: Configurable audio playback using native Paper/Spigot `Sound` keys with independent volume and pitch controls for first-join, recurring join, and quit events.
- **PlaceholderAPI Hook**: Seamless soft-dependency integration with PlaceholderAPI (`me.clip.placeholderapi`) for external placeholders (e.g. `%vault_prefix%`, `%vault_group%`), with automatic fallback to internal placeholders if PlaceholderAPI is absent.

---

## Permissions Matrix

| Permission Node | Default | Description |
|---|---|---|
| `neawelcomer.admin` | `op` | Grants access to `/neawelcomer` administrative commands and tab-completion. |
| `neawelcomer.join.see` | `true` | Allows players to receive recurring join broadcast messages and audio feedback. |
| `neawelcomer.quit.see` | `true` | Allows players to receive player quit broadcast messages and audio feedback. |
| `neawelcomer.firstjoin.see` | `true` | Allows players to receive first-join announcement broadcasts and audio feedback. |

---

## Command Reference

| Command | Aliases | Permission | Description |
|---|---|---|---|
| `/neawelcomer reload` | `/nw reload`, `/welcomer reload` | `neawelcomer.admin` | Hot-reloads `config.yml` and `lang.yml` with execution duration benchmarking (ms). |
| `/neawelcomer info` | `/nw info`, `/welcomer info` | `neawelcomer.admin` | Displays runtime status, version, platform target, MiniMessage engine status, and PlaceholderAPI hook status. |
| `/neawelcomer help` | `/nw help`, `/welcomer help` | `neawelcomer.admin` | Displays the interactive command help menu with supported subcommands and syntax. |

---

## Configuration & Placeholders

### Placeholder Reference

The following placeholders are supported across all message lines, titles, subtitles, and action bars:

| Placeholder | Description | Resolution Strategy |
|---|---|---|
| `{player}` | Player's exact username | `Player#getName()`, sanitized with `Placeholder.unparsed` |
| `{displayname}` | Player's formatted display name | `Player#displayName()`, sanitized component |
| `{rank}` | Primary permission group or Vault prefix | LuckPerms / Vault / PAPI (`%vault_prefix%` / `%vault_group%`) |
| `{player_number}` | Unique sequential join number (e.g. `#42`) | `UniquePlayerTracker`, atomic thread-safe counter |
| `{online}` | Current online player count | `Server#getOnlinePlayers().size()` |
| `{max_players}` | Server maximum player capacity | `Server#getMaxPlayers()` |
| `{world}` | World name where player joined/disconnected | `World#getName()` |
| `{prefix}` | Global plugin prefix defined in `lang.yml` | `lang.yml` configuration value |

### Configuration Structure

- **`config.yml`**: Controls core plugin behavior, module toggles (`disable-default-messages`, `placeholderapi`), event toggles (`first-join`, `join`, `quit`), title animation timings (`fade-in`, `stay`, `fade-out` in ticks), action bar toggles, and Bukkit `Sound` properties (sound key, volume, pitch).
- **`lang.yml`**: Controls all user-facing presentation layers using Adventure MiniMessage. Configures multi-line chat messages, screen titles, subtitles, action bar strings, command responses, and error handlers.

---

## Build & Installation Guide

### Prerequisites
- **Java Development Kit (JDK)**: Version 21 or higher
- **Apache Maven**: Version 3.9.0 or higher
- **Server Platform**: Paper, Purpur, or Folia 1.21.1+

### Building from Source

```bash
# Clone repository
git clone https://github.com/NeaCol/NeaWelcomer.git
cd NeaWelcomer

# Compile and package production artifact
mvn clean package
```

The compiled JAR artifact will be generated in `target/NeaWelcomer-1.0.0.jar`.

### Installation
1. Copy `target/NeaWelcomer-1.0.0.jar` into your server's `plugins/` directory.
2. (Optional) Install [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) for external placeholder expansions.
3. Start or restart your server to generate default configurations.
4. Modify `plugins/NeaWelcomer/config.yml` and `plugins/NeaWelcomer/lang.yml` to your desired specifications.
5. Apply changes in real-time by executing `/neawelcomer reload`.

---

<a id="español"></a>
# NeaWelcomer (Español)

**NeaWelcomer** es un plugin de conexión y bienvenida de alto rendimiento y grado profesional diseñado para servidores de Minecraft **Paper** y **Purpur 1.21.1+**. Desarrollado nativamente sobre Kyori Adventure, ofrece formato moderno mediante MiniMessage, seguimiento asíncrono no bloqueante de jugadores únicos, protección integral contra vulnerabilidades de inyección de etiquetas, entrega multicanal de mensajes (chat público, bienvenidas privadas, títulos en pantalla, barras de acción y audio espacial) y filtrado granular de visibilidad mediante permisos.

---

## Arquitectura y Aspectos Técnicos Destacados

- **Motor Adventure MiniMessage**: Soporte nativo para colores RGB TrueColor de 24 bits (`<#RRGGBB>`, `<color:#RRGGBB>`), gradientes lineales y multi-tono (`<gradient:#7C4DFF:#00BCD4>`), estilos tipográficos (`<bold>`, `<italic>`, `<underlined>`, `<strikethrough>`, `<obfuscated>`) y componentes interactivos (sugerencias flotantes `<hover:show_text:...>`, ejecución de comandos `<click:run_command:...>`, sugerencias de texto `<click:suggest_command:...>` y enlaces web `<click:open_url:...>`).
- **Sanitización contra Inyección de Etiquetas**: Los marcadores dinámicos (`{player}`, `{displayname}`, `{rank}`) se resuelven a través de `TagResolver` y `Placeholder.unparsed` de Kyori Adventure, aislando rigurosamente las cadenas de texto del analizador sintáctico para evitar ataques de inyección MiniMessage o ejecución no autorizada de comandos a través de nombres formateados.
- **Seguimiento Asíncrono de Jugadores Únicos (`UniquePlayerTracker`)**: Elimina el impacto crítico en el hilo principal (*main thread*) ocasionado por el escaneo tradicional del directorio `world/playerdata`. Ejecuta un escaneo inicial asíncrono no bloqueante mediante Java NIO `Files.newDirectoryStream` al iniciar el servidor, manteniendo y actualizando el contador en memoria en tiempo real con un `AtomicInteger` seguro para múltiples hilos.
- **Títulos y Subtítulos en Pantalla (`Title.Times`)**: Títulos cinemáticos proyectados en la pantalla del jugador mediante la API `Title.Times` de Adventure con tiempos configurables en ticks para animaciones de entrada (*fade-in*), permanencia (*stay*) y salida (*fade-out*) (20 ticks = 1 segundo).
- **Barras de Acción (Action Bars)**: Mensajes sutiles de conexión mostrados sobre la barra de acceso rápido del jugador con `player.sendActionBar(...)`, garantizando alta visibilidad sin saturar el historial del chat.
- **Filtrado de Visibilidad por Permisos**: Verificaciones independientes de permisos (`neawelcomer.join.see`, `neawelcomer.quit.see`, `neawelcomer.firstjoin.see`) que garantizan que las difusiones sean recibidas únicamente por jugadores autorizados, permitiendo conexiones silenciosas y avisos exclusivos para el equipo administrativo.
- **Efectos de Sonido Espacial**: Reproducción configurable de efectos de sonido utilizando identificadores nativos de `org.bukkit.Sound` en Paper con ajustes independientes de volumen y tono (*pitch*) para eventos de primer ingreso, reingreso y desconexión.
- **Integración con PlaceholderAPI**: Conexión opcional (*soft-dependency*) con PlaceholderAPI (`me.clip.placeholderapi`) para expandir marcadores externos (`%vault_prefix%`, `%vault_group%`), con respaldo automático a variables internas en caso de no encontrarse instalado.

---

## Matriz de Permisos

| Nodo de Permiso | Por Defecto | Descripción |
|---|---|---|
| `neawelcomer.admin` | `op` | Otorga acceso a los comandos administrativos de `/neawelcomer` y autocompletado con Tab. |
| `neawelcomer.join.see` | `true` | Permite a los jugadores visualizar los mensajes de difusión y escuchar sonidos de reingreso. |
| `neawelcomer.quit.see` | `true` | Permite a los jugadores visualizar los mensajes de difusión y escuchar sonidos de desconexión. |
| `neawelcomer.firstjoin.see` | `true` | Permite a los jugadores visualizar los mensajes de bienvenida de primer ingreso y sus efectos sonoros. |

---

## Referencia de Comandos

| Comando | Alias | Permiso | Descripción |
|---|---|---|---|
| `/neawelcomer reload` | `/nw reload`, `/welcomer reload` | `neawelcomer.admin` | Recarga en caliente `config.yml` y `lang.yml` reportando el tiempo exacto de ejecución (ms). |
| `/neawelcomer info` | `/nw info`, `/welcomer info` | `neawelcomer.admin` | Muestra el estado operativo, versión, plataforma de destino, estado de MiniMessage y conexión con PlaceholderAPI. |
| `/neawelcomer help` | `/nw help`, `/welcomer help` | `neawelcomer.admin` | Despliega el menú interactivo de ayuda con los subcomandos disponibles y su sintaxis. |

---

## Configuración y Marcadores de Posición (Placeholders)

### Marcadores de Posición Soportados

Los siguientes marcadores son compatibles en todas las líneas de mensaje, títulos, subtítulos y barras de acción:

| Marcador | Descripción | Mecanismo de Resolución |
|---|---|---|
| `{player}` | Nombre de usuario exacto del jugador | `Player#getName()`, sanitizado con `Placeholder.unparsed` |
| `{displayname}` | Nombre visible formateado del jugador | `Player#displayName()`, componente seguro |
| `{rank}` | Grupo de permisos principal o prefijo Vault | LuckPerms / Vault / PAPI (`%vault_prefix%` / `%vault_group%`) |
| `{player_number}` | Número secuencial único de ingreso (ej. `#42`) | `UniquePlayerTracker`, contador atómico seguro en memoria |
| `{online}` | Cantidad de jugadores actualmente conectados | `Server#getOnlinePlayers().size()` |
| `{max_players}` | Capacidad máxima de jugadores del servidor | `Server#getMaxPlayers()` |
| `{world}` | Nombre del mundo donde ocurrió la conexión/desconexión | `World#getName()` |
| `{prefix}` | Prefijo global del plugin definido en `lang.yml` | Valor configurado en `lang.yml` |

### Estructura de Archivos de Configuración

- **`config.yml`**: Administra el comportamiento global del plugin, activación de módulos (`disable-default-messages`, `placeholderapi`), módulos de eventos (`first-join`, `join`, `quit`), duraciones de animación de títulos (`fade-in`, `stay`, `fade-out` en ticks), conmutadores de barra de acción y propiedades de sonido Bukkit (clave de sonido, volumen, tono).
- **`lang.yml`**: Administra la capa de presentación visual para el usuario mediante Adventure MiniMessage. Define mensajes de chat multilínea, títulos en pantalla, subtítulos, barras de acción, respuestas de comandos y controladores de error.

---

## Guía de Compilación e Instalación

### Requisitos Previos
- **Java Development Kit (JDK)**: Versión 21 o superior
- **Apache Maven**: Versión 3.9.0 o superior
- **Plataforma de Servidor**: Servidor compatible con Paper, Purpur o Folia 1.21.1+

### Compilación desde Código Fuente

```bash
# Clonar el repositorio
git clone https://github.com/NeaCol/NeaWelcomer.git
cd NeaWelcomer

# Compilar y empaquetar el binario de producción
mvn clean package
```

El archivo JAR resultante se generará en `target/NeaWelcomer-1.0.0.jar`.

### Instalación
1. Copie el archivo generado `target/NeaWelcomer-1.0.0.jar` dentro del directorio `plugins/` de su servidor.
2. (Opcional) Instale [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) si desea utilizar marcadores externos.
3. Inicie o reinicie su servidor para generar los archivos de configuración predeterminados.
4. Edite `plugins/NeaWelcomer/config.yml` y `plugins/NeaWelcomer/lang.yml` según los requisitos de su comunidad.
5. Aplique los cambios en caliente ejecutando el comando `/neawelcomer reload`.
