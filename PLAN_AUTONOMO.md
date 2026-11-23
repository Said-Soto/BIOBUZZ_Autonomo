# Prácticas de autónomo BIOBUZZ

Los tres integrantes realizan TODAS las prácticas, individualmente y en el mismo orden. Ya saben conducir en TeleOp: el curso comienza con autonomía. No se reparten mecanismos entre alumnos.

## Punto de partida revisado

Repositorio de origen: https://github.com/manu2w2/BIOBUZZ_velocity_bucks
Copia del equipo: https://github.com/Said-Soto/BIOBUZZ_Autonomo

- master conserva la base del SDK; dev contiene los cambios recientes del robot.
- TeleOpMain.java usa CommandOpMode y CommandScheduler de SolversLib.
- Ya instancia DriveSubsystem, IntakeSubsystem y ShooterSubsystem.
- La inicialización opcional de TurretSubsystem y PinpointSubsystem depende de OdometryAimConfig.valid().
- La torreta utiliza TurretAimCommand. La presencia de estas clases NO confirma que Pinpoint ni sus ruedas de odometría estén instalados o calibrados.
- También existe una carpeta pedroPathing. Antes de usarla se debe revisar su configuración y localizador; su presencia no demuestra que las trayectorias estén listas.

Esta entrega añade el plan de enseñanza. No constituye un autónomo implementado, compilado ni probado en el robot.

## Forma de trabajo para los tres

1. Clonar este repositorio y partir de dev.
2. Cada alumno crea su rama: alumno-1/autonomo, alumno-2/autonomo o alumno-3/autonomo.
3. Todos resuelven el mismo ejercicio. Registrar resultados propios y explicar el código sin leer una solución ajena.
4. Crear un pull request hacia dev con código, mediciones y limitaciones. No mezclar las tres implementaciones como si fueran funciones distintas: comparar y elegir una versión común.
5. Mantener el TeleOp existente como referencia; reutilizar subsistemas y evitar dos controladores escribiendo al mismo motor.

Comandos de inicio:

~~~bash
git clone https://github.com/Said-Soto/BIOBUZZ_Autonomo.git
cd BIOBUZZ_Autonomo
git switch dev
git switch -c alumno-1/autonomo
~~~

Cambiar alumno-1 por el identificador correspondiente. Para publicar ramas necesitarán permisos de colaborador o usar sus propios forks y pull requests.

## Práctica 0: entender el código que ya funciona

Cada uno dibuja el flujo TeleOpMain -> comandos -> subsistemas -> motores. Identificar dónde se actualiza el scheduler y dónde se detiene cada mecanismo.

Entregar tabla de nombres de hardware, motores y reducciones exactas, direcciones, encoders disponibles, diámetro de ruedas, transmisión y configuración real del chasis. Confirmar modelo de cámara e instalación de Pinpoint. No elegir ecuaciones de mecanum únicamente porque las ruedas se describan como omnidireccionales.

Aprobación: explicar qué cambia al reemplazar la orden del joystick por una meta de distancia o posición. Ningún movimiento durante INIT.

## Práctica 1: primera acción autónoma medible

Crear un OpMode @Autonomous independiente y una acción que avance 50 cm y se detenga mediante realimentación. Empezar a potencia limitada y comprobar antes los signos de los encoders.

Calcular la conversión desde cuentas por vuelta de rueda y circunferencia; documentar si la especificación del encoder está referida al motor o al eje de salida para no contar dos veces la reducción. Incluir tolerancia, timeout y respuesta a STOP. Un movimiento por tiempo sirve para diagnóstico, no como criterio principal de llegada.

Entregar cinco intentos: distancia solicitada, distancia medida, error y duración. Meta inicial propuesta: error <= 3 cm; ajustar tras medir el sistema real.

## Práctica 2: giro y conservación del rumbo

Leer la IMU con orientación de montaje correcta. Girar +90 y -90 grados. Resolver el cruce de -180/180 grados. Agregar corrección de rumbo durante avance y limitar la salida del controlador.

Entregar cinco repeticiones por sentido y explicar el efecto de aumentar la ganancia proporcional. Meta inicial propuesta: error <= 3 grados. No ajustar PID por ensayo sin registrar cambios.

## Práctica 3: recorrido y estacionamiento

Programar avance -> giro -> avance -> parada usando condiciones de término. Cada acción debe tener éxito, timeout y limpieza al terminar o interrumpirse.

