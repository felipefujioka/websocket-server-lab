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

import com.pubnub.api.*;
import org.json.*;

public class Main {
	
	public static int counter = 0;
	public static SocketIOServer server;
	public static ArrayList<Order> orders;
	public static int currentSequenceNumber = 0;
	
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
		public int seq = 0;
		public int size;
		
		public Message() {
			this.seq = ++currentSequenceNumber;
			this.size = Main.orders.size();
		}
		
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

        Pubnub pubnub = new Pubnub("pub-c-ca89589b-68e4-48ee-85dd-2cc9cf01280a", "sub-c-6ded6650-d904-11e6-a478-02ee2ddab7fe");

        
        try {
        	  pubnub.subscribe("my_channel", new Callback() {
        	      @Override
        	      public void connectCallback(String channel, Object message) {
        	          pubnub.publish("my_channel", "Hello from the PubNub Java SDK", new Callback() {});
        	      }
        	 
        	      @Override
        	      public void disconnectCallback(String channel, Object message) {
        	          System.out.println("SUBSCRIBE : DISCONNECT on channel:" + channel
        	                     + " : " + message.getClass() + " : "
        	                     + message.toString());
        	      }
        	 
        	      public void reconnectCallback(String channel, Object message) {
        	          System.out.println("SUBSCRIBE : RECONNECT on channel:" + channel
        	                     + " : " + message.getClass() + " : "
        	                     + message.toString());
        	      }
        	 
        	      @Override
        	      public void successCallback(String channel, Object message) {
        	          System.out.println("SUBSCRIBE : " + channel + " : "
        	                     + message.getClass() + " : " + message.toString());
        	      }
        	 
        	      @Override
        	      public void errorCallback(String channel, PubnubError error) {
        	          System.out.println("SUBSCRIBE : ERROR on channel " + channel
        	                     + " : " + error.toString());
        	      }
        	    }
        	  );
        	} catch (PubnubException e) {
        	  System.out.println(e.toString());
        	}
        
        server = new SocketIOServer(config);
        
        server.addEventListener("subscribe", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String data, AckRequest ackRequest) {
            	if(data.equals("orders")) {
            		
            		// makes a snapshot of the state to send to client
            		ArrayList<Order> copy = new ArrayList<Order>(Main.orders.size());
            		synchronized(this) {
	            		Main.orders.forEach(item -> {
	            			copy.add(item);
	            		});
            		}
            		
            		copy.forEach(order -> {
            			Message msg = new Message();
            			msg.action = "added";
            			msg.type = "order";
            			msg.id = "" + order.id;
            			msg.fields = new Gson().toJson(order);
            			msg.seq = -1;
            			client.sendEvent("snapshot", msg);
            		});
            		// sends the updated current sequence number so the client can ignore old updates
            		Message readyMsg = new Message();
            		readyMsg.seq = -1;
        			client.sendEvent("ready", readyMsg);
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
        
        get("/hello", (req, res) -> {
        	
        	Callback callback = new Callback() {
    		  public void successCallback(String channel, Object response) {
    		    System.out.println(response.toString());
    		  }
    		  public void errorCallback(String channel, PubnubError error) {
    		    System.out.println(error.toString());
    		  }
    		};
    		
    		pubnub.publish("my_channel", "Hello from the PubNub Java SDK!" , callback);
    		
        	return "Hello!";
        });
        
        post("/orders", (req, res) -> {
        	
        	Gson gson = new Gson();
        	Order body = gson.fromJson(req.body(), Order.class);
        	if(body.id != null && !body.id.equals("") && !orders.contains(body)){
	        	orders.add(body);
	        	Message msg = new Message();
	        	msg.action = "added";
	        	msg.type = "order";
	        	msg.id = body.id;
	        	msg.fields = gson.toJson(body);
	        	Main.server.getBroadcastOperations().sendEvent("update", msg);
        	}
        	return "{}";
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
        	return "{}";
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
						Thread.sleep(100);
						synchronized(this) {
							Gson gson = new Gson();
							if (Main.orders.size() == 0 || Math.random() > 0.5){
								Order order = new Order();
								order.id = ""+((int)Math.floor(Math.random() * 1000));
								order.name = "name " + order.id;
								order.value = ""+((int)Math.floor((Math.random()*100)));
								Main.orders.add(order);
								Message msg = new Message();
					        	msg.action = "added";
					        	msg.type = "order";
					        	msg.id = "" + order.id;
					        	msg.fields = gson.toJson(order);
								Main.server.getBroadcastOperations().sendEvent("update", msg);
							}else {
								Order order = Main.orders.remove(0);
								Message msg = new Message();
					        	msg.action = "removed";
					        	msg.type = "order";
					        	msg.id = "" + order.id;
					        	msg.fields = gson.toJson(order);
								Main.server.getBroadcastOperations().sendEvent("update", msg);
							}
						}
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
			}
		}).start();

        new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				while(true) {
					try {
						Thread.sleep(50);
						synchronized(this) {
							Gson gson = new Gson();
							if (Main.orders.size() == 0 || Math.random() > 0.5){
								Order order = new Order();
								order.id = ""+((int)Math.floor(Math.random() * 1000));
								order.name = "name from 2 " + order.id;
								order.value = ""+((int)Math.floor((Math.random()*100)));
								Main.orders.add(order);
								Message msg = new Message();
					        	msg.action = "added";
					        	msg.type = "order";
					        	msg.id = "" + order.id;
					        	msg.fields = gson.toJson(order);
								Main.server.getBroadcastOperations().sendEvent("update", msg);
							}else {
								Order order = Main.orders.remove(0);
								Message msg = new Message();
					        	msg.action = "removed";
					        	msg.type = "order";
					        	msg.id = "" + order.id;
					        	msg.fields = gson.toJson(order);
								Main.server.getBroadcastOperations().sendEvent("update", msg);
							}
						}
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
			}
		}).start();
        
        Thread.sleep(Integer.MAX_VALUE);

        server.stop();
    }
}