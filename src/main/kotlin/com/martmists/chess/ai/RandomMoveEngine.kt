package com.martmists.chess.ai

import com.martmists.chess.game.Board
import com.martmists.chess.game.Move
import com.martmists.chess.game.MoveGenerator

class RandomMoveEngine : Engine {
    override fun genMove(board: Board): Move {
        return board.getPieces(board.whiteToMove).map { MoveGenerator.findMovesSmart(board, it) }.flatten().random()
    }
}
