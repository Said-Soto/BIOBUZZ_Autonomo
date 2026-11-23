# Autónomo BIOBUZZ: tres retos integradores

**Objetivo:** lograr un autónomo con Pedro Pathing que se desplace, apunte la torreta mediante trigonometría y AprilTags detectados por Limelight, dispare y termine estacionado. Utilizar el Control Hub y el Expansion Hub disponibles.

Desarrollar los tres retos como partes del mismo programa. **La única entrega final es un reporte breve del equipo y un video de prueba.** No se requieren entregas separadas por reto.

## Cómo usar las bases de código

Los ejemplos completos indican nombre de archivo y paquete. Los fragmentos de integración se insertan en clases existentes y muestran sus dependencias. Buscar TODO para identificar las piezas que deben completar. Mantener los tres retos y una única entrega final de reporte y video.

## Base de trabajo

Trabajar en dev y reutilizar los subsistemas existentes de chasis, intake, shooter y torreta. El proyecto declara Pedro Pathing 3.0.2, AutoTune 1.0.3 y SolversLib 0.3.6. Verificar compatibilidad al compilar; no copiar APIs antiguas de PathChain sin adaptarlas.

Confirmar localizador instalado, montaje de Limelight, referencia de cero de torreta y mapa de motores/encoders entre ambos hubs. Conservar el TeleOp operativo. Esta guía describe el trabajo por realizar; no acredita pruebas físicas ya realizadas.

## Reto 1. Recorrido autónomo con mecanismos simultáneos

**Indicación.** Crear un recorrido de inicio -> posición de tiro -> zona de recolección -> posición de tiro -> estacionamiento. Incluir una recta y una curva, orientación del chasis planificada y preparación del shooter o activación del intake durante el movimiento.

Medir las poses en el campo, validar localización y ajustar el Follower con las herramientas disponibles. Organizar la secuencia mediante comandos compatibles con SolversLib/Pedro o estados sin bloqueos. Actualizar el Follower y los mecanismos continuamente, con un solo responsable de cada actualización y de las salidas del chasis.

**Mini tutorial.** Definir primero las poses X/Y/rumbo; después construir la geometría del recorrido. Un punto de control Bézier moldea la curva y no necesariamente es un punto de paso. Elegir orientación constante para mantener el frente y orientación interpolada para girar durante el trayecto. Probar a velocidad moderada antes de incrementarla. Corregir signos, unidades y offsets del localizador antes de modificar ganancias.

**Comprobar.** El recorrido se repite desde la misma salida, prepara mecanismos sin detener el programa y responde a STOP. Cada tramo tiene condición de llegada y tiempo máximo; un timeout se registra como falla y no como llegada exitosa.


### Código base 1: seguir una ruta sin bloquear el programa

Crear AutoRutaBase.java en opmodes. Es una base de navegación: todavía no dispara ni incorpora visión. Los nombres y llamadas de Pedro coinciden con Constants.java y procedures/Tests.java revisados en este repositorio. Las coordenadas son ilustrativas en pulgadas porque el Constants actual configura INCH; el rumbo de Pose se expresa en radianes.

~~~java
package org.firstinspires.ftc.teamcode.opmodes;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import org.firstinspires.ftc.teamcode.pedroPathing.Constants;
import static com.pedropathing.api.Paths.line;

@Disabled // Retirar cuando los TODO estén resueltos y el montaje validado.
@Autonomous(name = "Base: recorrido Pedro")
public class AutoRutaBase extends LinearOpMode {
    private static final boolean CONFIGURACION_VALIDADA = false;
    private Follower follower;

