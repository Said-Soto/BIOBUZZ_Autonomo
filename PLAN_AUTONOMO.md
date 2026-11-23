# Autónomo BIOBUZZ: tres retos integradores

**Objetivo:** lograr un autónomo con Pedro Pathing que se desplace, apunte la torreta mediante trigonometría y AprilTags detectados por Limelight, dispare y termine estacionado. Utilizar el Control Hub y el Expansion Hub disponibles.

Desarrollar los tres retos como partes del mismo programa. **La única entrega final es un reporte breve del equipo y un video de prueba.** No se requieren entregas separadas por reto.

## Base de trabajo

Trabajar en dev y reutilizar los subsistemas existentes de chasis, intake, shooter y torreta. El proyecto declara Pedro Pathing 3.0.2, AutoTune 1.0.3 y SolversLib 0.3.6. Verificar compatibilidad al compilar; no copiar APIs antiguas de PathChain sin adaptarlas.

Confirmar localizador instalado, montaje de Limelight, referencia de cero de torreta y mapa de motores/encoders entre ambos hubs. Conservar el TeleOp operativo. Esta guía describe el trabajo por realizar; no acredita pruebas físicas ya realizadas.

## Reto 1. Recorrido autónomo con mecanismos simultáneos

**Indicación.** Crear un recorrido de inicio -> posición de tiro -> zona de recolección -> posición de tiro -> estacionamiento. Incluir una recta y una curva, orientación del chasis planificada y preparación del shooter o activación del intake durante el movimiento.

Medir las poses en el campo, validar localización y ajustar el Follower con las herramientas disponibles. Organizar la secuencia mediante comandos compatibles con SolversLib/Pedro o estados sin bloqueos. Actualizar el Follower y los mecanismos continuamente, con un solo responsable de cada actualización y de las salidas del chasis.

**Mini tutorial.** Definir primero las poses X/Y/rumbo; después construir la geometría del recorrido. Un punto de control Bézier moldea la curva y no necesariamente es un punto de paso. Elegir orientación constante para mantener el frente y orientación interpolada para girar durante el trayecto. Probar a velocidad moderada antes de incrementarla. Corregir signos, unidades y offsets del localizador antes de modificar ganancias.

**Comprobar.** El recorrido se repite desde la misma salida, prepara mecanismos sin detener el programa y responde a STOP. Cada tramo tiene condición de llegada y tiempo máximo; un timeout se registra como falla y no como llegada exitosa.

## Reto 2. Apuntado trigonométrico de torreta con Limelight

**Indicación.** Detectar el AprilTag correspondiente al objetivo válido de la alianza y calcular el ángulo de torreta hacia la abertura. Probar desde tres posiciones de tiro distintas, sin orientar manualmente la torreta.

Configurar el pipeline y verificar qué referencia devuelve Limelight. Si entrega el centro de un tag individual, aplicar el desplazamiento hasta el centro de la abertura. No asumir que reproduce automáticamente los clusters o metadatos del procesador AprilTag del SDK FTC. Comprobar que la celda esté orientada para recibir tiros y suspender alimentación mientras la colmena cambia de estado.

**Mini tutorial.** Transformar el punto de la abertura al marco del chasis y restar la posición del pivote. Usar X hacia delante y Y hacia la izquierda, convirtiendo previamente los ejes de la cámara:

~~~text
v = posicionAberturaEnChasis - posicionPivoteEnChasis
anguloObjetivo = atan2(v.y, v.x) - offsetCeroTorreta
distanciaHorizontal = sqrt(v.x^2 + v.y^2)
error = atan2(sin(anguloObjetivo - anguloActual),
              cos(anguloObjetivo - anguloActual))
~~~

Usar radianes internamente y unidades coherentes. Por ejemplo, un objetivo a 1.00 m delante y 0.30 m a la izquierda requiere aproximadamente 16.70 grados respecto al frente, antes de aplicar el cero mecánico.

Si la cámara gira con la torreta, su transformación depende del ángulo de torreta en el instante de captura. Si está fija al chasis, su montaje define una transformación fija. Considerar la separación entre cámara, pivote y lanzador; tx solo sirve como corrección directa cuando los signos, la referencia de objetivo y los offsets están comprobados.

Cerrar el control de posición con encoder y límites de giro. Filtrar detecciones, comprobar ID y caducidad, y exigir alineación estable antes de autorizar un tiro. No usar los tags móviles de la colmena como referencias fijas de localización global.

