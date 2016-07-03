package wetsch.jbcsclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

import android.os.AsyncTask;
import android.util.Log;

/*
 * Last modified: 6/8/2016
 * Changes:
 * Added command support.
 */

/**
 * This Async Task connects to the JBCS-Server and sends the data. 
 * @author kevin
 *
 */
public class NetworkClient extends AsyncTask<String[], Integer, Object>{
	String tag = this.getClass().getName();//holds the activities full path.class name.
	private Socket connection = null;//Connection object.
	private BufferedReader in = null;//Connections input stream.
	private PrintWriter out = null;//Connections output stream.
	private String host = null;//Listening server's IP address.
	private int port = 0;//Listening server's port number. 
	private Set<JBCSClientListener> listener;

	/**
	 * Takes the listening server's IP address, port number and a context
	 * @param host Listening server's IP address.
	 * @param port Listening server's port number.
	 * @param context The context of the activity that triggered the task.
	 */
	public NetworkClient(String host, int port, JBCSClientListener listener) {
		this.host = host;
		this.port =  port;
		if(listener != null){
			this.listener = new HashSet<JBCSClientListener>();
			this.listener.add(listener);
		}
	}
	
	public void addjbcsClientListener(JBCSClientListener listener){
		if(this.listener == null)
			this.listener = new HashSet<JBCSClientListener>();
		if(!this.listener.contains(listener))
			this.listener.add(listener);
	}
	
	public void removeJbcsClientListener(JBCSClientListener listener){
		if(this.listener != null)
			if(this.listener.contains(listener)){
				this.listener.remove(listener);
				if(this.listener.size() == 0)
					this.listener = null;
			}
	}
	
	//Close connection to server.
	private void closeConnection() throws IOException{
		out.close();
		in.close();
		connection.close();
	}
	
	//This method runs on a background thread.  It's job is to send the data to the server.
	@Override
	protected Object doInBackground(String[]... params) {
		String[] data = params[0];
		Object serverResponce = null;
		
		try {
			connection = new Socket(host, port);
			in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			out = new PrintWriter(connection.getOutputStream());
			
			switch (data[0]) {
				case "SEND_BARCODE_DATA":
					out.println(data[0]);
					out.println(data[1]);
					out.println(data[2]);
					out.flush();
					serverResponce = in.readLine();
				break;
			case "CHECK_CONNECTION":
				out.println(data[0]);
				out.flush();
				serverResponce = in.readLine();
				break;

			default:
				serverResponce = "Invalid command";
				break;
			}
			closeConnection();
			Log.i(tag, "Data transmission successful.");
			return serverResponce;
		} catch (IOException e) {
			Log.e(tag, "Client failed to transmit data to the server.");
			e.printStackTrace();
		}
		return null;
	}
	
	//This method runs after the doInBackground method.  It sends messages back to the user.
	@Override
	protected void onPostExecute(Object result) {
		if(listener != null){
			for(JBCSClientListener l : listener)
				l.onJbcsClientFinish(result);
		}
		super.onPostExecute(result);
	}
}