    @Override public void runOpMode() {
        // TODO: validar nombres, drivetrain, localizador y tuning antes de activar.
        if (!CONFIGURACION_VALIDADA) {
            telemetry.addLine("Completar configuración antes de ejecutar");
            telemetry.update();
            return;
        }
        try {
            follower = Constants.createFollower(hardwareMap);
            Pose inicio = new Pose(0, 0, 0);
            Pose tiro = new Pose(24, 0, 0); // TODO: medir pose real.
            Path ida = line(inicio, tiro).constant(0);
            Path vuelta = line(tiro, inicio).constant(0);
            follower.setPose(inicio);
            telemetry.addLine("Colocar físicamente en la pose inicial");
            telemetry.update();
            waitForStart();
            if (isStopRequested()) return;

            int tramo = 0;
            ElapsedTime tiempoTramo = new ElapsedTime();
            follower.follow(ida); // Iniciar una sola vez al entrar al tramo.
            tiempoTramo.reset();
            while (opModeIsActive() && tramo < 2) {
                follower.update(); // Una vez por iteración.
                // TODO: actualizar aquí shooter/visión sin sleeps.
                if (tiempoTramo.seconds() > 6.0) { // Ejemplo: medir y ajustar.
                    telemetry.addLine("TIMEOUT: recorrido incompleto");
                    telemetry.update();
                    break;
                }
                // isBusy no sustituye la validación física de precisión.
                if (!follower.isBusy()) {
                    tramo++;
                    if (tramo == 1) {
                        follower.follow(vuelta);
                        tiempoTramo.reset();
                    }
                }
                telemetry.addData("Tramo", tramo);
                telemetry.addData("Pose", follower.pose());
                telemetry.addData("Tiempo", tiempoTramo.seconds());
                telemetry.update();
                idle();
            }
        } finally {
            // Prueba aislada: cortar motores y no volver a actualizar el follower.
            for (DcMotor motor : hardwareMap.getAll(DcMotor.class)) {
                motor.setPower(0);
            }
        }
    }
}
~~~

**Completar:** sustituir poses, agregar curva y zona de recolección, definir tolerancias de llegada y ruta de estacionamiento. Cambiar la constante de validación solo después de revisar Constants: sus valores están documentados como provisionales. No crear DriveSubsystem escribiendo a los mismos motores mientras Pedro conduce.

**Cómo agregar una curva:** importar Paths.curve y construir, por ejemplo, curve(inicio, control, destino).constant(rumbo), usando Pose para cada punto. Los puntos se miden en el mismo marco; no copiar una curva de otro campo como trayectoria de competencia.

**Cómo integrarlo con SolversLib:** el ejemplo aislado usa LinearOpMode para hacer visible el bucle. Al llevarlo a CommandOpMode, iniciar la ruta en initialize del comando, observar llegada/timeout durante execute y liberar el chasis en end. Actualizar el Follower en un solo lugar. No ejecutar simultáneamente este OpMode de prueba y el TeleOp.

**Prueba de lectura:** explicar por qué follow no va dentro del bucle y por qué reiniciar tiempoTramo cada iteración impediría detectar un timeout.

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


### Código base 2A: obtener una detección nueva de Limelight

Crear VisionBase.java en control. Esta clase selecciona un tag por ID; **no decide aún si la celda es anotable ni convierte el tag en abertura**. Estas dos funciones se completan a continuación. Ejemplo para Limelight 3A y SDK FTC: confirmar modelo instalado antes de usarlo.

~~~java
package org.firstinspires.ftc.teamcode.control;

import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.robotcore.hardware.HardwareMap;

public final class VisionBase {
    private final Limelight3A camera;
    private double ultimoFrame = Double.NaN;
    public String estado = "SIN_DATOS";

    public static final class Deteccion {
        public final LLResult frame;
        public final LLResultTypes.FiducialResult tag;
        Deteccion(LLResult frame, LLResultTypes.FiducialResult tag) {
            this.frame = frame;
            this.tag = tag;
        }
    }

    public VisionBase(HardwareMap hardwareMap, int pipeline) {
        camera = hardwareMap.get(Limelight3A.class, "limelight");
        camera.pipelineSwitch(pipeline); // TODO: índice real del pipeline AprilTag.
        camera.start();
    }

    public Deteccion leerNueva(int idPermitido, long maxRetrasoMs) {
        LLResult frame = camera.getLatestResult();
        if (frame == null || !frame.isValid()) {
            estado = "SIN_OBJETIVO";
            return null;
        }
        // Staleness mide antigüedad de recepción; NO toda la latencia de captura.
        if (frame.getStaleness() > maxRetrasoMs) {
            estado = "DATOS_VIEJOS";
            return null;
        }
        double sello = frame.getTimestamp(); // Reloj local de Limelight.
        if (!Double.isFinite(sello) || sello == ultimoFrame) {
            estado = "SIN_FRAME_NUEVO";
            return null;
        }
        ultimoFrame = sello;
        for (LLResultTypes.FiducialResult tag : frame.getFiducialResults()) {
            if (tag.getFiducialId() == idPermitido) {
                estado = "TAG_SELECCIONADO";
                return new Deteccion(frame, tag);
            }
        }
        estado = "ID_NO_PERMITIDO";
        return null;
    }

