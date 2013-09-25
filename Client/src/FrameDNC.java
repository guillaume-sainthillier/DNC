import java.awt.*;
import java.awt.event.*;
import java.util.Vector;

import javax.swing.*;

import java.io.*;
import java.net.*;


public class FrameDNC extends JFrame implements WindowListener,ActionListener, KeyListener, MouseListener{

	
	private static final long serialVersionUID = 1L;

	private JTextField input_pseudo,input_msg;
	private JButton btnConnection,btnSendMsg;
	private Vector<String> users, historique, ongletsMP;
	private JList<String> listeUsers;
	private JMenuItem prive,ignore,fichier;
	private JPopupMenu actionUser;
	private JTabbedPane tabbedPane;
	private Vector<Vector<String>> chats;
	private Vector<JList<String>> listeChat;

	
	private JPanel panelConnexion, panelChat;
	private Socket socket;
	private BufferedReader in;
	private Thread t;
	
	private boolean isConnecte, isAway;
	private String adresseServeur, pseudo;
	private int numPort, lastIndexHistorique;
	
	
	public FrameDNC()
	{
		this(6000);
	}
	public FrameDNC(String adresse)
	{
		this(adresse,6000);
	}
	
	public FrameDNC(int port)
	{
		this((String)null,port);
	}
	
	public FrameDNC(String adresse, int port) 
	{		
		super("Dog is Not a Chat");
		
		
		this.adresseServeur = adresse;
		this.numPort 		= port;
		this.setResizable(false);
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		//Look&Feel
		try  // Gestion des exceptions éventuelles pour le Look&Feel
		{
		  UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			
		}catch (Exception e)
		{
			System.err.println("Mauvais look&feel employé");
			try
			{
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.MotifLookAndFeel");
			}catch(Exception e1)
			{
				e1.printStackTrace();
			}
		}  
		
		this.isAway 		= false;
        this.isConnecte 	= false;

        this.addWindowListener(this);    
        
        
        this.initLayoutConnection();        
		this.initMainConnection();
		
	}
	

/**
 * 
 * Méthodes sur l'UI
 * 
 */
	
	private void initLayoutConnection()
	{
		this.panelChat = new JPanel();
		
		
		
		//Position de la fenêtre sur l'ecran
		int largeurEcran = Toolkit.getDefaultToolkit().getScreenSize().width;
	    int hauteurEcran = Toolkit.getDefaultToolkit().getScreenSize().height;
	    
	    //Définition position / taille fenêtre
  		this.setBounds(largeurEcran*3/8,hauteurEcran*3/8,350,100);
  		this.setPreferredSize(new Dimension(350,150));
		  		
		this.input_pseudo	= new JTextField("");
		this.btnConnection	= new JButton("Connexion");
		
		
		this.panelChat.setLayout(new GridLayout(2,2));
		
		
		this.panelChat.add(new JLabel("Votre pseudo"));
		this.panelChat.add(this.input_pseudo);
		this.panelChat.add(new JLabel(""));
		this.panelChat.add(this.btnConnection);
		
		this.add(this.panelChat);
		
		this.btnConnection.addActionListener(this);
		this.input_pseudo.addActionListener(this);
	}
	
	private void initLayoutChat()
	{
		
		this.panelChat.removeAll();
		this.panelConnexion = new JPanel();
		
		this.setTitle(this.pseudo+ " is Not a Chat");
		 
		//Position de la fenêtre sur l'ecran
		int largeurEcran = Toolkit.getDefaultToolkit().getScreenSize().width;
	    int hauteurEcran = Toolkit.getDefaultToolkit().getScreenSize().height;
			    
		 //Définition position / taille fenêtre
		this.setBounds(largeurEcran*3/8,hauteurEcran*3/8,650,500);
		this.setPreferredSize(new Dimension(350,150));
		this.input_msg 	= new JTextField();
		this.btnSendMsg = new JButton("Envoyer");
		
		
		this.users = new Vector<String>();
		this.users.add(this.getHTMLFree(this.pseudo));
		
		this.listeUsers = new JList<String>(this.users);
		this.listeUsers.setPrototypeCellValue("Index 1234567890");
		this.listeUsers.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);		
		this.listeUsers.addMouseListener(this);
		
