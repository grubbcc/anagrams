package server;


public class ServerMain {

		
	public static void main(String[] args) {
		if(args.length == 0) {
			Server server = new Server(8118);
			server.start();
		}
		else if(args[0].equals("test")) {
			Server server = new Server(8117);
			server.start();
		}
	}
}