import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import HTTPClient.*;

public class Cliente {

	// http://www.innovation.ch/java/HTTPClient/

	/**
	 If the connection is kept open across requests then the requests may be pipelined. Pipelining here means that
	 a new request is sent before the response to a previous request is received. Since this can obviously enhance
	 performance by reducing the overall round-trip time for a series of requests to the same server, the HTTPClient 
	 has been written to support pipelining (at the expense of some extra code to keep track of the outstanding requests).

	 The programming model is always the same: for every request you send you get a response back which contains the
	 headers and data of the servers response. Now, to support pipelining, the fields in the response aren't necessarily
	 filled in yet when the HTTPResponse object is returned to the caller (i.e. the actual response headers and data
     haven't been read off the net), but the first call to any method in the response (e.g. a getStatusCode()) will
     wait till the response has actually been read and parsed. Also any previous requests will be forced to read their
     responses if they have not already done so (so e.g. if you send two consecutive requests and receive responses 
     r1 and r2, calling r2.getHeader("Content-type") will first force the complete response r1 to be read before reading
     the response r2). All this should be completely transparent, except for the fact that invoking a method on one
	 response may sometimes take a few seconds to complete, while the same method on a different response will return
	 immediately with the desired info.
	 * @throws IOException 
	 */

	private static String[] dameContenido(String dir) throws IOException{

		FileReader fr1 = new FileReader(dir);
		BufferedReader bf1 = new BufferedReader(fr1);

		int contador=0;

		while (bf1.readLine()!=null) {
			contador++;
		}
		bf1.close();
		fr1.close();

		String[] arrayRecursos = new String[contador];

		FileReader fr = new FileReader(dir);
		BufferedReader bf = new BufferedReader(fr);

		for(int i=0;i<contador;i++){
			String linea = bf.readLine();
			if(!(linea == null || linea.equals(""))){
				arrayRecursos[i]=linea;
			}
			else{
				System.err.println("fichero invalido \n");
				System.exit(1);
			}
		}

		bf.close();
		fr.close();
		return arrayRecursos;

	}

	public static void main(String[] args) {

		try
		{

			String[] contenido = dameContenido(args[0]);
			HTTPResponse[][] arrayR = new HTTPResponse[args.length-1][contenido.length];

			if (args.length < 2){
				System.err.println("argumentos invalidos \n");
				System.exit(1);
			}

			HTTPConnection[] arrayC = new HTTPConnection[args.length-1];
			for (int i = 1; i < args.length; i++) {
				arrayC[i-1] = new HTTPConnection(args[i]);

			}
			//array de respuestas [servidor][recurso]

			int[] arrayAcumuladores = new int[contenido.length];

			//recorre conexiones
			for (int j = 0; j < arrayC.length; j++) {	
				
				//Este primer get se hace sin pipeline, para que HTTPClient compruebe que el servidor responde
				String[] spliteado = contenido[0].split(" ");				
				arrayC[j].Get(spliteado[0]);
				
				//recorre recursos (pipeline se usa a partir de aquí)
				for(int numeroRecurso=0;numeroRecurso<contenido.length;numeroRecurso++){

					spliteado = contenido[numeroRecurso].split(" ");
					//Calculamos de cuanto debe ser cada una de las peticiones
					int cadaPeticion = Integer.parseInt(spliteado[1])/(args.length-1);

					NVPair[] arrayNV = new NVPair[1];

					arrayNV[0] = new NVPair("Range","bytes="+arrayAcumuladores[numeroRecurso]+"-"+(arrayAcumuladores[numeroRecurso]+cadaPeticion));

					arrayAcumuladores[numeroRecurso] = arrayAcumuladores[numeroRecurso]+cadaPeticion;
					arrayR[j][numeroRecurso] = arrayC[j].Get(spliteado[0],"",arrayNV);
					
					
				}
			}


			for (int j = 0; j < arrayC.length; j++) {	
				//recorre recursos
				for(int numeroRecurso=0;numeroRecurso<contenido.length;numeroRecurso++){

					String[] spliteado = contenido[numeroRecurso].split(" ");
					if (arrayR[j][numeroRecurso].getStatusCode() >= 300){
						System.err.println("Received Error recurso "+numeroRecurso+": "+arrayR[j][numeroRecurso].getReasonLine());
					}
					else{
						String filename = spliteado[0];
						FileWriter fw = new FileWriter(filename,true);
						fw.write(arrayR[j][numeroRecurso].getText());
						fw.close();
					}
				}
			}
		}
		catch (IOException ioe)
		{
			System.err.println(ioe.toString());
		}
		catch (ModuleException me)
		{
			System.err.println("Error handling request: " + me.getMessage());
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
}
