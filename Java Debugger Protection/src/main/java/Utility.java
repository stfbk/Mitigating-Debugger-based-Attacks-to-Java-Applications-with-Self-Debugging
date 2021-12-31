public class Utility {

    public native void evenMoreComplexProtection();

    static {
        System.out.println("before loading library");
        System.loadLibrary("hello");
        System.out.println("after loading library");
    }

    public Utility () {
        System.out.println("created an instance of Utility");
    }

    public void useless () {
        System.out.println("i am useless :(");
    }

}
