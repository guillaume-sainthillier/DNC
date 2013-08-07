import java.io.*;


public class ThreadNotification implements Runnable {

	private BufferedReader in;
	private FrameDNC f;
	public ThreadNotification(BufferedReader in,FrameDNC f) 
	{
		this.in = in;
		this.f = f;
	}
 
     public void run() 
     {
    	 String reponse;
    	 try 
    	 {
	    	 while(true)
	    	 {				
				reponse = this.in.readLine();
				System.out.println("Réponse du thread : " + reponse);
				if(reponse != null)
				{
					this.f.parsePacket(reponse);
				}else
					throw new Exception("La connexion avec le serveur a été interrompue de façon inopinée");
	    	 }
    	 }catch (Exception e)
    	 {
    		 this.f.erreur(e.getMessage(),2);
    	 }
     }
     
 }

