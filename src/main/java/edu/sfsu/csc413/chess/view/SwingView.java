/** This view was created with the help of Gemini 3.8 */
package edu.sfsu.csc413.chess.view;

import edu.sfsu.csc413.chess.engine.Game;
import edu.sfsu.csc413.chess.engine.GameStatus;
import edu.sfsu.csc413.chess.model.Color;
import edu.sfsu.csc413.chess.model.Move;
import edu.sfsu.csc413.chess.model.Piece;
import edu.sfsu.csc413.chess.model.PieceType;
import edu.sfsu.csc413.chess.model.Position;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * A graphical 2D chessboard view implemented with Swing.
 * This class was created with the help of Gemini 3.8
 *
 * <p>Supports click-to-move input, turn and status indicators, check and checkmate
 * alerts, pawn promotion selection windows, and smooth move animations.
 */
public class SwingView implements BoardView {

    private JFrame frame;
    private BoardPanel boardPanel;
    private JLabel turnLabel;
    private JLabel turnIconLabel;
    private JLabel alertLabel;
    private JPanel alertPanel;
    private JScrollPane alertLabelScroll;

    private GameController controller;
    private Game currentGame;

    private boolean currentlyInCheck = false;
    private Position selectedPosition = null;
    private final List<Position> legalDestinations = new ArrayList<>();

    // Keep track of move endpoints initiated by clicking to validate promotions
    private Position pendingMoveFrom = null;
    private Position pendingMoveTo = null;

    // Animation state
    private int previousHistorySize = 0;
    private Position animFrom = null;
    private Position animTo = null;
    private Piece animPiece = null;
    private float animProgress = 1.0f;
    private boolean isAnimating = false;
    private Timer animTimer;
    private static final int ANIMATION_DURATION_MS = 180;

    // Visual theme colors
    private static final java.awt.Color COLOR_LIGHT_SQUARE = new java.awt.Color(240, 217, 181);
    private static final java.awt.Color COLOR_DARK_SQUARE = new java.awt.Color(181, 136, 99);
    private static final java.awt.Color COLOR_SELECTED_LIGHT = new java.awt.Color(205, 210, 106);
    private static final java.awt.Color COLOR_SELECTED_DARK = new java.awt.Color(170, 162, 58);
    private static final java.awt.Color COLOR_DEST_DOT = new java.awt.Color(30, 70, 32, 100);
    private static final java.awt.Color COLOR_CHECK_GLOW = new java.awt.Color(235, 60, 60, 180);
    private static final java.awt.Color COLOR_BG_PANEL = new java.awt.Color(44, 47, 51);
    private static final java.awt.Color COLOR_TEXT_LIGHT = new java.awt.Color(235, 235, 235);

    private static final int SQUARE_SIZE = 75;
    private static final int BOARD_MARGIN = 24;
    private static final int BOARD_PIXEL_SIZE = BOARD_MARGIN * 2 + SQUARE_SIZE * Position.BOARD_SIZE;

    private Font pieceFont;

    public SwingView() {
        this.pieceFont = findPieceFont(56);
    }

    private static Font findPieceFont(int size) {
        String[] candidateFonts = {
                "Segoe UI Symbol", "DejaVu Sans", "Arial Unicode MS", "Lucida Sans Unicode", "SansSerif", "Dialog"
        };
        for (String name : candidateFonts) {
            Font f = new Font(name, Font.PLAIN, size);
            if (f.canDisplay('\u2654') && f.canDisplay('\u265A')) {
                return f;
            }
        }
        return new Font(Font.SANS_SERIF, Font.PLAIN, size);
    }

    @Override
    public void start(GameController controller) {
        this.controller = controller;
        this.currentGame = controller.game();
        this.previousHistorySize = currentGame.history().size();

        if (SwingUtilities.isEventDispatchThread()) {
            buildAndShowGui();
        } else {
            SwingUtilities.invokeLater(this::buildAndShowGui);
        }
    }

