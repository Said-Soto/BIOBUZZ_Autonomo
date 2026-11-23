# Autónomo BIOBUZZ con Pedro Pathing 3

Construir un autónomo repetible de precargas, recolección, lanzamiento y estacionamiento, con trayectorias continuas, mecanismos simultáneos y recuperación de fallas. Resolver las prácticas en orden y conservar mediciones de cada cambio.


## Objetivo final y entrega única

Implementar y demostrar un autónomo completo con Pedro Pathing, torreta con apuntado trigonométrico a partir de AprilTags y cámara Limelight. Integrar el hardware disponible: un Control Hub y un Expansion Hub. Mantener las funciones operativas del TeleOp.

La entrega formal consiste únicamente en:

1. **Un reporte técnico del equipo**, en PDF, con enlace al commit probado y anexos de medición dentro del mismo documento.
2. **Un video de prueba del equipo**, con enlace incluido en el reporte.

Los resultados de las prácticas son evidencia para estos dos productos; no se solicitan reportes ni videos separados por práctica. El código y los parámetros quedan en este repositorio y se identifican mediante un único commit de entrega.

### Comportamiento que debe demostrar el robot

- Inicializar pose, alianza, salida y referencia mecánica de torreta antes del arranque, sin movimiento inesperado.
- Recorrer una trayectoria Pedro hasta una posición de tiro, preparando los mecanismos en paralelo.
- Detectar el objetivo válido mediante Limelight, calcular su dirección con trigonometría y orientar la torreta automáticamente.
- Disparar únicamente con celda elegible, detección confiable o alternativa validada, alineación estable y shooter listo.
- Ejecutar un ciclo de recolección y retorno si dispone de tiempo; si no, elegir estacionamiento.
- Cancelar alimentación ante pérdida de condiciones y estacionarse antes de terminar AUTO, o detenerse si no hay localización confiable para hacerlo.
- Finalizar sin movimiento accionado durante la transición y conservar una referencia coherente para TeleOp.

## Especificación de Limelight y torreta

### A. Configuración y selección de objetivo

Confirmar modelo y firmware de Limelight. Si es Limelight 3A, usar la integración FTC del SDK y conexión/configuración correspondiente al Control Hub; no trasladar ejemplos de NetworkTables/roboRIO de FRC. Documentar nombre en hardwareMap, pipeline de AprilTags, familia, tamaño físico, resolución, exposición y frecuencia real de resultados.

Seleccionar por IDs de la alianza y geometría válida. No apuntar simplemente al tag más grande. Un tag visible no garantiza que la CELL sea elegible para recibir tiros. Documentar la relación ID -> celda -> alianza y validar su orientación en ambos estados de la colmena y durante el vuelco. Suspender disparos mientras cambie de estado.

Los clusters de BIOBUZZ del procesador AprilTag del SDK FTC pueden representar el centro de la abertura. **No asumir que Limelight devuelve automáticamente esa misma referencia o metadatos de cluster.** Verificar lo que produce el pipeline instalado. Si entrega tags individuales, transformar desde el tag detectado hasta el punto de tiro usando la geometría oficial/CAD medida. Si admite un punto de interés 3D adecuado, configurarlo y comprobarlo físicamente.

Mini tutorial: detectar identifica el tag; estimar pose obtiene su relación espacial con la cámara; transformar sitúa la abertura respecto a la torreta; atan2 obtiene el giro requerido; el controlador mueve el motor hasta ese ángulo. Son etapas distintas y comprobables por separado.

### B. Geometría trigonométrica obligatoria

Definir un marco de cálculo de chasis: X hacia delante, Y a la izquierda y Z arriba. Convertir explícitamente a este marco los ejes/unidades de Limelight y del localizador. Usar radianes internamente y grados solo al presentar o donde la API lo exija.

Notación: R_AB rota vectores expresados en B al marco A; p_AB es la posición del origen B expresada en A. C=cámara, T=tag, B=chasis. Todos los vectores deben usar la misma unidad.

