public class Main {

    public String greeting = "hello boy";

    public String getGreeting() {
    
        return "++" + greeting + "++";
    }

    public static void start() {
    
        Main main = new Main();
        
        System.out.println(main.greeting); 
    }

}