**Comprobar.** Desde las tres posiciones, la torreta converge hacia la abertura y muestra ID, ángulo objetivo/real y error. Al ocultar el objetivo o detectar uno inválido, bloquea el disparo y realiza una búsqueda limitada o cancela. La trigonometría determina dirección; RPM y elevación del tiro se calibran con pelotas reales.


### Seguimiento durante el movimiento, activado por botón

**Indicación.** En TeleOp, mantener presionado el botón de apuntado para que la torreta conserve la dirección hacia la abertura aunque el chasis avance, se desplace lateralmente o gire. Al soltarlo, cancelar el seguimiento y devolver la torreta al control manual, sin recentrarla automáticamente. Con el mando manual en reposo, detener la orden de giro; si existe retención de posición, retener únicamente el ángulo relativo al chasis. El botón no acciona el alimentador ni dispara.

Utilizar la asignación de apuntado del TeleOp si ya existe y documentarla. Interpretar la entrada como **mantener presionado**, no como un interruptor que queda activado tras una pulsación. Mientras esté presionado, el seguimiento tiene prioridad sobre el mando manual de torreta. Un solo controlador escribe a su motor.

**Mini tutorial.** Calcular el ángulo en cada ciclo a partir de la posición actual del pivote y del rumbo actual del chasis. Girar el chasis cambia el ángulo relativo de torreta; trasladarlo también cambia la dirección hacia el objetivo. La visión identifica y corrige el objetivo, y la odometría permite actualizar la geometría entre imágenes.

Para un objetivo expresado en el marco de campo, con rumbo de chasis psi, offset de pivote (a,b) en chasis y ángulo theta de torreta respecto a su cero:

~~~text
pivoteX = robotX + a*cos(psi) - b*sin(psi)
pivoteY = robotY + a*sin(psi) + b*cos(psi)
dx = objetivoX - pivoteX
dy = objetivoY - pivoteY
anguloObjetivo = wrap(atan2(dy, dx) - psi - offsetCeroTorreta)
error = atan2(sin(anguloObjetivo - theta),
              cos(anguloObjetivo - theta))
~~~

Todas las poses deben usar el mismo marco y unidades. wrap representa normalización angular. Elegir un ángulo equivalente que respete límites de giro y cables; no ordenar automáticamente el camino más corto si cruza un límite. Si el objetivo queda fuera de alcance mecánico, bloquear el disparo y mostrarlo en telemetría.

Cuando Limelight detecta la abertura, convertir su posición al campo usando pose del chasis y ángulo de torreta del instante de captura. No aplicar una imagen antigua como si fuera actual. Actualizar esa referencia con nuevas detecciones válidas y reinvalidarla si la colmena cambia de estado. El objetivo observado puede moverse; no conservar indefinidamente sus coordenadas ni utilizarlas para corregir la pose del robot como una referencia fija.

Lógica de control propuesta:

~~~text
si botonApuntado NO esta presionado:
    cancelar seguimiento y solicitudes automaticas pendientes
    ejecutar control manual de torreta
si botonApuntado esta presionado:
    seleccionar o actualizar objetivo valido
    si objetivo y localizacion son confiables:
        recalcular angulo con la pose actual
        controlar torreta con encoder
    si no hay informacion confiable:
        detener giro automatico y bloquear disparo
~~~

Definir un tiempo corto y medido para conservar seguimiento con odometría al perder una imagen, solo si la última referencia sigue siendo confiable. Mostrar BUSCANDO, SIGUIENDO, OBJETIVO_PERDIDO o FUERA_DE_LIMITE. Revalidar el objetivo al volver a presionar el botón; evitar saltos hacia una detección anterior.

**Comprobar dentro del mismo video.** Mantener el botón y ejecutar avance, desplazamiento lateral y giro de chasis: la torreta debe compensarlos y mantener la abertura dentro de la tolerancia angular definida. Soltar el botón durante el movimiento y demostrar que cesan las correcciones automáticas. Presionarlo otra vez y comprobar readquisición; ocultar brevemente el objetivo para verificar la respuesta. Registrar el error durante el movimiento, no solo al detenerse.

El seguimiento puede mantenerse mientras se mueve el robot; la autorización de disparo sigue requiriendo alineación y condiciones de tiro validadas. Para acertar mientras el robot se traslada, incorporar además compensación de velocidad y tiempo de vuelo mediante pruebas específicas: apuntar al objetivo en movimiento del chasis no resuelve por sí solo la trayectoria de la pelota.

