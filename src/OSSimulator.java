import javax.swing.*;

public class OSSimulator extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new OSSimulator().setVisible(true));
    }

    public OSSimulator() {
        super("OS Simulator");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        ModulePanel[] modules = {new CpuPanel(), new BankerPanel(), new PagingPanel(), new ReplacementPanel()};
        JTabbedPane tabs = new JTabbedPane();
        for (ModulePanel m : modules) tabs.addTab(m.getTitle(), m);
        add(tabs);
        setSize(1050, 720);
        setLocationRelativeTo(null);
    }
}