    public void cerrar() { camera.stop(); }
}
~~~

**Completar:** tabla de IDs de la alianza; comprobación de orientación de la celda; calidad/distancia razonable de la pose; timeout de adquisición. Para aceptar varios IDs, buscar en un conjunto permitido y mantener asociación ID-celda: al cambiar de tag dentro de la misma celda, convertir siempre al mismo punto de abertura.

Un null por SIN_FRAME_NUEVO no significa que el objetivo se perdió: solo que el bucle del robot corre más rápido que la cámara. Conservar el último objetivo aceptado con fecha de captura y caducidad. Una observación inválida o de celda no elegible puede exigir invalidarlo inmediatamente. El timestamp de cámara sirve aquí para reconocer duplicados; no restarlo directamente de System.nanoTime, porque son relojes distintos.

### Código base 2B: transformar el objetivo y resolver el ángulo

Crear AimMath.java en control. Esta parte es Java independiente del hardware. Todas las distancias recibidas son metros y todos los ángulos internos son radianes. Convertir la pose Pedro si usa pulgadas: metros = pulgadas * 0.0254.

~~~java
package org.firstinspires.ftc.teamcode.control;

public final class AimMath {
    private AimMath() { }

    public static double wrap(double a) {
        return Math.atan2(Math.sin(a), Math.cos(a));
    }

    // R es una rotación 3x3; p y t son vectores de tres componentes.
    // Devuelve R*p + t.
    public static double[] transformar(double[][] r, double[] p, double[] t) {
        return new double[] {
            r[0][0]*p[0] + r[0][1]*p[1] + r[0][2]*p[2] + t[0],
            r[1][0]*p[0] + r[1][1]*p[1] + r[1][2]*p[2] + t[1],
            r[2][0]*p[0] + r[2][1]*p[1] + r[2][2]*p[2] + t[2]
        };
    }

    // Convertir una abertura observada en chasis al marco de campo.
    // psi es el rumbo del chasis EN EL INSTANTE DE CAPTURA.
    public static double[] alCampo(double[] aberturaChasis,
                                   double robotX, double robotY, double psi) {
        double c = Math.cos(psi), s = Math.sin(psi);
        return new double[] {
            robotX + c*aberturaChasis[0] - s*aberturaChasis[1],
            robotY + s*aberturaChasis[0] + c*aberturaChasis[1]
        };
    }

    // Recalcular con la pose ACTUAL mientras el chasis se mueve.
    public static double anguloTorreta(double robotX, double robotY, double psi,
            double objetivoX, double objetivoY, double pivoteX, double pivoteY,
            double offsetCero) {
        double x = robotX + pivoteX*Math.cos(psi) - pivoteY*Math.sin(psi);
        double y = robotY + pivoteX*Math.sin(psi) + pivoteY*Math.cos(psi);
        return wrap(Math.atan2(objetivoY - y, objetivoX - x) - psi - offsetCero);
    }

    // Elegir una vuelta equivalente dentro del recorrido permitido.
    // NO recortar al tope: eso apuntaría a otro lugar.
    public static double equivalenteSeguro(double deseadoDeg, double actualDeg,
                                           double minimoDeg, double maximoDeg) {
        if (!Double.isFinite(deseadoDeg) || !Double.isFinite(actualDeg)
                || !Double.isFinite(minimoDeg) || !Double.isFinite(maximoDeg)
                || minimoDeg > maximoDeg) return Double.NaN;
        double kMin = Math.ceil((minimoDeg - deseadoDeg) / 360.0);
        double kMax = Math.floor((maximoDeg - deseadoDeg) / 360.0);
        if (kMin > kMax) return Double.NaN;
        double k = Math.rint((actualDeg - deseadoDeg) / 360.0);
        k = Math.max(kMin, Math.min(kMax, k));
        return deseadoDeg + 360.0*k;
    }
}
~~~

