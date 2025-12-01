package components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Tab component for navigation between different pages
 */
public class TabPanel extends JPanel {
    private static final Color PRIMARY_COLOR = new Color(37, 99, 235);
    private static final Color TEXT_SECONDARY = new Color(107, 114, 128);
    private static final Color BORDER_COLOR = new Color(229, 231, 235);

    private JLabel activeLabel;

    public TabPanel() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));
    }

    public void addTab(String iconText, String text, boolean active, Runnable onClick) {
        JLabel tab = createTab(iconText, text, active);
        tab.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setActiveTab(tab);
                onClick.run();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (tab != activeLabel) {
                    tab.setForeground(PRIMARY_COLOR);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (tab != activeLabel) {
                    tab.setForeground(TEXT_SECONDARY);
                }
            }
        });

        add(tab);

        if (active) {
            activeLabel = tab;
        }
    }

    private JLabel createTab(String iconText, String text, boolean active) {
        JLabel label = new JLabel(iconText + " " + text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        label.setForeground(active ? PRIMARY_COLOR : TEXT_SECONDARY);
        label.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, active ? 2 : 0, 0, PRIMARY_COLOR),
            new EmptyBorder(12, 20, 12, 20)
        ));
        label.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return label;
    }

    private void setActiveTab(JLabel tab) {
        if (activeLabel != null) {
            activeLabel.setForeground(TEXT_SECONDARY);
            activeLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 0, PRIMARY_COLOR),
                new EmptyBorder(12, 20, 12, 20)
            ));
        }

        tab.setForeground(PRIMARY_COLOR);
        tab.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, PRIMARY_COLOR),
            new EmptyBorder(12, 20, 12, 20)
        ));
        activeLabel = tab;
    }
}