~~~text
p_C_abertura = p_CT + R_CT * p_T_abertura
p_B_abertura = p_BC + R_BC * p_C_abertura
v_B = p_B_abertura - p_B_pivote
anguloObjetivo = atan2(v_B.y, v_B.x) - offsetCeroTorreta
distanciaHorizontal = sqrt(v_B.x^2 + v_B.y^2)
errorAngular = atan2(sin(anguloObjetivo - anguloActual),
                     cos(anguloObjetivo - anguloActual))
~~~

p_T_abertura es el offset calibrado del tag a la abertura. Si el resultado ya representa ese punto, no sumarlo otra vez. Si solo se obtiene cámara respecto al tag, invertir correctamente la transformación: R_CT=transpose(R_TC), p_CT=-R_CT*p_TC. No intercambiar transforms por su nombre sin comprobar el sentido.

Si la cámara gira con la torreta, R_BC y p_BC dependen de su ángulo y offsets. Usar el ángulo correspondiente al instante de captura; limitar o compensar errores por latencia. Si está fija al chasis, esa transformación es fija. Corregir el desplazamiento entre pivote y salida real del lanzador cuando afecte al tiro.

Ejemplo geométrico: v_B=(1.00, 0.30, 0.20) metros produce una dirección horizontal de aproximadamente 16.70 grados y distancia horizontal de 1.044 m. Esto prueba el cálculo; no es una pose del campo ni una calibración de tiro.

Mini tutorial alternativo 2D: después de convertir signos y usar un punto de interés que represente la abertura, tx puede proporcionar el error visual horizontal. Si cámara y lanzador comparten dirección y tienen offsets despreciables o compensados, el objetivo se puede obtener sumando esa corrección al ángulo actual. Validar moviendo un objetivo a izquierda y derecha; no asignar el signo por intuición. No sumar tx de un objetivo con la distancia de otro tag.

La estimación d=(alturaObjetivo-alturaCamara)/tan(inclinacionCamara+ty) solo se acepta como comparación bajo sus hipótesis geométricas: altura y pitch conocidos, referencia correcta y denominador alejado de cero. La colmena móvil y su geometría inclinada pueden invalidarla. Priorizar una pose 3D válida y contrastarla con medición física.

### C. Control y autorización de disparo

Referenciar la torreta mediante sensor o procedimiento físico repetible. Aplicar límites mecánicos y de cableado; si un ángulo equivalente no es alcanzable, reorientar el chasis o cancelar. El cálculo trigonométrico encuentra una dirección, no elimina los límites del mecanismo.

Controlar la posición con realimentación de encoder, salida limitada y ajuste registrado. Comprobar si el eje puede envolver libremente antes de elegir el camino angular corto. Filtrar el ángulo sin introducir retrasos excesivos y exigir permanencia dentro de tolerancia.

Crear estados BUSCAR, ADQUIRIR, APUNTAR, LISTO y OBJETIVO_PERDIDO. Publicar ID/celda, edad del resultado, dirección calculada, ángulo real, error y razón de bloqueo. Una lectura repetida no es un frame nuevo. Definir un umbral de caducidad medido y comprobar las APIs de latencia/timestamp disponibles.

La elevación geométrica atan2(v_B.z, distanciaHorizontal) apunta en línea recta: no resuelve la caída de una pelota. Calibrar RPM y elevación física, si es ajustable, mediante tiros reales. No inventar una velocidad de salida ni equiparar RPM de motor a velocidad de pelota.

## Integración de Control Hub y Expansion Hub

Registrar serial/nombre y puerto de cada dispositivo: motores de chasis, shooter, intake/alimentación, torreta, encoders y sensores. Confirmar que el total cabe en los puertos disponibles. Indicar conexiones entre hubs y alimentación según el sistema REV real; no asignar puertos sin inspeccionar el robot.

Separar consumo de motores de comunicación de cámara. Confirmar que cada encoder llega al puerto que corresponde al motor o al localizador configurado. Medir tiempo de ciclo con follower, scheduler y visión activos. Evitar dos actualizaciones independientes del mismo sensor y dos controladores escribiendo al mismo motor. Conservar todas las constantes en archivos versionados, incluida la calibración cámara/torreta.

## Entregable 1: reporte técnico único