**Conectar visión y geometría:**

~~~java
// Fragmento conceptual: completar matrices/vectores con la detección elegida.
// R_CT: rotación tag -> cámara; t_CT: posición del tag en cámara.
// R_BC y t_BC: montaje cámara -> chasis en el instante de captura.
double[] aberturaCamara = AimMath.transformar(R_CT, offsetTagAbertura, t_CT);
double[] aberturaChasis = AimMath.transformar(R_BC, aberturaCamara, t_BC);
double[] objetivoCampo = AimMath.alCampo(aberturaChasis, xCaptura, yCaptura, psiCaptura);
~~~

Validar dimensiones de matrices, valores finitos y distancia de objetivo positiva antes del cálculo. Esas variables son datos por implementar, no funciones de Limelight con esos nombres. Extraer la pose espacial del tag seleccionado, verificar sus ejes y construir la transformación. Si la API devuelve cámara respecto al tag, invertirla: R_CT=transpose(R_TC), t_CT=-R_CT*t_TC. Usar Euler solo después de confirmar ejes, unidades y orden de rotación; no asumir que yaw/pitch/roll se multiplican en cualquier orden. Validar primero con cámara y tag quietos en poses medidas.

**Completar la latencia:** mantener un historial acotado de pose de chasis y ángulo de torreta con un reloj monótono del Hub. Estimar el momento de captura a partir de tiempo de recepción y latencias disponibles, evitando contar el mismo componente dos veces; interpolar el historial. Verificar la semántica de timestamps/latencias en el SDK instalado. Si no hay historial válido, rechazar la muestra. Si cámara está en torreta, actualizar tanto rotación como traslación de montaje al ángulo de captura. Para empezar, validar en reposo y luego aumentar velocidad, midiendo el error.

**Pruebas rápidas sin motores:** objetivo (1, 0.3), robot (0,0), offsets cero -> 16.70 grados. Misma posición y chasis girado +90 grados -> -73.30 grados. Robot trasladado a (0,0.2), rumbo cero -> 5.71 grados. Explicar por qué la torreta debe girar aunque solo se traslade el chasis.

### Código base 2C: habilitación por botón y control existente

El TurretSubsystem ya contiene confirmCenter(), setTargetDegrees(), update(), stop() y su controlador P. Reutilizarlo; no agregar otro PID que escriba al mismo motor. Confirmar el cero físicamente antes de empezar. Revisar que getCurrentDegrees() tenga una lectura reciente: actualmente se actualiza dentro de update() cuando hay objetivo. Separar lectura de encoder y cálculo de salida si hace falta obtener mediciones también en manual y registrar historial.

Fragmento para el único comando que posea la torreta. Los valores robotX/robotY/psi se leen de la pose actual; objetivoX/objetivoY son la última abertura válida en campo:

~~~java
// evaluar cada ciclo; botonApuntado es el botón MANTENIDO elegido en TeleOp.
if (!botonApuntado) {
    // TODO: invalidar referencia antigua al cambiar de automático a manual.
    turret.stop();
    // TODO: pasar al comando manual existente si existe.
    // Sin comando manual implementado, esta base solamente detiene el giro.
} else if (!objetivoVigente || !localizacionConfiable || !turret.isCalibrated()) {
    turret.stop();
} else {
    double rad = AimMath.anguloTorreta(robotX, robotY, psi,
        objetivoX, objetivoY, pivoteX, pivoteY, offsetCero);
    double objetivoDeg = AimMath.equivalenteSeguro(Math.toDegrees(rad),
        turret.getCurrentDegrees(), minimoDeg, maximoDeg);
    if (!Double.isFinite(objetivoDeg) || !turret.setTargetDegrees(objetivoDeg)) {
        turret.stop(); // FUERA_DE_LIMITE o calibración inválida.
    } else {
        turret.update(); // Actualiza encoder y controlador existentes.
    }
}
~~~

