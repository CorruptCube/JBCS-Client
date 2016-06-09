/**
 * 
 */
package wetsch.jbcsclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import wetsch.jbcsclient.R;
import android.app.Activity;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

/*
 *Last modified on 8/25/2015
 * 
 */

/**
 * Activity window for displaying information about the application. 
 * @author kevin
 *@version 1.0 
 */

public class AboutApplicationActivity extends Activity {
	private String tag = getClass().getName();//Used fore log cat tagging.
	private AssetManager assets;//Used to access assets.
	private TextView dialogText;//Display information in the window.
	private StringBuilder dialogMessageText;//Build string to hold information.
	private InputStream messageData;//Used to read the file holding the information.



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about_activity_layout);
		activitySetup();
		populateDialog();
	}
	
	//Setup the activity.
	private void activitySetup(){
		assets = getAssets();
		dialogMessageText =new StringBuilder();
		dialogText = (TextView) findViewById(R.id.about_activity_text);
	}
	
	//Build the string to populate the dialog.
	private void populateDialog(){
		String line = null;
		dialogMessageText.append("Application Version:\t"+getAppVersion()+"\n\n");
		try {
			messageData = assets.open("about_app_dialog_message.txt");
			BufferedReader data = new BufferedReader(new InputStreamReader(messageData));
			while ((line = data.readLine()) != null){
				if(line.equals("\\n"))
					dialogMessageText.append("\n");
				else if(line.equals("\\n\\n"))
					dialogMessageText.append("\n\n");
				else if(line.equals("\\u2022"))
					dialogMessageText.append("\n\u2022 " + data.readLine());
				else
					dialogMessageText.append(line);
			}
			messageData.close();
			data.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		dialogText.setText(dialogMessageText.toString());
	}

	//Getting application version name.
	private String getAppVersion(){
		String versionName = null;
			try {
				versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
			} catch (NameNotFoundException e) {
				e.printStackTrace();
				Log.e(tag, "There was a problem while fetching the package version name.");
			}
			return versionName;
	}
}
