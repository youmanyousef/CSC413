package edu.sfsu.csc413.chess.model;

import edu.sfsu.csc413.chess.factory.PieceFactory;

import java.util.List;
import java.util.ArrayList;

public class Board {
    private final Piece[][] squares = new Piece[Position.BOARD_SIZE][Position.BOARD_SIZE];

    public Board() {

    }

    private void checkOnBoard(Position position) {
        int file = position.file();
        int rank = position.rank();
        if (!Position.isOnBoard(file, rank)) {
            throw new IllegalArgumentException("Not a valid position on the board.");
        }
    }

    public Piece pieceAt(Position position) {
        checkOnBoard(position);
        int file = position.file();
        int rank = position.rank();
        return squares[file][rank];

    }

    public boolean isEmpty(Position position) {
        if (pieceAt(position) == null) {
            return true;
        }
        return false;
    }

    public void place(Position position, Piece piece) {
        //assuming that place() can also replace existing pieces
        checkOnBoard(position);
        int file = position.file();
        int rank = position.rank();
        squares[file][rank] = piece;
    }

    public List<Position> positionsOf(Color color) {
        List<Position> tempList = new ArrayList<Position>();
        for (int f = 0; f < Position.BOARD_SIZE; f++) {
            for (int r = 0; r < Position.BOARD_SIZE; r++) {
                Piece tempPiece = squares[f][r];
                if (tempPiece == null) { continue; } 
                if (tempPiece.color() == color) {
                    tempList.add(new Position(f, r));
                }
            }
        }
        return tempList;
    }

    /**
     *
     * I've chosen to use PieceFactory to save time from writing a new switch statement.
     * This handle's all the promotion cases from pawn to other PieceTypes
     */
    public void apply(Move move) {   // lift the piece off `from`, set it down on `to`
        int from_f = move.from().file();
        int from_r = move.from().rank();
        int to_f = move.to().file();
        int to_r = move.to().rank();
        Piece fromPiece = squares[from_f][from_r];
        if (move.isPromotion()) {
            squares[to_f][to_r] = PieceFactory.create(move.promotesTo(), fromPiece.color());
        } else {
            squares[to_f][to_r] = squares[from_f][from_r];
        }
        squares[from_f][from_r] = null;
    }

    public void undo(Move move) {     // put `moved` back on `from`; put `captured` (or null) back on `to`
        int from_f = move.from().file();
        int from_r = move.from().rank();
        int to_f = move.to().file();
        int to_r = move.to().rank();
        Piece toPiece = squares[to_f][to_r];
        /*if (move.isPromotion()) {
            squares[to_f][to_r] = PieceFactory.create(PieceType.PAWN, toPiece.color());
        } else {
            squares[to_f][to_r] = squares[from_f][from_r];
        }*/
        squares[from_f][from_r] = move.moved();
        squares[to_f][to_r] = move.captured();
    }

    @Override
    public String toString() {
        StringBuilder text = new StringBuilder();
        for (int rank = Position.BOARD_SIZE - 1; rank >= 0; rank--) {  // rank 8 first
            // ... this rank's squares, file a to h: a letter per piece,
            //     a digit for each run of empties
            int currentEmptySquares = 0;
            for (int f = 0; f < Position.BOARD_SIZE; f++) {
                Position currentSquarePosition = new Position(f, rank);
                if (isEmpty(currentSquarePosition)) {
                    currentEmptySquares ++;
                } else {
                    
                    if (currentEmptySquares > 0) {
                        text.append(currentEmptySquares);
                        currentEmptySquares = 0;
                    }
                    text.append(squares[f][rank].symbol());
                }
            }

            if (currentEmptySquares > 0) {
                text.append(currentEmptySquares);
            }

            if (rank > 0) {
                text.append('/');
            }
        }
        return text.toString();
    }    

}
