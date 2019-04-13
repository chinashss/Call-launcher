
package io.rong.recognizer;
import android.os.Build;
import android.support.annotation.RequiresApi;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

//Client to call Cognitive Services Azure Authentication Token service in order to get an access token.
public class AzureAuthToken {

	//Name of header used to pass the subscription key to the token service
	public static final String OcpApimSubscriptionKeyHeader = "Ocp-Apim-Subscription-Key";
    
    //when to refresh the token
	public static final int TokenCacheDurationMins = 8;
    
    /// URL of the token service
    private String  _serviceUrl= "https://api.cognitive.microsoft.com/sts/v1.0/issueToken";

    /// Gets the subscription key.
    private String _subscriptionKey;

    //Cache the value of the last valid token obtained from the token service.
    private String _storedTokenValue = "";
  
    // When the last valid token was obtained.
    //private Instant  _storedTokenTime = Instant.MIN;
    private long _storedTokenTime=0;
    
    public AzureAuthToken(String subscriptionKey)
    {
    	this._subscriptionKey = subscriptionKey;
    }
    
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
	public String getAccessToken()
	{
    	 if ( System.currentTimeMillis() - this._storedTokenTime < TokenCacheDurationMins*60*1000)
         {
             return this._storedTokenValue;
         }
    	try
		{
    		String charset = "utf-8";
			URL url = new URL(this._serviceUrl);

			HttpsURLConnection  connection = (HttpsURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
	        connection.setRequestProperty(OcpApimSubscriptionKeyHeader, this._subscriptionKey);
	        connection.setDoOutput(true);
	        OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
	        out.close();
	        
	        int responseCode = connection.getResponseCode();
	        if ( responseCode == HttpURLConnection.HTTP_OK) 
	        {
	            // OK
	        	try(BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), charset)))
		        {
		            StringBuffer res = new StringBuffer(); 
		            String line;
		            while ((line = reader.readLine()) != null) 
		            {
		                res.append(line);
		            }
		          
		           this._storedTokenValue = "Bearer " + res.toString();
		           this._storedTokenTime = System.currentTimeMillis();
		           return this._storedTokenValue;
	        	}
	        }     
	 	} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		return null;
	}
	
}
