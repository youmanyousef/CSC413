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
import java.util.List;

/**
 * A 2D graphical chess board view implemented using Java Swing.
 *
 * <p>Players interact by clicking a piece to select it and then clicking a
 * destination square to move it. Moves and actions are delegated to
 * {@link GameController#handle(PlayerAction)}.
 *
 * <p>Features include turn indicator, game status / check alert banner,
 * legal move indicators, undo and utility controls, and smooth piece movement animations.
 */
public class SwingView implements BoardView {

    private static final int SQUARE_SIZE = 75;
    private static final int BOARD_MARGIN = 26;
    private static final int BOARD_PIXELS = SQUARE_SIZE * Position.BOARD_SIZE;
    private static final int PANEL_PIXELS = BOARD_PIXELS + BOARD_MARGIN * 2;
    private static final int ANIMATION_DURATION_MS = 220; // smooth and responsive

    // Board & UI colors
    private static final java.awt.Color LIGHT_SQUARE_COLOR = new java.awt.Color(240, 217, 181);
    private static final java.awt.Color DARK_SQUARE_COLOR = new java.awt.Color(181, 136, 99);
    private static final java.awt.Color BORDER_COLOR = new java.awt.Color(48, 46, 43);
    private static final java.awt.Color COORD_COLOR = new java.awt.Color(185, 185, 185);

    private static final java.awt.Color SELECTED_COLOR = new java.awt.Color(246, 236, 105, 190);
    private static final java.awt.Color SELECTED_BORDER_COLOR = new java.awt.Color(255, 215, 0);
    private static final java.awt.Color LAST_MOVE_COLOR = new java.awt.Color(245, 246, 130, 130);
    private static final java.awt.Color CHECK_COLOR = new java.awt.Color(230, 50, 50, 190);
    private static final java.awt.Color LEGAL_DOT_COLOR = new java.awt.Color(25, 95, 25, 120);
    private static final java.awt.Color LEGAL_CAPTURE_COLOR = new java.awt.Color(220, 50, 50, 170);

    private final JFrame frame;
    private final BoardPanel boardPanel;
    private final JLabel turnIndicatorDot;
    private final JLabel turnLabel;
    private final JLabel statusLabel;
    private final JPanel alertBanner;
    private final JLabel alertBannerLabel;
    private final JLabel messageLabel;

    private GameController controller;
    private Game game;
    private Position selectedSquare;

    // Smooth movement animation state
    private Move animatingMove;
    private Piece animatingPiece;
    private long animationStartTime;
    private float animationProgress = 1.0f; // 0.0 to 1.0
    private Timer animationTimer;
    private int previousHistoryCount = 0;

