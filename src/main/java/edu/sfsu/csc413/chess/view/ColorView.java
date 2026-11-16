package edu.sfsu.csc413.chess.view;

import edu.sfsu.csc413.chess.engine.Game;
import edu.sfsu.csc413.chess.engine.GameStatus;
import edu.sfsu.csc413.chess.model.Color;
import edu.sfsu.csc413.chess.model.Piece;
import edu.sfsu.csc413.chess.model.PieceType;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * A terminal view of the board styled with ANSI colors.
 *
 * <p>Pieces are distinguished by their piece symbol, with the background
 * matched to their side (white background for white pieces, black background
 * for black pieces) and contrasting character text. When a king is in check,
 * its background becomes red. Empty spaces on the board are colored light
 * brown. Alerts such as check have a red background, winner announcements have
 * a green background, and all other messages have a gray background. The screen
 * is cleared after every step in the game loop.
 *
 * <p>This view was created with the help of Gemini.
 */
public class ColorView implements BoardView {

    // ANSI escape codes
    private static final String RESET = "\u001B[0m";

    // Piece & cell styles
    private static final String BLACK_PIECE_STYLE = "\u001B[40m\u001B[97m";   // Black bg, bright white text
    private static final String WHITE_PIECE_STYLE = "\u001B[107m\u001B[30m";  // White bg, black text
    private static final String EMPTY_SPACE_STYLE = "\u001B[48;5;180m";        // Light brown (tan) bg

    // Alert backgrounds
    private static final String RED_ALERT_STYLE   = "\u001B[41m\u001B[97m";   // Red bg, white text
    private static final String GREEN_ALERT_STYLE = "\u001B[42m\u001B[97m";   // Green bg, white text
    private static final String GRAY_ALERT_STYLE  = "\u001B[100m\u001B[97m";  // Gray bg, white text

    private final PrintStream out;
    private final Scanner in;
    private final Color perspective;

    private final List<String> pendingMessages = new ArrayList<>();
    private boolean renderedSinceLastAction = false;

    /** A color view reading from standard input, drawn from white's side. */
    public ColorView() {
        this(System.out, new Scanner(System.in), Color.WHITE);
    }

    /** Injectable streams and a choice of which side to draw the board from. */
    public ColorView(PrintStream out, Scanner in, Color perspective) {
        this.out = out;
        this.in = in;
        this.perspective = perspective;
    }

    /** Clears the terminal screen and positions the cursor at the top-left. */
    private void clearScreen() {
        out.print("\u001B[H\u001B[2J");
        out.flush();
    }

    /**
     * Determines the display glyph and colors for a square:
     * - Empty space: light brown background
     * - King in check: red background
     * - Normal piece: background matching its side (white or black) with contrasting symbol
     */
    private String glyphFor(Piece piece, Color checkedSide) {
        if (piece == null) {
            return EMPTY_SPACE_STYLE + " " + RESET;
        }
        if (checkedSide != null && piece.type() == PieceType.KING && piece.color() == checkedSide) {
            String fg = (piece.color() == Color.WHITE) ? "\u001B[30m" : "\u001B[97m";
            return "\u001B[41m" + fg + piece.symbol() + RESET;
        }
        if (piece.color() == Color.WHITE) {
            return WHITE_PIECE_STYLE + piece.symbol() + RESET;
        } else {
            return BLACK_PIECE_STYLE + piece.symbol() + RESET;
        }
    }

    /**
     * Formats an alert or message with appropriate background color:
     * - Check alerts: red background
     * - Announcing winner: green background
     * - Everything else: gray background
     */
    private String styleMessage(String message) {
        if (message == null || message.isBlank()) {
            return "";
        }
        String lower = message.toLowerCase();
        String style;
        if (lower.contains("checkmate") || lower.contains("wins") || lower.contains("winner")) {
            style = GREEN_ALERT_STYLE;
        } else if (lower.contains("check")) {
            style = RED_ALERT_STYLE;
        } else {
            style = GRAY_ALERT_STYLE;
        }
        return style + " " + message + " " + RESET;
    }

    @Override
    public void render(Game game) {
        clearScreen();
        out.println();
        boolean inCheck = game.status() == GameStatus.CHECK || game.status() == GameStatus.CHECKMATE;
        Color checkedSide = inCheck ? game.sideToMove() : null;
        TextBoardRenderer renderer = new TextBoardRenderer(piece -> glyphFor(piece, checkedSide));
        out.println(renderer.render(game.board(), perspective));
        out.println();
        out.println(styleMessage(game.sideToMove() + " to move  |  " + game.status()));
        for (String msg : pendingMessages) {
            String styled = styleMessage(msg);
            if (!styled.isEmpty()) {
                out.println(styled);
            }
        }
        pendingMessages.clear();
        renderedSinceLastAction = true;
    }

    @Override
    public void showMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        // If render has already completed for this step or the game is quitting, print immediately.
        // Otherwise, buffer the message so it appears cleanly after clearScreen() in render().
        if (renderedSinceLastAction || message.contains("Goodbye")) {
            out.println(styleMessage(message));
        } else {
            pendingMessages.add(message);
        }
    }

    @Override
    public void start(GameController controller) {
        controller.runLoop();
    }

    @Override
    public PlayerAction nextAction(Game game) {
        renderedSinceLastAction = false;
        out.print("Your move (e.g. e2e4, 'undo', 'moves', 'quit'): ");
        out.flush();
        return PlayerAction.parse(in.hasNextLine() ? in.nextLine() : "quit");
    }

    @Override
    public void close() {
        for (String msg : pendingMessages) {
            String styled = styleMessage(msg);
            if (!styled.isEmpty()) {
                out.println(styled);
            }
        }
        pendingMessages.clear();
    }
}
