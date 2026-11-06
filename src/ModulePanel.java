import javax.swing.JPanel;

public abstract class ModulePanel extends JPanel {
    public abstract String getTitle();
    protected abstract void randomizeData();
    protected abstract void runSimulation();
}
