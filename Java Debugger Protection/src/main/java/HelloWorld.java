class HelloWorld {

    public static void main(String[] args) {
        System.out.println("before calling utility");
        Utility u = new Utility();
        System.out.println("after calling utility and before calling protection");
        u.evenMoreComplexProtection();
        System.out.println("after calling protection");
        System.exit(0);
    }
}