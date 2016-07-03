package wetsch.jbcsclient;

import wetsch.jbcsclient.R;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

/*
* Last modified 7/1/2016
 * Added Registration commands.
 * Added client message types to indicate how to display the message to the user.
  */

@SuppressLint("DrawAllocation")
public class NetworkClientPreferences extends Activity implements OnClickListener, JBCSClientListener{
	private SharedPreferences pref;//The object to access the shared preferences that hold the settings for the client.
	
	private TextView TextViewInstallID;
	private EditText edtHostIPAddress;//The server's host name or IP address
	private EditText edtPortNumber;//The server port number. 
	private Button btnSavePref;// Save the settings for the client.
	private Button btnCheckConfig;//Check if configuration is correct to connect to server.
	private Button btnRegisterDevice;//Register device with server.
	
	private ToggleButton tgbUseClient;//Turn on/off client.
	private AlertDialog clientMessageDialog;//Message dialog for the network client.
	private String SID;//The ID to be used to register the device with the server.
	
	//The method called to setup the activity.
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.network_client_pref);
		SID = Installation.id(this);
		setupObjects();
		setupClientMessageDialog();
		SetDefaultPreferences();
		//Setting cursor position on text fields.
		setCursorSelection(edtHostIPAddress);
		setCursorSelection(edtPortNumber);
	}
	
	
	//Inflates the option menu.
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.netclientmenu, menu);
		return true;
	}
	
	//Handles the menu items selected.
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.nc_menuitem_Send_test_Vaue:
			sendTestBarcodeValueAction();
			break;
		}
		return true;
	}

	
	//Set up the message dialog for the client.
	private void setupClientMessageDialog(){
		clientMessageDialog = new AlertDialog.Builder(this).create();
		clientMessageDialog.setTitle("Message from Client");
		clientMessageDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.btn_dismiss),
			    new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int which) {
			            dialog.dismiss();
			        }
			    });	
	}
	
	//apply Background
	private void setBackground(){
		DisplayMetrics metrics = this.getResources().getDisplayMetrics();
		int h = metrics.heightPixels;
        GradientDrawable gd = new GradientDrawable();
        gd.setColors(new int[]{Color.argb(255, 0, 0, 255), Color.argb(255, 0, 0, 0)});
        gd.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        gd.setGradientRadius(h/2);
        gd.setGradientCenter(0.5f,0.5f);
        LinearLayout l = (LinearLayout) findViewById(R.id.network_client_pref);
        l.setBackground(gd);
	}       

		
		

	//Set up the UI objects
	private void setupObjects(){
		pref = getSharedPreferences(Tools.networkClientPreferencesPath, MODE_PRIVATE);;
		edtHostIPAddress = (EditText) findViewById(R.id.edt_net_client_host_IP);
		edtPortNumber = (EditText) findViewById(R.id.edt_net_client_port);
		btnSavePref = (Button) findViewById(R.id.btn_save_pref);
		btnSavePref.setOnClickListener(this);
		btnCheckConfig = (Button) findViewById(R.id.btn_check_config);
		btnCheckConfig.setOnClickListener(this);
		btnRegisterDevice = (Button) findViewById(R.id.btn_register_device);
		btnRegisterDevice.setOnClickListener(this);
		tgbUseClient = (ToggleButton) findViewById(R.id.tgl_net_client_use);
		TextViewInstallID = (TextView) findViewById(R.id.tv_install_id);
		TextViewInstallID.setText("SID:" +SID);
		setBackground();
		
	}
	
	/*
	 * By default, if there is text in a text-field, the cursor is positioned to the left.
	 * this method sets it to the right if there is text in the field. 
	 */
	private void setCursorSelection(EditText object){
		if(object.getText().length() != 0){
			object.setSelection(object.getText().length());
		}
	}

	/*
	 * Checks if the preferences have been set by the user.
	 * If the Preferences have not been set yes, defaults are
	 * used.
	 */
	private void SetDefaultPreferences(){
		if(!pref.contains("host_name")){
			Editor e = pref.edit();
			e.putString("host_name", "");
			e.commit();
		}
		if(!pref.contains("port_number")){
			Editor e = pref.edit();
			e.putString("port_number", "9800");
			e.commit();
		}
		if(!pref.contains("use_client")){
			Editor e = pref.edit();
			e.putBoolean("use_client", false);
			e.commit();

		}
			
		edtHostIPAddress.setText(pref.getString("host_name", ""));
		edtPortNumber.setText(pref.getString("port_number", "9800"));
		tgbUseClient.setChecked(pref.getBoolean("use_client", false));
	}
	
	/*
	 * Saves the client Preferences set by the user.
	 * The save button calls this method.
	 */
	private void savePreferences(){
		Editor e = pref.edit();
		e.putString("host_name", edtHostIPAddress.getText().toString());
		e.putString("port_number", edtPortNumber.getText().toString());
		e.putBoolean("use_client", tgbUseClient.isChecked());
		e.commit();
	}
	
	/*
	 * Send barcode test data to the server.
	 */
	private void sendTestBarcodeValueAction(){
		String host = edtHostIPAddress.getText().toString();
		int port = Integer.parseInt(edtPortNumber.getText().toString());
		String serverCommand = NetworkClient.ServerCommands.sendBarcodeData;
		new NetworkClient(host, port, this, this).execute(new String[]{serverCommand, SID, "CODE_39", "0123456789"});
	}
	
	/*
	 * This method checks the client settings to make sure they are correct.
	 * If they are correct, the server will respond back indicating that the
	 * connection is okey, otherwise an error will be displayed.
	 */
	private void checkConnection(){
		String host = edtHostIPAddress.getText().toString();
		int port = Integer.parseInt(edtPortNumber.getText().toString());
		String serverCommand = NetworkClient.ServerCommands.checkConnection;
		new NetworkClient(host, port, this, this).execute(new String[]{serverCommand});
	}
	
	/*
	 * This method sends the registration request to register the device with the server.
	 * A confirmation dialog will pop-up asking the user to press ready when the user is
	 * ready to send the request to the server.
	 */
	private void RegisterDevice(){
		AlertDialog dialog = new AlertDialog.Builder(this).create();
		dialog.setTitle(getString(R.string.register_device_dialog_title));
		dialog.setMessage(getString(R.string.register_device_dialog_message));
		dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.btn_ready), new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String host = edtHostIPAddress.getText().toString();
				int port = Integer.parseInt(edtPortNumber.getText().toString());
				String serverCommand = NetworkClient.ServerCommands.registerDevice;
				new NetworkClient(host, port, NetworkClientPreferences.this, NetworkClientPreferences.this).execute(new String[]{serverCommand, SID});
			}
		});
		dialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.btn_Cancel), new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		dialog.show();
	}
	
	//Listeners
	
	//Listener for the buttons
	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.btn_save_pref){
			savePreferences();
			finish();
		}else if(v.getId() == R.id.btn_check_config){
			checkConnection();
		}else if(v.getId() == R.id.btn_register_device){
			RegisterDevice();
		}
	}
	
	//Listener for the client dialog messages.
	@Override
	public void onJbcsClientFinish(Object clientMessage) {
		Object[] message =  (Object[]) clientMessage;
		int messageType = (int) message[0];
		String messageString = (String) message[1];
		if(messageType == NetworkClient.clientErrorDialogMessage){
			clientMessageDialog.setTitle("Client Error");
			clientMessageDialog.setMessage(messageString);
			clientMessageDialog.show();
		}else if(messageType == NetworkClient.clientInfoDialogMessage){
			clientMessageDialog.setTitle("Client Message");
			clientMessageDialog.setMessage(messageString);
			clientMessageDialog.show();
		}else if(messageType == NetworkClient.clientInfoToastMessage)
			Toast.makeText(this, messageString, Toast.LENGTH_SHORT).show();
	}
	
}