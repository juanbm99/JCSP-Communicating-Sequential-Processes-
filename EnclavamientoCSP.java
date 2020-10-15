// Grupo 2M-B: Juan Bernal Mencia (z170277), Felipe León Fernández (z170308)
import org.jcsp.lang.Alternative;
import org.jcsp.lang.AltingChannelInput;
import org.jcsp.lang.CSProcess;
import org.jcsp.lang.Channel;
import org.jcsp.lang.Guard;
import org.jcsp.lang.One2OneChannel;
import org.jcsp.lang.ProcessManager;

/**
 * Implementation using channel replication
 */
public class EnclavamientoCSP implements CSProcess, Enclavamiento {

	/** WRAPPER IMPLEMENTATION */
	/**
	 * Channels for receiving external requests
	 * just one channel for nonblocking requests
	 */
	private final One2OneChannel chAvisarPresencia     = Channel.one2one();
	private final One2OneChannel chAvisarPasoPorBaliza = Channel.one2one();
	// leerCambioBarrera blocks depending on a boolean parameter
	private final One2OneChannel chLeerCambioBarreraT  = Channel.one2one();
	private final One2OneChannel chLeerCambioBarreraF  = Channel.one2one();
	// leerCambioFreno blocks depending on a boolean parameter
	private final One2OneChannel chLeerCambioFrenoT    = Channel.one2one();
	private final One2OneChannel chLeerCambioFrenoF    = Channel.one2one();
	// leerCambioSemaforo blocks depending on a semaphore id and a colour
	private final One2OneChannel[][] chLeerCambioSemaforo =
			new One2OneChannel[3][3];