Nombre de entrega: REPORTE_AUTONOMO_BIOBUZZ.pdf. Completar con resultados reales. Extensión orientativa: 8 a 12 páginas de contenido más anexos integrados si hacen falta.

| Sección | Contenido requerido |
|---|---|
| Identificación | Equipo, fecha, objetivo, commit probado y enlace al video |
| Hardware y configuración | Control Hub/Expansion Hub, mapa de puertos, Limelight, sensores y versiones |
| Arquitectura | Relación entre Pedro, scheduler, mecanismos y visión; dueño de cada actualización |
| Estrategia | Ruta y poses, alianza/salida, cronograma, condiciones para otro ciclo y estacionamiento |
| Localización y tuning | Procedimiento, parámetros finales y comparación antes/después |
| Geometría de torreta | Marcos, offsets, ecuaciones atan2/sin/cos, ejemplo numérico y validación de signos |
| Visión | Pipeline, IDs, elección de celda, transformación a abertura, latencia y pérdida de objetivo |
| Control del tiro | Cero/límites, estabilidad angular, RPM, dosificación y condiciones de autorización |
| Resultados | Veinte ejecuciones completas, tasa de éxito, tiempos, errores y causas de falla |
| Conclusiones | Qué funciona, limitaciones observadas y cambios prioritarios |
| Anexos | Tabla completa, parámetros, capturas y referencias, dentro del mismo PDF |

Incluir un diagrama de estados o decisiones y una gráfica de ángulo objetivo/real. Añadir las marcas de tiempo del video que demuestren cada requisito.

Tabla mínima de pruebas integrada en el reporte:

| Intento | Escenario | Tiempo AUTO | Error de pose | Error de torreta al tiro | Tiros/aciertos | Estacionó | Resultado y falla |
|---|---|---|---|---|---|---|---|
| Por medir | | | | | | | |

Distinguir acierto en abertura y vuelco efectivamente observado. No contar un giro del alimentador como anotación ni una salida por timeout como éxito.

## Entregable 2: video de prueba único

Nombre de entrega: VIDEO_AUTONOMO_BIOBUZZ.mp4, o un enlace reproducible. Duración orientativa de 6 a 10 minutos. Se permite editar entre pruebas; cada ejecución mostrada debe verse completa, con su inicio, final y cualquier falla, sin cortes internos ni aceleración.

| Bloque | Evidencia |
|---|---|
| Presentación breve | Robot real, hubs y ubicación de Limelight; commit mostrado |
| Trigonometría y torreta | Objetivo a izquierda/centro/derecha; ángulo calculado y real, ID y error visibles |
| Tres ejecuciones completas | Misma versión; trayectoria, apuntado, disparos y estacionamiento; al menos una con ciclo adicional |
| Pérdida de visión | Ocluir el objetivo: detener alimentación, búsqueda limitada o alternativa validada; recuperar detección |
| Selección correcta | Rechazar objetivo ajeno o celda no elegible; mostrar razón de bloqueo |
| Final y transición | Robot detenido al concluir AUTO; inicio de TeleOp coherente |

Tomar un plano donde se vean trayectoria y objetivo, acompañado de telemetría legible mediante captura sincronizada o inserto. No ejecutar el apuntado con correcciones manuales durante AUTO. Incluir en el reporte las veinte pruebas; no es necesario mostrar las veinte en el video.

## Criterios de aceptación de la entrega

- Un reporte y un video, ambos correspondientes al mismo commit y configuración.
- Autónomo ejecutado en el robot, con Pedro y mecanismos coordinados.
- Apuntado de torreta calculado con trigonometría a partir de una detección válida de Limelight.
- Evidencia de compensación tag-abertura y cámara-torreta; no basta centrar visualmente un tag.
- Rechazo de objetivo inválido, pérdida de visión y timeouts sin alimentación indebida.
- Meta inicial: al menos 18 de 20 ejecuciones completas cumplen la ruta y el estacionamiento; registrar por separado tasa de acierto y vuelcos. Toda ejecución debe detenerse dentro del período.
- Establecer tolerancias angulares a partir de la abertura útil, distancia, dispersión del tiro y error de medición. Como estimación inicial: tolerancia <= atan((anchoUtil/2 - margen)/distancia), solo con numerador positivo y en condiciones frontales; validar y reducir con tiros reales.

