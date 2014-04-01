import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Scanner;

public class DoThings {
	private static final String MESSAGE_STARTUP = "Get ready to Do Things!";
	private static final String MESSAGE_COMMAND = "Please enter a command: ";
	
	//private static Scanner scanUserInput;
	
	private static void displayFeedback(String str) {
		System.out.print(str);
	}
	
	private static void displayFeedbackLn(String str) {
		System.out.println(str);
	}
	
	private static String readCommand(String userInput) {
		displayFeedback(MESSAGE_COMMAND);
		//String userInput = scanUserInput.nextLine();
		Feedback feed = MainLogic.runLogic(userInput);
		//displayFeedbackLn(feed.toString());
		
		return feed.toString();
	}
	
	protected static String run(String userInput) {
		//scanUserInput = new Scanner(System.in);
		System.out.println(MESSAGE_STARTUP);
		return readCommand(userInput);
	}
	/*
	public static void main(String[] args) {
		DoThings program = new DoThings();
		program.run();
	}*/
}