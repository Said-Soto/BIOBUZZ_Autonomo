# Autónomo BIOBUZZ con Pedro Pathing 3

Construir un autónomo repetible de precargas, recolección, lanzamiento y estacionamiento, con trayectorias continuas, mecanismos simultáneos y recuperación de fallas. Resolver las prácticas en orden y conservar mediciones de cada cambio.

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

## Entregables y orden

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

**Entregar.** Tabla con desplazamiento real/estimado y error en ambos ejes y rumbo. Mostrar pose inmóvil y después de regresar al inicio. Resolver signos, escala y offsets antes de ajustar el seguidor.

## 2. Calibrar el Follower con AutoTune

**Indicación.** Ejecutar los procedimientos correspondientes al localizador, drivetrain y algoritmo de la versión instalada. Guardar configuración inicial y final con fecha, masa, ruedas, superficie, batería y commit. Validar rectas, laterales, curvas y cambios de orientación con el robot en configuración de competencia.

**Mini tutorial.** Primero corregir medición de posición; después caracterizar movimiento y frenado; finalmente ajustar respuesta de seguimiento. Un error proporcional a la distancia suele requerir revisar escala; oscilación alrededor de una referencia requiere revisar control y mecánica. Cambiar una familia de parámetros por vez y comparar la misma ruta. Consultar qué magnitudes expone AutoTune 1.0.3; no trasladar coeficientes de tutoriales de Pedro 1/2 al algoritmo de Pedro 3.

**Entregar.** Diez ejecuciones por ruta base, trayectoria deseada/estimada, tiempo, error final, sobrepaso y tiempo de estabilización. Usar además marcas o video para medir el error real: el localizador puede reportar una llegada correcta estando físicamente desviado.

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

**Entregar.** Imagen del recorrido, poses, unidades, orientación elegida y video superpuesto a la trayectoria. Medir mínimo espacio libre incluyendo error de seguimiento.

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

**Entregar.** Prueba de cancelación en recta y curva; seguimiento con shooter simultáneo; evidencia de que no se ejecutan comandos de joystick durante AUTO. Probar una secuencia reiniciada sin reiniciar el robot.

## 5. Implementar una transacción de lanzamiento

**Indicación.** Construir una secuencia de precargas con preparación, alineación, alimentación individual y recuperación. Separar solicitud de disparo y autorización física de alimentar. Validar mecánicamente que el alimentador puede dosificar.

**Mini tutorial.** Crear una condición conjunta: velocidad del shooter estable, apuntado válido, chasis suficientemente quieto, pelota disponible si existe detección y tiempo suficiente. Usar tolerancias e histéresis para evitar alternancias rápidas. Mantener la condición durante un intervalo evita disparar al cruzar fugazmente el objetivo.

Si cambia la distancia, volver a validar el ajuste de tiro. Construir una tabla experimental distancia/RPM/ángulo cuando el mecanismo permita variar este último; interpolar únicamente dentro del rango medido. Registrar recuperación de RPM después de cada disparo. No considerar que hubo anotación solo porque el alimentador giró.

**Entregar.** Veinte tiros repartidos entre al menos dos distancias operativas. Registrar acierto, RPM, error angular, recuperación y fallas. Cancelar la alimentación al perder condiciones de tiro y limitar el tiempo de preparación.

## 6. Recolectar mediante eventos y confirmación

**Indicación.** Ejecutar intake durante una trayectoria de recolección y detenerlo según capacidad o fin de zona. Activar eventos una sola vez por pasada; reinicializar sus banderas al comenzar otra ejecución. Utilizar callbacks compatibles con Pedro 3 o eventos propios de la rutina.

**Mini tutorial.** El progreso paramétrico de una curva no equivale directamente a porcentaje de distancia ni de tiempo. Un evento de ruta sirve para preparar un mecanismo; para confirmar captura hace falta evidencia del mecanismo o sensor. Aplicar filtrado temporal y detectar transiciones al contar pelotas. Un sensor ocupado continuamente no representa capturas nuevas.

Agregar recuperación acotada: detener alimentación, reversa breve calibrada, reintentar una vez y abandonar el ciclo si persiste el atasco. El tiempo limita la recuperación; la confirmación determina si funcionó.

**Entregar.** Pruebas con captura normal, pelota ausente y atasco. Medir tasa de captura, dobles conteos y duración. Revisar trayectoria al perder una pelota en lugar de prolongar indefinidamente el intake.

## 7. Fusionar apuntado por odometría y visión

**Indicación.** Calcular dirección al objetivo a partir de pose de chasis, offset de torreta y ángulo de torreta. Incorporar cámara para corregir alineación relativa, filtrando identidad del objetivo, antigüedad, calidad y saltos imposibles de medición. Verificar el tratamiento de tags móviles antes de cualquier corrección de pose global.

**Mini tutorial.** El error de rumbo debe normalizarse al intervalo angular elegido. El ángulo absoluto hacia el objetivo se convierte en un ángulo relativo al chasis y luego al cero mecánico de la torreta. La cámara mide desde su propio marco: un offset y una torreta giratoria cambian esa relación. Una imagen vieja no representa necesariamente la orientación actual.

Probar pérdida de visión, detección equivocada y detección intermitente. Conservar odometría como alternativa solo dentro de un error validado; si no hay confianza suficiente, cancelar el disparo. La búsqueda debe tener límites angulares y temporales.

**Entregar.** Veinte aproximaciones desde distintas poses con error angular, tiempo de adquisición y porcentaje de apuntados válidos. Comparar odometría sola y asistencia visual. No corregir la pose con un tag que se mueve como si fuera una referencia fija.

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

**Entregar.** Matriz de escenarios: nominal, salida desplazada, visión ausente, intake vacío, preparación lenta y poco tiempo. Mostrar decisión tomada y resultado, no solo una ejecución exitosa.

## 9. Validar para competencia

**Indicación.** Ejecutar veinte autónomos completos con cambios controlados de batería y pequeñas variaciones de colocación inicial. Medir desde una referencia física reproducible. Establecer metas iniciales de >= 90% de finalización y cero movimientos después del límite del período; revisar tolerancias de tiro con el mecanismo real.

**Mini tutorial.** Separar error de localización, seguimiento, mecanismo y estrategia. Si Pedro estima bien su objetivo pero el robot está lejos de la marca, investigar localización. Si la pose estimada no llega a la deseada, revisar seguimiento, restricciones y condiciones mecánicas. Si llega bien pero falla el tiro, revisar alineación, velocidad y dosificación.

Registrar el resultado explícito de cada acción y detener follower, scheduler y mecanismos al finalizar o cancelar. Validar que INIT de TeleOp no produzca movimiento durante la transición. Transferir pose solo si el marco, origen y localizador son compatibles; no resetear encoders o rumbo sin actualizar coherentemente el estado.

**Entregar.** Informe de tasa de éxito, tiempo mediano y percentil 95, aciertos, error final y causas de falla. Conservar configuración y commit de la versión validada.

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
