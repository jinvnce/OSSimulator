import javax.swing.JPanel;

/** Base class for every simulator tab. The main window treats all modules the same way (polymorphism). */
public abstract class ModulePanel extends JPanel {
    public abstract String getTitle();
    protected abstract void randomizeData();
    protected abstract void runSimulation();
}