		JScrollPane js = new JScrollPane ();
		js.setSize(200, 600);
	    js.setVerticalScrollBarPolicy (JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
	    js.setViewportView(this.listeUsers); // On l'incorpore dans la JListe
	    
	    
	    
	    this.chats = new Vector<Vector<String>>();
        this.ongletsMP = new Vector<String>();
	    this.historique = new Vector<String>();
	    this.lastIndexHistorique = 0;
		
	    this.listeChat = new Vector<JList<String>>();
       
	    this.panelConnexion.setLayout(new BorderLayout());
	    
	  
	    this.panelConnexion.add(js, BorderLayout.EAST);
	    
	    this.tabbedPane = new JTabbedPane();
		
	    this.panelConnexion.add(this.tabbedPane, BorderLayout.CENTER);
	    
	  
	  
		
		JPanel bSud = new JPanel();
		bSud.setLayout(new BorderLayout());
		
		bSud.add(this.input_msg,BorderLayout.CENTER);
		bSud.add(this.btnSendMsg,BorderLayout.EAST);
		
		this.panelConnexion.add(bSud, BorderLayout.SOUTH);

		this.btnSendMsg.addActionListener(this);
		this.input_msg.addActionListener(this);
		this.input_msg.addKeyListener(this);
		
		
		this.actionUser = new JPopupMenu("Lalalal");
		this.prive		= new JMenuItem("Discussion privée");
        this.ignore 	= new JMenuItem("Ignorer");
        this.fichier	= new JMenuItem("Envoyer un fichier");
        
        this.ignore.setEnabled(false);
        this.fichier.setEnabled(false);
		
        this.prive.addActionListener(this);
        this.ignore.addActionListener(this);
        this.fichier.addActionListener(this);
        
        this.actionUser.add(this.prive);
        this.actionUser.add(this.ignore);
        this.actionUser.add(this.fichier);     
        
        

        this.add(this.panelConnexion);
        
        this.addOnglet("Chat général");
	}
	
	public void erreur(String msg,int exitValue)
	{
		this.erreur(msg);
		
		try {
			this.closeConnection();
		}finally
		{
			System.exit(exitValue);
		}
	}
	
	public void erreur(String msg)
	{
		JOptionPane.showMessageDialog(this, msg, this.getTitle(), JOptionPane.OK_OPTION);
	}

	
	private String[] parseCmd(String commande)
	{
		return commande.split("\\|");
	}
	