Las variables del fragmento deben ser campos o datos del comando: no copiar el bloque como clase independiente. Registrar su valor y unidad al integrarlas. En TeleOpMain ya existe TurretAimCommand como comando por defecto: modificarlo o reemplazarlo, no ejecutarlo junto a otro control de torreta. Confirmar la función actual de sus botones antes de asignar uno.

**Completar:** prioridad manual/automático, borrado de referencia al soltar, lectura real del botón, límites desde TurretConfig, validez temporal del objetivo y telemetría. Para AUTO pasar habilitado=true únicamente durante fases que requieran apuntado; jamás leer gamepad para decidir en AUTO.

**Prueba:** avanzar, trasladarse lateralmente y girar manteniendo el botón. Soltar a mitad del movimiento, verificar cancelación y volver a presionar. Mostrar error angular y motivo de bloqueo. No hay que añadir otro video ni reporte.

## Reto 3. Autónomo completo con decisiones y recuperación

**Indicación.** Integrar el recorrido y el apuntado en una rutina que dispare precargas, realice un ciclo de recolección si alcanza el tiempo y termine estacionada. Programar también la opción de omitir el ciclo y estacionarse directamente.

Autorizar alimentación solamente cuando el shooter alcance velocidad estable, la torreta esté alineada y el objetivo sea válido. Dosificar según las posibilidades reales del mecanismo y esperar recuperación entre tiros. Si no hay sensor de pelota, declarar la limitación y no presentar un conteo estimado como detección confirmada.

**Mini tutorial.** Usar fases de preparación, recorrido, apuntado, tiro, recolección y estacionamiento. Cada fase termina por una condición observable o un timeout. Antes de otro ciclo comparar tiempo restante con duración medida del ciclo + estacionamiento + margen. Si falla visión, cancelar el tiro; si la pose sigue siendo confiable, usar la ruta de estacionamiento validada. Si falla la localización, detenerse.

**Comprobar.** Ejecutar cinco pruebas completas: tres nominales, una con pérdida temporal de visión y una con tiempo disponible reducido mediante configuración previa al arranque. Mostrar al menos una ejecución con recolección y otra que omita el ciclo. Buscar al menos cuatro de cinco ejecuciones con recorrido y estacionamiento correctos; registrar aciertos por separado. Todas deben detenerse dentro del período autónomo y permanecer sin movimiento accionado durante la transición.


### Código base 3A: decidir cuándo está listo para disparar

Crear Estabilidad.java en control. Sirve para exigir que una condición permanezca cierta: no basta con tocar fugazmente las RPM o el ángulo objetivo.

~~~java
package org.firstinspires.ftc.teamcode.control;

public final class Estabilidad {
    private double desde = Double.NaN;
    public void reset() { desde = Double.NaN; }
    public boolean cumple(boolean condicion, double ahoraS, double duracionS) {
        if (!condicion) { reset(); return false; }
        if (Double.isNaN(desde)) desde = ahoraS;
        return ahoraS - desde >= duracionS;
    }
}
~~~

Uso dentro del adaptador del robot, una vez por ciclo. Los parámetros son ajustes experimentales, no valores ya calibrados:

~~~java
boolean condiciones = objetivoVigente && celdaElegible && torretaDentroDeLimites
    && Math.abs(rpmObjetivo - shooter.getVelocityRpm()) <= toleranciaRpm
    && Math.abs(anguloObjetivoDeg - turret.getCurrentDegrees()) <= toleranciaAnguloDeg
    && chasisEstable;
boolean listoParaTiro = estabilidad.cumple(condiciones, ahoraS, permanenciaS);
~~~

La resta angular usa aquí el objetivo equivalente seguro dentro del recorrido mecánico, no un ángulo normalizado arbitrario. Revalidar condiciones durante alimentación. El seguimiento funciona al moverse, pero esta primera rutina dispara desde una posición estabilizada; disparar durante traslación requiere compensar también la trayectoria de la pelota.

**Detalle del repositorio:** ShooterSubsystem.setTargetRpm() reinicia su controlador. Llamarlo al entrar a la fase o cuando cambie realmente la consigna, no en cada loop. Llamar shooter.update() continuamente mientras esté activo. Igualmente, no ejecutar el ShooterCommand de gamepad como controlador paralelo en AUTO.

### Código base 3B: secuencia completa para rellenar con el robot real

