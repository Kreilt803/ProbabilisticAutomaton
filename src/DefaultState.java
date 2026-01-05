public class DefaultState implements State{
    private final String name;
    private final boolean isReceiver;

    public DefaultState(String name, boolean isReceiver){
        this.name=name;
        this.isReceiver=isReceiver;
    }

    @Override
    public String getName(){
        return name;
    }

    @Override
    public boolean isReceiver(){
        return isReceiver;
    }
}
