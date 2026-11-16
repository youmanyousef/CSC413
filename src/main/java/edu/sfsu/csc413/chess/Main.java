package edu.sfsu.csc413.chess;

import edu.sfsu.csc413.chess.engine.Game;
import edu.sfsu.csc413.chess.factory.BoardFactory;
import edu.sfsu.csc413.chess.model.Board;
import edu.sfsu.csc413.chess.view.*;

import java.util.List;
import java.util.Scanner;

/**
 * Entry point.
 *
 * <p>At M0 this does nothing but prove the toolchain works. It grows into the
 * real launcher as the engine appears underneath it.
 */

public final class Main {
    public static void main(String[] args) {

        System.out.println("CSC 413 Chess — environment OK.");
        Scanner scanner = new Scanner(System.in);
        Board board = BoardFactory.standard();
        Game game = new Game();
        /*TextBoardRenderer renderer = new TextBoardRenderer(PieceGlyphs.LETTERS);
        System.out.println(renderer.render(game.board()));
        // old moves - "e2e4", "e7e5"
        for (String notation : List.of("f2f3", "e7e5", "g2g4", "d8h4")) {
            game.play(game.findLegalMove(notation).orElseThrow());
        }
        System.out.println(renderer.render(game.board()));
        System.out.println();
        System.out.println(game.legalMoves());
        System.out.println();
        game.undoLastMove();
        System.out.println(renderer.render(game.board()));
        */
        String choice;
        if (args.length > 0) {
            choice = args[0];
        } else {
            System.out.println("Enter your view - 'swing' for SwingView, 'color' for ColorView, 'emoji' for EmojiView, anything else for 'ConsoleView': ");
            choice = scanner.hasNextLine() ? scanner.nextLine().trim().toLowerCase() : "";
        }

        BoardView view = switch (choice) {
            case "swing" -> new SwingView();
            case "emoji" -> new EmojiView();
            case "color" -> new ColorView();
            default -> new ConsoleView();
        };
        GameController controller = new GameController(game, view);
        controller.start();

    }

    private Main() {
    }
}