Crear AutoRutinaBase.java en control. Esta clase contiene la lógica del reto; RobotIO es una interfaz propia de este ejemplo, no una librería externa. El adaptador concreto conectará Pedro, visión y los subsistemas. Los parámetros de Config se rellenan con medidas; sus valores por defecto cero hacen que falle la validación en vez de ejecutar una rutina sin calibrar.

~~~java
package org.firstinspires.ftc.teamcode.control;

public final class AutoRutinaBase {
    public enum Destino { TIRO, RECOLECCION, ESTACIONAMIENTO }
    public enum Fase { APROXIMAR, APUNTAR, ALIMENTAR, RECUPERAR,
                       RECOLECTAR, REGRESAR, ESTACIONAR, FIN, FALLA }

    public interface RobotIO {
        void actualizar(); // Pose/follower, visión, torreta y shooter: un dueño cada uno.
        boolean localizacionConfiable();
        void irA(Destino destino); // Iniciar UNA vez, desde la pose actual validada.
        boolean recorridoTerminadoBien(); // Incluye tolerancia y baja velocidad.
        boolean recorridoFallo(); // No convertir timeout de Pedro en éxito.
        void prepararShooter(); // Consigna calibrada, sin resetearla cada loop.
        void habilitarApuntado(boolean habilitado);
        boolean listoParaTiro(); // Resultado actualizado de Estabilidad.
        boolean condicionesCriticasDeTiro(); // Visión/alineación/límites/chasis válidos.
        boolean puedeAlimentar(); // Sensor, o política explícita si no hay sensor.
        void alimentar(boolean activo);
        void recolectar(boolean activo);
        void detenerMecanismos();
        void detenerTodo(); // Cancelar comandos/rutas y cortar actuadores.
    }

    public static final class Config {
        public double limiteRutaS, limiteApuntadoS, pulsoAlimentacionS;
        public double recuperacionS, tiempoCicloS, tiempoEstacionarS, margenS;
        public double limiteAutoS;
        public int precargas, pedidosTrasRecolectar;
        public void validar() {
            double[] valores = {limiteRutaS, limiteApuntadoS, pulsoAlimentacionS,
                recuperacionS, tiempoCicloS, tiempoEstacionarS, margenS, limiteAutoS};
            for (double v : valores)
                if (!Double.isFinite(v) || v <= 0)
                    throw new IllegalArgumentException("Completar tiempos medidos");
            if (precargas < 1 || pedidosTrasRecolectar < 1
                    || limiteAutoS > 30 || tiempoEstacionarS + margenS >= limiteAutoS)
                throw new IllegalArgumentException("Revisar configuración AUTO");
        }
    }

    private final RobotIO io;
    private final Config c;
    private Fase fase = Fase.FIN;
    private double entradaS;
    private boolean cicloUsado;
    private int pedidos, cuota;
    public String motivo = "SIN_INICIAR";

    public AutoRutinaBase(RobotIO io, Config c) {
        c.validar(); this.io = io; this.c = c;
    }
    public Fase fase() { return fase; }
    private void entrar(Fase nueva, double t) { fase = nueva; entradaS = t; }
    private double edad(double t) { return t - entradaS; }
    private boolean finalizado() { return fase == Fase.FIN || fase == Fase.FALLA; }

    public void iniciar(double t) {
        cicloUsado = false; pedidos = 0; cuota = c.precargas;
        motivo = "EN_CURSO";
        io.alimentar(false); io.recolectar(false);
        io.prepararShooter(); io.habilitarApuntado(true);
        io.irA(Destino.TIRO);
        entrar(Fase.APROXIMAR, t);
    }
    private void fallar(String razon, double t) {
        motivo = razon;
        io.detenerTodo(); entrar(Fase.FALLA, t);
    }
    private void estacionar(double t) {
        io.habilitarApuntado(false); io.detenerMecanismos();
        io.irA(Destino.ESTACIONAMIENTO);
        entrar(Fase.ESTACIONAR, t);
    }
    public void cancelar() {
        io.detenerTodo(); fase = Fase.FIN; motivo = "CANCELADO";
    }

