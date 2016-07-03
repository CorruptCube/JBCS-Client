package wetsch.jbcsclient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.UUID;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class Installation {
    private static String sID = null;//The installation ID.
    private static final String INSTALLATION = "INSTALLATION";//Name of file to store ID.
	private static SharedPreferences pref = null;//The shared preferences object.

    /**
	 * This method checks to see if the app version matches the current app version .
	 * The about app dialog will be displayed if the version does not match.
	 * @param context The activity from which this method is called.
	 */

    public static void checkVersion(Context context){
		pref = context.getSharedPreferences(Tools.mainPreferencesFilePath, Context.MODE_PRIVATE);
		//Checking if the key that holds the app version exsists in shared preferences.
		if(!pref.contains("APP_VERSION")){
			Editor e = pref.edit();
			e.putString("APP_VERSION", context.getString(R.string.app_version));
			e.commit();
			context.startActivity(new Intent(context, AboutApplicationActivity.class));
			
		/*If the key that holds the app version exists, checking if the app version stored in the key 
		* matches.
		*/
		}else if(!pref.getString("APP_VERSION", "").equals(context.getString(R.string.app_version))){
			Editor e = pref.edit();
			e.putString("APP_VERSION", context.getString(R.string.app_version));
			e.commit();
			context.startActivity(new Intent(context, AboutApplicationActivity.class));
		}
	}
	
    /**
     * If the file does not exist, a new file is created in internal storage containing the new UUID.
     * If the file does exist, the UUID is extracted from the file and returned.
     * @return String representation of the UUID.
     */
    public synchronized static String id(Context context) {
        if (sID == null) {  
            File installation = new File(context.getFilesDir(), INSTALLATION);
            try {
                if (!installation.exists())
                    writeInstallationFile(installation);
                sID = readInstallationFile(installation);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return sID;
    }
    
    /*
     * Reads the file to collect the UUID stored inside..
     */
    private static String readInstallationFile(File installation) throws IOException {
        RandomAccessFile f = new RandomAccessFile(installation, "r");
        byte[] bytes = new byte[(int) f.length()];
        f.readFully(bytes);
        f.close();
        return new String(bytes);
    }
    /*
     * Writes the random UUID to the file.
     */
    private static void writeInstallationFile(File installation) throws IOException {
        FileOutputStream out = new FileOutputStream(installation);
        String id = UUID.randomUUID().toString();
        out.write(id.getBytes());
        out.close();
    }
}