# OS Simulator – Individual UI Asset Pack

This pack contains **separate reusable assets**, not one flattened screenshot.
The assets are designed around the supplied reference: navy casino-style background, cream parchment panels, crimson buttons, playing-card suits, and a jester-card app mark.

## Folders
- `backgrounds/` – 1600×900 PNG and scalable SVG wallpaper.
- `panels/` – transparent cream and navy panel frames. Use as decorative backgrounds; keep labels and controls as real Swing components.
- `buttons/` – red/navy/cream button backgrounds in normal, hover, and pressed states. Button text is intentionally not baked into the image.
- `logos/` – OS Simulator jester-card logo and CPU Scheduling title banner.
- `icons/` – individual transparent PNG symbols.
- `fonts/` – Noto Sans Mono Bold and Cabin SemiBold font files.
- `examples/` – put your own screenshots here if you want to compare layouts.

## Suggested colors
```text
Navy background   #0F2035
Navy panel        #193049
Deep navy         #142337
Cream             #F4E0C1
Cream shadow      #E7CFAB
Crimson           #B02736
Ink / outline     #142334
Muted blue        #263F59
Gold accent       #E1B169
```

## Java Swing: load and apply the fonts
Place the `fonts` folder in your application resources (for example `src/main/resources/assets/fonts/`).
If your project does not use Maven/Gradle resources, you can load the font from a relative file path during development.

```java
import java.awt.Font;
import java.io.InputStream;

public final class AppFonts {
    public static Font heading;
    public static Font body;

    public static void load() {
        try (InputStream in = AppFonts.class.getResourceAsStream(
                "/assets/fonts/NotoSansMono-Bold.ttf")) {
            heading = Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(Font.BOLD, 22f);
        } catch (Exception ex) {
            heading = new Font("Monospaced", Font.BOLD, 22);
        }

        try (InputStream in = AppFonts.class.getResourceAsStream(
                "/assets/fonts/Cabin-SemiBold.otf")) {
            body = Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(Font.PLAIN, 16f);
        } catch (Exception ex) {
            body = new Font("SansSerif", Font.PLAIN, 16);
        }
    }
}
```

Call `AppFonts.load()` once near the start of your program, before creating the UI. Then:
```java
titleLabel.setFont(AppFonts.heading.deriveFont(28f));
button.setFont(AppFonts.body.deriveFont(16f));
table.setFont(AppFonts.body.deriveFont(14f));
table.getTableHeader().setFont(AppFonts.heading.deriveFont(13f));
```

To set a general Swing default font before creating components:
```java
UIManager.put("Label.font", AppFonts.body);
UIManager.put("Button.font", AppFonts.body);
UIManager.put("ComboBox.font", AppFonts.body);
UIManager.put("TextField.font", AppFonts.body);
UIManager.put("Table.font", AppFonts.body);
UIManager.put("TableHeader.font", AppFonts.heading.deriveFont(13f));
```

## Java Swing: background image
Use a custom panel and paint the background image before Swing paints its child components:
```java
import javax.swing.*;
import java.awt.*;

public class BackgroundPanel extends JPanel {
    private final Image background;

    public BackgroundPanel(String resourcePath) {
        background = new ImageIcon(
            getClass().getResource(resourcePath)).getImage();
        setOpaque(false);
        setLayout(new BorderLayout());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(background, 0, 0, getWidth(), getHeight(), this);
    }
}
```
Example:
```java
JPanel root = new BackgroundPanel("/assets/backgrounds/casino_navy_background.png");
```

## Panels and buttons
For an image-backed panel, keep the contents as actual Swing controls so tables remain editable and accessible:
```java
ImageIcon panelSkin = new ImageIcon(
    getClass().getResource("/assets/panels/cream_panel_9slice.png"));
JLabel panelBackground = new JLabel(panelSkin);
panelBackground.setLayout(new BorderLayout());
panelBackground.add(yourRealTable, BorderLayout.CENTER);
```
For a button, the simplest option is to use the PNG as an icon while retaining the button's real text:
```java
JButton run = new JButton("Run Simulation");
run.setIcon(new ImageIcon(getClass().getResource(
    "/assets/buttons/red_button_normal.png")));
run.setHorizontalTextPosition(SwingConstants.CENTER);
run.setForeground(new Color(244, 224, 193));
run.setBorderPainted(false);
run.setContentAreaFilled(false);
run.setFocusPainted(false);
```
For hover/pressed visuals, use a custom `ButtonModel`/`MouseListener` to swap the image icon between `red_button_normal.png`, `red_button_hover.png`, and `red_button_pressed.png`.

## Important implementation notes
1. Do **not** replace your Gantt chart, tables, inputs, or calculations with screenshots. Keep those as real components.
2. Use `casino_navy_background.png` on the root content area; place cream panels behind input/result sections.
3. Use `os_simulator_jester_card_logo.png` in the title/sidebar area.
4. Use `cpu_scheduling_title_banner.png` as a decorative title banner, or recreate its text as a live label if you want the title to resize.
5. Use PNG for Java Swing transparency. SVG is included for the background if your build already has an SVG renderer; Java Swing does not render SVG natively.
6. Keep the asset paths under one resource folder so they are easy to change later.

## If your repository is still the original GitHub project
I could not inspect the repository source during this asset-pack build, so the code snippets assume Java Swing based on the desktop UI shown in your screenshots. If the project uses a different UI framework, tell me which one or upload the source ZIP and I can adapt the integration instructions to the actual files and class names.