    // t = segundos desde START, con el mismo reloj monótono en todo el OpMode.
    public void paso(double t) {
        if (finalizado()) return;
        if (t >= c.limiteAutoS) { fallar("LIMITE_AUTO", t); return; }
        if (!io.localizacionConfiable()) { fallar("LOCALIZACION", t); return; }
        if (fase != Fase.ESTACIONAR
                && t >= c.limiteAutoS - c.tiempoEstacionarS - c.margenS) {
            estacionar(t); return;
        }
        switch (fase) {
            case APROXIMAR:
            case REGRESAR:
                if (io.recorridoFallo() || edad(t) > c.limiteRutaS)
                    fallar("RUTA_TIRO", t);
                else if (io.recorridoTerminadoBien()) entrar(Fase.APUNTAR, t);
                break;
            case APUNTAR:
                if (edad(t) > c.limiteApuntadoS) {
                    motivo = "TIRO_OMITIDO"; estacionar(t);
                } else if (io.listoParaTiro() && io.puedeAlimentar()) {
                    io.alimentar(true); entrar(Fase.ALIMENTAR, t);
                }
                break;
            case ALIMENTAR:
                if (!io.condicionesCriticasDeTiro()) {
                    io.alimentar(false); motivo = "CONDICIONES_PERDIDAS";
                    estacionar(t);
                } else if (edad(t) >= c.pulsoAlimentacionS) {
                    io.alimentar(false);
                    pedidos++; // Órdenes de alimentación, NO aciertos confirmados.
                    entrar(Fase.RECUPERAR, t);
                }
                break;
            case RECUPERAR:
                if (edad(t) < c.recuperacionS) break;
                if (pedidos < cuota) entrar(Fase.APUNTAR, t);
                else if (!cicloUsado && c.limiteAutoS - t
                        > c.tiempoCicloS + c.tiempoEstacionarS + c.margenS) {
                    cicloUsado = true;
                    io.habilitarApuntado(false); io.recolectar(true);
                    io.irA(Destino.RECOLECCION); entrar(Fase.RECOLECTAR, t);
                } else estacionar(t);
                break;
            case RECOLECTAR:
                if (io.recorridoFallo() || edad(t) > c.limiteRutaS)
                    fallar("RUTA_RECOLECCION", t);
                else if (io.recorridoTerminadoBien()) {
                    io.recolectar(false);
                    pedidos = 0; cuota = c.pedidosTrasRecolectar;
                    io.habilitarApuntado(true);
                    io.irA(Destino.TIRO); entrar(Fase.REGRESAR, t);
                }
                break;
            case ESTACIONAR:
                if (io.recorridoFallo() || edad(t) > c.limiteRutaS)
                    fallar("RUTA_ESTACIONAMIENTO", t);
                else if (io.recorridoTerminadoBien()) {
                    io.detenerTodo(); entrar(Fase.FIN, t);
                }
                break;
            default: fallar("FASE_INESPERADA", t);
        }
    }
}
~~~

**Qué deben completar en RobotIO, sin devolver true como simulación de hardware:**

| Método o grupo | Base disponible y trabajo pendiente |
|---|---|
| actualizar | Llamar una vez a follower.update(), procesar nueva visión y actualizar controles activos. No duplicar estos update desde periodic o comandos. |
| irA | Construir/seleccionar Path de Pedro y llamar follower.follow(path) una vez. Definir rutas desde las posibles poses reales, incluyendo recuperación/estacionamiento. |
| recorridoTerminadoBien | Combinar final del seguimiento, error de pose/rumbo, velocidad y estabilidad temporal. Guardar por separado si terminó por timeout. |
| prepararShooter | Usar ShooterSubsystem.setTargetRpm(rpmMedidas) al entrar, con parámetros reales. |
| habilitarApuntado | Activar el cálculo del reto 2 en AUTO, sin botones. Desactivar también elimina una consigna automática pendiente. |
| listoParaTiro / condicionesCriticasDeTiro | Antes de alimentar, exigir Estabilidad y RPM reales. Durante el pulso, comprobar visión, alineación, límites y chasis; distinguir una caída normal breve de RPM de una falla crítica. Volver a exigir RPM estables antes de la siguiente pelota. |
| puedeAlimentar | Leer sensor si existe o definir número de solicitudes máximo según carga física verificada; documentar incertidumbre. |
| alimentar/recolectar | Conectar actuadores reales; pueden compartir transmisión: resolver prioridad, sentido y dosificación sin que ambos controladores se contradigan. |
| detenerTodo | Cancelar scheduler/follower y detener mecanismos y chasis; probar STOP desde cada fase. |