### Fuentes para la integración de visión

- [Limelight 3A y FTC](https://docs.limelightvision.io/docs/docs-limelight/getting-started/limelight-3a)
- [AprilTags 3D y punto de interés](https://docs.limelightvision.io/docs/docs-limelight/pipeline-apriltag/apriltag-3d)
- [Clusters BIOBUZZ del SDK FTC; no confundir con resultados de Limelight](https://ftc-docs.firstinspires.org/en/latest/tech_tips/tech-tips/tech-tip-apriltag-clusters/tech-tip-apriltag-clusters.html)

## Base del proyecto

Trabajar desde la rama dev. TeamCode/build.gradle declara:

| Componente | Versión declarada |
|---|---|
| Pedro Pathing revhub | 3.0.2 |
| Pedro tuning / AutoTune | 1.0.3 |
| SolversLib core y pedroPathing | 0.3.6 |
| FTC Dashboard | 0.6.0 |
| Panels | 1.0.12 |

TeleOpMain utiliza CommandOpMode y subsistemas de chasis, intake, shooter, torreta y Pinpoint. Reutilizar sus interfaces de hardware y separar las decisiones de autónomo de las entradas de los gamepads.

La coexistencia de dependencias no prueba su compatibilidad. Verificar imports y compilación del adaptador SolversLib/Pedro antes de construir la rutina. Pedro 3 cambió su API: identificar y adaptar referencias antiguas a PathChain, builders o paquetes de Pose. No actualizar librerías como parte de una sesión de tuning.

Esta guía define trabajo y ejemplos de apoyo; no declara el autónomo implementado ni probado.

## Secuencia de trabajo

| Práctica | Resultado |
|---|---|
| 1 | Localización validada y configuración reproducible |
| 2 | Follower calibrado y registro de repetibilidad |
| 3 | Ruta con rectas, curvas y orientación planificada |
| 4 | Adaptador de seguimiento y orquestación con comandos |
| 5 | Lanzamiento condicionado por velocidad y alineación |
| 6 | Recolección con eventos, confirmación y recuperación |
| 7 | Apuntado con odometría y visión |
| 8 | Ciclos adaptativos, variantes y estacionamiento |
| 9 | Validación de competencia y transferencia a TeleOp |

## 1. Validar localización y marcos de referencia

**Indicación.** Confirmar el localizador que utiliza el Follower, hardware conectado, offsets, resolución, signos y unidades. Verificar que PinpointSubsystem y Pedro describan la misma posición del robot. Si ambos leen el mismo dispositivo, centralizar su actualización y evitar resets independientes.

Marcar en el campo origen, ejes, rumbo cero, posición de salida y punto de referencia del robot. Documentar la transformación entre campo, chasis, torreta y cámara. Ejecutar desplazamientos longitudinales, laterales, rotaciones y un recorrido cerrado con medición externa.

**Mini tutorial.** La pose es posición X/Y más rumbo; no indica por sí sola dónde está la cámara. Un offset de cámara rota con el chasis y, si está en la torreta, también depende del ángulo de esta. Inicializar la pose una vez en una ubicación física reproducible. Reinicializarla durante la marcha puede ocultar deriva o crear saltos de control.

**Registrar para el reporte final.** Tabla con desplazamiento real/estimado y error en ambos ejes y rumbo. Mostrar pose inmóvil y después de regresar al inicio. Resolver signos, escala y offsets antes de ajustar el seguidor.

## 2. Calibrar el Follower con AutoTune

**Indicación.** Ejecutar los procedimientos correspondientes al localizador, drivetrain y algoritmo de la versión instalada. Guardar configuración inicial y final con fecha, masa, ruedas, superficie, batería y commit. Validar rectas, laterales, curvas y cambios de orientación con el robot en configuración de competencia.

**Mini tutorial.** Primero corregir medición de posición; después caracterizar movimiento y frenado; finalmente ajustar respuesta de seguimiento. Un error proporcional a la distancia suele requerir revisar escala; oscilación alrededor de una referencia requiere revisar control y mecánica. Cambiar una familia de parámetros por vez y comparar la misma ruta. Consultar qué magnitudes expone AutoTune 1.0.3; no trasladar coeficientes de tutoriales de Pedro 1/2 al algoritmo de Pedro 3.

**Registrar para el reporte final.** Diez ejecuciones por ruta base, trayectoria deseada/estimada, tiempo, error final, sobrepaso y tiempo de estabilización. Usar además marcas o video para medir el error real: el localizador puede reportar una llegada correcta estando físicamente desviado.

## 3. Diseñar trayectorias y orientación

**Indicación.** Crear poses con nombre: inicio, tiro, entrada de recolección, salida de recolección y estacionamiento. Diseñar una ruta en el Visualizer con al menos una recta y una curva Bézier. Revisar continuidad de tangentes, separación de obstáculos y huella completa del robot con intake y torreta.

Comparar orientación constante, interpolación lineal y tangente sobre la misma geometría. Elegir cada una por su función: recolectar, trasladarse o apuntar. Establecer restricciones de movimiento por segmento según capacidad del robot; comprobar la API disponible antes de nombrar setters.

**Mini tutorial.** Un punto de control de Bézier moldea la curva y normalmente no es un lugar por el que pase el robot. El rumbo del chasis se puede planificar por separado de la dirección de desplazamiento. Una curva suave de posición también debe ser compatible con la velocidad y rotación que puede ejecutar el chasis.

Fragmento orientativo de Pedro 3; no es un OpMode completo. Confirmar imports y unidades contra 3.0.2. Coordenadas ilustrativas, ajenas a una ruta de partido:

~~~java
PoseFactory factory = PoseFactory.degrees();
Pose salida = factory.of(18, 18, 0);
Pose tiro = factory.of(46, 30, 90);
Path acercamiento = Paths.line(salida, tiro).linear(salida, tiro);
~~~

Referencias: com.pedropathing.api.PoseFactory, com.pedropathing.api.Paths, com.pedropathing.math.Pose y com.pedropathing.paths.Path.

**Registrar para el reporte final.** Imagen del recorrido, poses, unidades, orientación elegida y video superpuesto a la trayectoria. Medir mínimo espacio libre incluyendo error de seguimiento.

## 4. Integrar Pedro con SolversLib

**Indicación.** Crear una acción de seguimiento que pueda componerse con preparación de shooter, intake y apuntado. Mantener un único dueño de las salidas del chasis y una única actualización del Follower por ciclo. Revisar si el módulo pedroPathing 0.3.6 ya ofrece un adaptador compatible con Pedro 3.0.2; si no, escribir una envoltura pequeña sobre la API instalada y compilarla aisladamente.

**Mini tutorial.** Un comando inicia una acción, la actualiza, comprueba si terminó y libera recursos. Un grupo secuencial espera cada terminación. Uno paralelo ejecuta acciones compatibles. Un deadline termina el grupo al concluir su acción principal; una carrera termina al finalizar cualquiera. Verificar nombres y disponibilidad de esas composiciones en SolversLib. Preparar el shooter durante el recorrido requiere actualizar ambos, no encadenar sleeps.

Contrato propuesto para el adaptador, expresado en pseudocódigo:

~~~text
initialize: iniciar seguimiento una sola vez; reiniciar temporizador
execute: consultar progreso y fallas; actualizar solo aquí si este módulo es dueño de update
isFinished: resultado = EXITO, TIMEOUT, LOCALIZACION_INVALIDA o CANCELADO
end: cancelar seguimiento cuando corresponda; liberar chasis; detener salidas si se cancela
~~~

Distinguir fin geométrico, fin del seguidor y llegada aceptada por la rutina. Comprobar error de posición/rumbo y velocidad durante un intervalo antes de disparar. Un timeout debe conservar su motivo; no continuar como si hubiera llegado correctamente.

**Registrar para el reporte final.** Prueba de cancelación en recta y curva; seguimiento con shooter simultáneo; evidencia de que no se ejecutan comandos de joystick durante AUTO. Probar una secuencia reiniciada sin reiniciar el robot.

## 5. Implementar una transacción de lanzamiento

**Indicación.** Construir una secuencia de precargas con preparación, alineación, alimentación individual y recuperación. Separar solicitud de disparo y autorización física de alimentar. Validar mecánicamente que el alimentador puede dosificar.

**Mini tutorial.** Crear una condición conjunta: velocidad del shooter estable, apuntado válido, chasis suficientemente quieto, pelota disponible si existe detección y tiempo suficiente. Usar tolerancias e histéresis para evitar alternancias rápidas. Mantener la condición durante un intervalo evita disparar al cruzar fugazmente el objetivo.

Si cambia la distancia, volver a validar el ajuste de tiro. Construir una tabla experimental distancia/RPM/ángulo cuando el mecanismo permita variar este último; interpolar únicamente dentro del rango medido. Registrar recuperación de RPM después de cada disparo. No considerar que hubo anotación solo porque el alimentador giró.

**Registrar para el reporte final.** Veinte tiros repartidos entre al menos dos distancias operativas. Registrar acierto, RPM, error angular, recuperación y fallas. Cancelar la alimentación al perder condiciones de tiro y limitar el tiempo de preparación.

## 6. Recolectar mediante eventos y confirmación

**Indicación.** Ejecutar intake durante una trayectoria de recolección y detenerlo según capacidad o fin de zona. Activar eventos una sola vez por pasada; reinicializar sus banderas al comenzar otra ejecución. Utilizar callbacks compatibles con Pedro 3 o eventos propios de la rutina.

**Mini tutorial.** El progreso paramétrico de una curva no equivale directamente a porcentaje de distancia ni de tiempo. Un evento de ruta sirve para preparar un mecanismo; para confirmar captura hace falta evidencia del mecanismo o sensor. Aplicar filtrado temporal y detectar transiciones al contar pelotas. Un sensor ocupado continuamente no representa capturas nuevas.

Agregar recuperación acotada: detener alimentación, reversa breve calibrada, reintentar una vez y abandonar el ciclo si persiste el atasco. El tiempo limita la recuperación; la confirmación determina si funcionó.

**Registrar para el reporte final.** Pruebas con captura normal, pelota ausente y atasco. Medir tasa de captura, dobles conteos y duración. Revisar trayectoria al perder una pelota en lugar de prolongar indefinidamente el intake.

## 7. Fusionar apuntado por odometría y visión

**Indicación.** Calcular dirección al objetivo a partir de pose de chasis, offset de torreta y ángulo de torreta. Incorporar cámara para corregir alineación relativa, filtrando identidad del objetivo, antigüedad, calidad y saltos imposibles de medición. Verificar el tratamiento de tags móviles antes de cualquier corrección de pose global.

**Mini tutorial.** El error de rumbo debe normalizarse al intervalo angular elegido. El ángulo absoluto hacia el objetivo se convierte en un ángulo relativo al chasis y luego al cero mecánico de la torreta. La cámara mide desde su propio marco: un offset y una torreta giratoria cambian esa relación. Una imagen vieja no representa necesariamente la orientación actual.

Probar pérdida de visión, detección equivocada y detección intermitente. Conservar odometría como alternativa solo dentro de un error validado; si no hay confianza suficiente, cancelar el disparo. La búsqueda debe tener límites angulares y temporales.

**Registrar para el reporte final.** Veinte aproximaciones desde distintas poses con error angular, tiempo de adquisición y porcentaje de apuntados válidos. Comparar odometría sola y asistencia visual. No corregir la pose con un tag que se mueve como si fuera una referencia fija.

## 8. Construir ciclos adaptativos y variantes

**Indicación.** Integrar precargas -> recolección -> retorno -> tiro -> decisión de repetir -> estacionamiento. Mantener una versión corta de respaldo. Usar un único modelo de poses y transformaciones verificadas para alianzas e inicios, evitando duplicar la lógica completa.

**Mini tutorial.** La decisión de otro ciclo debe usar tiempo restante, duración conservadora del ciclo, desplazamiento a estacionamiento y margen. Usar un percentil alto medido, no solo el promedio. Si falla una ruta, no iniciar la siguiente asumiendo que se alcanzó su inicio nominal: planificar una recuperación desde pose actual confiable o detenerse.

Esquema de decisiones; los nombres representan funciones del proyecto por implementar:

~~~text
si hay falla de localizacion: cancelar y detener
si no alcanza tiempo para ciclo + estacionamiento + margen: estacionar
si falta pelota y queda tiempo de recuperacion: recuperar una vez
si shooter o vision no listos dentro del limite: omitir tiro
si ciclo termina correctamente: actualizar estadisticas y decidir
~~~

Para transformar rutas, verificar ejes, orientación y elementos asimétricos del campo. Transformar también objetivos de torreta y pose inicial. Coordinar corredor y horario de uso con el aliado.

**Registrar para el reporte final.** Matriz de escenarios: nominal, salida desplazada, visión ausente, intake vacío, preparación lenta y poco tiempo. Mostrar decisión tomada y resultado, no solo una ejecución exitosa.

## 9. Validar para competencia

**Indicación.** Ejecutar veinte autónomos completos con cambios controlados de batería y pequeñas variaciones de colocación inicial. Medir desde una referencia física reproducible. Establecer metas iniciales de >= 90% de finalización y cero movimientos después del límite del período; revisar tolerancias de tiro con el mecanismo real.

**Mini tutorial.** Separar error de localización, seguimiento, mecanismo y estrategia. Si Pedro estima bien su objetivo pero el robot está lejos de la marca, investigar localización. Si la pose estimada no llega a la deseada, revisar seguimiento, restricciones y condiciones mecánicas. Si llega bien pero falla el tiro, revisar alineación, velocidad y dosificación.

Registrar el resultado explícito de cada acción y detener follower, scheduler y mecanismos al finalizar o cancelar. Validar que INIT de TeleOp no produzca movimiento durante la transición. Transferir pose solo si el marco, origen y localizador son compatibles; no resetear encoders o rumbo sin actualizar coherentemente el estado.

**Registrar para el reporte final.** Informe de tasa de éxito, tiempo mediano y percentil 95, aciertos, error final y causas de falla. Conservar configuración y commit de la versión validada.

## Telemetría y registro

Mostrar: fase, resultado de acción, tiempo restante, pose deseada/estimada, error de posición/rumbo, velocidad, estado del Follower, RPM objetivo/real, ángulo de torreta, validez/edad de visión y estado de captura. Limitar frecuencia de presentación para no saturar el ciclo; conservar registro con mayor detalle cuando sea necesario.

| Commit | Configuración | Prueba | Intento | Duración | Error XY/rumbo | Tiros/capturas | Resultado | Causa de falla |
|---|---|---|---|---|---|---|---|---|
| | | | | | | | | |

Publicar cambios en ramas por función, por ejemplo feature/auto-precargas o feature/auto-ciclos, y revisar por pull request hacia dev. Adjuntar video, registros y parámetros. No introducir lecturas ficticias de sensores ni sustituir un timeout por éxito.

## Referencias técnicas

Consultar la documentación correspondiente a las versiones declaradas y verificar firmas con Android Studio. Los ejemplos oficiales actuales pueden evolucionar; no copiar automáticamente imports de Ivy dentro del scheduler SolversLib.

- [Pedro 3 y cambios de API](https://pedropathing.com/docs/pathing/pedro3)
- [Creación de trayectorias](https://pedropathing.com/docs/pathing/reference/api)
- [Orientación e interpolación](https://pedropathing.com/docs/pathing/reference/interpolation)
- [Organización del autónomo](https://pedropathing.com/docs/pathing/guide/auto-usage)
- [Estado del Follower](https://pedropathing.com/docs/pathing/guide/follow-state)
- [Restricciones de llegada](https://pedropathing.com/docs/pathing/reference/endconstraints)
- [Pruebas de tuning](https://pedropathing.com/docs/pathing/tuning/test)
- [Integración SolversLib/Pedro: comprobar compatibilidad de versión](https://docs.seattlesolvers.com/pedro-pathing/pedro-commands/followpathcommand)
- [Reglamento vigente y Team Updates BIOBUZZ](https://ftc-resources.firstinspires.org/ftc/game)
