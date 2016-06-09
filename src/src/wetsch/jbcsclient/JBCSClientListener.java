package wetsch.jbcsclient;

public interface JBCSClientListener {
	/**
	 *This method is called by the JBCS network client.  
	 *When the network client finishes the background task, 
	 *it calls this method passing the message result.
	 * @param clientMessage
	 */
	void onJbcsClientFinish(Object clientMessage);
}
