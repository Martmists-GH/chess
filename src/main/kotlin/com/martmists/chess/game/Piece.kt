package com.martmists.chess.game

class Piece {
    var type = PieceType.INVALID
    var white = false
    var moved = false

    fun empty() {
        type = PieceType.EMPTY
        white = false
        moved = false
    }

    fun from(p: Piece) {
        this.type = p.type
        this.white = p.white
        this.moved = p.moved
    }
}
