
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.jms.connection.CachingConnectionFactory;


public class HangmanUtil {
	public static void main(String...args) throws JMSException, InterruptedException {
		 Connection connection =  (Connection) new ActiveMQConnectionFactory(
				Constants.USERNAME, Constants.PASSWORD, Constants.ACTIVEMQ_URL).createConnection();
			connection.start();
			Session session = connection.createSession(false,Session.AUTO_ACKNOWLEDGE);
			MessageConsumer consumer = session.createConsumer(session.createQueue(Constants.QUEUE_PREFIX + Constants.GAMEQUEUE));
			for(int i = 0; i < 10; i++) {
				System.out.println("Cleaning...");
				String text = ((TextMessage) consumer.receive()).getText();
				System.out.println(text);
			}
			
			consumer.close();
			session.close();
			connection.close();
			System.err.println("Done");
	}
}
