import com.corundumstudio.socketio.AckRequest;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;

import static spark.Spark.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import com.google.gson.Gson;

public class Main {
	
	public static int counter = 0;
	public static SocketIOServer server;
	public static ArrayList<Order> orders;
	
	public static class Order {
		public String id;
		public String name;
		public String value;
		
		public boolean equals(Object obj) {
			if (obj == null) return false;
		    if (obj == this) return true;
		    return ((Order)obj).id.equals(this.id);
		}
	}
	
	public static class Message {
		public String action;
		public String type;
		public String id;
		public String fields;
	}
	
	public static String getMD5(Object obj) {
		String json = new Gson().toJson(obj);
		try {
			return MessageDigest.getInstance("MD5").digest(json.getBytes()).toString();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} 
	}
	
    public static void main(String[] args) throws InterruptedException {
    	
    	orders = new ArrayList<Order>();
    	Configuration config = new Configuration();
        config.setHostname("localhost");
        config.setPort(10443);

        server = new SocketIOServer(config);
        
        server.addEventListener("subscribe", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String data, AckRequest ackRequest) {
            	if(data.equals("orders")) {
            		Main.orders.forEach(order -> {
            			Message msg = new Message();
            			msg.action = "added";
            			msg.type = "order";
            			msg.id = "" + order.id;
            			msg.fields = new Gson().toJson(order);
            			client.sendEvent("update", msg);
            		});
            	}
            }
        });
        
        server.addEventListener("message", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String data, AckRequest ackRequest) {

            }
        });
        
        server.addConnectListener(new ConnectListener() {
			
			@Override
			public void onConnect(SocketIOClient client) {
				// TODO Auto-generated method stub
//				client.sendEvent("connect", "Connected as: " + client.getSessionId());
				
			}
		});
        
        server.addDisconnectListener(new DisconnectListener() {
			
			@Override
			public void onDisconnect(SocketIOClient client) {
				// TODO Auto-generated method stub
				client.sendEvent("disconnect", "Disconnected");
			}
		});
        
        options("/*", (request, response) -> {

            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }

            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }

            return "OK";
        });

        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Request-Method", "*");
            response.header("Access-Control-Allow-Headers", "*");
            // Note: this may or may not be necessary in your particular application
            response.type("application/json");
        });
        
        post("/orders", (req, res) -> {
        	
        	Gson gson = new Gson();
        	Order body = gson.fromJson(req.body(), Order.class);
        	if(!orders.contains(body)){
	        	orders.add(body);
	        	Message msg = new Message();
	        	msg.action = "added";
	        	msg.type = "order";
	        	msg.id = body.id;
	        	msg.fields = gson.toJson(body);
	        	Main.server.getBroadcastOperations().sendEvent("update", msg);
        	}
        	return "posted";
        });
        
        put("/orders/:id", (req, res) -> {
        	Gson gson = new Gson();
        	Order body = gson.fromJson(req.body(), Order.class);
        	orders.remove(body);
        	orders.add(body);
        	Message msg = new Message();
        	msg.action = "changed";
        	msg.type = "order";
        	msg.id = body.id;
        	msg.fields = gson.toJson(body);
        	Main.server.getBroadcastOperations().sendEvent("update", msg);
        	return "updated";
        });
        
        delete("/orders/:id", (req, res) -> {
        	Order obj = new Order();
        	obj.id = req.params(":id");
        	orders.remove(obj);
        	
        	Message msg = new Message();
        	msg.action = "removed";
        	msg.type = "order";
        	msg.id = req.params(":id");
        	Main.server.getBroadcastOperations().sendEvent("update", msg);
        	return "deleted";
        });
        
        server.start();
        
        new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				while(true) {
					try {
						Thread.sleep(2000);
//						Message msg = new Message();
//			        	msg.action = "added";
//			        	msg.type = "order";
//			        	msg.id = "" + Math.random();
//						Main.server.getBroadcastOperations().sendEvent("update", new Gson().toJson(msg));
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