package de.visuflow.hero;

public class TargetHero {

    int increment(int i) {
    	if(i>4) {
    		return i + 2;
    	} else {
    		return i + 1;
    	}
    }

    public void entryPoint() {
        int i = 100;
        int j = 200;
        int k = increment(i);
        System.out.println("value of k is: "+k);
     //   int l = increment(j);
    }

    public static void main(String[] args) {
          TargetHero th = new TargetHero(); 
          th.entryPoint();
    }
}
