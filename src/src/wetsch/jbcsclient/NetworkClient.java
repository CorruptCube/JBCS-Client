package wetsch.jbcsclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

/*
* Last modified 7/1/2016
 * Added Registration commands.
 * Added client message types to indicate how to display the message to the user.
  */

/**
 * This Async Task connects to the JBCS-Server and sends the data. 
 * @author kevin
 *
 */
public class NetworkClient extends AsyncTask<String[], Integer, Object[]>{
	
	private Context context = null;//The activity that invoked this async task.
	private String tag = this.getClass().getName();//holds the activities full path.class name.
	private String connectionFaildError = null;////Holds the connection error message
	private Socket connection = null;//Connection object.
	private BufferedReader in = null;//Connections input stream.
	private PrintWriter out = null;//Connections output stream.
	private String host = null;//Listening server's IP address.
	private int port = 0;//Listening server's port number. 
	private Set<JBCSClientListener> listener;
	private ProgressDialog pd = null;//Dialdog used to show client wait status.
	//Dialog indecators
	private final int pdWaitForServer = 0;//Tell the dialog that it is waiting for the server.
	private final int pdDone = 1;//Tell the dialog it is done and can close.
	/**
	 * Client Error dialog message.
	 * Used if the client encountered an error, such as a connection error.
	 */
	public static final int clientErrorDialogMessage = 2;
	/**
	 * Client Notification dialog message.
	 * Used when the server responded back with a message.
	 */
	public static final int clientInfoDialogMessage = 3;
	/**
	 * Client notification Toast Message.
	 * Used when the server received the barcode data.
	 */
	public static final int clientInfoToastMessage = 4;
	
	/**
	 * Takes the listening server's IP address, port number and a context
	 * @param host Listening server's IP address.
	 * @param port Listening server's port number.
	 * @param context The context of the activity that triggered the task.
	 */
	public NetworkClient(String host, int port, JBCSClientListener listener, Context context) {
		this.context = context;
		this.host = host;
		this.port =  port;
		if(listener != null){
			this.listener = new HashSet<JBCSClientListener>();
			this.listener.add(listener);
		}
		connectionFaildError = context.getString(R.string.connection_error_message);
		pd = new ProgressDialog(context);
		pd.setTitle("Client Status");
		pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
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
	protected Object[] doInBackground(String[]... params) {
		String[] data = params[0];
		Object serverResponce[] = null;
		
		try {
			connection = new Socket(host, port);
			in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			out = new PrintWriter(connection.getOutputStream());
			
			switch (data[0]) {
				case ServerCommands.sendBarcodeData:
					out.println(data[0]);
					out.flush();
					String drsStatus = in.readLine();
					if(drsStatus.equals(ServerCommands.drsEnabled)){
						out.println(data[1]);
						out.flush();
						String registrationStatus = in.readLine();
						if(registrationStatus.equals(ServerCommands.unregistered))
							serverResponce = new Object[]{clientInfoDialogMessage, context.getString(R.string.unregistered_device_message)};
						else if(registrationStatus.equals(ServerCommands.registered)){
							out.println(data[2]);
							out.println(data[3]);
							out.flush();
							if(in.readLine().equals(ServerCommands.dataReceived))
								serverResponce = new Object[]{clientInfoToastMessage, context.getString(R.string.data_received_message)};
						}
					}else if(drsStatus.equals(ServerCommands.drsDisabled)){
						out.println(data[2]);
						out.println(data[3]);
						out.flush();
						if(in.readLine().equals(ServerCommands.dataReceived))
							serverResponce = new Object[]{clientInfoToastMessage, context.getString(R.string.data_received_message)};
					}
					break;
			case ServerCommands.checkConnection:
				out.println(data[0]);
				out.flush();
				if(in.readLine().equals(ServerCommands.connectionOk))
					serverResponce = new Object[]{clientInfoDialogMessage, context.getString(R.string.connection_ok_message)};
				break;
			case ServerCommands.registerDevice:
				out.println(data[0]);
				out.println(data[1]);
				out.flush();
				publishProgress(pdWaitForServer);
				String message = in.readLine();
				if(message.equals(ServerCommands.registered))
					serverResponce = new Object[]{clientInfoDialogMessage, context.getString(R.string.device_already_registered_message)};
				else if(message.equals(ServerCommands.registrationRequestInactive))
					serverResponce = new Object[]{clientInfoDialogMessage, context.getString(R.string.registration_request_inactive_message)};
				else if(message.equals(ServerCommands.deviceRegistered))
					serverResponce = new Object[]{clientInfoDialogMessage, context.getString(R.string.Device_registered_message)};
				else if(message.equals(ServerCommands.deviceRejected))
					serverResponce = new Object[]{clientInfoDialogMessage, context.getString(R.string.device_rejected_message)};
				publishProgress(pdDone);
				break;

			default:
				serverResponce = new Object[]{clientErrorDialogMessage, "Client received an invalid command."};
				break;
			}
			closeConnection();
			Log.i(tag, "Data transmission successful.");
			return serverResponce;
		} catch (IOException e) {
			Log.e(tag, "Client failed to transmit data to the server.");
			e.printStackTrace();
		}
		return new Object[]{clientErrorDialogMessage, connectionFaildError};
	}
	
	
	
	@Override
	protected void onProgressUpdate(Integer... values) {
		if(values[0] == pdWaitForServer){
			pd.setMessage("Waiting for server.");
			pd.show();
		}else if(values[0] == pdDone)
			pd.dismiss();
		super.onProgressUpdate(values);
	}

	//This method runs after the doInBackground method.  It sends messages back to the user.
	@Override
	protected void onPostExecute(Object[] result) {
		if(listener != null){
			for(JBCSClientListener l : listener)
				l.onJbcsClientFinish(result);
		}
		super.onPostExecute(result);
	}
	
	public static class ServerCommands{
		/**
		 * Command to check the server connection.
		 */
		public static final String checkConnection = "CHECK_CONNECTION";
		/**
		 * Command to register a new device.
		 */
		public static final String registerDevice = "REGISTER_DEVICE";
		/**
		 * Server response to indicate the connection is OK.
		 */
		public static final String connectionOk = "CONNECTION_OK";
		/**
		 * Command to send the barcode data.
		 */
		public static final String sendBarcodeData = "SEND_BARCODE_DATA";
		/**
		 * Server response to indicate the barcode data was received.
		 */
		public static final String dataReceived = "BC_DATA_RECEIVED";
		/**
		 * Server response to indicate the device is not registered with the server.
		 */
		public static final String unregistered = "UNREGISTERED";
		/**
		 * Server response to indicate the device is registered with the server.
		 */
		public static final String registered = "REGISTERED";
		/**
		 * Server response to indicate the device registration request was accepted by the server.
		 */
		public static final String deviceRegistered = "DEVICE_REGISTERED";
		/**
		 * Server response to indicate the device registration request was rejected by the server.
		 */
		public static final String deviceRejected = "DEVICE_REJECTED";
		/**
		 * Server response to indicate the Device registration system in enforced.
		 */
		public static final String drsEnabled = "DRS_ENABLED"; 
		/**
		 * Server response to indicate the device registration system is not enforced.
		 */
		public static final String drsDisabled = "DRS_DISABLED";
		/**
		 * Server response to indicate the server is not accepting registration requests.
		 */
		public static final String registrationRequestInactive = "REGISTRATION_REQUEST_INACTIVE";
		
	}
}