    public SwingView() {
        frame = new JFrame("CSC 413 Chess — 2D View");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Header Panel: Turn indicator and Game Status
        JPanel headerPanel = new JPanel(new BorderLayout(12, 0));
        headerPanel.setBackground(new java.awt.Color(38, 36, 33));
        headerPanel.setBorder(new EmptyBorder(10, 16, 10, 16));

        JPanel turnBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        turnBox.setOpaque(false);

        turnIndicatorDot = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean isWhite = game == null || game.sideToMove() == Color.WHITE;
                g2.setColor(isWhite ? java.awt.Color.WHITE : new java.awt.Color(30, 30, 30));
                g2.fillOval(1, 1, 16, 16);
                g2.setColor(isWhite ? new java.awt.Color(160, 160, 160) : java.awt.Color.WHITE);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(1, 1, 16, 16);
                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(20, 20);
            }
        };

        turnLabel = new JLabel("Turn: WHITE");
        turnLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        turnLabel.setForeground(java.awt.Color.WHITE);

        turnBox.add(turnIndicatorDot);
        turnBox.add(turnLabel);

        statusLabel = new JLabel("Game in progress");
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        statusLabel.setForeground(new java.awt.Color(207, 216, 220));
        statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        headerPanel.add(turnBox, BorderLayout.WEST);
        headerPanel.add(statusLabel, BorderLayout.EAST);

        // Alert Banner: Prominently warns about CHECK, CHECKMATE, or STALEMATE
        alertBanner = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        alertBanner.setBackground(new java.awt.Color(211, 47, 47));
        alertBanner.setVisible(false);

        alertBannerLabel = new JLabel("");
        alertBannerLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        alertBannerLabel.setForeground(java.awt.Color.WHITE);
        alertBanner.add(alertBannerLabel);

        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.add(headerPanel, BorderLayout.NORTH);
        topContainer.add(alertBanner, BorderLayout.SOUTH);

        // Center Panel: Chess Board
        boardPanel = new BoardPanel();

        // Footer Panel: User feedback message and action buttons
        JPanel footerPanel = new JPanel(new BorderLayout(10, 8));
        footerPanel.setBackground(new java.awt.Color(38, 36, 33));
        footerPanel.setBorder(new EmptyBorder(10, 16, 12, 16));

        messageLabel = new JLabel("Welcome to Chess! Click a piece to select, then click a square to move.");
        messageLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        messageLabel.setForeground(new java.awt.Color(236, 239, 241));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonPanel.setOpaque(false);

        JButton undoButton = new JButton("Undo");
        undoButton.setFocusPainted(false);
        undoButton.setToolTipText("Take back the last move");
        undoButton.addActionListener(e -> {
            if (controller != null) {
                stopAnimation();
                selectedSquare = null;
                controller.handle(new PlayerAction.Undo());
            }
        });

        JButton movesButton = new JButton("Legal Moves");
        movesButton.setFocusPainted(false);
        movesButton.setToolTipText("List legal moves in the message bar");
        movesButton.addActionListener(e -> {
            if (controller != null) {
                controller.handle(new PlayerAction.ShowMoves());
            }
        });

        JButton quitButton = new JButton("Quit");
        quitButton.setFocusPainted(false);
        quitButton.setToolTipText("Exit the game");
        quitButton.addActionListener(e -> {
            if (controller != null) {
                stopAnimation();
                controller.handle(new PlayerAction.Quit());
            }
        });

        buttonPanel.add(undoButton);
        buttonPanel.add(movesButton);
        buttonPanel.add(quitButton);

        footerPanel.add(messageLabel, BorderLayout.NORTH);
        footerPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Assemble Window Layout
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.add(topContainer, BorderLayout.NORTH);
        mainContainer.add(boardPanel, BorderLayout.CENTER);
        mainContainer.add(footerPanel, BorderLayout.SOUTH);

        frame.setContentPane(mainContainer);
        frame.setResizable(false);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopAnimation();
                if (controller != null) {
                    controller.handle(new PlayerAction.Quit());
                }
            }
        });
    }

    @Override
    public void render(Game game) {
        this.game = game;
        runOnEdt(() -> {
            List<Move> history = game.history();
            int currentHistoryCount = history.size();

            // Detect forward move and initiate smooth movement animation
            if (currentHistoryCount == previousHistoryCount + 1 && !history.isEmpty()) {
                Move lastMove = history.getLast();
                startMoveAnimation(lastMove);
            } else {
                stopAnimation();
            }
            previousHistoryCount = currentHistoryCount;

            updateStatusHeader();
            boardPanel.repaint();
        });
    }

    @Override
    public void showMessage(String message) {
        runOnEdt(() -> {
            if (message != null && !message.isBlank()) {
                messageLabel.setText(message);
            }
        });
    }

    @Override
    public void start(GameController controller) {
        this.controller = controller;
        runOnEdt(() -> {
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    @Override
    public PieceType promotionChoice(Game game) {
        if (GraphicsEnvironment.isHeadless()) {
            return PieceType.QUEEN;
        }
        String[] options = {"Queen", "Rook", "Bishop", "Knight"};
        int choice = JOptionPane.showOptionDialog(
                frame,
                "Select piece to promote pawn to:",
                "Pawn Promotion",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );
        return switch (choice) {
            case 1 -> PieceType.ROOK;
            case 2 -> PieceType.BISHOP;
            case 3 -> PieceType.KNIGHT;
            default -> PieceType.QUEEN;
        };
    }

    @Override
    public void close() {
        stopAnimation();
        runOnEdt(frame::dispose);
    }

    private void updateStatusHeader() {
        if (game == null) {
            return;
        }

        Color side = game.sideToMove();
        turnLabel.setText("Turn: " + side);
        turnIndicatorDot.repaint();

        GameStatus status = game.status();
        switch (status) {
            case IN_PROGRESS -> {
                statusLabel.setText("In Progress");
                statusLabel.setForeground(new java.awt.Color(207, 216, 220));
                alertBanner.setVisible(false);
            }
            case CHECK -> {
                statusLabel.setText(side + " is in CHECK!");
                statusLabel.setForeground(new java.awt.Color(255, 82, 82));
                alertBanner.setBackground(new java.awt.Color(211, 47, 47));
                alertBannerLabel.setText("⚠ ALERT: " + side + " KING IS IN CHECK!");
                alertBanner.setVisible(true);
            }
            case CHECKMATE -> {
                statusLabel.setText("Checkmate! " + side.opposite() + " wins!");
                statusLabel.setForeground(new java.awt.Color(105, 240, 174));
                alertBanner.setBackground(new java.awt.Color(46, 125, 50));
                alertBannerLabel.setText("★ CHECKMATE! " + side.opposite() + " WINS! ★");
                alertBanner.setVisible(true);
            }
            case STALEMATE -> {
                statusLabel.setText("Stalemate! Draw.");
                statusLabel.setForeground(new java.awt.Color(255, 171, 64));
                alertBanner.setBackground(new java.awt.Color(230, 124, 11));
                alertBannerLabel.setText("Draw by Stalemate.");
                alertBanner.setVisible(true);
            }
        }
        frame.revalidate();
    }

    private void startMoveAnimation(Move move) {
        stopAnimation();
        animatingMove = move;
        // Piece standing on destination square after the move
        Piece pieceOnTo = game.board().pieceAt(move.to());
        animatingPiece = pieceOnTo != null ? pieceOnTo : move.moved();
        animationStartTime = System.currentTimeMillis();
        animationProgress = 0.0f;

        animationTimer = new Timer(15, e -> {
            long elapsed = System.currentTimeMillis() - animationStartTime;
            float t = Math.min(1.0f, (float) elapsed / ANIMATION_DURATION_MS);
            // Smoothstep curve for silky easing: 3t^2 - 2t^3
            animationProgress = t * t * (3.0f - 2.0f * t);
            boardPanel.repaint();

            if (t >= 1.0f) {
                stopAnimation();
                boardPanel.repaint();
            }
        });
        animationTimer.start();
    }

    private void stopAnimation() {
        if (animationTimer != null) {
            animationTimer.stop();
            animationTimer = null;
        }
        animatingMove = null;
        animatingPiece = null;
        animationProgress = 1.0f;
    }

    private void runOnEdt(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    /**
     * Inner panel responsible for drawing the 8x8 board, pieces, coordinates,
     * highlights, and animations, and handling mouse clicks.
     */
    private class BoardPanel extends JPanel {

        private final Font coordFont = new Font(Font.SANS_SERIF, Font.BOLD, 12);
        private Font chessFont;

        BoardPanel() {
            setPreferredSize(new Dimension(PANEL_PIXELS, PANEL_PIXELS));
            setBackground(BORDER_COLOR);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    handleBoardClick(e.getX(), e.getY());
                }
            });
        }

        private Font getChessFont(int size) {
            if (chessFont != null && chessFont.getSize() == size) {
                return chessFont;
            }
            String[] preferredFonts = {"Segoe UI Symbol", "DejaVu Sans", "Arial Unicode MS", Font.SANS_SERIF};
            for (String fontName : preferredFonts) {
                Font f = new Font(fontName, Font.PLAIN, size);
                if (f.canDisplay('\u2654') && f.canDisplay('\u265A')) {
                    chessFont = f;
                    return chessFont;
                }
            }
            chessFont = new Font(Font.SANS_SERIF, Font.PLAIN, size);
            return chessFont;
        }

        private void handleBoardClick(int mouseX, int mouseY) {
            if (game == null || controller == null) {
                return;
            }

            // Immediately finish any active animation before handling input
            if (animationTimer != null && animationTimer.isRunning()) {
                stopAnimation();
                repaint();
            }

            if (game.isGameOver()) {
                showMessage("Game is over (" + game.status() + ").");
                return;
            }

            int col = (mouseX - BOARD_MARGIN) / SQUARE_SIZE;
            int row = (mouseY - BOARD_MARGIN) / SQUARE_SIZE;

            if (col < 0 || col >= Position.BOARD_SIZE || row < 0 || row >= Position.BOARD_SIZE) {
                return;
            }

            int file = col;
            int rank = Position.BOARD_SIZE - 1 - row;
            Position clickedPos = new Position(file, rank);

            if (selectedSquare == null) {
                Piece piece = game.board().pieceAt(clickedPos);
                if (piece == null) {
                    showMessage("Square " + clickedPos + " is empty. Click a piece to move.");
                } else if (piece.color() != game.sideToMove()) {
                    showMessage("It is " + game.sideToMove() + "'s turn. Please select a " + game.sideToMove() + " piece.");
                } else {
                    selectedSquare = clickedPos;
                    showMessage("Selected " + piece.color() + " " + piece.type() + " at " + clickedPos + ". Click destination.");
                    repaint();
                }
            } else {
                if (clickedPos.equals(selectedSquare)) {
                    // Clicked same piece: deselect
                    selectedSquare = null;
                    showMessage("Selection cleared.");
                    repaint();
                } else {
                    Piece piece = game.board().pieceAt(clickedPos);
                    if (piece != null && piece.color() == game.sideToMove()) {
                        // Switch selection to another own piece
                        selectedSquare = clickedPos;
                        showMessage("Selected " + piece.color() + " " + piece.type() + " at " + clickedPos + ". Click destination.");
                        repaint();
                    } else {
                        // Attempt to move from selectedSquare to clickedPos
                        Position from = selectedSquare;
                        Position to = clickedPos;
                        selectedSquare = null;
                        repaint();
                        controller.handle(new PlayerAction.Move(from.toString() + to.toString()));
                    }
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            drawCoordinates(g2);
            drawSquares(g2);
            drawHighlights(g2);
            drawPieces(g2);
            drawLegalMoveIndicators(g2);
            drawAnimatingPiece(g2);
        }

        private void drawCoordinates(Graphics2D g2) {
            g2.setFont(coordFont);
            g2.setColor(COORD_COLOR);
            FontMetrics fm = g2.getFontMetrics();

            // Files a-h along top and bottom
            for (int file = 0; file < Position.BOARD_SIZE; file++) {
                char fileChar = (char) ('a' + file);
                String str = String.valueOf(fileChar);
                int charWidth = fm.stringWidth(str);
                int x = BOARD_MARGIN + file * SQUARE_SIZE + (SQUARE_SIZE - charWidth) / 2;

                int yTop = (BOARD_MARGIN - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(str, x, yTop);

                int yBottom = BOARD_MARGIN + BOARD_PIXELS + (BOARD_MARGIN - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(str, x, yBottom);
            }

            // Ranks 1-8 along left and right
            for (int rank = 0; rank < Position.BOARD_SIZE; rank++) {
                int row = Position.BOARD_SIZE - 1 - rank;
                String str = String.valueOf(rank + 1);
                int charWidth = fm.stringWidth(str);
                int y = BOARD_MARGIN + row * SQUARE_SIZE + (SQUARE_SIZE - fm.getHeight()) / 2 + fm.getAscent();

                int xLeft = (BOARD_MARGIN - charWidth) / 2;
                g2.drawString(str, xLeft, y);

                int xRight = BOARD_MARGIN + BOARD_PIXELS + (BOARD_MARGIN - charWidth) / 2;
                g2.drawString(str, xRight, y);
            }
        }

        private void drawSquares(Graphics2D g2) {
            for (int row = 0; row < Position.BOARD_SIZE; row++) {
                for (int col = 0; col < Position.BOARD_SIZE; col++) {
                    int file = col;
                    int rank = Position.BOARD_SIZE - 1 - row;
                    boolean isLight = (file + rank) % 2 != 0;

                    int x = BOARD_MARGIN + col * SQUARE_SIZE;
                    int y = BOARD_MARGIN + row * SQUARE_SIZE;

                    g2.setColor(isLight ? LIGHT_SQUARE_COLOR : DARK_SQUARE_COLOR);
                    g2.fillRect(x, y, SQUARE_SIZE, SQUARE_SIZE);
                }
            }
        }

        private void drawHighlights(Graphics2D g2) {
            if (game == null) {
                return;
            }

            // Highlight last move from history
            List<Move> history = game.history();
            if (!history.isEmpty()) {
                Move last = history.getLast();
                highlightSquare(g2, last.from(), LAST_MOVE_COLOR);
                highlightSquare(g2, last.to(), LAST_MOVE_COLOR);
            }

            // Highlight King in check
            GameStatus status = game.status();
            if (status == GameStatus.CHECK || status == GameStatus.CHECKMATE) {
                Color inCheckSide = game.sideToMove();
                for (Position pos : game.board().positionsOf(inCheckSide)) {
                    Piece piece = game.board().pieceAt(pos);
                    if (piece != null && piece.type() == PieceType.KING) {
                        highlightSquare(g2, pos, CHECK_COLOR);
                        // Extra red alert border on the king
                        int col = pos.file();
                        int row = Position.BOARD_SIZE - 1 - pos.rank();
                        int x = BOARD_MARGIN + col * SQUARE_SIZE;
                        int y = BOARD_MARGIN + row * SQUARE_SIZE;
                        g2.setColor(new java.awt.Color(255, 30, 30));
                        g2.setStroke(new BasicStroke(3.0f));
                        g2.drawRect(x + 1, y + 1, SQUARE_SIZE - 3, SQUARE_SIZE - 3);
                        break;
                    }
                }
            }

            // Highlight selected square
            if (selectedSquare != null) {
                highlightSquare(g2, selectedSquare, SELECTED_COLOR);
                int col = selectedSquare.file();
                int row = Position.BOARD_SIZE - 1 - selectedSquare.rank();
                int x = BOARD_MARGIN + col * SQUARE_SIZE;
                int y = BOARD_MARGIN + row * SQUARE_SIZE;

                g2.setColor(SELECTED_BORDER_COLOR);
                g2.setStroke(new BasicStroke(3.0f));
                g2.drawRect(x + 1, y + 1, SQUARE_SIZE - 2, SQUARE_SIZE - 2);
            }
        }

        private void highlightSquare(Graphics2D g2, Position pos, java.awt.Color color) {
            int col = pos.file();
            int row = Position.BOARD_SIZE - 1 - pos.rank();
            int x = BOARD_MARGIN + col * SQUARE_SIZE;
            int y = BOARD_MARGIN + row * SQUARE_SIZE;

            g2.setColor(color);
            g2.fillRect(x, y, SQUARE_SIZE, SQUARE_SIZE);
        }

        private void drawPieces(Graphics2D g2) {
            if (game == null) {
                return;
            }

            int fontSize = (int) (SQUARE_SIZE * 0.72);
            Font font = getChessFont(fontSize);
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics(font);

            boolean canDisplayFigures = font.canDisplay('\u2654');

            for (int file = 0; file < Position.BOARD_SIZE; file++) {
                for (int rank = 0; rank < Position.BOARD_SIZE; rank++) {
                    Position pos = new Position(file, rank);

                    // If a piece is actively animating towards this square, skip drawing it statically
                    if (animatingMove != null && pos.equals(animatingMove.to())) {
                        continue;
                    }

                    Piece piece = game.board().pieceAt(pos);
                    if (piece == null) {
                        continue;
                    }

                    int col = file;
                    int row = Position.BOARD_SIZE - 1 - rank;
                    int x = BOARD_MARGIN + col * SQUARE_SIZE;
                    int y = BOARD_MARGIN + row * SQUARE_SIZE;

                    drawPieceGlyph(g2, piece, x, y, font, fm, canDisplayFigures);
                }
            }
        }

        private void drawAnimatingPiece(Graphics2D g2) {
            if (animatingMove == null || animatingPiece == null) {
                return;
            }

            int fontSize = (int) (SQUARE_SIZE * 0.72);
            Font font = getChessFont(fontSize);
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics(font);
            boolean canDisplayFigures = font.canDisplay('\u2654');

            int fromCol = animatingMove.from().file();
            int fromRow = Position.BOARD_SIZE - 1 - animatingMove.from().rank();
            int fromX = BOARD_MARGIN + fromCol * SQUARE_SIZE;
            int fromY = BOARD_MARGIN + fromRow * SQUARE_SIZE;

            int toCol = animatingMove.to().file();
            int toRow = Position.BOARD_SIZE - 1 - animatingMove.to().rank();
            int toX = BOARD_MARGIN + toCol * SQUARE_SIZE;
            int toY = BOARD_MARGIN + toRow * SQUARE_SIZE;

            // Interpolate position based on smooth animation progress
            int curX = Math.round(fromX + (toX - fromX) * animationProgress);
            int curY = Math.round(fromY + (toY - fromY) * animationProgress);

            // Subtle drop-shadow to give elevation effect while animating
            drawPieceShadow(g2, animatingPiece, curX + 4, curY + 4, fm, canDisplayFigures);

            // Draw the moving piece
            drawPieceGlyph(g2, animatingPiece, curX, curY, font, fm, canDisplayFigures);
        }

        private void drawPieceShadow(Graphics2D g2, Piece piece, int x, int y, FontMetrics fm, boolean canDisplayFigures) {
            String glyph = canDisplayFigures
                    ? PieceGlyphs.FIGURES.glyphFor(piece)
                    : String.valueOf(Character.toUpperCase(piece.symbol()));
            int textX = x + (SQUARE_SIZE - fm.stringWidth(glyph)) / 2;
            int textY = y + (SQUARE_SIZE - fm.getHeight()) / 2 + fm.getAscent();

            g2.setColor(new java.awt.Color(0, 0, 0, 70));
            g2.drawString(glyph, textX, textY);
        }

        private void drawPieceGlyph(Graphics2D g2, Piece piece, int x, int y, Font font, FontMetrics fm, boolean canDisplayFigures) {
            String glyph = canDisplayFigures
                    ? PieceGlyphs.FIGURES.glyphFor(piece)
                    : String.valueOf(Character.toUpperCase(piece.symbol()));

            int textX = x + (SQUARE_SIZE - fm.stringWidth(glyph)) / 2;
            int textY = y + (SQUARE_SIZE - fm.getHeight()) / 2 + fm.getAscent();

            // High-contrast rendering: primary piece color + contrasting outline halo
            boolean isWhite = piece.color() == Color.WHITE;
            java.awt.Color fillColor = isWhite
                    ? new java.awt.Color(255, 255, 255)
                    : new java.awt.Color(25, 25, 25);
            java.awt.Color outlineColor = isWhite
                    ? new java.awt.Color(20, 20, 20, 220)
                    : new java.awt.Color(245, 245, 245, 220);

            // 8-direction outline halo ensures readability across both light and dark squares
            g2.setColor(outlineColor);
            g2.drawString(glyph, textX - 1, textY - 1);
            g2.drawString(glyph, textX + 1, textY - 1);
            g2.drawString(glyph, textX - 1, textY + 1);
            g2.drawString(glyph, textX + 1, textY + 1);
            g2.drawString(glyph, textX, textY - 1);
            g2.drawString(glyph, textX, textY + 1);
            g2.drawString(glyph, textX - 1, textY);
            g2.drawString(glyph, textX + 1, textY);

            // Primary piece fill
            g2.setColor(fillColor);
            g2.drawString(glyph, textX, textY);
        }

        private void drawLegalMoveIndicators(Graphics2D g2) {
            if (selectedSquare == null || game == null) {
                return;
            }

            List<Move> legalMoves = game.legalMoves();
            for (Move move : legalMoves) {
                if (!move.from().equals(selectedSquare)) {
                    continue;
                }

                Position to = move.to();
                int col = to.file();
                int row = Position.BOARD_SIZE - 1 - to.rank();
                int x = BOARD_MARGIN + col * SQUARE_SIZE;
                int y = BOARD_MARGIN + row * SQUARE_SIZE;

                Piece targetPiece = game.board().pieceAt(to);
                if (targetPiece == null) {
                    // Empty square: central dot
                    int radius = SQUARE_SIZE / 6;
                    int cx = x + SQUARE_SIZE / 2;
                    int cy = y + SQUARE_SIZE / 2;

                    g2.setColor(LEGAL_DOT_COLOR);
                    g2.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
                } else {
                    // Capture square: circular target ring
                    g2.setColor(LEGAL_CAPTURE_COLOR);
                    g2.setStroke(new BasicStroke(3.5f));
                    g2.drawOval(x + 5, y + 5, SQUARE_SIZE - 10, SQUARE_SIZE - 10);
                }
            }
        }
    }
}
