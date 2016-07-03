package wetsch.jbcsclient;


import wetsch.jbcsclient.R;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/*
 * Last modified 7/1/2016
 * Added client message types to indicate how to display the message to the user.
 */

public class HomeActivity extends Activity implements OnClickListener, JBCSClientListener{

	private AlertDialog clientMessageDialog;//Message dialog for the network client.
	private Button btnScan;//starts barcode scanner
	private Button btnResend;//Resends barcode data to server.
	private TextView formatTxt, contentTxt;//Holds the barcode data.
	private TextView netClientStatusInfo;//holds client status.
	private String clientHostAddress;//Connecting host address.
	private int clientHostPort;//Connecting port number.
	private boolean useClient;//Is client enabled?
	private boolean activityStarted;//has the activity started? 
	private BarCodeData bcData = null;//Holds the barcode data collected

	
	//Creates the activity. 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		activitySetup();
		setupClientMessageDialog();
		Installation.checkVersion(this);


		formatTxt.setText("CODE_13");
		contentTxt.setText("0123456789");
		bcData = new BarCodeData("CODE_13", "0123456789");

		activityStarted = true;
	}

	//Inflates the option menu.
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.home, menu);
		return true;
	}
	
	//Handles the menu items selected.
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent = null;
		switch (item.getItemId()) {
		case R.id.action_net_client_settings:
			intent = new Intent(HomeActivity.this, NetworkClientPreferences.class);
			startActivity(intent);
			break;
		case R.id.action_about:
			intent = new Intent(HomeActivity.this, AboutApplicationActivity.class);
			startActivity(intent);
			
			break;
		}
		return true;
	}

	
	//Setup activity.
	private void activitySetup(){
		netClientStatusInfo = (TextView) findViewById(R.id.net_client_status);
		btnScan = (Button)findViewById(R.id.btn_scan);
		btnScan.setOnClickListener(this);
		btnResend = (Button) findViewById(R.id.btn_resend);
		btnResend.setOnClickListener(this);
		formatTxt = (TextView)findViewById(R.id.scan_format);
		contentTxt = (TextView)findViewById(R.id.scan_content);
		loadPreferences();
		SetBackground();

	}
	
	//apply Background
	private void SetBackground(){


		DisplayMetrics metrics = this.getResources().getDisplayMetrics();
		int h = metrics.heightPixels;
        GradientDrawable gd = new GradientDrawable();
        gd.setColors(new int[]{Color.argb(255, 0, 0, 255), Color.argb(255, 0, 0, 0)});
        gd.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        gd.setGradientRadius(h/2);
        gd.setGradientCenter(0.5f,0.5f);
        LinearLayout l = (LinearLayout) findViewById(R.id.activity_home);
        l.setBackground(gd);
	}       

	
	//Loads the shared preferences.
	private void loadPreferences(){
		String ncpFileName = "network_client_preferences";
		SharedPreferences pref = getSharedPreferences(ncpFileName, MODE_PRIVATE);
		 clientHostAddress = pref.getString("host_name", "").toString();
		clientHostPort = Integer.parseInt(pref.getString("port_number", "9800"));
		useClient = pref.getBoolean("use_client", false);
		if(useClient){
			netClientStatusInfo.setText("Connect to: " + clientHostAddress +":" + clientHostPort);
			if(bcData != null)
				btnResend.setVisibility(View.VISIBLE);
		}else{
			netClientStatusInfo.setText("Network client is off.");
			btnResend.setVisibility(View.INVISIBLE);
		}
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

	
	//Starts the barcode scanner.
	private void startScan(){
		IntentIntegrator scanIntegrator = new IntentIntegrator(this);
		scanIntegrator.initiateScan();
	}

	
	//Sends the barcode data to the server.
	private void sendToServer(BarCodeData data){
		if(!useClient)
			return;
		if(clientHostAddress.length() > 0 && clientHostPort != 0){
			String serverCommand = NetworkClient.ServerCommands.sendBarcodeData;
			String deviceId = Installation.id(this);
			String[] message = new String[]{serverCommand, deviceId , data.getBarcodeType(), data.getBarcodeValue()};
			NetworkClient client = new NetworkClient(clientHostAddress, clientHostPort, this, this);
			client.execute(message);
		}
	}
	//Handle what to do when activity resumes.
	@Override
	protected void onResume() {
		super.onResume();
		if(activityStarted){
			loadPreferences();
		}
	}

	//Listeners

	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.btn_scan){
			startScan();
		}if(v.getId() == R.id.btn_resend){
			sendToServer(bcData);
		}
	}
	
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
			clientMessageDialog.setMessage((String)messageString);
			clientMessageDialog.show();
		}else if(messageType == NetworkClient.clientInfoToastMessage)
			Toast.makeText(this,(String)messageString, Toast.LENGTH_SHORT).show();;
	}

	//Handle the result collected by scanner.
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		try{
			IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
			if (scanningResult != null) {
				if(scanningResult.getFormatName().equals(null) || scanningResult.getContents().equals(null))
					return;
				bcData = new BarCodeData(scanningResult.getFormatName(), scanningResult.getContents());
				formatTxt.setText("FORMAT: " + bcData.getBarcodeType());
				contentTxt.setText("CONTENT: " + bcData.getBarcodeValue());
				if(!useClient)
					return;
				sendToServer(bcData);
				btnResend.setVisibility(View.VISIBLE);
				}
			}catch(Exception e){
			e.printStackTrace();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
}