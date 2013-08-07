# -*- coding: utf-8 -*-

class InfoClient

	attr_accessor :etat, :pseudo
	attr_reader :socket
	def initialize(socket,pseudo)
		@socket = socket
		@pseudo = pseudo
		@etat = "FREE"
	end

	def puts(msg)
		@socket.puts(msg)
	end

	def gets
		@socket.gets
	end
end