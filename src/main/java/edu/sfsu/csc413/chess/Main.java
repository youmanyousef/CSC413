package edu.sfsu.csc413.chess;

import edu.sfsu.csc413.chess.model.Board;
import edu.sfsu.csc413.chess.model.Color;
import edu.sfsu.csc413.chess.model.Piece;
import edu.sfsu.csc413.chess.model.PieceType;
import edu.sfsu.csc413.chess.model.Position;
import edu.sfsu.csc413.chess.view.PieceGlyphs;
import edu.sfsu.csc413.chess.view.TextBoardRenderer;

/**
 * Entry point.
 *
 * <p>At M0 this does nothing but prove the toolchain works. It grows into the
 * real launcher as the engine appears underneath it.
 */
public final class Main {

    public static void main(String[] args) {
        System.out.println("CSC 413 Chess — environment OK.");
        // System.out.println("Printed from main.");

        Board board = new Board();
        PieceType backRankType[] = {
            PieceType.ROOK,
            PieceType.KNIGHT,
            PieceType.BISHOP,
            PieceType.QUEEN,
            PieceType.KING,
            PieceType.BISHOP,
            PieceType.KNIGHT,
            PieceType.ROOK
        }; 
        Color rankColor = Color.WHITE;
        for (int rank = 0; rank < Position.BOARD_SIZE; rank += Position.BOARD_SIZE - 1) {
            //for (PieceType rankType : backRank) {
            for (int file = 0; file < Position.BOARD_SIZE; file++) {
                board.place(
                    new Position(file, rank), 
                    new Piece(rankColor, backRankType[file])
                );
            }
            rankColor = Color.BLACK;
        }
        int pawnRanks[] = {Position.BOARD_SIZE - 2, 1};
        for (Integer rank : pawnRanks) {
            for (int file = 0; file < Position.BOARD_SIZE; file++) {
                board.place(
                    new Position(file, rank),
                    new Piece(rankColor, PieceType.PAWN)
                );
            }
            rankColor = Color.WHITE;
        }

        System.out.println(new TextBoardRenderer(PieceGlyphs.LETTERS).render(board));
        System.out.println("Printed from main.");
    }

    private Main() {
        
    }
}
