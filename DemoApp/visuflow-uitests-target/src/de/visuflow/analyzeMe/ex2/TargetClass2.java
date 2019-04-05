package de.visuflow.analyzeMe.ex2;

public class TargetClass2 {
	
	 String data;

	  public static void main(String[] args) {
	    TargetClass2 target = new TargetClass2();
	    TargetClass2 anotherTarget = new TargetClass2();
	    target.data = getSecret();
	    anotherTarget.data = copyData(target.data);
	    target.overwrite("");
	    dangerousTransmission(target.data);
	  }

	  private static String copyData(String a) {
	    String c = a;
	    return c;
	  }

	  private static void dangerousTransmission(String key) {
	    System.out.println("Key handed out: " + key);
	  }

	  private void overwrite(String newData) {
	    this.data = newData;
	  }

	  private static String getSecret() {
	    return "I am secret.";
	}
}