Primera rutina de campo: separarse de la pared y terminar al menos parcialmente en la LOADING ZONE propia. Coordinar inicio y recorrido con el aliado. Dibujar ruta y medir distancias en el campo, sin inventarlas desde una foto.

Aprobación propuesta: 9 éxitos en 10 intentos. Registrar error final y tiempo de estacionamiento para reservar margen al final del autónomo.

## Práctica 4: secuencias y acciones simultáneas

Usar la arquitectura de comandos existente: comprender initialize, execute, isFinished, end y los requisitos de cada comando, verificando sus firmas en la versión instalada de SolversLib.

Preparar el lanzador mientras el chasis se desplaza. Mantener actualización de sensores y capacidad de detenerse. Evitar sleep prolongados y bucles internos que impidan actualizar scheduler o controles de cancelación. Los comandos autónomos no deben depender de botones ni heredar comandos por defecto que lean joysticks.

Aprobación: STOP funciona durante cualquier etapa y ningún comando mantiene motores activos tras finalizar.

## Práctica 5: disparo de precargas

Reutilizar ShooterSubsystem. Establecer RPM y esperar velocidad dentro de una tolerancia durante un intervalo. Alimentar individualmente cuando el mecanismo lo permita, esperar recuperación y repetir. Revisar cómo se alimentan las pelotas en el hardware antes de implementar la dosificación.

Si no alcanza velocidad antes del timeout, cancelar el disparo. No asumir que cuatro precargas garantizan un vuelco. Calibrar primero desde una posición fija. La orientación de torreta debe partir de una referencia física conocida; resetear un encoder no encuentra por sí solo el centro.

Entregar diez tiros con RPM objetivo/real, distancia, acierto y tiempo de recuperación. Registrar atascos y dobles alimentaciones.

## Práctica 6: cámara y localización

Todos implementan lectura y telemetría de detecciones antes de controlar movimiento con visión. Confirmar librería, versión del SDK, calibración y transformación cámara/robot; si está sobre torreta, considerar su ángulo.

Distinguir apuntado relativo a un objetivo y posición absoluta en el campo. Verificar en documentación oficial de temporada qué referencias son móviles antes de usarlas como puntos fijos. Si se pierde el objetivo, limitar la búsqueda por tiempo y usar una alternativa previamente validada.

Decisión común: usar encoders e IMU para la primera rutina; incorporar Pedro Pathing cuando el localizador real y sus parámetros estén verificados. Si Pinpoint no está instalado, sus clases no sustituyen ese hardware.

## Práctica 7: autónomo integrado

Secuencia objetivo: salir -> ir a posición medida -> apuntar -> esperar velocidad -> disparar precargas con recuperación -> estacionar -> detener todo.

Cada estado incluye condición de éxito y tiempo máximo. Antes de iniciar otro ciclo comprobar: tiempo restante > tiempo medido para estacionar + margen. Al fallar visión o lanzamiento, omitir esa acción y ejecutar únicamente la alternativa de estacionamiento validada. Si la localización deja de ser confiable, detenerse en vez de conducir a ciegas.

Conservar una rutina de respaldo de salida y estacionamiento. Programar variantes de alianza e inicio solo después de validar la primera ruta.

Aprobación propuesta: 9/10 ejecuciones completas, sin ayuda humana, sin interferencia con el aliado y con todos los mecanismos detenidos antes de terminar el período.

## Registro obligatorio por alumno

| Fecha | Commit | Práctica | Intento | Objetivo | Medición | Error/acierto | Duración | Batería | Falla y cambio |
|---|---|---|---|---|---|---|---|---|---|
| | | | | | | | | | |

Cada entrega incluye código, explicación breve, tabla de pruebas y video. Evaluación sugerida: funcionamiento 40%, repetibilidad 30%, explicación 20%, registro 10%. Las tolerancias anteriores son metas didácticas propuestas, no exigencias del reglamento.

## Reglas y documentación

Verificar manual vigente y Team Updates antes de cada evento: https://ftc-resources.firstinspires.org/ftc/game
Documentación de programación: https://ftc-docs.firstinspires.org/en/latest/programming_resources/

La estrategia debe respetar el período autónomo, el cese de movimiento durante la transición y la interacción permitida con los elementos del campo. Consultar el manual y Q&A para las condiciones exactas de puntuación.
