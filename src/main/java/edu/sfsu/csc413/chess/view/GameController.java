package edu.sfsu.csc413.chess.view;

import edu.sfsu.csc413.chess.engine.Game;
import edu.sfsu.csc413.chess.engine.GameStatus;
import edu.sfsu.csc413.chess.engine.MoveGenerator;
import edu.sfsu.csc413.chess.model.Color;
import edu.sfsu.csc413.chess.model.Move;
import edu.sfsu.csc413.chess.model.PieceType;

import java.net.CookieHandler;
import java.util.List;
import java.util.Optional;

/**
 * Connects the player's decisions to the game: carry out what was asked, then
 * ask the view to redraw.
 *
 * <p>This is the C in MVC. It is the only class that knows about both the
 * {@link Game} and the {@link BoardView}. It holds no game state of its own:
 * the model owns the position, the view owns the display, and the controller
 * owns only the conversation between them.
 *
 * <p>When you are done, this class mentions no {@code Scanner}, no
 * {@code System.out}, no window, and none of the words "undo", "moves", or
 * "quit". Those belong to the view and to {@link PlayerAction#parse}.
 */
public class GameController {

    private final Game game;
    private final BoardView view;
    private boolean closed = false;

    public GameController(Game game, BoardView view) {
        this.game = game;
        this.view = view;
    }

    /** The game being played, so a view can read it in order to draw it. */
    public Game game() {
        return game;
    }

    /**
     * Greets the player, draws the opening position, and hands control to the
     * view with {@link BoardView#start}.
     */
    public void start() {
        //throw new UnsupportedOperationException("M6: implement GameController.start");
        view.showMessage("Welcome to Chess!");
        view.render(game);
        view.start(this);
        closed = false;
    }

    /**
     * The ask-and-handle loop, for views that can wait for input.
     *
     * <p>Asks the view for the next action and handles it, until the player
     * quits or the game is over. A console view calls this from its
     * {@code start}; an event-driven view never calls it.
     */
    public void runLoop() {
        //throw new UnsupportedOperationException("M6: implement GameController.runLoop");
        while (!game.isGameOver() && !closed) {
            PlayerAction action = view.nextAction(game);
            handle(action);
            GameStatus status = MoveGenerator.status(game.board(), game.sideToMove());
            view.showMessage(gameStateMessage(status, game.sideToMove()));
            if (status.isGameOver()) {
                view.close();
                closed = true;
                break;
            }
        }
    }
    /**
     * Helper function to get messages for game status
     */
    private String gameStateMessage(GameStatus status, Color color) {
        return switch(status) {
            case IN_PROGRESS -> "";
            case CHECK -> color.opposite().name() + " is in check.";
            case CHECKMATE -> "Checkmate. "+ color.opposite().name() +" wins.";
            case STALEMATE -> "Stalemate. Nobody wins.";
        };
    }
    /**
     * Carries out one player action and redraws.
     *
     * <p>Every route into the game goes through here: {@link #runLoop()} calls
     * it, and so will a button's click handler in M7. The handout lists what
     * each of the four actions does and what the player is told.
     */
    public void handle(PlayerAction action) {
        //throw new UnsupportedOperationException("M6: implement GameController.handle");

        switch (action) {

            case PlayerAction.Move(String notation) -> {
                /*if (notation.length() == 4) {
                    notation += view.promotionChoice(game).symbol();
                }
                final String temp = notation;
                Optional<Move> legalMove = game.legalMoves()
                        .stream()
                        .filter(move -> move.toString().equals(temp))
                        .findFirst();

                if (legalMove.isPresent()) {
                    game.play(legalMove.get());
                } else {
                    view.showMessage("Not a legal move: '" + notation + "'.");
                }*/
                /*
                boolean movePlayed = false;

                for (Move move : game.legalMoves()) {
                    String promotionAttempt = "";

                    if (move.isPromotion() && notation.length() == 4) {
                        promotionAttempt = String.valueOf(
                                view.promotionChoice(game).symbol()
                        );
                    }

                    if (move.toString().equals(notation)
                            || move.toString().equals(notation + promotionAttempt)) {

                        game.play(move);
                        movePlayed = true;
                        break;
                    }
                }

                if (!movePlayed) {
                    view.showMessage("Not a legal move: '" + notation + "'.");
                }*/

                    List<Move> legalMoves = game.legalMoves();

                    // First, handle ordinary moves and explicit promotion notation.
                    Optional<Move> matchingMove = legalMoves.stream()
                            .filter(move -> move.toString().equals(notation))
                            .findFirst();

                    if (matchingMove.isPresent()) {
                        game.play(matchingMove.get());
                        break;
                    }

                    // If the player entered a four-character promotion move,
                    // ask the view which piece to promote to.
                    if (notation.length() == 4) {
                        PieceType promotion = view.promotionChoice(game);
                        String promotedNotation;
                        if (game.sideToMove() == Color.WHITE) {
                            promotedNotation = notation + Character.toLowerCase(promotion.symbol());
                        } else {
                            promotedNotation = notation + promotion.symbol();
                        }

                        matchingMove = legalMoves.stream()
                                .filter(move -> move.toString().equals(promotedNotation))
                                .findFirst();

                        if (matchingMove.isPresent()) {
                            game.play(matchingMove.get());
                            break;
                        }
                    }

                    view.showMessage("Not a legal move: '" + notation + "'.");

            }
            case PlayerAction.Undo() -> {
                Optional<Move> lastMove = game.undoLastMove();

                if (lastMove.isEmpty()) {
                    view.showMessage("Nothing to undo.");
                }
            }

            case PlayerAction.ShowMoves() -> {
                List<Move> moves = game.legalMoves();

                StringBuilder message = new StringBuilder();
                message.append(moves.size()).append(" legal moves: ");

                for (Move move : moves) {
                    message.append(move).append(" ");
                }

                view.showMessage(message.toString());
            }

            case PlayerAction.Quit() -> {
                view.showMessage("Goodbye.");
                view.close();
                closed = true;
                return;
            }
        }
        view.render(game);


    }
}
