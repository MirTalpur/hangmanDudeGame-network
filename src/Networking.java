import java.util.ArrayList;
import java.util.Random;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;

@Configuration
@ComponentScan
@EnableAutoConfiguration
public class Networking implements MessageListener {
	Connection connection = null;
	HangmanMessageListener listener = null;
	Session session = null;
	ArrayList<MessageConsumer> consumers = new ArrayList<MessageConsumer>();
	private String game_challange_queue_name = null;
	
	public Networking() {}
	public Networking(HangmanMessageListener msg_listener) throws JMSException {
		@SuppressWarnings("resource")
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Networking.class);
		ConnectionFactory factory =  context.getBean(CachingConnectionFactory.class);
		listener = msg_listener;
	
		connection = factory.createConnection();
		connection.start();
		session = connection.createSession(false,Session.AUTO_ACKNOWLEDGE);
	}
	
	
	public String randomString(int size) {
		String s = "abcdefghijklmnopqrstuvwxyz";
		Random r = new Random();
		String randomStr = "";
		for(int i = 0; i < size; i++)
			randomStr += s.charAt(r.nextInt(s.length()));
		return randomStr;
	}
	
	
	public void end() throws JMSException {
		if(game_challange_queue_name != null) {
			queueMessage(game_challange_queue_name, Constants.PLAYER_QUIT);
		}
		for(MessageConsumer consumer : consumers)
			consumer.close();
		session.close();
		connection.close();
		System.err.println("ENDED ALL CONNECTIONS");
	}
	
	
	@Bean
	ConnectionFactory connectionFactory() {
		return new CachingConnectionFactory(new ActiveMQConnectionFactory(
				Constants.USERNAME, Constants.PASSWORD, Constants.ACTIVEMQ_URL));
	}
	
	private String readQueueSync(String queue_name) throws JMSException {
		Queue queue = session.createQueue(Constants.QUEUE_PREFIX + queue_name);
		MessageConsumer consumer = session.createConsumer(queue);
		String word = ((TextMessage) consumer.receive()).getText();
		consumer.close();
		System.err.println("READ QUEUE " + queue_name + " synchronously. Got item: " + word);
		return word;
	}
	
	private void readQueueAsync(String queue_name) throws JMSException {
		Queue queue = session.createQueue(Constants.QUEUE_PREFIX + queue_name);
		MessageConsumer consumer = session.createConsumer(queue);
		consumers.add(consumer);
		consumer.setMessageListener(this);
		System.err.println("READING " +  queue_name + " asynchronously");
		
	}
	
	private void queueMessage(String queue_name, String message) throws JMSException {
		Queue queue = session.createQueue(Constants.QUEUE_PREFIX +  queue_name);
		MessageProducer producer = session.createProducer(queue);
		producer.send(session.createTextMessage(message));
		producer.close();
		System.err.println(message + " added to " + queue_name);
	}
	
	public void GamerAcceptChallange() throws Exception {
		if(game_challange_queue_name == null)
			throw new Exception("No challange available. Did you forget to call GetGameChallangeWord?");
		else {
			queueMessage(game_challange_queue_name, Constants.ChallangeAccepted);
			System.err.println("ACCEPTING CHALLANGE");
		}
		
	}
	
	public void GamerRejectChallange() throws Exception {
		if(game_challange_queue_name == null)
			throw new Exception("No challange available. Did you forget to call GetGameChallangeWord?");
		else {
			queueMessage(game_challange_queue_name, Constants.ChallangeRejected);
			System.err.println("REJECTING CHALLANGE");
		}
	}
	
	
	public String GamerGetGameChallangeWord() throws JMSException {
		String word = readQueueSync(Constants.GAMEQUEUE);
		if(word != null) {
			game_challange_queue_name = word;
			return word.substring(0, word.indexOf('-'));
		}
		return word;
	}
	
	public void ChallangerCreateGame(String word) throws JMSException {
		String random_hash = randomString(5);
		game_challange_queue_name = word+"-"+random_hash;
		queueMessage(Constants.GAMEQUEUE, game_challange_queue_name );
		readQueueAsync(game_challange_queue_name);
		
	}
	
	public void GamerGuessLetter(String guess) throws Exception {
		if(game_challange_queue_name == null)
			throw new Exception("No challange available. Did you forget to call GetGameChallangeWord?");
		queueMessage(game_challange_queue_name, Constants.GUESS_PREFIX + guess);
	}


	public void onMessage(Message msg) {
		try {
			String text = ((TextMessage) msg).getText();
			System.err.println("Received Message: " + text);
				
			
			if(text == Constants.ChallangeAccepted) {
				listener.challangeAccepted();
			}
			else if(text == Constants.ChallangeRejected) {
				listener.challangeRejected();
			}
			else if(text == Constants.PLAYER_QUIT) {
				listener.playerQuit();
			}
			else if(text.contains(Constants.GUESS_PREFIX)) {
				listener.playerGuessed(text.substring(Constants.GUESS_PREFIX.length()));
			}
				
			
		} catch (JMSException e) {
			e.printStackTrace();
		}
		
	}
	
	
	
	public static void main(String...args) throws Exception {
		Networking gamer = new Networking(new SampleListenerClass());
		Networking challanger = new Networking(new SampleListenerClass());
		
		
		challanger.ChallangerCreateGame("HelloWorld");
		
		
		gamer.GamerGetGameChallangeWord();
		gamer.GamerAcceptChallange();
		gamer.GamerGuessLetter("S");
		
		
		System.in.read();
		gamer.end();
		challanger.end();
		
		
	}
}