	private void addOnglet(String pseudo)
	{
		this.ongletsMP.add(pseudo);
		int size = this.ongletsMP.size() -1;
		 
		this.chats.add(new Vector<String>());
	    this.listeChat.add(new JList<String>(new Vector<String>()));
	    
	   
		this.listeChat.get(size).setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		
		
		JScrollPane js2 = new JScrollPane ();
	    js2.setVerticalScrollBarPolicy (JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
	    js2.setHorizontalScrollBarPolicy (JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	    js2.setViewportView(this.listeChat.get(size)); // On l'incorpore dans la JListe
	    
	    JPanel mainChat = new JPanel();
	    mainChat.setLayout(new BorderLayout());
	    mainChat.add(js2,BorderLayout.CENTER);
	    
	    this.tabbedPane.addTab(pseudo, null, mainChat);
	}
	
	
	public void setBusy(String pseudo,boolean withNotification)
	{
		String user;
		for(int i = 0; i < this.users.size(); i++)
		{
			user = this.users.get(i);
			if(user.equalsIgnoreCase(this.getHTMLFree(pseudo)))
			{
				this.users.set(i,this.getHTMLBusy(pseudo));
				this.listeUsers.setListData(this.users);
				if(withNotification)
				{
					this.addTexte(pseudo + " est absent",0);
					this.majTexte(0);
				}
				continue;
			}
		}
	}
	
	public void setFree(String pseudo, boolean withNotification)
	{
		String user;
		for(int i = 0; i < this.users.size(); i++)
		{
			user = this.users.get(i);
			if(user.equalsIgnoreCase(this.getHTMLBusy(pseudo)))
			{
				this.users.set(i,this.getHTMLFree(pseudo));
				this.listeUsers.setListData(this.users);
				if(withNotification)
				{
					this.addTexte(pseudo + " n'est plus absent",0);
					this.majTexte(0);
				}
				continue;
			}
		}
	}
	
	public void parseErreur(String nomCmd, String[] args)
	{
		switch(nomCmd)
		{
			case "CONNECT":
				this.erreur("Impossible de se connecter avec le pseudo " + args[0] + ": " +args[1]);
			break;
			case "MP":
				boolean estDansMP = false;
				for(int i = 1; i < this.ongletsMP.size(); i++)
				{
					if(args[0].equalsIgnoreCase(this.ongletsMP.get(i)))
					{
						this.addTexte(args[0] +" : " + args[1],i);
						this.majTexte(i);
						estDansMP = true;
						continue;
					}
				}
				
				if(!estDansMP)
				{
					this.addTexte(args[0] +" : " + args[1],0);
					this.majTexte(0);
				}
			break;
			case "BUSY":
				this.addTexte("Vous ne pouvez pas vous mettre absent",0);
				this.majTexte(0);
			break;
			case "FREE":
				this.addTexte("Vous ne pouvez pas vous remettre actif",0);
				this.majTexte(0);
			break;
			case "PRIV":
				this.erreur("Impossible d'inviter " + args[0] + " : " + args[1]);
			break;
			case "ACCEPT":
				this.erreur("Impossible d'accepter " + args[0] + " : "+args[1]);
			break;
			case "REFUSE":
				this.erreur("Impossible de refuser " + args[0] + " : "+args[1]);
			break;
			case "END":
				this.erreur("Impossible de mettre fin à la conversation avec " +args[0] + " : "+args[1]);
			break;
			case "MESS":
				this.addTexte("Impossible d'envoyer " + args[0]);
				this.majTexte();
			break;
			case "INVALIDPACKET":
				System.err.println("Packet invalide : " + args[0]);
			break;
			default:
			System.out.println("PacketErreur " + nomCmd + "non traité");
			break;
		}
	}
	
	private String encodeDelimiter(String msg)
	{
		return msg.replaceAll("\\|", "\\\\u007c");
	}
	
	private String decodeDelimiter(String msg)
	{
		return msg.replaceAll("\\\\u007c", "\\|");
	}
	
	public void parseOK(String retour, String nomCmd, String[] args)
	{
		switch(nomCmd)
		{
			case "CONNECT":
				this.isConnecte = true;
				this.pseudo 	= args[0];
			
				this.initLayoutChat();	
				this.initChatConnection();				
			break;
			case "MESS":
				this.addTexte(this.pseudo + " : " + this.decodeDelimiter(args[0]),0);
				this.majTexte(0);
			break;
			case "BUSY":
				this.isAway = true;
				this.setBusy(this.pseudo,false);
			break;
			case "FREE":
				this.isAway = false;
				this.setFree(this.pseudo,false);
			break;
			case "END":
				for(int i = 1; i < this.ongletsMP.size(); i++)
				{
					if(args[0].equalsIgnoreCase(this.ongletsMP.get(i)))
					{
						this.ongletsMP.remove(i);
						this.tabbedPane.remove(i);
						continue;
					}
				}
			break;
			case "PRIV":
				if(retour.equalsIgnoreCase("NOK"))
					this.erreur(args[0] + " est indisponible");
				else if(retour.equalsIgnoreCase("OK"))
					this.addOnglet(args[0]);
			break;
			case "ACCEPT":
				this.addOnglet(args[0]);
			break;
			case "MP":
				for(int i = 1; i < this.ongletsMP.size(); i++)
				{
					if(args[0].equalsIgnoreCase(this.ongletsMP.get(i)))
					{
						this.addTexte(this.pseudo +" : " + this.decodeDelimiter(args[1]),i);
						this.majTexte(i);
						continue;
					}
				}
			break;
			case "NICK":
				for(int i = 0; i < this.users.size(); i++)
				{
					if(this.pseudo.equalsIgnoreCase(this.getPseudoFromHTML(this.users.get(i))))
					{
						this.users.set(i, this.isAway ?	this.getHTMLBusy(args[0]) : this.getHTMLFree(args[0]) );
						this.listeUsers.setListData(this.users);
						this.pseudo = args[0];
						this.setTitle(this.pseudo + " is Not a Chat");
						continue;
					}
				}
				
			break;

			default:
				System.err.println("PacketOK " + nomCmd + " non supporté");
			break;
		}
	}
	
	
	public void parseNotif(String nomCmd, String[] args)
	{
		if(args.length > 0 && args[0].equalsIgnoreCase(this.pseudo))
			return;
		switch(nomCmd)
		{
			case "NLEAVE":
				this.users.remove(this.getHTMLBusy(args[0]));
				this.users.remove(this.getHTMLFree(args[0]));
				this.listeUsers.setListData(this.users);
				this.addTexte(args[0] + " s'est déconnecté",0);
				this.majTexte(0);
			break;
			case "NJOIN":
				this.users.add(this.getHTMLFree(args[0]));
				this.listeUsers.setListData(this.users);
				this.addTexte(args[0]+ " s'est connecté",0);
				this.majTexte(0);
			break;
			case "NMESS":
				this.addTexte(args[0]+ " : " + this.decodeDelimiter(args[1]),0);
				this.majTexte(0);
			break;
			case "NNICK":
				String oldPseudo = args[0], newPseudo = args[1];
				for(int i = 1; i < this.ongletsMP.size(); i++)
				{
					if(oldPseudo.equalsIgnoreCase(this.ongletsMP.get(i)))
					{
						this.ongletsMP.set(i, newPseudo);
						this.tabbedPane.setTitleAt(i, newPseudo);
						continue;
					}
				}
				
				for(int i = 0; i < this.users.size(); i++)
				{
					if(oldPseudo.equalsIgnoreCase(this.getPseudoFromHTML(this.users.get(i))))
					{
						this.users.set(i, oldPseudo.equalsIgnoreCase(this.getHTMLBusy(oldPseudo)) ?
											this.getHTMLBusy(newPseudo) : 
											this.getHTMLFree(newPseudo) );
						this.listeUsers.setListData(this.users);
						this.addTexte(oldPseudo + " s'est renommé en " + newPseudo,0);
						this.majTexte(0);
						continue;
					}
				}
			break;
			case "NMP":
				for(int i = 0; i < this.ongletsMP.size(); i++)
				{
					if(args[0].equalsIgnoreCase(this.ongletsMP.get(i)))
					{
						this.addTexte(args[0] + " : " + args[1],i);
						this.majTexte(i);
						continue;
					}
				}
			break;
			case "LISTCMD":
				this.addTexte("Liste des commandes : ");
				for(int i = 0; i < args.length; i++)
				{
					this.addTexte(args[i]);
				}
				this.majTexte();
			break;
			case "LIST":
				this.users = new Vector<String>();
				for(int i = 0; i < args.length; i++)
				{
					this.users.add(this.getHTMLFree(args[i]));					
				}
				this.listeUsers.setListData(this.users);
			break;
			case "LISTBUSY":
				for(int i = 0; i < args.length; i++)
				{
					this.setBusy(args[i],false);
				}
			break;
			case "NBUSY":
				this.setBusy(args[0],true);				
			break;
			case "NFREE":
				this.setFree(args[0],true);			
			break;
			case "PRIV":
				int retour = JOptionPane.showConfirmDialog(this, args[0] + " veut discuter avec vous, l'accepter ?",this.getTitle(),JOptionPane.YES_NO_OPTION);
				this.send((retour == JOptionPane.YES_OPTION ? "ACCEPT" : "REFUSE") + "|"+args[0]);	
			break;
			case "NEND":
				for(int i = 1; i < this.ongletsMP.size(); i++)
				{
					if(args[0].equalsIgnoreCase(this.ongletsMP.get(i)))
					{
						this.addTexte(args[0] + " a terminé la conversation privée",0);
						this.majTexte(0);
						this.ongletsMP.remove(i);
						this.tabbedPane.remove(i);
						continue;
					}
				}
			break;
			default:
				System.err.println("PacketNotif " + nomCmd + " non supporté");
			break;
		}
	}
	
	

	public void parsePacket(String packet) 
	{
		String args[] = this.parseCmd(packet);
		String params[];
		
		if(this.isErreur(packet))
		{
			if(args.length > 1)
			{
				params = new String[args.length - 2];
				for(int i = 2; i < args.length; i++)
					params[i-2] = args[i];
				this.parseErreur(args[1], params);
			}else
			{
				System.out.println(packet + " non traité");
			}
		}else if(packet.matches("^\\+(.*)$"))
		{
			if(args.length > 1)
			{
				params = new String[args.length - 2];
				for(int i = 2; i < args.length; i++)
					params[i-2] = args[i];
				this.parseOK(args[0].substring(1,args[0].length()),args[1], params);
			}else
			{
				System.err.println(packet + " non supporté");
			}
		}else
		{
			params = new String[args.length - 1];
			for(int i = 1; i < args.length; i++)
				params[i-1] = args[i];
			this.parseNotif(args[0], params);
		}
	}

	
	
	
	private void closeConnection()
	{
		try 
		{
			if(this.socket != null)
			{
				if(this.isConnecte)
				{					
					for(int i = 1; i < this.ongletsMP.size(); i++)
						this.send("END|" +this.ongletsMP.get(i));
					
					this.send("QUIT");
				}
				
				this.socket.close();
			}	
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
	}
	
	
	
	private void send(String msg)
	{
		System.out.println("Envoi de " + msg);
		try {
			if(this.socket != null && this.socket.getOutputStream() != null)
			{
				 new PrintWriter(new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream())),true).println(msg);
			}
		} catch (IOException e) {
			e.printStackTrace();
			erreur("La connexion avec le serveur a été interrompue",2);
		}
	}
	
	private String getHTMLFree(String nom)
	{
		return "<html><img src=file:./img/vert.png></img>"+nom+"</html>";
	}
	
	private String getHTMLBusy(String nom)
	{
		return "<html><img src=file:./img/away.png></img>"+nom+"</html>";
	}
	
	private void initMainConnection()
	{
		 try
	        {
	           this.socket		= new Socket(this.adresseServeur,this.numPort);
			   this.in 			= new BufferedReader(new InputStreamReader(this.socket.getInputStream()));	
			   this.t = new Thread(new ThreadNotification(this.in,this));
		       this.t.start();
	        }
	        catch (IOException e)
	        {
	        	erreur("La connexion au serveur "+this.adresseServeur+" sur le port "+this.numPort+" a échoué\n"+e.getMessage(),3);
	        }	
	}
	
	private void initChatConnection()
	{
		this.send("LIST");
		this.send("LISTBUSY");        
	}
	
	private boolean isErreur(String requete)
	{
		return requete.matches("^\\-ERR(.)*");
	}
	
	
	
	
	private String getPseudoFromHTML(String html)
	{
		return html.replaceAll("\\<.*?\\>", "");
	}
	
	private void addTexte(String texte)
	{
		int index = this.tabbedPane.getSelectedIndex();
		if(index == -1)
			index = 0;
		if(index >= 0 && index < this.chats.size())
		{
			this.addTexte(texte,index);
		}
	}
	
	private void addTexte(String texte, int index)
	{
		this.chats.get(index).add(texte);
	}
	
	private void majTexte()
	{
		int index = this.tabbedPane.getSelectedIndex();
		if(index == -1)
			index = 0;
		if(index >= 0 && index < this.chats.size())
		{
			this.majTexte(index);
		}
	}
	
	private void majTexte(int index)
	{
		this.listeChat.get(index).setListData(this.chats.get(index));
	}
	
	private void showPopup(MouseEvent e) {
		if (e.isPopupTrigger()) 
		{
			int index = this.listeUsers.locationToIndex(e.getPoint());
			this.listeUsers.setSelectedIndex(index);
			
			if(index >= 0 && index < this.users.size())
			{
				String htmlUser = this.users.get(index);
				String pseudo	= this.getPseudoFromHTML(htmlUser);
				
				if(htmlUser.equalsIgnoreCase(this.getHTMLBusy(this.pseudo))
				|| htmlUser.equalsIgnoreCase(this.getHTMLFree(this.pseudo))	
				|| htmlUser.equalsIgnoreCase(this.getHTMLBusy(pseudo))
				|| this.ongletsMP.contains(pseudo))
				{
					this.prive.setEnabled(false);
				}else
				{
					this.prive.setEnabled(true);
				}
			}
			this.actionUser.show(e.getComponent(),
			e.getX(), e.getY());
		}		 
    }
	
/**
 * 
 * 	Listeners implémentés
 * 
 */	 
	@Override
	public void windowClosing(WindowEvent e) {
		this.closeConnection();
		System.exit(0);
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
		if(e.getSource() == this.listeUsers)
			this.showPopup(e);
	}
	
	@Override
	public void mouseReleased(MouseEvent e) {
		if(e.getSource() == this.listeUsers)
			this.showPopup(e);
	}
	
	@Override
	public void keyPressed(KeyEvent ke) {
		if(ke.getSource() == this.input_msg)
		{
			if(ke.getKeyCode() == 38 || ke.getKeyCode() == 40)
			{
				if(ke.getKeyCode() == 38) // Flèche haut
				{
					if(this.lastIndexHistorique < this.historique.size() -1)
					{
						this.input_msg.setText(this.historique.get(this.lastIndexHistorique++));
					}
				}else//Flèche bas
				{
					if(this.lastIndexHistorique > 0)
					{
						this.input_msg.setText(this.historique.get(this.lastIndexHistorique--));
	
					}
				}
			}
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent ae) {
		if(ae.getSource() == this.btnConnection || ae.getSource() == this.input_pseudo) //Connexion au chat
		{
			String pseudo = this.input_pseudo.getText().trim();
			pseudo = pseudo.replaceAll("\\|", "_");
			if(pseudo.trim().length() == 0)
			{
				erreur("Le pseudo ne peut être vide");
			}else
			{
				this.send("CONNECT|"+pseudo);
			}
		}else if(ae.getSource() == this.btnSendMsg || ae.getSource() == this.input_msg) //Envoi de texte dans la zone de chat
		{
			String msg = this.encodeDelimiter(this.input_msg.getText().trim());
			
			if(msg.length() > 0)
			{
				this.historique.add(0,msg);
				if(this.isAway && !msg.toLowerCase().matches("^/away$"))
				{					
					this.addTexte("Vous êtes absent.");
					this.majTexte();
				}else
				{
					if(msg.toLowerCase().matches("^/help$")) // /HELP
					{
						this.send("HELP");
					}else if(msg.toLowerCase().matches("^/away$"))// /AWAY
					{
						if(this.isAway) // On le remet dans les gens
						{
							this.send("FREE");
						}else // Le client s'absente
						{
							this.send("BUSY");
						}						
					}else if(msg.toLowerCase().matches("^/nick (.+)$"))// /AWAY
					{
						this.send("NICK|" + msg.substring("/nick ".length()));
					}else if((this.tabbedPane.getSelectedIndex() > 0 && msg.toLowerCase().matches("^/end$"))
							|| msg.toLowerCase().matches("^/end (.+)$"))// END[ user]
					{
						if(msg.toLowerCase().matches("^/end (.+)$")) // END user*
							this.send("END|"+msg.substring("/end ".length()));
						else
							this.send("END|"+this.ongletsMP.get(this.tabbedPane.getSelectedIndex()));	
					}else
					{
						int index = this.tabbedPane.getSelectedIndex();
						if(index > 0) //MP
						{
							this.send("MESS|" + msg + "|"+ this.ongletsMP.get(index));
						}else //Chat général
							this.send("MESS|" + msg);				
					}				
				}
				this.input_msg.setText("");
			}
		}else if(ae.getSource() == this.prive) //Demande de discussion privée
		{			
			int index = this.listeUsers.getSelectedIndex();
			if(index >= 0 && index < this.users.size())
			{
				this.send("PRIV|"+ this.getPseudoFromHTML(this.users.get(index)));
			}
		}
	}
	
	
	
/**
 * 
 * 	Listeners non implémentés
 * 
 */
	
	@Override
	public void windowActivated(WindowEvent e) {}

	@Override
	public void windowClosed(WindowEvent e) {}

	@Override
	public void windowDeactivated(WindowEvent e) {}
	
	@Override
	public void windowDeiconified(WindowEvent e) {}

	@Override
	public void windowIconified(WindowEvent e) {}

	@Override
	public void windowOpened(WindowEvent e) {}

	@Override
	public void keyReleased(KeyEvent arg0) {}

	@Override
	public void keyTyped(KeyEvent arg0) {}
	@Override
	public void mouseClicked(MouseEvent e) {}
	@Override
	public void mouseEntered(MouseEvent e) {}
	@Override
	public void mouseExited(MouseEvent e) {}
	

}
