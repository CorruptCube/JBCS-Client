package wetsch.jbcsclient;

import wetsch.jbcsclient.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ToggleButton;

/*
 * Last modified: 6/8/2016
 * Changes:
 * converted Connection request to a comand passed to the client.
 */

public class NetworkClientPreferences extends Activity implements OnClickListener, JBCSClientListener{
	private String filename;
	
	private SharedPreferences pref;
	
	private EditText edtHostIPAddress;
	private EditText edtPortNumber;
	private Button btnSavePref;
	private Button btnCheckConfig;
	private ToggleButton tgbUseClient;;
	private AlertDialog clientMessageDialog;//Message dialog for the network client.

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.network_client_pref);
		
		setupObjects();
		setupClientMessageDialog();
		LoadPreferences();
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

	
	private void setupObjects(){
		filename = "network_client_preferences";
		pref = getSharedPreferences(filename, MODE_PRIVATE);;
		edtHostIPAddress = (EditText) findViewById(R.id.edt_net_client_host_IP);
		edtPortNumber = (EditText) findViewById(R.id.edt_net_client_port);
		btnSavePref = (Button) findViewById(R.id.btn_save_pref);
		btnSavePref.setOnClickListener(this);
		btnCheckConfig = (Button) findViewById(R.id.btn_check_config);
		btnCheckConfig.setOnClickListener(this);
		tgbUseClient = (ToggleButton) findViewById(R.id.tgl_net_client_use);
		
	}

	
	private void LoadPreferences(){
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
	
	private void savePreferences(){
		Editor e = pref.edit();
		e.putString("host_name", edtHostIPAddress.getText().toString());
		e.putString("port_number", edtPortNumber.getText().toString());
		e.putBoolean("use_client", tgbUseClient.isChecked());
		e.commit();
	}
	
	private void checkConnection(){
		String host = edtHostIPAddress.getText().toString();
		int port = Integer.parseInt(edtPortNumber.getText().toString());
		new NetworkClient(host, port, this).execute(new String[]{"CHECK_CONNECTION"});

	}
	
	//Listeners
	
	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.btn_save_pref){
			savePreferences();
			finish();
		}else if(v.getId() == R.id.btn_check_config){
			checkConnection();
		}
	}
	
	@Override
	public void onJbcsClientFinish(Object clientMessage) {
		if(clientMessage == null){
			clientMessageDialog.setMessage(getString(R.string.connection_error_message));
			clientMessageDialog.show();
		}else{
			clientMessageDialog.setMessage((String)clientMessage);
			clientMessageDialog.show();

		}
	}
}