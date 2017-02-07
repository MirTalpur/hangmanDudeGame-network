
public class SampleListenerClass implements HangmanMessageListener {

	public void recieve(String message) {
		System.out.println(message);
		
	}

	public void challangeAccepted() {
		System.out.println("The CHALLANGE WAS ACCEPTED");
		
	}

	public void challangeRejected() {
		System.out.println("The CHALLANGE WAS REJECTED");		
	}

	public void playerQuit() {
		System.out.println("THE OTHER PLAYER QUIT");
	}


	public void playerGuessed(String guess) {
		System.out.println("OTHER PLAYER GUESSED: " + guess);
		
	}

}