En AUTO no se leen botones. Reutilizar el mismo controlador de seguimiento, habilitado y deshabilitado por las fases de la rutina. En TeleOp, la habilitación depende exclusivamente del botón mantenido.

## Reto 3. Autónomo completo con decisiones y recuperación

**Indicación.** Integrar el recorrido y el apuntado en una rutina que dispare precargas, realice un ciclo de recolección si alcanza el tiempo y termine estacionada. Programar también la opción de omitir el ciclo y estacionarse directamente.

Autorizar alimentación solamente cuando el shooter alcance velocidad estable, la torreta esté alineada y el objetivo sea válido. Dosificar según las posibilidades reales del mecanismo y esperar recuperación entre tiros. Si no hay sensor de pelota, declarar la limitación y no presentar un conteo estimado como detección confirmada.

**Mini tutorial.** Usar fases de preparación, recorrido, apuntado, tiro, recolección y estacionamiento. Cada fase termina por una condición observable o un timeout. Antes de otro ciclo comparar tiempo restante con duración medida del ciclo + estacionamiento + margen. Si falla visión, cancelar el tiro; si la pose sigue siendo confiable, usar la ruta de estacionamiento validada. Si falla la localización, detenerse.

**Comprobar.** Ejecutar cinco pruebas completas: tres nominales, una con pérdida temporal de visión y una con tiempo disponible reducido mediante configuración previa al arranque. Mostrar al menos una ejecución con recolección y otra que omita el ciclo. Buscar al menos cuatro de cinco ejecuciones con recorrido y estacionamiento correctos; registrar aciertos por separado. Todas deben detenerse dentro del período autónomo y permanecer sin movimiento accionado durante la transición.

## Entrega final del equipo

### Un reporte de 3 a 5 páginas

Incluir únicamente:

1. **Solución:** diagrama sencillo de la secuencia, trayectoria y configuración relevante de hardware/librerías.
2. **Apuntado:** ecuaciones empleadas, ubicación de cámara, offsets y explicación de cómo se obtiene la dirección de la abertura.
3. **Resultados:** tabla de las cinco pruebas, parámetros principales y conclusión breve sobre funcionamiento y limitaciones.

Añadir enlace al commit probado y al video. Las capturas o gráficas que ayuden a explicar el resultado van dentro del mismo reporte; no se solicitan anexos independientes.

| Prueba | Escenario | Tiempo | Tiros/aciertos | Estacionamiento | Falla o decisión tomada |
|---|---|---|---|---|---|
| 1 | Nominal | | | | |
| 2 | Nominal | | | | |
| 3 | Nominal | | | | |
| 4 | Pérdida temporal de visión | | | | |
| 5 | Tiempo disponible reducido | | | | |

### Un video de 3 a 5 minutos

Mostrar el apuntado desde las tres posiciones y su seguimiento mientras el chasis se mueve con el botón mantenido; demostrar también la liberación del botón. Incluir una ejecución completa con ciclo de recolección y una prueba de pérdida de visión. Dejar cada ejecución completa sin cortes internos ni aceleración. Mostrar el campo y telemetría legible de fase, tiempo, ID, ángulo de torreta y RPM. Explicar brevemente la decisión de omitir otro ciclo.

No hace falta grabar las cinco pruebas en el video final ni hacer una presentación adicional. Utilizar resultados reales de la misma versión de código identificada en el reporte.

## Referencias de apoyo

- [Pedro Pathing 3](https://pedropathing.com/docs/pathing/pedro3)
- [Trayectorias](https://pedropathing.com/docs/pathing/reference/api) y [organización del autónomo](https://pedropathing.com/docs/pathing/guide/auto-usage)
- [SolversLib/Pedro: verificar compatibilidad](https://docs.seattlesolvers.com/pedro-pathing/pedro-commands/followpathcommand)
- [Limelight 3A para FTC: confirmar modelo instalado](https://docs.limelightvision.io/docs/docs-limelight/getting-started/limelight-3a)
- [AprilTags y puntos de interés 3D](https://docs.limelightvision.io/docs/docs-limelight/pipeline-apriltag/apriltag-3d)
- [Clusters BIOBUZZ del SDK FTC](https://ftc-docs.firstinspires.org/en/latest/tech_tips/tech-tip-apriltag-clusters/tech-tip-apriltag-clusters.html)
- [Manual y actualizaciones oficiales BIOBUZZ](https://ftc-resources.firstinspires.org/ftc/game)
