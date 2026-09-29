package edu.sfsu.csc413.chess.model;

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
