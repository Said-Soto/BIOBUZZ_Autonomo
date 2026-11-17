# Investigación inicial de la torreta

Revisado el 17 de noviembre de 2026. Este documento registra el diseño pendiente; no valida el mecanismo físico ni habilita movimiento.

## BIOBUZZ

- La alianza lanza POLLEN y NECTAR a la CELL orientada hacia arriba para provocar el HIVE TIP. El HIVE gira entre dos estados; no es un blanco fijo. Evitar suponer que siempre está disponible la misma celda. [Manual BIOBUZZ, TU05](https://ftc-resources.firstinspires.org/ftc/game/cm-html/BIOBUZZ%20Competition%20Manual%20-%20TU05.htm).
- La regla G417 limita la interacción con el HIVE y permite inducir su movimiento lanzando elementos a la CELL orientada hacia arriba. El manual advierte que lanzar a una celda orientada hacia abajo durante el movimiento puede interferir con el TIP. Verificar manual y Q&A vigentes antes de competir. [Manual BIOBUZZ](https://ftc-resources.firstinspires.org/ftc/game/cm-html/BIOBUZZ%20Competition%20Manual%20-%20TU04.htm).
- Los AprilTag clusters de las CELLS se mueven junto al HIVE. El SDK 12 documentado en el README del proyecto advierte que no sirven como referencias fijas para localización absoluta. Pueden proporcionar información relativa al objetivo, pero antes de usar visión hay que confirmar cámara, montaje, detección válida, identidad y estado de la CELL. [FIRST: AprilTag clusters](https://ftc-docs.firstinspires.org/en/latest/tech_tips/tech-tips/tech-tip-apriltag-clusters/tech-tip-apriltag-clusters.html).

## Referencia Aztecbot

Aztecbot 22571 participó en el México Premier Event de julio de 2026, en la temporada DECODE. Se revisó [TurretSub de la rama premier](https://github.com/manu2w2/Aztecbot-22571/blob/f6b9bfcca766d6727c6c14d14796a6ccb8b658ae/TeamCode/src/main/java/Subsistemas/TurretSub.java) y su [TeleOp azul](https://github.com/manu2w2/Aztecbot-22571/blob/f6b9bfcca766d6727c6c14d14796a6ccb8b658ae/TeamCode/src/main/java/org/firstinspires/ftc/teamcode/opModes/teleOps/TeleOp_azulC.java).

- Emplea `MotorEx`, `PController`, encoder, conversión a grados, saturación y límites angulares; calcula el objetivo a partir de la posición y orientación del robot y un punto del campo.
- Sus relaciones, límites, ganancia y coordenadas corresponden a su robot y no se deben asumir para este. Su constructor reinicia el encoder sin homing; esto solo representa un cero físico si el mecanismo empieza en la referencia conocida. Su cálculo usa una pose de Pedro, que no debe activarse en el TeleOp actual. Su `periodic()` accede a la pose antes de comprobar `enabled` y requiere que se haya establecido.
- En BIOBUZZ, un punto fijo por sí solo no confirma cuál CELL está hacia arriba. Si se añade apuntado asistido, necesitará identificación y datos vigentes del blanco, comportamiento ante pérdida de señal, límites mecánicos verificados y una alternativa manual.

## Datos necesarios antes de implementar el movimiento

El equipo confirmó HD Hex con encoder, cajas reales 2.89:1 y 3.61:1, engrane del motor de 47 dientes y de la torreta de 100; relación total ≈22.197:1 y ≈621.5 cuentas por vuelta. Limelight 3A en torreta; cero mediante colocación manual centrada. Se solicitó 180° de recorrido total: límites provisionales −90° a +90°, sujetos a prueba de holgura de cables. No existen sensores de límite.

`TurretConfig` expone la configuración en Panels; `TurretSubsystem` utiliza `MotorEx` y `PController`, requiere confirmación explícita del centro, bloquea objetivos fuera del rango y detiene al invalidar parámetros. La ganancia y potencia son valores iniciales sin ajuste. Los límites dependen de un cero correcto y encoder funcional: no sustituyen sensores. Los TeleOp de alianza crean el subsistema únicamente cuando se confirme la configuración de odometría.

Falta integrar el seguimiento de la Limelight y verificar pipeline, identificación de CELL, orientación y calibración de cámara respecto al shooter. Un tag individual puede estar descentrado respecto a la abertura; no se debe confundir con el centro del cluster. Debe detenerse al perder o envejecer la detección y evitar apuntar a una CELL no válida. No copiar límites de otros robots ni realizar homing contra topes desconocidos.

## Apuntado basado en odometría

Se solicita seleccionar el punto de la HIVE según el lado del campo y orientar la torreta desde la pose del robot. `TurretAimGeometry` calcula el ángulo relativo con `atan2`, orientación del robot, desplazamiento del pivote y orientación del cero; utiliza distancias en una unidad común y ángulos en grados antihorarios. Rechaza datos no finitos y la posición coincidente con el objetivo. El Pinpoint se lee directamente sin Follower; cada TeleOp fija su alianza y el comando selecciona el punto por lado según el eje y umbral configurados. Los offsets, pose inicial, marco y coordenadas reales aún deben medirse.

Una CELL se mueve cuando el HIVE bascula: la odometría por sí sola no determina qué CELL está orientada hacia arriba. Antes de automatizar el disparo es necesario un estado de HIVE fiable o confirmación del operador; no tratar los AprilTags móviles como puntos fijos de localización. La Limelight de la torreta queda pendiente de configuración e integración.

## Configuración y operación

Seleccionar `SolversLib: TeleOp Rojo` o `SolversLib: TeleOp Azul`. En `OdometryAimConfig`, definir el nombre `pinpoint`, offsets de los pods en milímetros (X pod lateral positivo a la izquierda; Y pod longitudinal positivo hacia adelante), sus direcciones, pose inicial del robot en pulgadas y grados antihorarios, posición del pivote respecto del robot, orientación del cero, coordenadas de las dos CELL de cada alianza y el eje, umbral y sentido que separan ambos lados. La pose inicial, límites y objetivos deben compartir el mismo marco de campo. Los pods seleccionados son goBILDA 4-bar. Los valores cero son provisionales: solo marcar `measuredConfigurationConfirmed` tras medir y comprobar todo y luego reiniciar el OpMode.

Con la torreta centrada y el robot inmóvil en la pose inicial configurada antes de INIT, esperar el mensaje `Pose inicial lista` antes de PLAY. Si se pulsa PLAY antes, reiniciar el OpMode. Después de PLAY, gamepad 2 Y confirma el centro físico y el bumper derecho activa o desactiva el apuntado. La selección de lado usa la pose del robot y los puntos configurados; datos inválidos, fallo reportado por Pinpoint, cambio de configuración o un objetivo fuera de alcance detienen el motor y requieren reactivar. Cambiar la configuración del motor o su inversión requiere reiniciar el OpMode y recalibrar el centro. El botón Y no es un sensor y no evita que el operador confirme un centro incorrecto. STOP cancela los comandos y manda potencia cero.

Los valores de Limelight en `TurretConfig` son reservas configurables, sin lectura ni seguimiento implementados. La odometría no identifica si la CELL está arriba ni impide que el piloto dispare o active el transfer; el operador debe comprobarlo. Probar primero sin proyectiles, con potencia reducida, holgura de cables verificada y STOP accesible. No hay detección automática garantizada de encoder desconectado, deslizamiento, deriva de odometría o lectura congelada.
