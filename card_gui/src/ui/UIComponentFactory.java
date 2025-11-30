package ui;

import constants.AppConstants;
import models.BorrowedBook;
import models.Transaction;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Factory class for creating UI components with consistent styling
 */
public class UIComponentFactory {

    public static JButton createPrimaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(AppConstants.PRIMARY_COLOR);
        button.setBorder(new EmptyBorder(12, 24, 12, 24));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(29, 78, 216));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(AppConstants.PRIMARY_COLOR);
            }
        });

        return button;
    }

    public static JButton createSecondaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(AppConstants.TEXT_PRIMARY);
        button.setBackground(new Color(229, 231, 235));
        button.setBorder(new EmptyBorder(12, 24, 12, 24));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(209, 213, 219));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(229, 231, 235));
            }
        });

        return button;
    }

    public static JButton createSuccessButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(AppConstants.SUCCESS_COLOR);
        button.setBorder(new EmptyBorder(12, 24, 12, 24));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(22, 163, 74));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(AppConstants.SUCCESS_COLOR);
            }
        });

        return button;
    }

    public static JButton createDangerButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(AppConstants.DANGER_COLOR);
        button.setBorder(new EmptyBorder(12, 24, 12, 24));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(220, 38, 38));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(AppConstants.DANGER_COLOR);
            }
        });

        return button;
    }

    public static JPanel createSuccessMessage(String title, String message) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setBackground(new Color(220, 252, 231));
        panel.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(134, 239, 172), 1, true),
            new EmptyBorder(12, 16, 12, 16)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        JLabel iconLabel = new JLabel("\u2713");
        iconLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        iconLabel.setForeground(new Color(22, 163, 74));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBackground(new Color(220, 252, 231));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(new Color(22, 163, 74));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel messageLabel = new JLabel("<html>" + message + "</html>");
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        messageLabel.setForeground(new Color(21, 128, 61));
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(4));
        textPanel.add(messageLabel);

        panel.add(iconLabel);
        panel.add(Box.createHorizontalStrut(12));
        panel.add(textPanel);

        return panel;
    }

    public static JPanel createBookCard(BorrowedBook book) {
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBackground(new Color(249, 250, 251));
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(AppConstants.BORDER_COLOR, 1, true),
            new EmptyBorder(15, 20, 15, 20)
        ));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBackground(new Color(249, 250, 251));

        JLabel bookName = new JLabel(book.getBookName() + " (" + book.getBookId() + ")");
        bookName.setFont(new Font("Segoe UI", Font.BOLD, 15));
        bookName.setForeground(AppConstants.TEXT_PRIMARY);

        JLabel dates = new JLabel("Mượn: " + book.getBorrowDate() + " | Hạn trả: " + book.getDueDate());
        dates.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dates.setForeground(AppConstants.TEXT_SECONDARY);

        textPanel.add(bookName);
        textPanel.add(Box.createVerticalStrut(5));
        textPanel.add(dates);

        JLabel statusLabel = new JLabel(book.getStatus());
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        statusLabel.setOpaque(true);
        statusLabel.setBorder(new EmptyBorder(5, 12, 5, 12));

        if (book.getStatus().equals("Đang mượn")) {
            statusLabel.setForeground(AppConstants.SUCCESS_COLOR);
            statusLabel.setBackground(new Color(220, 252, 231));
        } else if (book.getStatus().equals("Quá hạn")) {
            statusLabel.setForeground(AppConstants.DANGER_COLOR);
            statusLabel.setBackground(new Color(254, 226, 226));
        } else {
            statusLabel.setForeground(new Color(217, 119, 6));
            statusLabel.setBackground(new Color(254, 243, 199));
        }

        card.add(textPanel, BorderLayout.CENTER);
        card.add(statusLabel, BorderLayout.EAST);

        return card;
    }

    public static JPanel createTransactionCard(Transaction tx) {
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBackground(new Color(249, 250, 251));
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(AppConstants.BORDER_COLOR, 1, true),
            new EmptyBorder(15, 20, 15, 20)
        ));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBackground(new Color(249, 250, 251));

        JLabel typeLabel = new JLabel(tx.getType());
        typeLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        typeLabel.setForeground(AppConstants.TEXT_PRIMARY);

        JLabel dateLabel = new JLabel(tx.getDate());
        dateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        dateLabel.setForeground(AppConstants.TEXT_SECONDARY);

        textPanel.add(typeLabel);
        textPanel.add(dateLabel);

        JLabel amountLabel = new JLabel(String.format("%+,d VND", tx.getAmount()));
        amountLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        amountLabel.setForeground(tx.getAmount() >= 0 ? AppConstants.SUCCESS_COLOR : AppConstants.DANGER_COLOR);

        card.add(textPanel, BorderLayout.CENTER);
        card.add(amountLabel, BorderLayout.EAST);

        return card;
    }

    public static JPasswordField createPasswordField() {
        JPasswordField field = new JPasswordField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        field.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(AppConstants.BORDER_COLOR, 1, true),
            new EmptyBorder(12, 14, 12, 14)
        ));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        return field;
    }

    private UIComponentFactory() {
        // Prevent instantiation
    }
}