    private void buildAndShowGui() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        frame = new JFrame("CSC 413 Chess");
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (controller != null) {
                    controller.handle(new PlayerAction.Quit());
                } else {
                    close();
                }
            }
        });

        JPanel mainContainer = new JPanel(new BorderLayout(0, 8));
        mainContainer.setBackground(COLOR_BG_PANEL);
        mainContainer.setBorder(new EmptyBorder(10, 14, 10, 14));

        // Top Header Panel: Turn display and Alert message
        JPanel topPanel = new JPanel(new BorderLayout(10, 6));
        topPanel.setOpaque(false);

        // Turn Info Panel
        JPanel turnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        turnPanel.setOpaque(false);

        turnIconLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean isWhite = (currentGame != null && currentGame.sideToMove() == Color.WHITE);
                g2.setColor(isWhite ? java.awt.Color.WHITE : new java.awt.Color(35, 35, 35));
                g2.fillOval(2, 2, 16, 16);
                g2.setColor(isWhite ? new java.awt.Color(160, 160, 160) : java.awt.Color.WHITE);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(2, 2, 16, 16);
                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(22, 22);
            }
        };

        turnLabel = new JLabel("White to move");
        turnLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        turnLabel.setForeground(COLOR_TEXT_LIGHT);

        turnPanel.add(turnIconLabel);
        turnPanel.add(turnLabel);

        // Controls on top-right: Undo & Show Legal Moves
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        controlsPanel.setOpaque(false);

        JButton undoButton = new JButton("Undo");
        undoButton.setFocusable(false);
        undoButton.addActionListener(e -> {
            if (controller != null) {
                controller.handle(new PlayerAction.Undo());
            }
        });

        JButton movesButton = new JButton("Moves");
        movesButton.setFocusable(false);
        movesButton.addActionListener(e -> {
            if (controller != null) {
                controller.handle(new PlayerAction.ShowMoves());
            }
        });

        controlsPanel.add(undoButton);
        controlsPanel.add(movesButton);

        topPanel.add(turnPanel, BorderLayout.WEST);
        topPanel.add(controlsPanel, BorderLayout.EAST);

        // Alert Panel
        /*
        alertPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));

        alertPanel.setBackground(new java.awt.Color(54, 57, 63));
        alertPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new java.awt.Color(70, 74, 82), 1),
                new EmptyBorder(3, 10, 3, 10)
        ));

        alertLabel = new JLabel("Welcome to Chess!");
        alertLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        alertLabel.setForeground(new java.awt.Color(220, 220, 220));
        //alertPanel.add(alertLabel);

        alertLabelScroll = new JScrollPane(
                alertLabel,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        alertLabelScroll.setOpaque(false);
        alertLabelScroll.getViewport().setOpaque(false);
        alertLabelScroll.setBorder(BorderFactory.createEmptyBorder());
        JScrollBar hBar = alertLabelScroll.getHorizontalScrollBar();
        hBar.setPreferredSize(new Dimension(Integer.MAX_VALUE, 5));
        hBar.setOpaque(false);
        alertPanel.add(alertLabelScroll, BorderLayout.CENTER);
        */
        alertPanel = new JPanel(new BorderLayout());
        alertPanel.setBackground(new java.awt.Color(54, 57, 63));
        alertPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new java.awt.Color(70, 74, 82), 1),
                new EmptyBorder(3, 10, 3, 10)
        ));

        alertLabel = new JLabel("Welcome to Chess!", SwingConstants.CENTER);
        alertLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        alertLabel.setForeground(new java.awt.Color(220, 220, 220));

        alertLabelScroll = new JScrollPane(
                alertLabel,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        alertLabelScroll.setOpaque(false);
        alertLabelScroll.getViewport().setOpaque(false);
        alertLabelScroll.setBorder(BorderFactory.createEmptyBorder());
        alertLabelScroll.setViewportBorder(null);

        JScrollBar hBar = alertLabelScroll.getHorizontalScrollBar();
        hBar.setPreferredSize(new Dimension(0, 0));
        hBar.setOpaque(false);

        alertPanel.add(alertLabelScroll, BorderLayout.CENTER);

        topPanel.add(alertPanel, BorderLayout.SOUTH);
        mainContainer.add(topPanel, BorderLayout.NORTH);

        // Center Chessboard Panel
        boardPanel = new BoardPanel();
        mainContainer.add(boardPanel, BorderLayout.CENTER);

        frame.setContentPane(mainContainer);
        frame.pack();
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        updateTurnAndAlertDisplay();
    }

    @Override
    public void render(Game game) {
        this.currentGame = game;

        int currentHistorySize = game.history().size();
        if (currentHistorySize > previousHistorySize && !game.history().isEmpty()) {
            Move lastMove = game.history().getLast();
            triggerMoveAnimation(lastMove);
        } else {
            isAnimating = false;
            if (animTimer != null && animTimer.isRunning()) {
                animTimer.stop();
            }
        }
        previousHistorySize = currentHistorySize;

        SwingUtilities.invokeLater(() -> {
            updateTurnAndAlertDisplay();
            if (boardPanel != null) {
                boardPanel.repaint();
            }
        });
    }

    @Override
    public void showMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            if (alertLabel != null && message != null && !message.isBlank()) {
                alertLabel.setText(message);
                if (message.startsWith("Not a legal move") || message.startsWith("Illegal")) {
                    alertPanel.setBackground(new java.awt.Color(138, 38, 38));
                    alertLabel.setForeground(java.awt.Color.WHITE);
                } else {
                    alertPanel.setBackground(new java.awt.Color(54, 57, 63));
                    alertLabel.setForeground(new java.awt.Color(220, 220, 220));
                }
            }
        });
    }

    @Override
    public PieceType promotionChoice(Game game) {
        // If a move is initiated by clicking, ensure it is a legal promotion before spawning the dialog.
        // Illegal moves should not spawn the promotion choices window.
        if (pendingMoveFrom != null && pendingMoveTo != null) {
            boolean isLegalPromotion = game.legalMoves().stream().anyMatch(
                    m -> m.isPromotion()
                            && m.from().equals(pendingMoveFrom)
                            && m.to().equals(pendingMoveTo)
            );
            if (!isLegalPromotion) {
                return PieceType.QUEEN;
            }
        } else {
            boolean anyLegalPromotion = game.legalMoves().stream().anyMatch(Move::isPromotion);
            if (!anyLegalPromotion) {
                return PieceType.QUEEN;
            }
        }

        return promptPromotionDialog(game.sideToMove());
    }

    private PieceType promptPromotionDialog(Color color) {
        final PieceType[] chosen = new PieceType[]{PieceType.QUEEN};

        Runnable showModalDialog = () -> {
            JDialog dialog = new JDialog(frame, "Pawn Promotion", true);
            dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            dialog.setLayout(new BorderLayout(12, 12));

            JPanel messagePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            messagePanel.setBorder(new EmptyBorder(12, 12, 0, 12));
            JLabel titleLabel = new JLabel("Pawn reached the 8th rank! Choose promotion piece:");
            titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            messagePanel.add(titleLabel);
            dialog.add(messagePanel, BorderLayout.NORTH);

            JPanel buttonsPanel = new JPanel(new GridLayout(1, 4, 10, 10));
            buttonsPanel.setBorder(new EmptyBorder(10, 16, 16, 16));

            PieceType[] types = {PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT};
            for (PieceType type : types) {
                String symbol = getPieceGlyph(type, color);
                JButton btn = new JButton("<html><center><font size='+2'>" + symbol + "</font><br>" + type.name() + "</center></html>");
                btn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
                btn.setFocusPainted(false);
                btn.addActionListener(e -> {
                    chosen[0] = type;
                    dialog.dispose();
                });
                buttonsPanel.add(btn);
            }

            dialog.add(buttonsPanel, BorderLayout.CENTER);
            dialog.pack();
            dialog.setLocationRelativeTo(frame);
            dialog.setVisible(true);
        };

        if (SwingUtilities.isEventDispatchThread()) {
            showModalDialog.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(showModalDialog);
            } catch (Exception e) {
                return PieceType.QUEEN;
            }
        }

        return chosen[0];
    }

    private void updateTurnAndAlertDisplay() {
        if (currentGame == null || turnLabel == null) {
            return;
        }

        Color side = currentGame.sideToMove();
        GameStatus status = currentGame.status();

        turnLabel.setText(side == Color.WHITE ? "White to move" : "Black to move");
        turnIconLabel.repaint();

        switch (status) {
            case CHECK -> {
                if (!currentlyInCheck) {
                    currentlyInCheck = true;
                    alertLabel.setText("⚠️ CHECK! " + side.name() + " King is in check!");
                }
                //if (!alertLabel.getText().contains("legal moves")) {
                //}
                alertPanel.setBackground(new java.awt.Color(160, 90, 20));
                alertLabel.setForeground(java.awt.Color.WHITE);
            }
            case CHECKMATE -> {
                alertLabel.setText("🏆 CHECKMATE! " + side.opposite().name() + " wins!");
                alertPanel.setBackground(new java.awt.Color(140, 25, 25));
                alertLabel.setForeground(java.awt.Color.WHITE);
            }
            case STALEMATE -> {
                alertLabel.setText("½-½ STALEMATE! Game is a draw.");
                alertPanel.setBackground(new java.awt.Color(80, 80, 80));
                alertLabel.setForeground(java.awt.Color.WHITE);
            }
            case IN_PROGRESS -> {
                currentlyInCheck = false;
                if (alertLabel.getText().startsWith("⚠️ CHECK!")
                        || alertLabel.getText().startsWith("🏆 CHECKMATE!")
                        || alertLabel.getText().startsWith("½-½ STALEMATE!")) {
                    alertLabel.setText("Game in progress");
                    alertPanel.setBackground(new java.awt.Color(54, 57, 63));
                    alertLabel.setForeground(new java.awt.Color(220, 220, 220));
                }
            }
        }
    }

    private void triggerMoveAnimation(Move move) {
        if (move == null) {
            return;
        }
        animFrom = move.from();
        animTo = move.to();
        animPiece = move.moved();
        animProgress = 0.0f;
        isAnimating = true;

        if (animTimer != null && animTimer.isRunning()) {
            animTimer.stop();
        }

        final long startTime = System.currentTimeMillis();
        animTimer = new Timer(15, e -> {
            long elapsed = System.currentTimeMillis() - startTime;
            float progress = (float) elapsed / ANIMATION_DURATION_MS;
            if (progress >= 1.0f) {
                animProgress = 1.0f;
                isAnimating = false;
                animTimer.stop();
            } else {
                animProgress = progress;
            }
            if (boardPanel != null) {
                boardPanel.repaint();
            }
        });
        animTimer.start();
    }

    @Override
    public void close() {
        if (animTimer != null && animTimer.isRunning()) {
            animTimer.stop();
        }
        if (frame != null) {
            SwingUtilities.invokeLater(() -> {
                frame.setVisible(false);
                frame.dispose();
            });
        }
    }

    private static String getPieceGlyph(PieceType type, Color color) {
        boolean white = (color == Color.WHITE);
        return switch (type) {
            case KING -> white ? "♔" : "♚";
            case QUEEN -> white ? "♕" : "♛";
            case ROOK -> white ? "♖" : "♜";
            case BISHOP -> white ? "♗" : "♝";
            case KNIGHT -> white ? "♘" : "♞";
            case PAWN -> white ? "♙" : "♟";
        };
    }

    /**
     * Inner custom component representing the 8x8 chessboard with coordinates and animations.
     */
    private class BoardPanel extends JPanel {

        BoardPanel() {
            setPreferredSize(new Dimension(BOARD_PIXEL_SIZE, BOARD_PIXEL_SIZE));
            setBackground(COLOR_BG_PANEL);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    handleBoardClick(e.getX(), e.getY());
                }
            });
        }

        private void handleBoardClick(int mouseX, int mouseY) {
            if (currentGame == null || controller == null || currentGame.isGameOver()) {
                return;
            }

            // Snap animation to completion if clicked during animation
            if (isAnimating) {
                isAnimating = false;
                if (animTimer != null && animTimer.isRunning()) {
                    animTimer.stop();
                }
                repaint();
            }

            int boardX = mouseX - BOARD_MARGIN;
            int boardY = mouseY - BOARD_MARGIN;
            if (boardX < 0 || boardY < 0) {
                return;
            }

            int file = boardX / SQUARE_SIZE;
            int row = boardY / SQUARE_SIZE;
            if (file < 0 || file >= Position.BOARD_SIZE || row < 0 || row >= Position.BOARD_SIZE) {
                return;
            }

            int rank = 7 - row;
            Position clickedPos = new Position(file, rank);
            Piece clickedPiece = currentGame.board().pieceAt(clickedPos);

            if (selectedPosition == null) {
                // First click: select a piece belonging to current player
                if (clickedPiece != null && clickedPiece.color() == currentGame.sideToMove()) {
                    selectedPosition = clickedPos;
                    updateLegalDestinationsFor(clickedPos);
                    repaint();
                }
            } else {
                // Second click
                if (clickedPos.equals(selectedPosition)) {
                    // Deselect
                    selectedPosition = null;
                    legalDestinations.clear();
                    repaint();
                } else if (clickedPiece != null && clickedPiece.color() == currentGame.sideToMove()) {
                    // Switch selection to another friendly piece
                    selectedPosition = clickedPos;
                    updateLegalDestinationsFor(clickedPos);
                    repaint();
                } else {
                    // Destination square chosen: perform move
                    Position from = selectedPosition;
                    Position to = clickedPos;
                    selectedPosition = null;
                    legalDestinations.clear();

                    pendingMoveFrom = from;
                    pendingMoveTo = to;
                    try {
                        controller.handle(new PlayerAction.Move(from.toString() + to.toString()));
                    } finally {
                        pendingMoveFrom = null;
                        pendingMoveTo = null;
                    }
                }
            }
        }

        private void updateLegalDestinationsFor(Position pos) {
            legalDestinations.clear();
            if (currentGame != null) {
                for (Move move : currentGame.legalMoves()) {
                    if (move.from().equals(pos) && !legalDestinations.contains(move.to())) {
                        legalDestinations.add(move.to());
                    }
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            drawBoardBackgroundAndCoordinates(g2);
            drawBoardSquares(g2);
            drawHighlights(g2);
            drawPieces(g2);

            g2.dispose();
        }

        private void drawBoardBackgroundAndCoordinates(Graphics2D g2) {
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            FontMetrics fm = g2.getFontMetrics();

            // Draw rank labels (1-8) along left and right
            for (int r = 0; r < Position.BOARD_SIZE; r++) {
                String rankStr = String.valueOf(8 - r);
                int y = BOARD_MARGIN + r * SQUARE_SIZE + (SQUARE_SIZE + fm.getAscent()) / 2 - 2;

                g2.setColor(new java.awt.Color(160, 160, 160));
                int xLeft = (BOARD_MARGIN - fm.stringWidth(rankStr)) / 2;
                g2.drawString(rankStr, xLeft, y);

                int xRight = BOARD_MARGIN + SQUARE_SIZE * Position.BOARD_SIZE + (BOARD_MARGIN - fm.stringWidth(rankStr)) / 2;
                g2.drawString(rankStr, xRight, y);
            }

            // Draw file labels (a-h) along top and bottom
            for (int f = 0; f < Position.BOARD_SIZE; f++) {
                String fileStr = String.valueOf((char) ('a' + f));
                int x = BOARD_MARGIN + f * SQUARE_SIZE + (SQUARE_SIZE - fm.stringWidth(fileStr)) / 2;

                g2.setColor(new java.awt.Color(160, 160, 160));
                int yTop = (BOARD_MARGIN + fm.getAscent()) / 2 - 1;
                g2.drawString(fileStr, x, yTop);

                int yBottom = BOARD_MARGIN + SQUARE_SIZE * Position.BOARD_SIZE + (BOARD_MARGIN + fm.getAscent()) / 2;
                g2.drawString(fileStr, x, yBottom);
            }

            // Border around board
            g2.setColor(new java.awt.Color(70, 74, 82));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRect(BOARD_MARGIN - 1, BOARD_MARGIN - 1, SQUARE_SIZE * Position.BOARD_SIZE + 2, SQUARE_SIZE * Position.BOARD_SIZE + 2);
        }

        private void drawBoardSquares(Graphics2D g2) {
            for (int row = 0; row < Position.BOARD_SIZE; row++) {
                for (int col = 0; col < Position.BOARD_SIZE; col++) {
                    int x = BOARD_MARGIN + col * SQUARE_SIZE;
                    int y = BOARD_MARGIN + row * SQUARE_SIZE;
                    boolean isLight = ((row + col) % 2 == 0);
                    g2.setColor(isLight ? COLOR_LIGHT_SQUARE : COLOR_DARK_SQUARE);
                    g2.fillRect(x, y, SQUARE_SIZE, SQUARE_SIZE);
                }
            }
        }

        private void drawHighlights(Graphics2D g2) {
            if (currentGame == null) {
                return;
            }

            // 1. King in Check highlight
            GameStatus status = currentGame.status();
            if (status == GameStatus.CHECK || status == GameStatus.CHECKMATE) {
                Position kingPos = findKingPosition(currentGame.sideToMove());
                if (kingPos != null) {
                    int col = kingPos.file();
                    int row = 7 - kingPos.rank();
                    int kx = BOARD_MARGIN + col * SQUARE_SIZE;
                    int ky = BOARD_MARGIN + row * SQUARE_SIZE;

                    g2.setColor(COLOR_CHECK_GLOW);
                    g2.fillRect(kx, ky, SQUARE_SIZE, SQUARE_SIZE);

                    g2.setColor(new java.awt.Color(200, 20, 20, 240));
                    g2.setStroke(new BasicStroke(3.0f));
                    g2.drawRect(kx + 1, ky + 1, SQUARE_SIZE - 2, SQUARE_SIZE - 2);
                }
            }

            // 2. Selected Square highlight
            if (selectedPosition != null) {
                int col = selectedPosition.file();
                int row = 7 - selectedPosition.rank();
                int sx = BOARD_MARGIN + col * SQUARE_SIZE;
                int sy = BOARD_MARGIN + row * SQUARE_SIZE;
                boolean isLight = ((row + col) % 2 == 0);
                g2.setColor(isLight ? COLOR_SELECTED_LIGHT : COLOR_SELECTED_DARK);
                g2.fillRect(sx, sy, SQUARE_SIZE, SQUARE_SIZE);

                g2.setColor(new java.awt.Color(130, 140, 40));
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawRect(sx + 1, sy + 1, SQUARE_SIZE - 2, SQUARE_SIZE - 2);
            }

            // 3. Legal move destination indicators
            for (Position dest : legalDestinations) {
                int col = dest.file();
                int row = 7 - dest.rank();
                int dx = BOARD_MARGIN + col * SQUARE_SIZE;
                int dy = BOARD_MARGIN + row * SQUARE_SIZE;

                Piece targetPiece = currentGame.board().pieceAt(dest);
                if (targetPiece == null) {
                    // Small circular dot in center for empty square
                    int dotRadius = 11;
                    int centerX = dx + SQUARE_SIZE / 2;
                    int centerY = dy + SQUARE_SIZE / 2;
                    g2.setColor(COLOR_DEST_DOT);
                    g2.fillOval(centerX - dotRadius, centerY - dotRadius, dotRadius * 2, dotRadius * 2);
                } else {
                    // Ring / corners for capturing enemy piece
                    g2.setColor(new java.awt.Color(180, 40, 40, 160));
                    g2.setStroke(new BasicStroke(4.0f));
                    g2.drawOval(dx + 6, dy + 6, SQUARE_SIZE - 12, SQUARE_SIZE - 12);
                }
            }
        }

        private Position findKingPosition(Color color) {
            if (currentGame == null) {
                return null;
            }
            for (int f = 0; f < Position.BOARD_SIZE; f++) {
                for (int r = 0; r < Position.BOARD_SIZE; r++) {
                    Position p = new Position(f, r);
                    Piece piece = currentGame.board().pieceAt(p);
                    if (piece != null && piece.type() == PieceType.KING && piece.color() == color) {
                        return p;
                    }
                }
            }
            return null;
        }

        private void drawPieces(Graphics2D g2) {
            if (currentGame == null) {
                return;
            }

            g2.setFont(pieceFont);

            // Draw all board pieces (except the arriving animated piece at animTo while animating)
            for (int col = 0; col < Position.BOARD_SIZE; col++) {
                for (int row = 0; row < Position.BOARD_SIZE; row++) {
                    int rank = 7 - row;
                    Position pos = new Position(col, rank);

                    if (isAnimating && pos.equals(animTo)) {
                        continue;
                    }

                    Piece piece = currentGame.board().pieceAt(pos);
                    if (piece != null) {
                        int x = BOARD_MARGIN + col * SQUARE_SIZE;
                        int y = BOARD_MARGIN + row * SQUARE_SIZE;
                        drawSinglePiece(g2, piece, x, y);
                    }
                }
            }

            // Draw the animated piece gliding smoothly across the board
            if (isAnimating && animPiece != null && animFrom != null && animTo != null) {
                int startX = BOARD_MARGIN + animFrom.file() * SQUARE_SIZE;
                int startY = BOARD_MARGIN + (7 - animFrom.rank()) * SQUARE_SIZE;
                int endX = BOARD_MARGIN + animTo.file() * SQUARE_SIZE;
                int endY = BOARD_MARGIN + (7 - animTo.rank()) * SQUARE_SIZE;

                // Smoothstep interpolation (3t^2 - 2t^3)
                float t = animProgress;
                float smoothT = t * t * (3.0f - 2.0f * t);

                double currentX = startX + (endX - startX) * smoothT;
                double currentY = startY + (endY - startY) * smoothT;

                // Draw floating piece drop shadow
                drawPieceShadow(g2, animPiece, (int) Math.round(currentX) + 4, (int) Math.round(currentY) + 5);
                // Draw piece
                drawSinglePiece(g2, animPiece, (int) Math.round(currentX), (int) Math.round(currentY));
            }
        }

        private void drawSinglePiece(Graphics2D g2, Piece piece, int squareX, int squareY) {
            String solidGlyph = getSolidGlyph(piece.type());
            String outlineGlyph = getOutlineGlyph(piece.type());

            FontMetrics fm = g2.getFontMetrics(pieceFont);
            int glyphWidth = fm.stringWidth(solidGlyph);
            int gx = squareX + (SQUARE_SIZE - glyphWidth) / 2;
            int gy = squareY + (SQUARE_SIZE - fm.getHeight()) / 2 + fm.getAscent() - 2;

            if (piece.color() == Color.WHITE) {
                // White piece: solid white interior + crisp dark outline
                g2.setColor(java.awt.Color.WHITE);
                g2.drawString(solidGlyph, gx, gy);

                g2.setColor(new java.awt.Color(25, 25, 25));
                g2.drawString(outlineGlyph, gx, gy);
            } else {
                // Black piece: subtle light halo + solid dark interior
                g2.setColor(new java.awt.Color(255, 255, 255, 175));
                g2.drawString(outlineGlyph, gx, gy);

                g2.setColor(new java.awt.Color(30, 30, 30));
                g2.drawString(solidGlyph, gx, gy);
            }
        }

        private void drawPieceShadow(Graphics2D g2, Piece piece, int squareX, int squareY) {
            String solidGlyph = getSolidGlyph(piece.type());
            FontMetrics fm = g2.getFontMetrics(pieceFont);
            int glyphWidth = fm.stringWidth(solidGlyph);
            int gx = squareX + (SQUARE_SIZE - glyphWidth) / 2;
            int gy = squareY + (SQUARE_SIZE - fm.getHeight()) / 2 + fm.getAscent() - 2;

            g2.setColor(new java.awt.Color(0, 0, 0, 70));
            g2.drawString(solidGlyph, gx, gy);
        }

        private static String getSolidGlyph(PieceType type) {
            return switch (type) {
                case KING -> "\u265A";
                case QUEEN -> "\u265B";
                case ROOK -> "\u265C";
                case BISHOP -> "\u265D";
                case KNIGHT -> "\u265E";
                case PAWN -> "\u265F";
            };
        }

        private static String getOutlineGlyph(PieceType type) {
            return switch (type) {
                case KING -> "\u2654";
                case QUEEN -> "\u2655";
                case ROOK -> "\u2656";
                case BISHOP -> "\u2657";
                case KNIGHT -> "\u2658";
                case PAWN -> "\u2659";
            };
        }
    }
}
