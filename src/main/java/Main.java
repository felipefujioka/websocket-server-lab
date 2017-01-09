import com.corundumstudio.socketio.AckRequest;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;

import static spark.Spark.*;

public class Main {
	
	public static int counter = 0;
	public static SocketIOServer server;
	
    public static void main(String[] args) throws InterruptedException {
    	
    	Configuration config = new Configuration();
        config.setHostname("localhost");
        config.setPort(10443);

        server = new SocketIOServer(config);
        
        server.addEventListener("message", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String data, AckRequest ackRequest) {
//                server.getBroadcastOperations().sendEvent("message", data);
                client.sendEvent("message", "count: " + Main.counter);
            }
        });
        
        server.addConnectListener(new ConnectListener() {
			
			@Override
			public void onConnect(SocketIOClient client) {
				// TODO Auto-generated method stub
				client.sendEvent("connect", "Connected as: " + client.getSessionId());
				
			}
		});
        
        server.addDisconnectListener(new DisconnectListener() {
			
			@Override
			public void onDisconnect(SocketIOClient client) {
				// TODO Auto-generated method stub
				client.sendEvent("disconnect", "Disconnected");
			}
		});
        
        get("/hello", (req, res) -> {
        	Main.counter++;
        	return "{'bla':'bla'}";
        });
        
        server.start();
        
        new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				while(true) {
					try {
						Thread.sleep(2000);
						Main.server.getBroadcastOperations().sendEvent("update", "{type: 'A', id:'1'}");
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
			}
		}).run();

//        Thread.sleep(Integer.MAX_VALUE);

        server.stop();
    }
}