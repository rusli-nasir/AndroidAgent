package org.ocsinventoryng.android.agent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.ocsinventoryng.android.actions.OCSLog;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

public class OCSLaunchActivity extends Activity {

	private File[] mFiles;
	private String[] mPackageNames;
	private String[] mPackageVersions;
	private String[] mIdOCS;
	private int[] mVersionCode;
	private OCSLog mOcslog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ocs_launch);
		mOcslog = OCSLog.getInstance();
		
		File dirSofts = getExternalCacheDir();
		mFiles = dirSofts.listFiles();
		mPackageNames = new String[mFiles.length];
		mPackageVersions = new String[mFiles.length];
		mIdOCS = new String[mFiles.length];
		mVersionCode = new int[mFiles.length];
		
		StringBuffer sb = new StringBuffer("OCS installations :\n\n");
		for (int i = 0; i < mFiles.length; ++i) {
	    	String filename=mFiles[i].getName();
	    	mIdOCS[i]=filename.substring(0,filename.indexOf(".apk"));
	    	
	        PackageManager pm = getPackageManager();
	        PackageInfo pkgInfo = pm.getPackageArchiveInfo(mFiles[i].getPath(), 
	                               PackageManager.GET_ACTIVITIES);
	    	mPackageNames[i]=pkgInfo.applicationInfo.packageName;
	    	mPackageVersions[i]=pkgInfo.versionName;
	    	mVersionCode[i]=pkgInfo.versionCode;
	    	mOcslog.debug("package : "+mPackageNames[i]+"/"+pkgInfo.versionCode);
	    	mOcslog.debug("package : "+mPackageNames[i]+"/"+mPackageVersions[i]);
	    	sb.append(filename).append(" :\n  ").append(pkgInfo.applicationInfo.packageName)
	    		.append("\n").append("  version:").append(pkgInfo.versionName)
	    		.append("\n\n");
	    	OCSDownloadInfos infos = null;
			try {
				infos = getInfos(mIdOCS[i]);
				if  (infos != null )
				if ( infos.getNotify_text() != null)
					sb.append("\n"+infos.getNotify_text());
			} catch (IOException e) {
				mOcslog.error(filename+" : "+e.getMessage());
			}
			
			sb.append("\n");
			
			File finst=mFiles[i];
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.setDataAndType(Uri.fromFile(finst), "application/vnd.android.package-archive");
			// Special case of agent itself
			if ( mPackageNames[i].equals(this.getPackageName() ) ) {
				mOcslog.debug(this.getPackageName()+" detected");
				// Save id;version code for asynchrone check later
				String idctx=mIdOCS[i]+":"+pkgInfo.versionCode;
				try {
					FileOutputStream fos = getApplicationContext().openFileOutput("update.flag", 0);
					fos.write(idctx.getBytes());
					fos.close();
				} catch (Exception e) {}
				
				startActivity(intent);
				mOcslog.debug("stop activity");
				this.finish();
			} else 	
				startActivityForResult(intent, i+1);
		}
		TextView vMsg = (TextView) findViewById(R.id.textInstall);
		vMsg.setText(sb.toString());
 		mOcslog.debug(sb.toString());
	}
	
	/**
	 * Called at end of an Activity
	 * Set a little context file named with package name and 
	 * contain OCS pakageid and version code
	 * For asynchronous verification by OCSInstallReceiver.java 
	 */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	int no=requestCode-1;
    	mOcslog = OCSLog.getInstance();
  	
    	String packageName=mPackageNames[no];
    	if (packageName == null )
    		return;
    	String ctx=mIdOCS[no]+":"+mVersionCode[no]+":";
		// Save requestid and versioncode in a package context file for verification in OCSInstallReceiver
    	try {
			FileOutputStream fos = getApplicationContext().openFileOutput(packageName+".inst", 0);
			fos.write(ctx.getBytes());
			fos.close();
		} catch (Exception e) {
			mOcslog.error(e.getMessage());
		}
     }
    /**
     * Read informations file of OSC package
     * @param id OCS id of the package 
     * @return OCSDownloadInfos object builded from info file
     * @throws IOException
     */
	private OCSDownloadInfos getInfos(String id) throws IOException {
		StringBuilder sb = new StringBuilder();
		File finfos = new File (getApplicationContext().getFilesDir(), id+".info");
		FileInputStream fis = new FileInputStream(finfos);
		InputStreamReader isr = new InputStreamReader(fis);
		BufferedReader br =new BufferedReader(isr);
		String ligne;
		while ((ligne = br.readLine()) != null ) 
			sb.append(ligne);
		br.close();
		isr.close();
		return new OCSDownloadInfos(sb.toString());
	}

}