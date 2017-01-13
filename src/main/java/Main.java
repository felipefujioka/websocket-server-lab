import static spark.Spark.*;

import java.net.URISyntaxException;
import java.util.ArrayList;

import com.google.gson.Gson;

import io.deepstream.*;

public class Main {
	
	public static int counter = 0;
	public static ArrayList<Order> orders;
	public static int currentSequenceNumber = 0;
	public static DeepstreamClient ds;
	
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

    public static void main(String[] args) throws InterruptedException, URISyntaxException {
    	
    	orders = new ArrayList<Order>();
        
		Main.ds = new DeepstreamClient( "localhost:6020" );
		Main.ds.login();
		System.out.println("deepstream login");
		List list = Main.ds.record.getList("orders");
		list.setEntries(new ArrayList<String>());
        
        
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
    		
        	return "Hello!";
        });
        
        post("/orders", (req, res) -> {
        	
        	Gson gson = new Gson();
        	Order body = gson.fromJson(req.body(), Order.class);
        	if(body.id != null && !body.id.equals("") && !orders.contains(body)){
	        	orders.add(body);
	        	Record record  = Main.ds.record.getRecord("orders/"+body.id);
	        	record.set(body);
	        	List local = Main.ds.record.getList("orders");
	        	local.addEntry("orders/"+body.id);
	        	
        	}
        	return "{}";
        });
        
        put("/orders/:id", (req, res) -> {
        	Gson gson = new Gson();
        	Order body = gson.fromJson(req.body(), Order.class);
        	if(body.id != null && !body.id.equals("") && orders.contains(body)){
	        	orders.remove(body);
	        	orders.add(body);
	//        	Message msg = new Message();
	//        	msg.action = "changed";
	//        	msg.type = "order";
	//        	msg.id = body.id;
	//        	msg.fields = gson.toJson(body);
	        	Record record  = Main.ds.record.getRecord("orders/"+body.id);
	        	record.set(body);
        	}
        	return "{}";
        });
        
        delete("/orders/:id", (req, res) -> {
        	Order obj = new Order();
        	obj.id = req.params(":id");
        	orders.remove(obj);
        	
        	Record record  = Main.ds.record.getRecord("orders/"+obj.id);
        	record.delete();
        	List local = Main.ds.record.getList("orders");
        	local.removeEntry("orders/"+obj.id);
        	
        	return "deleted";
        });
        
//        new Thread(new Runnable() {
//			
//			@Override
//			public void run() {
//				// TODO Auto-generated method stub
//				while(true) {
//					try {
//						Thread.sleep(100);
//						synchronized(this) {
//							if (Main.orders.size() == 0 || Math.random() > 0.5){
//								Order order = new Order();
//								order.id = ""+((int)Math.floor(Math.random() * 1000));
//								order.name = "name " + order.id;
//								order.value = ""+((int)Math.floor((Math.random()*100)));
//								Main.orders.add(order);
//							}else {
//								Main.orders.remove(0);
//							}
//							List list = Main.ds.record.getList("orders");
//							Stream<String> stream = Main.orders.stream().map(order -> "orders/"+order.id);
//							String[] array = stream.toArray(String[]::new); 
//							list.setEntries(Arrays.asList(array));
//						}
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//					
//				}
//			}
//		}).start();

//        new Thread(new Runnable() {
//			
//			@Override
//			public void run() {
//				// TODO Auto-generated method stub
//				while(true) {
//					try {
//						Thread.sleep(500);
//						synchronized(this) {
//							List list = Main.ds.record.getList("orders");
//							if (Main.orders.size() == 0 || Math.random() > 0.5){
//								Order order = new Order();
//								order.id = ""+((int)Math.floor(Math.random() * 1000));
//								order.name = "name from 2 " + order.id;
//								order.value = ""+((int)Math.floor((Math.random()*100)));
//								Main.orders.add(order);
//								Record record = Main.ds.record.getRecord("orders/"+order.id);
//								record.set("id", order.id);
//								record.set("name", order.name);
//								record.set("value", order.value);
//								list.addEntry("orders/"+order.id);
//							}else {
//								Order order = Main.orders.remove(0);
//								list.removeEntry("orders/"+order.id);
//							}
//						}
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//					
//				}
//			}
//		}).start();
        
//        Thread.sleep(Integer.MAX_VALUE);
    }
}