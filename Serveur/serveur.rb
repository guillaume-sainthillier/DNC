# -*- coding: utf-8 -*-
require 'socket'
require './InfoClient.rb'


	# Vérification des paramètres
	raise "Usage: #{$0} <port>\n" if ARGV.length != 1

	adresse,port = "localhost",ARGV[0] 	if ARGV.length == 1
	adresse,port = ARGV[2],ARGV[1] 		if ARGV.length == 2
	port = ARGV[0]
	raise "Le port doit être un nombre" if not port.match(/^([\d]+)$/)
	PORT = port.to_i

	#fin vérifs

def parseCmd(commande)
	return commande.split(/\|/)
end

def notifyAll(requete, evenOnAway = true)
	$socketsClient.each do |c|
		c.puts(requete) if (evenOnAway or (!evenOnAway and c.etat == "FREE"))
	end
end

def getInfoClient(socketFrom, pseudoTo)
	from = nil
	to = nil

	$socketsClient.each do |c|
		from = c if c.socket == socketFrom
		to 	 = c if c.pseudo.downcase == pseudoTo.downcase
		break if from and to
	end

	return from,to
end

def connecter(pseudo,client)
	from,to = getInfoClient(client,pseudo)

	

	if not to and not from and pseudo.match(/^#{$regexUser}$/) then
		$socketsClient << InfoClient.new(client,pseudo)
		puts "#{pseudo} s'est connecté"
		client.puts("+OK|CONNECT|#{pseudo}")
		notifyAll("NJOIN|#{pseudo}")

		$demandesMP[pseudo.downcase]		= Array.new
		$demandesMPTmp[pseudo.downcase]		= Array.new
	else
		client.puts("-ERR|CONNECT|#{pseudo}|pseudo déjà utilisé") 		if to
		client.puts("-ERR|CONNECT|#{pseudo}|vous êtes déjà connecté") 	if from
		client.puts("-ERR|CONNECT|#{pseudo}|format de pseudo invalide") if not pseudo.match(/^#{$regexUser}$/)
	end
end

def nickname(client,pseudo)
	from,to = getInfoClient(client,pseudo)

	if from and not to and pseudo.match(/^#{$regexUser}$/) then

		oldPseudo = from.pseudo

		from.pseudo = pseudo


		#Mise à jour des tableaux en cas de changement de pseudo
		$demandesMPTmp.delete(oldPseudo.downcase) 									if $demandesMPTmp[oldPseudo.downcase]
		$demandesMP[pseudo.downcase]	= Array.new
		$demandesMP[pseudo.downcase] 	= $demandesMP.delete(oldPseudo.downcase) 	if $demandesMP[oldPseudo.downcase]

		client.puts("+OK|NICK|#{pseudo}")
		notifyAll("NNICK|#{oldPseudo}|#{pseudo}")
	else
		client.puts("-ERR|NICK|#{pseudo}|pseudo déjà utilisé") 		 if to
		client.puts("-ERR|NICK|#{pseudo}|format de pseudo invalide") if not pseudo.match(/^#{$regexUser}$/)
	end
end

def quitter(client)
	from,to = getInfoClient(client,"")

	if from then
		puts "#{from.pseudo} s'est déconnecté"
		notifyAll("NLEAVE|#{from.pseudo}") if $socketsClient.delete(from)
	end
end

def listeUser(client)
	
	liste = Array.new
	$socketsClient.each do |c|
		liste << c.pseudo
	end

	client.puts("LIST|"+ liste.join("|"))
end

def listeBusy(client)

	tabBusy = Array.new
	$socketsClient.each do |c|
		tabBusy << c.pseudo if c.etat == "BUSY"
	end

	client.puts("LISTBUSY|" + tabBusy.join("|"))
end

def envoiMessage(client,message,pseudoTo = false)
	msg = message.join(" ").force_encoding("UTF-8")
	

	if not pseudoTo then # Message général
		$socketsClient.each do |c|
			if client == c.socket then
				if c.etat == "FREE" then
					puts "#{c.pseudo}@all: #{msg} "
					c.puts("+OK|MESS|#{msg}")
					notifyAll("NMESS|#{c.pseudo}|#{msg}",false)
				else
					c.puts("-ERR|MESS|#{msg}")
				end
				break
			end
		end
	else # Message privé
		from,to = getInfoClient(client,pseudoTo)
		if from and to and to.etat != "BUSY" and $demandesMP[from.pseudo.downcase].include?(to) then
			puts "#{from.pseudo}@#{to.pseudo}: #{msg} "
			from.puts("+OK|MP|#{to.pseudo}|#{msg}")
			to.puts("NMP|#{from.pseudo}|#{msg}")
		else
			from.puts("-ERR|MP|#{pseudoTo}|Utilisateur déconnecté")			if from and not to
			from.puts("-ERR|MP|#{pseudoTo}|Utilisateur absent") 			if from and to and to.etat == "BUSY"
			from.puts("-ERR|MP|#{pseudoTo}|Conversation non engagée") 		if from and to and not $demandesMP[from.pseudo.downcase].include?(to)
		end
	end
end


def busy(client)

	from,to = getInfoClient(client,"")

	if from then
		if from.etat == "FREE" then
			from.etat = "BUSY"
			puts "#{from.pseudo} est busy"
			from.puts("+OK|BUSY")
			notifyAll("NBUSY|#{from.pseudo}")
		else
			c.puts("-ERR|BUSY")
		end
	end
end

def free(client)
	from,to = getInfoClient(client,"")

	if from then
		if from.etat == "BUSY" then
			from.etat = "FREE"
			puts "#{from.pseudo} est free"
			from.puts("+OK|FREE")
			notifyAll("NFREE|#{from.pseudo}")
		else
			c.puts("-ERR|FREE")
		end
	end
end



def demandeDiscussionPrivee(client,pseudo) # PRIV 
	from, to = getInfoClient(client,pseudo)


	if from and to and from != to then
		puts "#{from.pseudo} veut parler à #{to.pseudo}"

		if $demandesMPTmp[from.pseudo.downcase] and $demandesMPTmp[from.pseudo.downcase].include?(to) then #Si demande déjà faite 
			from.puts("-ERR|PRIV|#{pseudo}|Demande déjà établie")
		else
			to.puts("PRIV|#{from.pseudo}")
			$demandesMPTmp[from.pseudo.downcase] 		<< to	
			$demandesMPTmp[to.pseudo.downcase] 			<< from	
		end
	else
		from.puts("-ERR|PRIV|#{pseudo}|Utilisateur non trouvé") 	if from and from != to
		from.puts("-ERR|PRIV|#{pseudo}|La Schizophrénie se soigne") if from and from != to
	end

end

def receptionDemandePrivee(client,pseudo, isOK) # ACCEPT/REFUSE
	

	from, to = getInfoClient(client,pseudo)
	if from and to and $demandesMPTmp[from.pseudo.downcase].delete(to) and $demandesMPTmp[to.pseudo.downcase].delete(from) then
		
		puts "#{from.pseudo} "+ (isOK ? "ACCEPTE" : "REFUSE") +" de parler à #{to.pseudo}"
		if isOK then # Si ACCEPT
			$demandesMP[from.pseudo.downcase] 		<< to	
			$demandesMP[to.pseudo.downcase] 		<< from	

			to.puts("+OK|PRIV|#{from.pseudo}")
			from.puts("+OK|ACCEPT|#{to.pseudo}")
		else
			to.puts("+NOK|PRIV|#{from.pseudo}")
		end
	else
		from.puts("-ERR|"+(isOK ? "ACCEPT" : "REFUSE")+"|#{pseudo}|Conversation non engagée") if from
	end
end

def quitterConversationPrivee(client, pseudo)
	from, to = getInfoClient(client,pseudo)
	if from and to and $demandesMP[from.pseudo.downcase].include?(to) then
		from.puts("+OK|END|#{to.pseudo}")
		to.puts("NEND|#{from.pseudo}")
		$demandesMP[from.pseudo.downcase].delete(to)
		$demandesMP[to.pseudo.downcase].delete(from)
	else
		from.puts("-ERR|END|#{pseudo}|Utilisateur déconnecté") 	 if from and not to
		from.puts("-ERR|END|#{pseudo}|Conversation non engagée") if from and to and not $demandesMP[from.pseudo.downcase].include?(to)
	end
end

def traiter(message,client)
	puts "PAQUET RECU: '#{message}'"

	case message
		when /^help$/i then # Affiche les commandes dispo
			client.puts("LISTCMD|"+ $listeCommandes.join("|"))
		when /^connect\|(.+)$/i then # Demande de connection
			args = parseCmd(message)
			connecter(args[1],client)
		when /^nick\|(.+)$/i then # Demande de connection
			args = parseCmd(message)
			nickname(client,args[1])
		when /^quit$/i then # Client qui s'en va
			quitter(client)
		when /^list$/i then # Liste des utilisateurs connectés
			listeUser(client)
		when /^mess\|([^\|]+)$/i then # Envoi d'un message à tous les gens non busy
			args = parseCmd(message.gsub(/\"/,""))
			args.slice!(0)
			envoiMessage(client,args)
		when /^mess\|([^\|]+)\|#{$regexUser}$/i then # Envoi d'un message privé
			args = parseCmd(message)
			args.slice!(0)
			to = args.slice!(-1)
			envoiMessage(client,args,to)
		when /^busy/i then # Demande de mise en busy
			busy(client)
		when /^free/i then # Demande de mise en free
			free(client)
		when /^listbusy$/i then # Liste des utilisateurs busy
			listeBusy(client)
		when (/^priv\|#{$regexUser}$/i ) then # Demande de discussion privée
			args = parseCmd(message)
			demandeDiscussionPrivee(client,args[1])
		when /^(accept|refuse)\|#{$regexUser}$/i then # Demande de discussion privée
			args = parseCmd(message)
			receptionDemandePrivee(client,args[1], args[0].match(/^accept$/i))
		when /^end\|#{$regexUser}$/i then # Demande de discussion privée
			args = parseCmd(message)
			quitterConversationPrivee(client,args[1])
		else
			client.puts("-ERR|INVALIDPACKET|#{message}")
	end
end

	begin
		$socketsClient 	= Array.new
		$demandesMP 	= Hash.new
		$demandesMPTmp 	= Hash.new
		$regexUser		= /\w([\w\d_-]+)/i
		# Mise en place d’un gestionnaire pour capturer SIGINT
		trap(:SIGINT) do
			puts "\nBye"
			exit(1)
		end
		
		$listeCommandes = [	"CONNECT pseudo : ",
							"MOIS : Affiche le mois du serveur",
							"HEURE : Affiche l'heure du serveur",
							"PAQUES : Affiche la date du jour de paques",
							"ASCENSION : Affiche la date de l'ascension",
							"QUIT : Quitte le client",
							"EXIT : Quitte le client",
							"HELP : Affiche les commandes disponibles"].sort

		TCPServer.open(PORT) do |server|
			loop do
				STDOUT.puts("En attente de connexion...")
			    Thread.fork(server.accept) do |client| 
			        client.puts("+OK") #On accepte le client (un peu useless sans vérif de bannissement à l'ip)

			        while requete = client.gets.chomp do
			        	traiter(requete,client)
			        end
			    end
			end
		end

		exit(0)
	rescue Exception => e
		STDERR.puts(e)
		exit(2)
	end

	
