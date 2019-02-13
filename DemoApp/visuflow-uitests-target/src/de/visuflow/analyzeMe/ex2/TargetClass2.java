package de.visuflow.analyzeMe.ex2;

public class TargetClass2 {
	
	private void leak(String data) {
		System.out.println("Leak: " + data);
	}

	public void sourceToSink() {
		String x = getSecret();
		String y = x;
		leak(y);
	}
	
	public String sourceToReturn() {
        String x = getSecret();
        String y = x;
        return y;
    }
	
	public String sourceToLeakAndReturn() {
        String x = getSecret();
        String y = x;
        leak(y);
        return x;
    }
	
	/**
	 * This does not work, because the assignment
	 * gets removed during optimization
	 */
	public void taintOverwriteByConstant() {
        String x = getSecret();
        String y = x;
        leak(y);
        x = "foobar";
        leak(x);
    }
	
	public void taintedBase() {
        String x = getSecret();
        Container c = new Container();
        c.taintedField = x;
        c.notTainted = "hello";
        leak(c.notTainted);
        System.out.println(c.taintedField);
    }
	
	public void taintedInstanceOfOperand() {
	    String x = getSecret();
	    boolean isString = x instanceof String;
	    System.out.println(isString);
	}
	
	public void taintedLogicOperand() {
	    int x = getSecretInteger();
	    int y = x | 12;
	    System.out.println(y);
	}
	
	public void taintedArithmeticOperand() {
	    int x = getSecretInteger();
	    int y = x + 12;
	    System.out.println(y);
	}
	
	private String getSecret() {
		return "top secret";
	}
	
	private int getSecretInteger() {
	    return 42;
	}
	
	private class Container {
	    public String taintedField;
	    public String notTainted = "foobar";
	}
	
	public static void main(String[] args) {
		TargetClass2 t = new TargetClass2();
		t.leak(t.getSecret());
		
		boolean bla = true;
		if(bla) {
			System.out.println("bla");
			bla=true;
			System.out.println("--");
		} else {
			System.out.println("!bla");
			bla=false;
			System.out.println("##");
		}
	}
}