El pulso de alimentación es una base de dosificación, no una confirmación de salida. Si agregan sensor, sustituir su condición final por transición detectada con un tiempo máximo de respaldo. Agregar recuperación de atasco acotada dentro del mismo reto, sin otra entrega.

### Conectar la rutina a un OpMode

Este esquema usa los métodos reales de la clase anterior. Crear un adaptador RobotIO concreto antes de convertirlo en un OpMode ejecutable; los TODO son piezas deliberadamente pendientes.

~~~java
// CAMPOS del OpMode:
AutoRutinaBase.RobotIO io;
AutoRutinaBase rutina;
com.qualcomm.robotcore.util.ElapsedTime reloj =
    new com.qualcomm.robotcore.util.ElapsedTime();

// INIT:
// TODO: io = new AdaptadorRobot(hardwareMap, ...); implementar esa clase.
// TODO: crear Config y rellenar TODOS sus parámetros medidos.
// rutina = new AutoRutinaBase(io, config);
// TODO: esperar calibración real, confirmar cero de torreta y fijar pose inicial.

// START, solo si INIT terminó correctamente:
reloj.reset();
rutina.iniciar(0);

// LOOP, una vez por ciclo, protegido con manejo de falla que llame detenerTodo:
io.actualizar();
rutina.paso(reloj.seconds());
// Mostrar rutina.fase(), rutina.motivo y telemetría del robot.

// STOP y también FINALLY ante excepción:
// si rutina != null: rutina.cancelar();
// si io != null: io.detenerTodo();
// cerrar VisionBase cuando corresponda.
~~~

No pegar todos estos fragmentos consecutivos como si fueran un archivo compilable: colocar CAMPOS, INIT, START, LOOP y STOP en los métodos correspondientes del OpMode. En CommandOpMode, revisar qué actualiza super.run() y qué comandos se registran: no dejar comandos por defecto de joystick controlando motores en AUTO. Mantener un solo scheduler.

**Configuración por completar:** tiempos máximos por ruta, ventana de apuntado, permanencia estable, pulso real de alimentación, recuperación, tiempo conservador del ciclo, tiempo de estacionamiento y margen antes de 30 s. Medirlos con las cinco pruebas ya solicitadas; no son cinco entregas nuevas.

**Cómo probar de menor a mayor:** verificar matemáticas sin motores; leer Limelight con telemetría; mover torreta con potencia limitada; ejecutar recorrido sin pelotas; probar una alimentación; integrar precargas; activar ciclo opcional y pérdida de visión. Si una etapa falla, inspeccionar su estado y datos antes de cambiar varias ganancias.

**Ayuda para depurar:**

| Síntoma | Revisar primero |
|---|---|
| Pedro reinicia la ruta constantemente | follow llamado dentro del loop en vez de al entrar |
| Nunca alcanza RPM estables | setTargetRpm repetido cada ciclo, dirección/escala del encoder, carga mecánica |
| Apunta al lado opuesto al girar | Signo del rumbo, grados/radianes, resta de psi |
| Apunta al tag pero falla la abertura | Offset tag-abertura y transformación de montaje |
| Funciona quieto pero falla al moverse | Tiempo de captura, historial y cámara montada sobre torreta |
| Sigue apuntando al soltar | Otra orden automática o comando por defecto sigue escribiendo |
| Se dispara sin estar listo | Condiciones ficticias, estabilidad sin reinicio o referencia caducada |
| Finaliza la ruta pero los motores siguen actuando | Retención de pose y salida del follower; cerrar control y cortar salidas |

Los bloques completos de Java se ofrecen como bases para adaptar; los fragmentos conceptuales lo indican expresamente. No se ha compilado el proyecto Android ni probado este material en el robot. Los TODO de geometría, hardware y parámetros deben resolverse con mediciones, no con valores inventados.

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
