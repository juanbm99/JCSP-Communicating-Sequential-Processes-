// Grupo 2M-B: Juan Bernal Mencia (z170277), Felipe León Fernández (z170308)

//package Prac1;

import es.upm.babel.cclib.Monitor;

public class EnclavamientoMonitor implements Enclavamiento {

	//	Atributos:
	private boolean p;						// para controlar la presencia o no de coches en la via		
	private boolean estFrenoEsperado; 		// para guardar el estado del freno
	private boolean estBarreraEsperado;		// para guardar el estado de la barrera
	
	private int trains[];					// para controlar el numero de trenes en cada segmento de la via
	private Control.Color color[];			// para controlar el color de los 3 semaforos a lo largo de la via
	
	private Control.Color esperado[];		// para controlar el color de los 3 semaforos a lo largo de la via
	
	private Monitor.Cond cBarrera;			// condition para controlar los procesos relacionados con la barrera
	private Monitor.Cond cFreno;			// condition para controlar los procesos relacionados con el freno
									
	private Monitor.Cond[] cSemaforos;		// condition controlar los procesos relacionados con los semaforos

	Monitor mutex = new Monitor();			// Monitor que garantiza la exclusion mutua y la sincronizacion por las condiciones
														

	public EnclavamientoMonitor () {
		// Inicializacion de los atributos
		p = false;
		
		estFrenoEsperado = false;
		estBarreraEsperado = false;
		
		trains = new int[4];
		trains[1] = 0;
		trains[2] = 0;
		trains[3] = 0;
		
		color = new Control.Color[4];
		color[1] = Control.Color.VERDE;
		color[2] = Control.Color.VERDE;
		color[3] = Control.Color.VERDE;
		
		esperado = new Control.Color[4];
		esperado[1] = Control.Color.VERDE;
		esperado[2] = Control.Color.VERDE;
		esperado[3] = Control.Color.VERDE;
		
		cBarrera = mutex.newCond();
		cFreno = mutex.newCond();
		
		cSemaforos = new Monitor.Cond[4];
		cSemaforos[1] = mutex.newCond();
		cSemaforos[2] = mutex.newCond();
		cSemaforos[3] = mutex.newCond();
	}

	
	public void avisarPresencia(boolean presencia) {
		// 	Entrada al monitor
		mutex.enter();
		
		//	Ya que la CPRE es siempre cierta, este metodo no es bloqueante por lo que continua para realizar la POST
		
		// 	Implementacion de la POST
		p = presencia;					
		coloresCorrectos();				
										
		
		// 	Llamada a codigo de desbloqueo
		desbloqueo();
		
		// 	Salida del monitor
		mutex.leave();
	}

	
	public boolean leerCambioBarrera(boolean actual) {
		// 	Entrada al monitor
		mutex.enter();
		
		// Almacenamos el estado de la barrera antes de bloquear
		estBarreraEsperado = (trains[1] + trains[2] == 0);
		
		//	Chequeo de la CPRE y posible bloqueo
		if(actual==(trains[1] + trains[2] == 0)){		
			cBarrera.await();								
		}													
		
		// 	Llamada al codigo de desbloqueo
		desbloqueo();		
		
		// 	Salida del monitor
		mutex.leave();									
		
		return estBarreraEsperado;	
	}


	public boolean leerCambioFreno(boolean actual) {
		// 	Entrada al monitor
		mutex.enter();
		
		// Almacenamos el estado del freno antes de bloquear
		estFrenoEsperado = ((trains[1] > 1 || trains[2] > 1 || (trains[2] == 1 && p)));
		
		//	Chequeo de la CPRE y posible bloqueo
		if(actual == (trains[1] > 1 || trains[2] > 1 || (trains[2] == 1 && p))) {	
			cFreno.await();															 
		}																			
																			
		// 	Llamada al codigo de desbloqueo
		desbloqueo();
		
		// 	Salida del monitor
		mutex.leave();
		
		return estFrenoEsperado;	
	}

	public Control.Color leerCambioSemaforo(int i, Control.Color actual) throws PreconditionFailedException {
		//Chequeo de la PRE
			if(i == 0) {									
				throw new PreconditionFailedException();
			}
		// 	Entrada al monitor
		mutex.enter();
		
		// Almacenamos el estado del semáforo antes de bloquear
		esperado[i] = color[i];
		
		// 	Chequeo de la CPRE y posible bloqueo
		if(actual.equals(color[i])) {						
			cSemaforos[i].await(); 						
		}
		
		// 	Llamada al codigo de desbloqueo
		desbloqueo();
		
		// 	Salida del monitor
		mutex.leave();
		
		// Devuelve el color que hay en el enclavamiento del semáforo i 
		return color[i];	
	}

	
	public void avisarPasoPorBaliza(int i) throws PreconditionFailedException {
		// Chequeo de la PRE
				if(i == 0) {		
					throw new PreconditionFailedException();	
				}
		// Entrada al monitor
		mutex.enter();	
		
		// 	Ya que la CPRE es siempre cierta, este metodo no es bloqueante por lo que continua para realizar la POST
		
		// 	Implementacion de la POST
		trains[i-1] = trains[i-1] - 1;					
		trains[i] = trains[i] + 1;						
		coloresCorrectos();		
		
		// Llamada al codigo de desbloqueo
		desbloqueo();								
		
		// Salida del monitor
		mutex.leave();
	}
	
	/*	Metodo que asegura que se cumple que los semaforos tienen
	 * 	los colores que se especifican en el enunciado en 
	 *	funcion del estado de los trenes en los segmentos y la 
	 *	presencia de coches en las vias
	 */
	private void coloresCorrectos() {
		//	Comprobacion y cambio del color del semaforo 1 
		
		if(trains[1] > 0) {
			color[1] = Control.Color.ROJO;
		}else if(trains[1] == 0 && (trains[2] > 0 || p)){
			color[1] = Control.Color.AMARILLO;
		}else if(trains[1] == 0 && trains[2] == 0 && !p){
			color[1] = Control.Color.VERDE;
		}
		
		//	Comprobacion y cambio del color del semaforo 2
		
		if(trains[2] > 0 || p){
			color[2] = Control.Color.ROJO;
		}else if(trains[2] == 0 && !p){
			color[2] = Control.Color.VERDE;
		}
		
		//	Comprobacion y cambio del color del semaforo 3
		
		if(!color[3].equals(Control.Color.VERDE)){
			color[3] = Control.Color.VERDE;
		}
	}
	
	/*
	 * Código de desbloqueo común para todos los métodos
	 */
	
	private void desbloqueo() {
		if(cSemaforos[1].waiting() > 0 && !esperado[1].equals(color[1])) {
			cSemaforos[1].signal();
		}else if(cSemaforos[2].waiting() > 0 && !esperado[2].equals(color[2])) {
			cSemaforos[2].signal();
		}else if(cSemaforos[3].waiting() > 0 && !esperado[3].equals(color[3])) {
			cSemaforos[3].signal();
		}else if(cFreno.waiting() > 0 && (estFrenoEsperado != (trains[1] > 1 || trains[2] > 1 || (trains[2] == 1 && p)))) {
			cFreno.signal();
		}else if(cBarrera.waiting() > 0 && (estBarreraEsperado != (trains[1] + trains[2] == 0))) {
			cBarrera.signal();	
		}
	}
}
