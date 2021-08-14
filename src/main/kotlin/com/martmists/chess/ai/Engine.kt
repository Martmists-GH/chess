package com.martmists.chess.ai

import com.martmists.chess.game.Board
import com.martmists.chess.game.Move

interface Engine {
    fun genMove(board: Board) : Move
}