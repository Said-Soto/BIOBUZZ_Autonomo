# TeleOp, hardware y dependencias

Estado revisado el 17 de noviembre de 2026. Orden de desarrollo: chasis y parada → intake → torreta → shooter → coordinación. Arquitectura: OpModes → comandos → subsistemas → hardware; los nombres y potencias están en `RobotConstants`.

## Chasis y controles

Seleccionar **SolversLib: TeleOp Rojo** o **SolversLib: TeleOp Azul**. Gamepad 1: stick izquierdo Y avanza/retrocede, stick izquierdo X desplaza lateralmente y stick derecho X gira. `GamepadEx` ya invierte Y; no invertirlo nuevamente. El comando requiere el chasis y lo detiene al finalizar o ser interrumpido; al terminar el OpMode se cancelan los comandos y se vuelve a detener el chasis.

| Ubicación | Nombre en configuración | Orden en `MecanumDrive` |
| --- | --- | --- |
| Delantero izquierdo | `front_left` | 1 |
| Delantero derecho | `front_right` | 2 |
| Trasero izquierdo | `back_left` | 3 |
| Trasero derecho | `back_right` | 4 |

Estos nombres reflejan el código, no una inspección física. Los cuatro motores se configuran en `FORWARD`, `RUN_WITHOUT_ENCODER` y `RawPower`; `MecanumDrive` invierte la salida derecha. Potencia máxima inicial: 0.25 (`DRIVE_MAX_POWER`). Verificar cableado, montaje y sentido antes de cambiarla.

El intake frontal usa `intakeMotor` y el transfer usa `transferMotor` en la configuración del Robot Controller. Ambos se inicializan en `FORWARD`, `RUN_WITHOUT_ENCODER`, `RawPower` y potencia cero, y reciben la misma potencia. Mantener A para intake y B para invertir; al soltar o pulsar ambos se detienen. La potencia inicial es 0.25 (`INTAKE_POWER`). El sentido lógico igual no garantiza giro físico igual: verificar montaje y cableado. La torreta usa un motor HD Hex con encoder, transmisión aproximada 22.20:1 y límites provisionales de ±90°. El control por Pinpoint requiere configurar y confirmar la geometría; ver `TORRETA_INVESTIGACION.md`. El shooter usa un REV HD Hex con encoder y transmisión 1:1; configurar `shooterMotor` con el tipo y reducción correctos en el Robot Controller; el cálculo usa 28 cuentas por vuelta para el motor sin caja. REV especifica 6000 RPM en vacío, no como velocidad objetivo ([REV](https://www.revrobotics.com/rev-41-1291/)). La velocidad objetivo `SHOOTER_TARGET_RPM` es 3500 RPM. X alterna encendido y apagado, y STOP detiene el motor. La telemetría muestra objetivo y velocidad medida en RPM, además de potencia. El motor usa `MotorEx` de SolversLib en `RawPower`; el control es FF + PD propio con `RUN_WITHOUT_ENCODER`: potencia = kF × objetivo + kP × error − kD × aceleración medida filtrada, limitada entre 0 y 1. Las ganancias iniciales en `RobotConstants` son estimaciones sin ajustar; kF se aproxima con la velocidad nominal sin carga, sin compensación de voltaje. Deben ajustarse físicamente y comprobarse con y sin carga. La rueda gira libremente al parar (FLOAT).

Los tres ejemplos de Pedro y `chasis_mecanum` están deshabilitados. Este último usaba inversión izquierda y otra implementación; no debe reactivarse sin reconciliarlo y probarlo. El paquete `pedroPathing`, dependencias y AutoTune permanecen; las herramientas de ajuste pueden mover el robot y requieren configuración y pruebas específicas. El TeleOp principal no crea ni actualiza un `Follower`.

## Dependencias y compatibilidad

| Componente | Versión | Decisión |
| --- | --- | --- |
| FTC SDK | 12.0.0 | Conservar: [versión oficial BIOBUZZ](https://github.com/FIRST-Tech-Challenge/FtcRobotController/releases/tag/v12.0). Mantener Driver Station compatible. |
| SolversLib core, adaptador Pedro y módulo Photon | 0.3.6 | Conservar como conjunto del [quickstart oficial](https://github.com/FTC-23511/SolversLib-Quickstart/blob/master/TeamCode/build.gradle); la documentación de versiones consultada no permite afirmar que sea la última publicada. Photon es el módulo de SolversLib, no una dependencia independiente de PhotonFTC; no se activa explícitamente en TeleOp. |
| Pedro revhub / AutoTune tuning | 3.0.2 / 1.0.3 | Actualizados juntos según el [quickstart oficial de Pedro](https://github.com/Pedro-Pathing/Quickstart/blob/master/build.dependencies.gradle). Compilan los ejemplos y herramientas; el ajuste y funcionamiento en hardware siguen sin probarse. |
| FTC Dashboard | 0.6.0 | Actualizado desde 0.5.1 a la [versión estable publicada](https://github.com/acmerobotics/ftc-dashboard/releases/tag/v0.6.0); compilación y APK completados, integración en robot pendiente. |
| Panels fullpanels | 1.0.12 | Conservada, como en el quickstart de SolversLib. No se pudo confirmar una versión estable posterior ni su compatibilidad en ejecución. |
| Gradle / AGP | 9.1.0 / 8.13.2 | Conservar la pareja de FTC SDK 12. Una migración de AGP/Gradle debe evaluarse junto con el SDK, no como actualización aislada. |
| Java | fuente y destino 8; JDK de compilación 21 | El JDK que ejecuta Gradle es distinto del nivel de lenguaje. [AGP 8.13 requiere JDK 17](https://developer.android.com/build/releases/gradle-plugin); conservar Java 8 del SDK. El JDK 21 emite advertencias de obsolescencia para source/target 8. |

El árbol Gradle resuelve dependencias transitivas (incluida Kotlin); construir no garantiza compatibilidad de ejecución con Control Hub, Dashboard, Panels o localizadores. Las migraciones mayores, cambios de API y ajustes de localización se deben tratar por separado y verificar con sus herramientas y hardware.

## Verificación física pendiente

No se ha conectado ni movido el robot. Con supervisión, espacio despejado, ruedas levantadas, potencia al 25 % y STOP accesible: confirmar los cuatro nombres y posiciones, probar cada eje brevemente, comprobar avance, desplazamiento y giro, soltar sticks y pulsar STOP; verificar que las cuatro ruedas se detienen. Corregir cableado o configuración si el sentido no coincide antes de probar en el suelo. Con los mecanismos despejados y sin piezas, comprobar por separado el sentido físico de ambos motores y su parada al soltar A o B y al pulsar STOP. Antes de activar el shooter, confirmar modelo, encoder, sentido, RPM objetivo y ganancias FF/PD; probar sin proyectiles ni personas frente a la salida, con STOP accesible, y esperar a que la rueda se detenga. La torreta permanece desactivada hasta configurar y confirmar los parámetros de odometría y objetivos.

## Compilación

Con Android Studio/JDK compatible y SDK Android instalado: `gradlew.bat :TeamCode:compileDebugJavaWithJavac :TeamCode:testDebugUnitTest :TeamCode:assembleDebug`. La compilación Java valida código fuente; `assembleDebug` además genera el APK. `testDebugUnitTest` informó `NO-SOURCE`: no hay pruebas unitarias ejecutadas. Estas comprobaciones no sustituyen las pruebas físicas.
