public class MainDNC {

	
	public static void main(String[] args) {
	
		int port = 8080;
		String adresse = "sainthillier.fr";
		
		if(args.length >= 1)
		{
			adresse = args[0];
		}
		if(args.length >= 2)
		{
			port = Integer.parseInt(args[1]);
		}
		FrameDNC c = new FrameDNC(adresse,port);
		c.setVisible(true); 
	}	
}
