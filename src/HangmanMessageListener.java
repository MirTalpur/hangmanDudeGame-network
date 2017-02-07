
public interface HangmanMessageListener {
	public void challangeAccepted();
	public void challangeRejected();
	public void playerQuit();
	public void playerGuessed(String guess);
}
