import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class AppTheme {

    public enum ThemeMode {
        DARK, LIGHT
    }

    private static ThemeMode currentMode = ThemeMode.DARK;

    public static ThemeMode getThemeMode() {
        return currentMode;
    }

    public static void setThemeMode(ThemeMode mode) {
        currentMode = mode;
    }

    // Dynamic Colors based on active theme mode
    public static Color getBackground() {
        return currentMode == ThemeMode.DARK ? new Color(10, 15, 26) : new Color(245, 247, 250);
    }

    public static Color getSurface() {
        return currentMode == ThemeMode.DARK ? new Color(20, 26, 41) : Color.WHITE;
    }

    public static Color getSurfaceLight() {
        return currentMode == ThemeMode.DARK ? new Color(30, 39, 61) : new Color(230, 235, 245);
    }

    public static Color getText() {
        return currentMode == ThemeMode.DARK ? new Color(248, 250, 252) : new Color(15, 23, 42);
    }

    public static Color getTextMuted() {
        return currentMode == ThemeMode.DARK ? new Color(148, 163, 184) : new Color(71, 85, 105);
    }

    public static Color getBorder() {
        return currentMode == ThemeMode.DARK ? new Color(38, 52, 85) : new Color(203, 213, 225);
    }

    public static Color getAccent() {
        return new Color(99, 102, 241); // Indigo primary
    }

    public static Color getAccentHover() {
        return new Color(79, 70, 229);
    }

    public static Color getSuccess() {
        return new Color(16, 185, 129); // Emerald
    }

    public static Color getWarning() {
        return new Color(245, 158, 11); // Amber
    }

    public static Color getError() {
        return new Color(239, 68, 68); // Red
    }

    // Canvas specific backgrounds for highest visualization contrast
    public static Color getCanvasBackground() {
        return currentMode == ThemeMode.DARK ? new Color(6, 9, 15) : Color.WHITE;
    }

    // Sentinel Colors for dynamic text binding
    public static final Color TEXT = new Color(0, 0, 1, 0);
    public static final Color TEXT_MUTED = new Color(0, 0, 2, 0);

    // Typography
    public static final Font TITLE = new Font("SansSerif", Font.BOLD, 22);
    public static final Font SUBTITLE = new Font("SansSerif", Font.PLAIN, 14);
    public static final Font BODY = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font BODY_BOLD = new Font("SansSerif", Font.BOLD, 13);
    public static final Font SMALL = new Font("SansSerif", Font.PLAIN, 11);
    public static final Font SMALL_BOLD = new Font("SansSerif", Font.BOLD, 11);
    public static final Font MONO = new Font("Monospaced", Font.PLAIN, 12);

    private AppTheme() {} // utility class

    public static JLabel createLabel(String text, Font font, Color color) {
        JLabel label = new JLabel(text) {
            @Override
            public Color getForeground() {
                if (color == TEXT_MUTED) return AppTheme.getTextMuted();
                if (color == TEXT) return AppTheme.getText();
                return super.getForeground();
            }
        };
        label.setFont(font);
        label.setForeground(color == TEXT ? getText() : (color == TEXT_MUTED ? getTextMuted() : color));
        return label;
    }

    public static void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(BorderFactory.createLineBorder(getBorder(), 1));
        scrollPane.setBackground(getBackground());
        scrollPane.getViewport().setBackground(getBackground());
        scrollPane.getVerticalScrollBar().setBackground(getBackground());
        scrollPane.getVerticalScrollBar().setUnitIncrement(12);
        scrollPane.getHorizontalScrollBar().setBackground(getBackground());
    }

    public static JToggleButton createThemeToggle(Runnable onToggle) {
        JToggleButton toggle = new JToggleButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean isDark = AppTheme.getThemeMode() == ThemeMode.DARK;
                g2.setColor(isDark ? new Color(30, 41, 67) : new Color(226, 232, 240));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(AppTheme.getBorder());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        toggle.setContentAreaFilled(false);
        toggle.setFocusPainted(false);
        toggle.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        toggle.setFont(SMALL_BOLD);
        toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        Runnable updateText = () -> {
            boolean isDark = AppTheme.getThemeMode() == ThemeMode.DARK;
            toggle.setText(isDark ? "☀ Light" : "☾ Dark");
            toggle.setForeground(AppTheme.getText());
        };

        updateText.run();

        toggle.addActionListener(e -> {
            boolean isDark = AppTheme.getThemeMode() == ThemeMode.DARK;
            AppTheme.setThemeMode(isDark ? ThemeMode.LIGHT : ThemeMode.DARK);
            updateText.run();
            if (onToggle != null) onToggle.run();
        });

        return toggle;
    }

    // Custom rounded text field
    public static class RoundedTextField extends JTextField {
        private final String placeholder;

        public RoundedTextField(String text, String placeholder) {
            super(text);
            this.placeholder = placeholder;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            setFont(BODY);
        }

        @Override
        public Color getBackground() {
            return AppTheme.getThemeMode() == ThemeMode.DARK ? new Color(15, 23, 42) : new Color(241, 245, 249);
        }

        @Override
        public Color getForeground() {
            return AppTheme.getText();
        }

        @Override
        public Color getCaretColor() {
            return AppTheme.getText();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

            if (isFocusOwner()) {
                g2.setColor(getAccent());
                g2.setStroke(new BasicStroke(1.5f));
            } else {
                g2.setColor(AppTheme.getBorder());
                g2.setStroke(new BasicStroke(1.0f));
            }
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            g2.dispose();

            super.paintComponent(g);

            // Draw placeholder if empty
            if (getText().isEmpty() && placeholder != null && !isFocusOwner()) {
                Graphics2D gPlaceholder = (Graphics2D) g.create();
                gPlaceholder.setColor(AppTheme.getTextMuted());
                gPlaceholder.setFont(BODY);
                FontMetrics fm = gPlaceholder.getFontMetrics();
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                gPlaceholder.drawString(placeholder, 12, y);
                gPlaceholder.dispose();
            }
        }
    }

    // Custom styled rounded button
    public static class RoundedButton extends JButton {
        private final boolean primary;
        private boolean hovered = false;

        public RoundedButton(String text, boolean primary) {
            super(text);
            this.primary = primary;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
            setFont(BODY_BOLD);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }
            });
        }

        @Override
        public Color getForeground() {
            if (!isEnabled()) return AppTheme.getTextMuted();
            if (primary) return Color.WHITE;
            return hovered ? AppTheme.getText() : AppTheme.getTextMuted();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (!isEnabled()) {
                g2.setColor(currentMode == ThemeMode.DARK ? new Color(30, 41, 59, 120) : new Color(226, 232, 240, 120));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(AppTheme.getBorder());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            } else {
                if (primary) {
                    g2.setColor(hovered ? getAccentHover() : getAccent());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    g2.setColor(new Color(255, 255, 255, 40));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                } else {
                    g2.setColor(hovered ? getSurfaceLight() : getSurface());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    g2.setColor(hovered ? getAccent() : AppTheme.getBorder());
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                }
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
