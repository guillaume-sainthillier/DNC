Dog is Not a Cat
===

DNC est un logiciel de Messagerie Instantanée façon IRC.


Le client permet de:
  - Se connecter à un chat de dicussion avec un pseudo choisi
  - Discuter dans le canal global
  - Gérer son statut (Disponible / Occupé)
  - Ouvrir une conversation privée avec un membre connecté et disponible
  
Concrètement, vous disposez des commandes suivantes:
  - /help : Affiche la liste des commandes disponibles
  - /away : Active/Désactive la disponibilité
  - /nick [pseudo] : Changer de pseudo
  - /end  : Termine la discussion privée en cours
  - /exit : Quitte le client

Côté serveur, celui-ci est écrit en Ruby et implémente le protocole définit dans le fichier PROTOCOL.txt.

N'importe quel client (écrit dans n'importe quel langage) peut directement intérargir avec le serveur, dès lors qu'il implémente lui aussi le protocole.


Retrouvez la procédure d'installation de l'application sur http://guillaume-sainthillier.github.io/dnc
