package de.userstudy.target1;

public class Target {

  String knowledge;

  public static void main(String[] args) {
    Target target = new Target();
    Target anotherTarget = new Target();
    target.knowledge = Target.getSecret();
    if(target.knowledge.equals("abc")){
    	target.knowledge = "No Secret!";
    } else {
    	anotherTarget.copyTarget(target);
    }
    dangerousTransmission(target.knowledge);
    dangerousTransmission(anotherTarget.knowledge);
  }

  private void copyTarget(Target original) {
    this.knowledge = original.knowledge;
  }

  private static void dangerousTransmission(String key) {
    System.out.println("Key handed out: " + key);
  }

  private static String getSecret() {
    return "I am secret.";
  }

}