	public EnclavamientoCSP () {
		// pending initializations
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				chLeerCambioSemaforo[i][j] = Channel.one2one();
			}
		}
		new ProcessManager(this).start();

	}

	public void avisarPresencia(boolean presencia) {
		chAvisarPresencia.out().write(presencia);
	}

	public void avisarPasoPorBaliza(int i) {
		if (i == 0 )
			throw new PreconditionFailedException("Baliza 0 no existe");

		chAvisarPasoPorBaliza.out().write(i);
	}

	public boolean leerCambioBarrera(boolean abierta) {
		One2OneChannel chreply = Channel.one2one();
		if (abierta) {
			chLeerCambioBarreraT.out().write(chreply);
		} else {
			chLeerCambioBarreraF.out().write(chreply);
		}
		return (Boolean) chreply.in().read();
	}

	public boolean leerCambioFreno(boolean accionado) {
		One2OneChannel chreply = Channel.one2one();
		if (accionado) {
			chLeerCambioFrenoT.out().write(chreply);
		} else {
			chLeerCambioFrenoF.out().write(chreply);
		}
		return (Boolean) chreply.in().read();
	}

	/** notice that the exception must be thrown outside the server */
	public Control.Color leerCambioSemaforo (int i, Control.Color color) {
		if (i == 0 || i > 3)
			throw new PreconditionFailedException("Semaforo 0 no existe");

		One2OneChannel chreply = Channel.one2one();

		chLeerCambioSemaforo[i-1][color.ordinal()].out().write(chreply);

		return (Control.Color) chreply.in().read();
	}

	/** SERVER IMPLEMENTATION */
	
	public void run() {
		// resource state is kept in the server
		// Declaracion del estado del recurso
		
		// Presencia
		boolean presencia;
		// Trenes
		int[] trenes;
		// Color
		Control.Color[] color;

		// state initialization
		// Inicialización del recurso
		presencia = false;
		trenes = new int[4];
		color = new Control.Color[4];
		
		trenes[1] = 0;
		trenes[2] = 0;
		trenes[3] = 0;
		
		color[1]=Control.Color.VERDE;
		color[2]=Control.Color.VERDE;
		color[3]=Control.Color.VERDE;

		// mapping request numbers to channels and vice versa
		// 0 <--> chAvisarPresencia
		// 1 <--> chAvisarPasoPorBaliza
		// 2 <--> chLeerCambioBarreraT
		// 3 <--> chLeerCambioBarreraF
		// 4 <--> chLeerCambioFrenoT
		// 5 <--> chLeerCambioFrenoF
		// 6 + (3*(i-1)) + j <--> chLeerCambioSemaforo[i][j]
		Guard[] inputs = new AltingChannelInput[15];
		inputs[0] = chAvisarPresencia.in();
		inputs[1] = chAvisarPasoPorBaliza.in();
		inputs[2] = chLeerCambioBarreraT.in();
		inputs[3] = chLeerCambioBarreraF.in();
		inputs[4] = chLeerCambioFrenoT.in();
		inputs[5] = chLeerCambioFrenoF.in();
		for (int i = 6; i < 15; i++) {
			inputs[i] = chLeerCambioSemaforo[(i-6) / 3][(i-6) % 3].in();
		}

		Alternative services = new Alternative(inputs);
		int chosenService = 0;

		// conditional sincronization
		boolean[] sincCond = new boolean[15];
		// algunas condiciones de recepción no varían durante
		// la ejecución del programa
		sincCond[0] = true;
		sincCond[1] = true;

		while (true){
			// Actualizamos la sincronización condicional mediante el cumplimiento de sus CPREs
			sincCond[2] = !(trenes[1] + trenes[2] == 0);										// leerCambioBarreraT
			sincCond[3] = (trenes[1] + trenes[2] == 0);											// leerCambioBarreraF
			sincCond[4] = !(trenes[1] > 1 || trenes[2] > 1 || (trenes[2] == 1 && presencia));	// leerCambioFrenoT
			sincCond[5] = (trenes[1] > 1 || trenes[2] > 1 || (trenes[2] == 1 && presencia));	// leerCambioFrenoF
			sincCond[6] = !color[1].equals(Control.Color.ROJO);									// Semaforo 1 - Rojo
			sincCond[7] = !color[1].equals(Control.Color.AMARILLO);								// Semaforo 1 - Amarillo
			sincCond[8] = !color[1].equals(Control.Color.VERDE);								// Semaforo 1 - Verde
			sincCond[9] = !color[2].equals(Control.Color.ROJO);									// Semaforo 2 - Rojo
			sincCond[10] = !color[2].equals(Control.Color.AMARILLO);							// Semaforo 2 - Amarillo
			sincCond[11] = !color[2].equals(Control.Color.VERDE);								// Semaforo 2 - Verde
			sincCond[12] = !color[3].equals(Control.Color.ROJO);								// Semaforo 3 - Rojo
			sincCond[13] = !color[3].equals(Control.Color.AMARILLO);							// Semaforo 3 - Amarillo
			sincCond[14] = !color[3].equals(Control.Color.VERDE);								// Semaforo 3 - Verde

			// esperar petición
			chosenService = services.fairSelect(sincCond);
			One2OneChannel chreply; // lo usamos para contestar a los clientes

			switch(chosenService){
				case 0: // avisarPresencia
					//@ assume inv & pre && cpre of operation;
					// Lectura del mensaje del canal
					boolean p = (Boolean) chAvisarPresencia.in().read();
	
					// Actualización del estado del recurso
					presencia = p;
					coloresCorrectos(trenes, presencia, color);
					
					break;
				case 1: // avisarPasoPorBaliza
					//@ assume inv & pre && cpre of operation;
					// Lectura del mensaje del canal
					int i = (Integer) chAvisarPasoPorBaliza.in().read();
	
					// Actualización del estado del recurso
					trenes[i-1] = trenes[i-1] - 1;					
					trenes[i] = trenes[i] + 1;
					
					coloresCorrectos(trenes, presencia, color);
										
					break;
				case 2: // leerCambioBarrera(true)
					//@ assume inv & pre && cpre of operation;
					// Lectura del mensaje del canal y procesamiento de petición
					 chreply = (One2OneChannel) chLeerCambioBarreraT.in().read();	
	
					// POST: valor a devolver al cliente
					boolean barreraT = (trenes[1] + trenes[2] == 0);
					
					// Contestación al cliente
					chreply.out().write(barreraT);
	
					break;
				case 3: // leerCambioBarrera(false)
					//@ assume inv & pre && cpre of operation;
					// Lectura del mensaje del canal y procesamiento de petición				
					 chreply = (One2OneChannel) chLeerCambioBarreraF.in().read();
					
					// POST: valor a devolver al cliente
					boolean barreraF = !(trenes[1] + trenes[2] == 0);
					
					// Contestación al cliente
					chreply.out().write(barreraF);
					
					break;
				case 4: // leerCambioFreno(true)
					//@ assume inv & pre && cpre of operation;
					// Lectura del mensaje del canal y procesamiento de petición	
					 chreply = (One2OneChannel) chLeerCambioFrenoT.in().read();
					
					// POST: valor a devolver al cliente
					boolean frenoT = (trenes[1] > 1 || trenes[2] > 1 || (trenes[2] == 1 && presencia));
					
					// Contestación al cliente
					chreply.out().write(frenoT);
	
					break;
				case 5: // leerCambioFreno(false)
					//@ assume inv & pre && cpre of operation;
					// Lectura del mensaje del canal y procesamiento de petición					
					chreply = (One2OneChannel) chLeerCambioFrenoF.in().read();
					
					// POST: valor a devolver al cliente
					boolean frenoF = !(trenes[1] > 1 || trenes[2] > 1 || (trenes[2] == 1 && presencia));
					
					// Contestación al cliente
					chreply.out().write(frenoF);
	
					break;
				default: // leerCambioSemaforo(queSemaforo,queColor)					
					// Decodificación del numero del semáforo y color		
			
					int fila = ((chosenService-6) / 3);
					int columna = ((chosenService-6) % 3);					
					
					
					// Lectura del mensaje del canal
					chreply = (One2OneChannel) chLeerCambioSemaforo[fila][columna].in().read();
									
					// Contestación al cliente
					chreply.out().write(color[fila+1]);
					
					break;
			} // SWITCH
		} // SERVER LOOP
	} // run()

	// métodos auxiliares varios
	
	/*	Metodo que asegura que se cumple que los semaforos tienen
	 * 	los colores que se especifican en el enunciado en 
	 *	funcion del estado de los trenes en los segmentos y la 
	 *	presencia de coches en las vias
	 */
	
	private void coloresCorrectos(int[] trenes, boolean presencia, Control.Color[] color) {
		//	Comprobacion y cambio del color del semaforo 1 en el enclavamiento
		if(trenes[1] > 0) {
			color[1] = Control.Color.ROJO;
		}else if(trenes[1] == 0 && (trenes[2] > 0 || presencia)){
			color[1] = Control.Color.AMARILLO;
		}else if(trenes[1] == 0 && trenes[2] == 0 && !presencia){
			color[1] = Control.Color.VERDE;
		}
		
		//	Comprobacion y cambio del color del semaforo 2
		
		if(trenes[2] > 0 || presencia){
			color[2] = Control.Color.ROJO;
		}else if(trenes[2] == 0 && !presencia){
			color[2] = Control.Color.VERDE;
		}
		
		//	Comprobacion y cambio del color del semaforo 3
		
		if(!color[3].equals(Control.Color.VERDE)){
			color[3] = Control.Color.VERDE;
		}
	}
} // end CLASS
